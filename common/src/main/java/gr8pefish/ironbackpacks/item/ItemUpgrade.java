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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import gr8pefish.ironbackpacks.platform.Services;

public class ItemUpgrade extends Item implements IUpgrade {

    public ItemUpgrade(Item.Properties properties) {
        super(properties);
    }

    @Nonnull
    public String getDescriptionId(@Nonnull ItemStack stack) {
        BackpackUpgrade backpackUpgrade = getUpgrade(stack);
        if (backpackUpgrade.isNull())
            return getDescriptionId();

        return "upgrade.ironbackpacks." + backpackUpgrade.getIdentifier().getPath();
    }

    /** 26.x names items by component; an upgrade's name depends on the upgrade it holds. */
    @Nonnull
    @Override
    public Component getName(@Nonnull ItemStack stack) {
        return Component.translatable(getDescriptionId(stack));
    }

    /** 1.12 getSubItems: the blank upgrade, then every upgrade by minimum tier. */
    public static void fillCreativeTab(java.util.function.Consumer<ItemStack> subItems) {
        subItems.accept(new ItemStack(RegistrarIronBackpacks.UPGRADE.get()));

        List<BackpackUpgrade> sortedUpgrades = Lists.newArrayList(IronBackpacksAPI.getUpgrades());
        sortedUpgrades.sort(Comparator.comparingInt(BackpackUpgrade::getMinimumTier));

        for (BackpackUpgrade upgrade : sortedUpgrades)
            if (!upgrade.isNull())
                subItems.accept(IronBackpacksAPI.getStack(upgrade));
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nonnull TooltipContext context, @Nonnull net.minecraft.world.item.component.TooltipDisplay display,
                                @Nonnull java.util.function.Consumer<Component> tooltip, @Nonnull TooltipFlag advanced) {
        BackpackUpgrade backpackUpgrade = getUpgrade(stack);
        if (backpackUpgrade.isNull())
            return;

        if (!ItemBackpack.isLeftShiftDown()) {
            tooltip.accept(ItemBackpack.gray(Component.translatable("tooltip.ironbackpacks.shift").withStyle(ChatFormatting.ITALIC)));
            return;
        }

        tooltip.accept(ItemBackpack.gray(Component.translatable("tooltip.ironbackpacks.upgrade.cost", backpackUpgrade.getApplicationCost())));
        tooltip.accept(ItemBackpack.gray(Component.translatable("tooltip.ironbackpacks.upgrade.minimum_tier", backpackUpgrade.getMinimumTier() + 1)));
        tooltip.accept(Component.empty());
        Component desc = Component.translatable("upgrade.ironbackpacks." + backpackUpgrade.getIdentifier().getPath() + ".desc");
        if (Services.PLATFORM.isClient())
            gr8pefish.ironbackpacks.client.ClientEventHandler.wrap(desc, 200).forEach(line -> tooltip.accept(ItemBackpack.gray(line)));
        else
            tooltip.accept(ItemBackpack.gray(desc));

        super.appendHoverText(stack, context, display, tooltip, advanced);
    }
}
