package gr8pefish.ironbackpacks.container;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.Preconditions;

import gr8pefish.ironbackpacks.IronBackpacks;
import gr8pefish.ironbackpacks.api.backpack.BackpackInfo;
import gr8pefish.ironbackpacks.api.backpack.IBackpack;
import gr8pefish.ironbackpacks.api.backpack.variant.BackpackSize;
import gr8pefish.ironbackpacks.core.RegistrarIronBackpacks;
import gr8pefish.ironbackpacks.network.EquippedBackpackClient;
import gr8pefish.ironbackpacks.util.InventoryBlacklist;
import gr8pefish.ironbackpacks.util.Utils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * The backpack's container class, holding all the slots of the backpack.
 */
public class ContainerBackpack extends AbstractContainerMenu {

    /** The 1.12 GuiHandler ids: a right clicked backpack, the open key's backpack from the inventory, the equipped one. */
    public enum Mode { HELD, INVENTORY, EQUIPPED }

    // Fields

    @Nonnull
    private final BackpackInfo backpackInfo;
    @Nonnull
    private final BackpackSize backpackSize;
    @Nonnull
    private final ItemStack backpackStack;
    /**
     * Offhand used or not.
     */
    private int blocked = -1;
    private ItemStack blockedStack = ItemStack.EMPTY;

    // Constructor

    public ContainerBackpack(int windowId, @Nonnull ItemStack backpackStack, @Nonnull Inventory inventoryPlayer, @Nullable InteractionHand hand) {
        super(RegistrarIronBackpacks.BACKPACK_MENU.get(), windowId);
        Preconditions.checkNotNull(backpackStack, "backpackStack cannot be null");
        Preconditions.checkNotNull(inventoryPlayer, "inventoryPlayer cannot be null");

        BackpackInfo backpackInfo = BackpackInfo.fromStack(backpackStack);
        IItemHandler itemHandler = backpackInfo.getInventory();

        Preconditions.checkNotNull(backpackInfo, "backpackInfo cannot be null");
        Preconditions.checkNotNull(itemHandler, "itemHandler cannot be null");

        this.backpackInfo = backpackInfo;
        this.backpackStack = backpackStack;
        this.backpackSize = backpackInfo.getVariant().getBackpackSize();

        setupSlots(inventoryPlayer, itemHandler, hand);
    }

    // Opening (1.12 GuiHandler: the server and the client each look up the backpack for the gui id and hand)

    /** The backpack a gui id / hand opens for the player, empty if none. */
    @Nonnull
    public static ItemStack getBackpackFor(@Nonnull Player player, @Nonnull Mode mode, @Nonnull InteractionHand hand) {
        return switch (mode) {
            case HELD, INVENTORY -> Utils.getNonequippedBackpackFromInventory(player, hand);
            case EQUIPPED -> player.level().isClientSide ? EquippedBackpackClient.equipped : player.getData(RegistrarIronBackpacks.EQUIPPED_BACKPACK);
        };
    }

    @Nullable
    private static ContainerBackpack create(int windowId, @Nonnull Player player, @Nonnull Mode mode, @Nonnull InteractionHand hand) {
        ItemStack selected = getBackpackFor(player, mode, hand);
        if (selected.isEmpty())
            return null;

        return switch (mode) {
            case HELD, EQUIPPED -> new ContainerBackpack(windowId, selected, player.getInventory(), hand);
            case INVENTORY -> new ContainerBackpack(windowId, selected, player.getInventory(), null).setBlockedStack(selected);
        };
    }

    public static void open(@Nonnull ServerPlayer player, @Nonnull Mode mode, @Nonnull InteractionHand hand) {
        ItemStack selected = getBackpackFor(player, mode, hand);
        if (selected.isEmpty())
            return;

        opener.open(player, new SimpleMenuProvider((windowId, inventory, p) -> create(windowId, p, mode, hand), selected.getHoverName()), buf -> {
            buf.writeEnum(mode);
            buf.writeEnum(hand);
        });
    }

    /** How a backpack menu is opened: {@link ServerPlayer#openMenu} (the GameTests' mock players open it locally). */
    public interface Opener {
        void open(ServerPlayer player, net.minecraft.world.MenuProvider provider, java.util.function.Consumer<RegistryFriendlyByteBuf> extraData);
    }

    public static Opener opener = (player, provider, extraData) -> player.openMenu(provider, extraData);

    @Nonnull
    public static ContainerBackpack fromNetwork(int windowId, @Nonnull Inventory inventory, @Nonnull RegistryFriendlyByteBuf buf) {
        Mode mode = buf.readEnum(Mode.class);
        InteractionHand hand = buf.readEnum(InteractionHand.class);
        ContainerBackpack container = create(windowId, inventory.player, mode, hand);
        // no backpack on this side (1.12 returned no gui): an empty one keeps the window consistent until it closes
        return container != null ? container : new ContainerBackpack(windowId, new ItemStack(RegistrarIronBackpacks.BACKPACK.get()), inventory, null);
    }

    // Override

    @Override
    public boolean stillValid(@Nonnull Player player) {
        return true;
    }

    @Nonnull
    @Override
    public ItemStack quickMoveStack(@Nonnull Player player, int slotIndex) {
        Slot slot = this.getSlot(slotIndex);

        if (!slot.mayPickup(player))
            return slot.getItem();

        if (slotIndex == blocked)
            return ItemStack.EMPTY;

        if (!slot.hasItem())
            return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        if (!stack.isEmpty() && ItemStack.matches(stack, blockedStack))
            return ItemStack.EMPTY;

        ItemStack newStack = stack.copy();

        if (InventoryBlacklist.INSTANCE.isBlacklisted(slot.getItem()))
            return ItemStack.EMPTY;
        else if (slotIndex < backpackSize.getTotalSize()) {
            if (!this.moveItemStackTo(stack, backpackSize.getTotalSize(), this.slots.size(), true))
                return ItemStack.EMPTY;
            slot.setChanged();
        } else if (!this.moveItemStackTo(stack, 0, backpackSize.getTotalSize(), false))
            return ItemStack.EMPTY;

        if (stack.isEmpty())
            slot.set(ItemStack.EMPTY);
        else
            slot.setChanged();

        slot.onTake(player, newStack);
        return newStack;
    }

    @Override
    public void clicked(int slotId, int button, @Nonnull ClickType flag, @Nonnull Player player) {
        if (slotId < 0 || slotId > slots.size()) {
            super.clicked(slotId, button, flag, player);
            return;
        }

        Slot slot = slots.get(slotId);
        if (!canTake(slotId, slot, button, player, flag))
            return;

        super.clicked(slotId, button, flag, player);
    }

    @Override
    public void removed(@Nonnull Player playerIn) {
        super.removed(playerIn);
        if (!(backpackStack.getItem() instanceof IBackpack)) {
            IronBackpacks.LOGGER.debug("Attempted to close backpack on non-IBackpack item {}. Changes will not persist.");
            return;
        }
        ((IBackpack) backpackStack.getItem()).updateBackpack(backpackStack, backpackInfo);
    }

    // Helper

    public boolean canTake(int slotId, Slot slot, int button, Player player, ClickType clickType) {
        // Block interaction with open backpack slot
        if (slotId == blocked)
            return false;

        ItemStack slotStack = slot.getItem();
        if (!slotStack.isEmpty() && ItemStack.matches(slotStack, blockedStack))
            return false;

        // Block placing of backpacks and blacklisted stacks into backpack inventory
        if (slotId <= backpackSize.getTotalSize() - 1) {
            if (InventoryBlacklist.INSTANCE.isBlacklisted(getCarried()))
                return false;

            if (getCarried().getItem() instanceof IBackpack) // TODO - Check for nesting upgrades and properly handle
                return false;
        }

        // Moving items by shift clicking them
        if(clickType == ClickType.QUICK_MOVE){
            if (slotStack.getItem() instanceof IBackpack) // TODO - Check for nesting upgrades and properly handle
                return false;
        }

        // Hotbar swapping via number keys (1.21 also swaps with the offhand: button 40, a slot outside this container)
        if (clickType == ClickType.SWAP) {
            ItemStack hotbarStack;
            if (button == Inventory.SLOT_OFFHAND) {
                hotbarStack = player.getOffhandItem();
            } else {
                int hotbarId = backpackSize.getTotalSize() + 27 + button; // Backpack slots + main inventory + hotbar id
                // Block swapping with open backpack slot
                if (blocked == hotbarId)
                    return false;

                Slot hotbarSlot = getSlot(hotbarId);
                hotbarStack = hotbarSlot.getItem();
            }
            if (!hotbarStack.isEmpty() && ItemStack.matches(hotbarStack, blockedStack))
                return false;

            // Block swapping of backpacks and blacklisted stacks into backpack inventory
            if (slotId <= backpackSize.getTotalSize() - 1) {
                if (InventoryBlacklist.INSTANCE.isBlacklisted(slotStack) || InventoryBlacklist.INSTANCE.isBlacklisted(hotbarStack))
                    return false;

                if (slotStack.getItem() instanceof IBackpack || hotbarStack.getItem() instanceof IBackpack) // TODO - Check for nesting upgrades and properly handle
                    return false;
            }
        }

        return true;
    }

    /**
     * The display name of the backpack.
     *
     * @return - The name as a Component
     */
    @Nonnull
    public Component getName() {
        return backpackStack.getHoverName();
    }

    /**
     * Gets the {@link BackpackSize} of the backpack.
     *
     * @return - The BackpackSize
     */
    public BackpackSize getBackpackSize() {
        return backpackSize;
    }

    @Nonnull
    public ItemStack getBackpackStack() {
        return backpackStack;
    }

    // Setup slots
    // All credit for code below here goes to copygirl (comments/javadocs/precondition checks mine though)

    /**
     * Sets up the backpack's slots, delegating where necessary.
     *
     * @param inventoryPlayer - The player's inventory
     * @param itemHandler     - The IItemHandler of the backpack
     */
    private void setupSlots(@Nonnull Inventory inventoryPlayer, @Nonnull IItemHandler itemHandler, @Nullable InteractionHand hand) {
        Preconditions.checkNotNull(inventoryPlayer, "inventoryPlayer cannot be null");
        Preconditions.checkNotNull(itemHandler, "itemHandler cannot be null");

        setupBackpackSlots(itemHandler);
        setupPlayerSlots(inventoryPlayer, hand);
    }

    /**
     * Sets up the slots for the backpack specifically.
     *
     * @param itemHandler - The {@link IItemHandler} for the backpack.
     */
    private void setupBackpackSlots(@Nonnull IItemHandler itemHandler) {
        Preconditions.checkNotNull(itemHandler, "itemHandler cannot be null");

        int xOffset = 1 + getContainerInvXOffset();
        int yOffset = 1 + getBorderTop();
        for (int y = 0; y < backpackSize.getRows(); y++, yOffset += 18)
            for (int x = 0; x < backpackSize.getColumns(); x++)
                addSlot(new SlotItemHandler(itemHandler, x + y * backpackSize.getColumns(), xOffset + x * 18, yOffset));
    }

    /**
     * Sets up the slots for the player specifically.
     *
     * @param inventoryPlayer - the {@link Inventory} for the player.
     */
    private void setupPlayerSlots(@Nonnull Inventory inventoryPlayer, @Nullable InteractionHand hand) {
        Preconditions.checkNotNull(inventoryPlayer, "inventoryPlayer cannot be null");

        int xOffset = 1 + getPlayerInvXOffset();
        int yOffset = 1 + getBorderTop() + getContainerInvHeight() + getBufferInventory();

        //Inventory
        for (int y = 0; y < 3; y++, yOffset += 18)
            for (int x = 0; x < 9; x++)
                addSlot(new Slot(inventoryPlayer, x + y * 9 + 9, xOffset + x * 18, yOffset));

        //Hotbar
        yOffset += getBufferHotbar();
        for (int x = 0; x < 9; x++) {
            Slot slot = addSlot(new Slot(inventoryPlayer, x, xOffset + x * 18, yOffset) {
                @Override
                public boolean mayPickup(@Nonnull final Player playerIn) {
                    ItemStack slotStack = getItem();
                    return index != blocked && (!slotStack.isEmpty() && !ItemStack.matches(slotStack, blockedStack));
                }
            });
            if (x == inventoryPlayer.selected && hand == InteractionHand.MAIN_HAND)
                blocked = slot.index;
        }
    }

    public ContainerBackpack setBlockedStack(ItemStack blockedStack) {
        this.blockedStack = blockedStack;
        return this;
    }

    // GUI/slot setup helpers

    /**
     * Returns the size of the top border in pixels.
     */
    public int getBorderTop() {
        return 17;
    }

    /**
     * Returns the size of the side border in pixels.
     */
    public int getBorderSide() {
        return 7;
    }

    /**
     * Returns the size of the bottom border in pixels.
     */
    public int getBorderBottom() {
        return 7;
    }

    /**
     * Returns the space between container and player inventory in pixels.
     */
    public int getBufferInventory() {
        return 13;
    }

    /**
     * Returns the space between player inventory and hotbar in pixels.
     */
    public int getBufferHotbar() {
        return 4;
    }

    /**
     * Returns the size of the maximum number of columns possible.
     */
    public int getMaxColumns() {
        return BackpackSize.MAX.getColumns();
    }

    /**
     * Returns the size of the maximum number of rows possible.
     */
    public int getMaxRows() {
        return BackpackSize.MAX.getRows();
    }

    /**
     * Returns the total width of the container in pixels.
     */
    public int getWidth() {
        return Math.max(backpackSize.getColumns(), 9) * 18 + getBorderSide() * 2;
    }

    /**
     * Returns the total height of the container in pixels.
     */
    public int getHeight() {
        return getBorderTop() + (backpackSize.getRows() * 18) +
                getBufferInventory() + (4 * 18) +
                getBufferHotbar() + getBorderBottom();
    }

    /**
     * Returns the size of the container's width, only the inventory/slots, not the border, in pixels.
     */
    public int getContainerInvWidth() {
        return backpackSize.getColumns() * 18;
    }

    /**
     * Returns the size of the container's height, only the inventory/slots, not the border, in pixels.
     */
    public int getContainerInvHeight() {
        return backpackSize.getRows() * 18;
    }

    /**
     * Returns the size of the x offset for the backpack container in pixels.
     */
    public int getContainerInvXOffset() {
        return getBorderSide() + Math.max(0, (getPlayerInvWidth() - getContainerInvWidth()) / 2);
    }

    /**
     * Returns the size of the x offset for the player's inventory in pixels.
     */
    public int getPlayerInvXOffset() {
        return getBorderSide() + Math.max(0, (getContainerInvWidth() - getPlayerInvWidth()) / 2);
    }

    /**
     * Returns the size of the player's inventory width, not including the borders, in pixels.
     */
    public int getPlayerInvWidth() {
        return 9 * 18;
    }

    /**
     * Returns the size of the player's inventory height, including the hotbar, in pixels.
     */
    public int getPlayerInvHeight() {
        return 4 * 18 + getBufferHotbar();
    }

}
