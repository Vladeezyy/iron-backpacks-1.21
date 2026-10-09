package gr8pefish.ironbackpacks.item;

import java.util.Comparator;
import java.util.List;

import javax.annotation.Nonnull;

import com.google.common.collect.Lists;

import gr8pefish.ironbackpacks.api.IronBackpacksAPI;
import gr8pefish.ironbackpacks.api.backpack.BackpackInfo;
import gr8pefish.ironbackpacks.api.backpack.IBackpack;
import gr8pefish.ironbackpacks.api.backpack.variant.BackpackSpecialty;
import gr8pefish.ironbackpacks.api.backpack.variant.BackpackType;
import gr8pefish.ironbackpacks.api.upgrade.BackpackUpgrade;
import gr8pefish.ironbackpacks.container.ContainerBackpack;
import gr8pefish.ironbackpacks.core.RegistrarIronBackpacks;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import gr8pefish.ironbackpacks.platform.Services;

public class ItemBackpack extends Item implements IBackpack {

    public ItemBackpack() {
        super(new Item.Properties().stacksTo(1));
    }

    /** 1.12 getUnlocalizedName(stack): "item.ironbackpacks.backpack.ironbackpacks.iron" and so on. */
    @Nonnull
    @Override
    public String getDescriptionId(@Nonnull ItemStack stack) {
        BackpackInfo backpackInfo = getBackpackInfo(stack);
        return super.getDescriptionId(stack) + "." + backpackInfo.getVariant().getBackpackType().getIdentifier().toString().replace(":", ".");
    }

    @Nonnull
    @Override
    public InteractionResultHolder<ItemStack> use(@Nonnull Level world, @Nonnull Player player, @Nonnull InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);

        BackpackInfo info = getBackpackInfo(held);
        if (info.getOwner() == null) {
            info.setOwner(player.getGameProfile().getId());
            updateBackpack(held, info);
        }

        if (info.hasUpgrade(RegistrarIronBackpacks.get(RegistrarIronBackpacks.UPGRADE_LOCK)) && !player.getGameProfile().getId().equals(info.getOwner()))
            return InteractionResultHolder.fail(held);

        // 1.12 World.playSound(x, y, z, ..., false) only plays on the client
        if (world.isClientSide)
            world.playLocalSound(player.getX(), player.getY(), player.getZ(), RegistrarIronBackpacks.BACKPACK_OPEN.get(), SoundSource.NEUTRAL, 1.0F, 1.0F, false);
        if (player instanceof ServerPlayer serverPlayer)
            ContainerBackpack.open(serverPlayer, ContainerBackpack.Mode.HELD, hand);

        return InteractionResultHolder.success(held);
    }

    /** 1.12 getSubItems: every variant, by tier. */
    public static void fillCreativeTab(CreativeModeTab.Output subItems) {
        List<BackpackType> sortedTypes = Lists.newArrayList(IronBackpacksAPI.getBackpackTypes());
        sortedTypes.sort(Comparator.comparingInt(BackpackType::getTier));

        for (BackpackType backpackType : sortedTypes) {
            if (backpackType.getIdentifier().equals(IronBackpacksAPI.NULL))
                continue;

            if (!backpackType.hasSpecialties()) {
                subItems.accept(IronBackpacksAPI.getStack(backpackType, BackpackSpecialty.NONE));
            } else {
                for (BackpackSpecialty specialty : BackpackSpecialty.values()) {
                    if (specialty == BackpackSpecialty.NONE)
                        continue;

                    subItems.accept(IronBackpacksAPI.getStack(backpackType, specialty));
                }
            }
        }
    }

    @Override
    public boolean isBarVisible(@Nonnull ItemStack stack) {
        return getBackpackInfo(stack).hasUpgrade(RegistrarIronBackpacks.get(RegistrarIronBackpacks.UPGRADE_DAMAGE_BAR));
    }

    /** 1.12 getDurabilityForDisplay: the free share of the backpack's total capacity (empty slots count as 64). */
    public double getDurabilityForDisplay(@Nonnull ItemStack stack) {
        int total = 0;
        int full = 0;

        BackpackInfo backpackInfo = getBackpackInfo(stack);
        for (int i = 0; i < backpackInfo.getInventory().getSlots(); i++) {
            ItemStack invStack = backpackInfo.getInventory().getStackInSlot(i);
            if (!invStack.isEmpty()) {
                full += invStack.getCount();
                total += invStack.getMaxStackSize();
            } else {
                total += 64;
            }
        }

        return 1.0D - ((double) full / (double) total);
    }

    /** The 1.12 bar: 13 * (1 - durability) pixels. */
    @Override
    public int getBarWidth(@Nonnull ItemStack stack) {
        return (int) Math.round(13.0D - getDurabilityForDisplay(stack) * 13.0D);
    }

    /** The 1.12 default bar colour: hue (1 - durability) / 3, red when empty to green when full. */
    @Override
    public int getBarColor(@Nonnull ItemStack stack) {
        return Mth.hsvToRgb(Math.max(0.0F, (float) (1.0F - getDurabilityForDisplay(stack))) / 3.0F, 1.0F, 1.0F);
    }

    /** NeoForge IItemExtension (overridden by name: common code doesn't see it): no re-equip animation when the data changes. */
    public boolean shouldCauseReequipAnimation(@Nonnull ItemStack oldStack, @Nonnull ItemStack newStack, boolean slotChanged) {
        return slotChanged;
    }

    /** Fabric FabricItem, the same for Fabric. */
    public boolean allowComponentsUpdateAnimation(Player player, InteractionHand hand, ItemStack oldStack, ItemStack newStack) {
        return false;
    }

    /** 1.12 addInformation; extra tooltip lines were gray (GuiScreen.renderToolTip). */
    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nonnull TooltipContext context, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag advanced) {
        BackpackInfo backpackInfo = getBackpackInfo(stack);
        if (backpackInfo.getVariant().getBackpackType().hasSpecialties())
            tooltip.add(gray(Component.translatable("tooltip.ironbackpacks.backpack.emphasis." + backpackInfo.getVariant().getBackpackSpecialty().getName())));
        tooltip.add(gray(Component.translatable("tooltip.ironbackpacks.backpack.tier", backpackInfo.getVariant().getBackpackType().getTier() + 1)));
        tooltip.add(gray(Component.translatable("tooltip.ironbackpacks.backpack.upgrade.used", backpackInfo.getPointsUsed(), backpackInfo.getMaxPoints())));
        if (!isLeftShiftDown() && !backpackInfo.getUpgrades().isEmpty()) {
            tooltip.add(gray(Component.translatable("tooltip.ironbackpacks.shift").withStyle(ChatFormatting.ITALIC)));
        } else if (!backpackInfo.getUpgrades().isEmpty()) {
            tooltip.add(Component.empty());
            tooltip.add(gray(Component.translatable("tooltip.ironbackpacks.backpack.upgrade.list", backpackInfo.getPointsUsed(), backpackInfo.getMaxPoints())));
            for (BackpackUpgrade upgrade : backpackInfo.getUpgrades())
                tooltip.add(gray(Component.literal("  - ").append(Component.translatable("upgrade.ironbackpacks." + upgrade.getIdentifier().getPath()))));
        }
    }

    static Component gray(Component line) {
        return line.copy().withStyle(ChatFormatting.GRAY);
    }

    /** 1.12 Keyboard.isKeyDown(KEY_LSHIFT); false outside the client. */
    static boolean isLeftShiftDown() {
        return Services.PLATFORM.isClient() && gr8pefish.ironbackpacks.client.ClientEventHandler.isLeftShiftDown();
    }
}
