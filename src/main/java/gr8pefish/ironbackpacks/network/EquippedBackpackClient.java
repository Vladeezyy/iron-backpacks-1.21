package gr8pefish.ironbackpacks.network;

import net.minecraft.world.item.ItemStack;

/** The client's copy of the equipped backpack (1.12 GuiHandler.equipped, set by {@link MessageSetEquippedBackpack}). */
public final class EquippedBackpackClient {
    public static ItemStack equipped = ItemStack.EMPTY;

    private EquippedBackpackClient() {}
}
