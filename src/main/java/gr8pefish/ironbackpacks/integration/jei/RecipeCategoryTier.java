package gr8pefish.ironbackpacks.integration.jei;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import gr8pefish.ironbackpacks.core.RegistrarIronBackpacks;
import gr8pefish.ironbackpacks.core.recipe.BackpackTierRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.crafting.Ingredient;

/** "Increase Backpack Tier": the tier recipes on the 1.12 crafting grid background, with the description below. */
public class RecipeCategoryTier implements IRecipeCategory<BackpackTierRecipe> {

    public static final RecipeType<BackpackTierRecipe> TYPE = new RecipeType<>(RegistrarIronBackpacks.id("tier"), BackpackTierRecipe.class);

    private final Component title;
    private final IDrawable background;
    private final IDrawable icon;

    public RecipeCategoryTier(IGuiHelper guiHelper) {
        title = Component.translatable("jei.ironbackpacks.increaseTier.name");
        ResourceLocation backgroundLocation = RegistrarIronBackpacks.id("textures/jei/crafting_grid.png");
        background = guiHelper.createDrawable(backgroundLocation, 0, 0, 166, 130);
        icon = guiHelper.createDrawable(backgroundLocation, 168, 0, 16, 16);
    }

    @Nonnull
    @Override
    public RecipeType<BackpackTierRecipe> getRecipeType() {
        return TYPE;
    }

    @Nonnull
    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public int getWidth() {
        return 166;
    }

    @Override
    public int getHeight() {
        return 130;
    }

    @Nullable
    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(@Nonnull IRecipeLayoutBuilder builder, @Nonnull BackpackTierRecipe recipe, @Nonnull IFocusGroup focuses) {
        // 1.12 slot positions are the 18x18 slot corners; the item sits 1 pixel in
        builder.addSlot(RecipeIngredientRole.OUTPUT, 119, 30).addItemStack(recipe.getResultItem(registryAccess()));

        List<Ingredient> ingredients = recipe.getIngredients();
        int width = recipe.getWidth();
        for (int y = 0; y < 3; ++y) {
            for (int x = 0; x < 3; ++x) {
                var slot = builder.addSlot(RecipeIngredientRole.INPUT, (x * 18) + 24, (y * 18) + 12);
                if (x < width && y < recipe.getHeight())
                    slot.addIngredients(ingredients.get(x + y * width));
            }
        }
    }

    @Override
    public void draw(@Nonnull BackpackTierRecipe recipe, @Nonnull IRecipeSlotsView recipeSlotsView, @Nonnull GuiGraphics g, double mouseX, double mouseY) {
        background.draw(g);
        Font font = Minecraft.getInstance().font;
        // Adds the recipes type necessary at the top of the screen
        g.drawString(font, Component.translatable("jei.description.shapedCrafting"), 43, 0, 0x424242, false);

        // Adds the tier description
        List<FormattedCharSequence> description = font.split(Component.translatable("jei.ironbackpacks.increaseTier.desc"), 160);
        for (int i = 0; i < description.size(); i++)
            g.drawString(font, description.get(i), 10, 69 + (i * 8), 0, false);
    }

    @Override
    public void getTooltip(@Nonnull ITooltipBuilder tooltip, @Nonnull BackpackTierRecipe recipe, @Nonnull IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        if (mouseX >= 52 && mouseX <= 130 && mouseY >= 85 && mouseY <= 91) {
            tooltip.add(Component.translatable("jei.ironbackpacks.increaseTier.name"));
            tooltip.add(Component.translatable("jei.ironbackpacks.increaseTier.desc2").withStyle(ChatFormatting.GRAY));
        }
    }

    private static net.minecraft.core.RegistryAccess registryAccess() {
        var level = Minecraft.getInstance().level;
        return level == null ? net.minecraft.core.RegistryAccess.EMPTY : level.registryAccess();
    }
}
