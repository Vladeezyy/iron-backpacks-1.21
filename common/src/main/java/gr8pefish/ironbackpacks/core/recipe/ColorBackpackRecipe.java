package gr8pefish.ironbackpacks.core.recipe;

import java.util.Optional;

import javax.annotation.Nonnull;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import gr8pefish.ironbackpacks.api.IronBackpacksAPI;
import gr8pefish.ironbackpacks.api.backpack.BackpackInfo;
import gr8pefish.ironbackpacks.api.backpack.variant.BackpackSpecialty;
import gr8pefish.ironbackpacks.api.backpack.variant.BackpackVariant;
import gr8pefish.ironbackpacks.core.RegistrarIronBackpacks;
import gr8pefish.ironbackpacks.util.ColorUtil;
import gr8pefish.ironbackpacks.util.Utils;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

/**
 * Class for defining how the recipes work for coloring backpacks.
 *
 * Currently:
 * - Backpack + Dye = Colored Backpack, to the color of the dye
 * - Colored Backpack + Water Bucket = Uncolored Backpack
 */
public class ColorBackpackRecipe extends ShapedRecipe {

    /** The pattern (vanilla keeps ShapedRecipe.pattern package private). */
    private final ShapedRecipePattern shapedPattern;

    private final ResourceLocation typeId;
    private final BackpackSpecialty specialty;
    private final Optional<IngredientBackpack> inputBackpack;

    public ColorBackpackRecipe(String group, CraftingBookCategory category, ShapedRecipePattern pattern, ResourceLocation type, BackpackSpecialty specialty,
                               Optional<IngredientBackpack> inputBackpack) {
        super(group, category, pattern, IronBackpacksAPI.getStack(new BackpackVariant(IronBackpacksAPI.getBackpackType(type), specialty)));
        this.shapedPattern = pattern;
        this.typeId = type;
        this.specialty = specialty;
        this.inputBackpack = inputBackpack;
    }

    @Nonnull
    @Override
    public ItemStack assemble(@Nonnull CraftingInput matrix, @Nonnull HolderLookup.Provider registries) {
        ItemStack backpack = RecipeUtil.getFirstBackpackInGrid(matrix);
        if (backpack.isEmpty())
            return super.assemble(matrix, registries);

        ItemStack upgraded = getResultItem(registries).copy();
        BackpackInfo upgradedInfo = Utils.getBackpackInfoFromStack(backpack);
        upgradedInfo.setRGBColor(getDyeColor(matrix));

        return IronBackpacksAPI.applyPackInfo(upgraded, upgradedInfo);
    }

    // Helper

    /**
     * Gets the color to dye the backpack from a crafting grid.
     *
     * @param matrix - the crafting inventory to search
     * @return - a RGB color as an {@link int}
     */
    private int getDyeColor(@Nonnull CraftingInput matrix) {
        ItemStack stack = getFirstDyeOrWaterBucketInGrid(matrix);
        if (stack.isEmpty()) return BackpackInfo.NO_COLOR; //Error, return no color
        if (stack.is(Items.WATER_BUCKET)) return BackpackInfo.NO_COLOR; //Wash away color
        DyeColor dye = ColorUtil.dyeColorOf(stack);
        return dye == null ? BackpackInfo.NO_COLOR : dye.getTextureDiffuseColor() & 0xFFFFFF; //Get dye RGB color and return it if possible, otherwise return no color
    }

    /**
     * Gets the first dye (c:dyes) or water bucket ItemStack in a crafting grid.
     *
     * @param matrix - the crafting inventory to search
     * @return - a ItemStack which is known to be a backpack
     */
    @Nonnull
    private static ItemStack getFirstDyeOrWaterBucketInGrid(@Nonnull CraftingInput matrix) {
        ItemStack stack = ItemStack.EMPTY;
        for (int i = 0; i < matrix.size(); i++) {
            stack = matrix.getItem(i);
            if (!stack.isEmpty()) {
                if (ColorUtil.dyeColorOf(stack) != null) {
                    return stack;
                }
                if (stack.is(Items.WATER_BUCKET)) {
                    return stack;
                }
            }
        }
        return stack;
    }

    @Override
    public boolean matches(@Nonnull CraftingInput input, @Nonnull net.minecraft.world.level.Level level) {
        return super.matches(input, level) && inputBackpack.map(b -> b.matches(input)).orElse(true);
    }

    @Nonnull
    @Override
    public net.minecraft.core.NonNullList<net.minecraft.world.item.crafting.Ingredient> getIngredients() {
        return inputBackpack.map(b -> b.display(super.getIngredients())).orElseGet(super::getIngredients);
    }

    @Nonnull
    @Override
    public RecipeSerializer<?> getSerializer() {
        return RegistrarIronBackpacks.COLOR_RECIPE.get();
    }

    public static class Serializer implements RecipeSerializer<ColorBackpackRecipe> {
        private static final MapCodec<ColorBackpackRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                com.mojang.serialization.Codec.STRING.optionalFieldOf("group", "").forGetter(ColorBackpackRecipe::getGroup),
                CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(ColorBackpackRecipe::category),
                ShapedRecipePattern.MAP_CODEC.forGetter(r -> r.shapedPattern),
                ResourceLocation.CODEC.fieldOf("backpack_type").forGetter(r -> r.typeId),
                BackpackSpecialty.CODEC.fieldOf("specialty").forGetter(r -> r.specialty),
                IngredientBackpack.CODEC.optionalFieldOf("input_backpack").forGetter(r -> r.inputBackpack)
        ).apply(i, ColorBackpackRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, ColorBackpackRecipe> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, ColorBackpackRecipe::getGroup,
                CraftingBookCategory.STREAM_CODEC, ColorBackpackRecipe::category,
                ShapedRecipePattern.STREAM_CODEC, r -> r.shapedPattern,
                ResourceLocation.STREAM_CODEC, r -> r.typeId,
                BackpackSpecialty.STREAM_CODEC, r -> r.specialty,
                ByteBufCodecs.optional(IngredientBackpack.STREAM_CODEC), r -> r.inputBackpack,
                ColorBackpackRecipe::new);

        @Override
        public MapCodec<ColorBackpackRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ColorBackpackRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
