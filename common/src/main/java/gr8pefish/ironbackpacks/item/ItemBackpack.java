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
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import gr8pefish.ironbackpacks.platform.Services;

public class ItemBackpack extends Item implements IBackpack {

    public ItemBackpack(Item.Properties properties) {
        super(properties.stacksTo(1));
    }

    /** 1.12 getUnlocalizedName(stack): "item.ironbackpacks.backpack.ironbackpacks.iron" and so on. */
    @Nonnull
    public String getDescriptionId(@Nonnull ItemStack stack) {
        // asked every frame for every dropped backpack (its name tag): the type only, the key built once per type
        BackpackType type = BackpackInfo.headerOf(stack).getVariant().getBackpackType();
        return DESCRIPTION_IDS.computeIfAbsent(type.getIdentifier(),
                id -> getDescriptionId() + "." + id.toString().replace(":", "."));
    }

    /** 26.x names items by component; the backpack's name still depends on its type. */
    @Nonnull
    @Override
    public Component getName(@Nonnull ItemStack stack) {
        return Component.translatable(getDescriptionId(stack));
    }

    private static final java.util.Map<net.minecraft.resources.Identifier, String> DESCRIPTION_IDS = new java.util.concurrent.ConcurrentHashMap<>();

    @Nonnull
    @Override
    public InteractionResult use(@Nonnull Level world, @Nonnull Player player, @Nonnull InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);

        BackpackInfo info = getBackpackInfo(held);
        if (info.getOwner() == null) {
            info.setOwner(player.getUUID());
            updateBackpack(held, info);
        }

        if (info.hasUpgrade(RegistrarIronBackpacks.get(RegistrarIronBackpacks.UPGRADE_LOCK)) && !player.getUUID().equals(info.getOwner()))
            return InteractionResult.FAIL;

        // 1.12 World.playSound(x, y, z, ..., false) only plays on the client
        if (world.isClientSide())
            world.playLocalSound(player.getX(), player.getY(), player.getZ(), RegistrarIronBackpacks.BACKPACK_OPEN.get(), SoundSource.NEUTRAL, 1.0F, 1.0F, false);
        if (player instanceof ServerPlayer serverPlayer)
            ContainerBackpack.open(serverPlayer, ContainerBackpack.Mode.HELD, hand);

        return InteractionResult.SUCCESS;
    }

    /** 1.12 getSubItems: every variant, by tier. */
    public static void fillCreativeTab(java.util.function.Consumer<ItemStack> subItems) {
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
        // asked every frame for every backpack in a GUI: the upgrades only, not the inventory
        return BackpackInfo.headerOf(stack).hasUpgrade(RegistrarIronBackpacks.get(RegistrarIronBackpacks.UPGRADE_DAMAGE_BAR));
    }

    /** 1.12 getDurabilityForDisplay: the free share of the backpack's total capacity (empty slots count as 64). */
    public double getDurabilityForDisplay(@Nonnull ItemStack stack) {
        int total = 0;
        int full = 0;

        // read straight from the stored contents (asked twice per frame for a shown bar): no inventory copy
        int slots = BackpackInfo.headerOf(stack).getVariant().getBackpackSize().getTotalSize();
        int filled = 0;
        for (net.minecraft.world.item.ItemStackTemplate invStack : stack.getOrDefault(RegistrarIronBackpacks.PACK_INVENTORY.get(),
                net.minecraft.world.item.component.ItemContainerContents.EMPTY).nonEmptyItems()) {
            if (filled++ >= slots) break;
            full += invStack.count();
            total += invStack.getMaxStackSize();
        }
        total += 64 * (slots - Math.min(filled, slots));

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
    public void appendHoverText(@Nonnull ItemStack stack, @Nonnull TooltipContext context, @Nonnull net.minecraft.world.item.component.TooltipDisplay display,
                                @Nonnull java.util.function.Consumer<Component> tooltip, @Nonnull TooltipFlag advanced) {
        BackpackInfo backpackInfo = getBackpackInfo(stack);
        if (backpackInfo.getVariant().getBackpackType().hasSpecialties())
            tooltip.accept(gray(Component.translatable("tooltip.ironbackpacks.backpack.emphasis." + backpackInfo.getVariant().getBackpackSpecialty().getName())));
        tooltip.accept(gray(Component.translatable("tooltip.ironbackpacks.backpack.tier", backpackInfo.getVariant().getBackpackType().getTier() + 1)));
        tooltip.accept(gray(Component.translatable("tooltip.ironbackpacks.backpack.upgrade.used", backpackInfo.getPointsUsed(), backpackInfo.getMaxPoints())));
        if (!isLeftShiftDown() && !backpackInfo.getUpgrades().isEmpty()) {
            tooltip.accept(gray(Component.translatable("tooltip.ironbackpacks.shift").withStyle(ChatFormatting.ITALIC)));
        } else if (!backpackInfo.getUpgrades().isEmpty()) {
            tooltip.accept(Component.empty());
            tooltip.accept(gray(Component.translatable("tooltip.ironbackpacks.backpack.upgrade.list", backpackInfo.getPointsUsed(), backpackInfo.getMaxPoints())));
            for (BackpackUpgrade upgrade : backpackInfo.getUpgrades())
                tooltip.accept(gray(Component.literal("  - ").append(Component.translatable("upgrade.ironbackpacks." + upgrade.getIdentifier().getPath()))));
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
