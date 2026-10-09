package gr8pefish.ironbackpacks.core.recipe;

import javax.annotation.Nonnull;

import gr8pefish.ironbackpacks.api.backpack.IBackpack;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;

/**
 * Helper methods for recipes
 */
public class RecipeUtil {

    /**
     * Gets the first backpack ItemStack in a crafting grid.
     *
     * @param matrix - the crafting inventory to search
     * @return - a ItemStack which is known to be a backpack
     */
    @Nonnull
    public static ItemStack getFirstBackpackInGrid(@Nonnull CraftingInput matrix) {
        ItemStack stack = ItemStack.EMPTY;
        for (int i = 0; i < matrix.size(); i++) {
            stack = matrix.getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof IBackpack) {
                return stack;
            }
        }
        return stack;
    }

}
