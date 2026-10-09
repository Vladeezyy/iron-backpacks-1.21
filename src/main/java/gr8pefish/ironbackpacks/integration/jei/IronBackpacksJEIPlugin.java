package gr8pefish.ironbackpacks.integration.jei;

import java.util.List;

import javax.annotation.Nonnull;

import gr8pefish.ironbackpacks.api.backpack.BackpackInfo;
import gr8pefish.ironbackpacks.api.backpack.IBackpack;
import gr8pefish.ironbackpacks.core.RegistrarIronBackpacks;
import gr8pefish.ironbackpacks.core.recipe.BackpackTierRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

// TODO - Figure out why recipes are merged with normal crafting category
@JeiPlugin
public class IronBackpacksJEIPlugin implements IModPlugin {

    public static IJeiRuntime runtime;

    @Nonnull
    @Override
    public ResourceLocation getPluginUid() {
        return RegistrarIronBackpacks.id("jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registry) {
        registry.addRecipeCategories(new RecipeCategoryTier(registry.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level == null)
            return;
        List<BackpackTierRecipe> recipes = level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING).stream()
                .map(RecipeHolder::value).filter(r -> r instanceof BackpackTierRecipe).map(r -> (BackpackTierRecipe) r).toList();
        registration.addRecipes(RecipeCategoryTier.TYPE, recipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registry) {
        registry.addRecipeCatalyst(new ItemStack(Items.CRAFTING_TABLE), RecipeCategoryTier.TYPE);
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration subtypeRegistry) {
        // 1.12 useNbtForSubtypes(UPGRADE): one entry per upgrade
        subtypeRegistry.registerSubtypeInterpreter(RegistrarIronBackpacks.UPGRADE.get(), new ISubtypeInterpreter<>() {
            @Override
            public Object getSubtypeData(@Nonnull ItemStack stack, @Nonnull UidContext context) {
                return stack.get(RegistrarIronBackpacks.UPGRADE_ID.get());
            }

            @Nonnull
            @Override
            public String getLegacyStringSubtypeInfo(@Nonnull ItemStack stack, @Nonnull UidContext context) {
                ResourceLocation upgrade = stack.get(RegistrarIronBackpacks.UPGRADE_ID.get());
                return upgrade == null ? "" : upgrade.toString();
            }
        });
        subtypeRegistry.registerSubtypeInterpreter(RegistrarIronBackpacks.BACKPACK.get(), new ISubtypeInterpreter<>() {
            @Override
            public Object getSubtypeData(@Nonnull ItemStack stack, @Nonnull UidContext context) {
                return getLegacyStringSubtypeInfo(stack, context);
            }

            @Nonnull
            @Override
            public String getLegacyStringSubtypeInfo(@Nonnull ItemStack s, @Nonnull UidContext context) {
                if (!(s.getItem() instanceof IBackpack))
                    return "";

                BackpackInfo backpackInfo = ((IBackpack) s.getItem()).getBackpackInfo(s);
                return backpackInfo.getVariant().getBackpackType().getIdentifier().toString() + "|" + backpackInfo.getVariant().getBackpackSpecialty();
            }
        });
    }

    @Override
    public void onRuntimeAvailable(@Nonnull IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }
}
