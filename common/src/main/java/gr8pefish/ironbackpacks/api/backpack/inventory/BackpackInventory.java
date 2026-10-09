package gr8pefish.ironbackpacks.api.backpack.inventory;

import javax.annotation.Nonnull;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

/**
 * A backpack's slots (1.12: a Forge ItemStackHandler; a vanilla container here, so both loaders share it), with the
 * item handler names the 1.12 code used.
 */
public class BackpackInventory extends SimpleContainer {

    public BackpackInventory(int size) {
        super(size);
    }

    public int getSlots() {
        return getContainerSize();
    }

    @Nonnull
    public ItemStack getStackInSlot(int slot) {
        return getItem(slot);
    }

    public void setStackInSlot(int slot, @Nonnull ItemStack stack) {
        setItem(slot, stack);
    }
}
