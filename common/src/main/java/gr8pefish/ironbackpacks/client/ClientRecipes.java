package gr8pefish.ironbackpacks.client;

import java.util.List;

import gr8pefish.ironbackpacks.core.recipe.BackpackTierRecipe;
import net.minecraft.world.item.crafting.Recipe;

/**
 * The tier recipes the server sent (26.x clients have no recipe manager): the loaders sync them (NeoForge
 * OnDatapackSyncEvent.sendRecipes, Fabric RecipeSynchronization) and pass them here for JEI.
 */
public final class ClientRecipes {

    private static volatile List<BackpackTierRecipe> tierRecipes = List.of();

    public static List<BackpackTierRecipe> tierRecipes() {
        return tierRecipes;
    }

    public static void received(Iterable<? extends Recipe<?>> recipes) {
        List<BackpackTierRecipe> tier = new java.util.ArrayList<>();
        for (Recipe<?> recipe : recipes)
            if (recipe instanceof BackpackTierRecipe tierRecipe)
                tier.add(tierRecipe);
        tierRecipes = List.copyOf(tier);
    }

    private ClientRecipes() {}
}
