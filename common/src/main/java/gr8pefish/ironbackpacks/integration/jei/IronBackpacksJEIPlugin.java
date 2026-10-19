package gr8pefish.ironbackpacks.integration.jei;

import javax.annotation.Nonnull;

import gr8pefish.ironbackpacks.api.backpack.BackpackInfo;
import gr8pefish.ironbackpacks.api.backpack.IBackpack;
import gr8pefish.ironbackpacks.core.RegistrarIronBackpacks;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import gr8pefish.ironbackpacks.client.ClientRecipes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

// TODO - Figure out why recipes are merged with normal crafting category
@JeiPlugin
public class IronBackpacksJEIPlugin implements IModPlugin {

    public static IJeiRuntime runtime;

    @Nonnull
    @Override
    public Identifier getPluginUid() {
        return RegistrarIronBackpacks.id("jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registry) {
        registry.addRecipeCategories(new RecipeCategoryTier(registry.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // 26.x: the client has no recipe manager; the loaders keep the synced tier recipes
        registration.addRecipes(RecipeCategoryTier.TYPE, ClientRecipes.tierRecipes());
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
        });
        subtypeRegistry.registerSubtypeInterpreter(RegistrarIronBackpacks.BACKPACK.get(), new ISubtypeInterpreter<>() {
            @Override
            public Object getSubtypeData(@Nonnull ItemStack s, @Nonnull UidContext context) {
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
