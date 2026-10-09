package gr8pefish.ironbackpacks.api.upgrade;

import javax.annotation.Nonnull;

import com.google.common.base.Preconditions;

import gr8pefish.ironbackpacks.api.IronBackpacksAPI;
import gr8pefish.ironbackpacks.core.RegistrarIronBackpacks;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Used to mark an item as an upgrade so it can be picked up by internal handling.
 */
public interface IUpgrade {

    /**
     * Gets the container object for all upgrade data from the stack.
     *
     * @param stack The stack to get the upgrade from
     * @return the container object for all upgrade data
     */
    @Nonnull
    default BackpackUpgrade getUpgrade(@Nonnull ItemStack stack) {
        Preconditions.checkNotNull(stack, "ItemStack cannot be null");

        ResourceLocation upgrade = stack.get(RegistrarIronBackpacks.UPGRADE_ID.get());
        if (upgrade != null)
            return IronBackpacksAPI.getUpgrade(upgrade);

        return IronBackpacksAPI.getUpgrade(IronBackpacksAPI.NULL);
    }
}
