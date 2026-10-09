package gr8pefish.ironbackpacks.item;

import java.util.Comparator;
import java.util.List;

import javax.annotation.Nonnull;

import com.google.common.collect.Lists;

import gr8pefish.ironbackpacks.api.IronBackpacksAPI;
import gr8pefish.ironbackpacks.api.upgrade.BackpackUpgrade;
import gr8pefish.ironbackpacks.api.upgrade.IUpgrade;
import gr8pefish.ironbackpacks.core.RegistrarIronBackpacks;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import gr8pefish.ironbackpacks.platform.Services;

public class ItemUpgrade extends Item implements IUpgrade {

    public ItemUpgrade() {
        super(new Item.Properties());
    }

    @Nonnull
    @Override
    public String getDescriptionId(@Nonnull ItemStack stack) {
        BackpackUpgrade backpackUpgrade = getUpgrade(stack);
        if (backpackUpgrade.isNull())
            return super.getDescriptionId(stack);

        return "upgrade.ironbackpacks." + backpackUpgrade.getIdentifier().getPath();
    }

    /** 1.12 getSubItems: the blank upgrade, then every upgrade by minimum tier. */
    public static void fillCreativeTab(CreativeModeTab.Output subItems) {
        subItems.accept(new ItemStack(RegistrarIronBackpacks.UPGRADE.get()));

        List<BackpackUpgrade> sortedUpgrades = Lists.newArrayList(IronBackpacksAPI.getUpgrades());
        sortedUpgrades.sort(Comparator.comparingInt(BackpackUpgrade::getMinimumTier));

        for (BackpackUpgrade upgrade : sortedUpgrades)
            if (!upgrade.isNull())
                subItems.accept(IronBackpacksAPI.getStack(upgrade));
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nonnull TooltipContext context, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag advanced) {
        BackpackUpgrade backpackUpgrade = getUpgrade(stack);
        if (backpackUpgrade.isNull())
            return;

        if (!ItemBackpack.isLeftShiftDown()) {
            tooltip.add(ItemBackpack.gray(Component.translatable("tooltip.ironbackpacks.shift").withStyle(ChatFormatting.ITALIC)));
            return;
        }

        tooltip.add(ItemBackpack.gray(Component.translatable("tooltip.ironbackpacks.upgrade.cost", backpackUpgrade.getApplicationCost())));
        tooltip.add(ItemBackpack.gray(Component.translatable("tooltip.ironbackpacks.upgrade.minimum_tier", backpackUpgrade.getMinimumTier() + 1)));
        tooltip.add(Component.empty());
        Component desc = Component.translatable("upgrade.ironbackpacks." + backpackUpgrade.getIdentifier().getPath() + ".desc");
        if (Services.PLATFORM.isClient())
            gr8pefish.ironbackpacks.client.ClientEventHandler.wrap(desc, 200).forEach(line -> tooltip.add(ItemBackpack.gray(line)));
        else
            tooltip.add(ItemBackpack.gray(desc));

        super.appendHoverText(stack, context, tooltip, advanced);
    }
}
