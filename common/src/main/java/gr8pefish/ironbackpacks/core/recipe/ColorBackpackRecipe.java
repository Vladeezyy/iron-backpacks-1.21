package gr8pefish.ironbackpacks.core.recipe;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import gr8pefish.ironbackpacks.api.IronBackpacksAPI;
import gr8pefish.ironbackpacks.api.backpack.BackpackInfo;
import gr8pefish.ironbackpacks.api.backpack.variant.BackpackSpecialty;
import gr8pefish.ironbackpacks.api.backpack.variant.BackpackVariant;
import gr8pefish.ironbackpacks.util.ColorUtil;
import gr8pefish.ironbackpacks.util.Utils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.display.RecipeDisplay;

/**
 * Class for defining how the recipes work for coloring backpacks.
 *
 * Currently:
 * - Backpack + Dye = Colored Backpack, to the color of the dye
 * - Colored Backpack + Water Bucket = Uncolored Backpack
 */
public class ColorBackpackRecipe extends ShapedRecipe {

    public static final MapCodec<ColorBackpackRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Recipe.CommonInfo.MAP_CODEC.forGetter(r -> r.commonInfo),
            CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(r -> r.bookInfo),
            ShapedRecipePattern.MAP_CODEC.forGetter(r -> r.shapedPattern),
            Identifier.CODEC.fieldOf("backpack_type").forGetter(r -> r.typeId),
            BackpackSpecialty.CODEC.fieldOf("specialty").forGetter(r -> r.specialty),
            IngredientBackpack.CODEC.optionalFieldOf("input_backpack").forGetter(r -> r.inputBackpack)
    ).apply(i, ColorBackpackRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, ColorBackpackRecipe> STREAM_CODEC = StreamCodec.composite(
            Recipe.CommonInfo.STREAM_CODEC, r -> r.commonInfo,
            CraftingRecipe.CraftingBookInfo.STREAM_CODEC, r -> r.bookInfo,
            ShapedRecipePattern.STREAM_CODEC, r -> r.shapedPattern,
            Identifier.STREAM_CODEC, r -> r.typeId,
            BackpackSpecialty.STREAM_CODEC, r -> r.specialty,
            IngredientBackpack.OPTIONAL_STREAM_CODEC, r -> r.inputBackpack,
            ColorBackpackRecipe::new);
    public static final RecipeSerializer<ColorBackpackRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);

    /** The pattern (vanilla keeps ShapedRecipe.pattern private). */
    private final ShapedRecipePattern shapedPattern;

    private final Identifier typeId;
    private final BackpackSpecialty specialty;
    private final Optional<IngredientBackpack> inputBackpack;
    private final ItemStackTemplate result;

    public ColorBackpackRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo, ShapedRecipePattern pattern, Identifier type,
                               BackpackSpecialty specialty, Optional<IngredientBackpack> inputBackpack) {
        this(commonInfo, bookInfo, pattern, type, specialty, inputBackpack,
                IronBackpacksAPI.getTemplate(new BackpackVariant(IronBackpacksAPI.getBackpackType(type), specialty)));
    }

    private ColorBackpackRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo, ShapedRecipePattern pattern, Identifier type,
                                BackpackSpecialty specialty, Optional<IngredientBackpack> inputBackpack, ItemStackTemplate result) {
        super(commonInfo, bookInfo, pattern, result);
        this.shapedPattern = pattern;
        this.typeId = type;
        this.specialty = specialty;
        this.inputBackpack = inputBackpack;
        this.result = result;
    }

    @Nonnull
    @Override
    public ItemStack assemble(@Nonnull CraftingInput matrix) {
        ItemStack backpack = RecipeUtil.getFirstBackpackInGrid(matrix);
        if (backpack.isEmpty())
            return super.assemble(matrix);

        ItemStack upgraded = result.create();
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

    /** The recipe book and JEI: the backpack slot shows the backpacks that fit. */
    @Override
    public List<RecipeDisplay> display() {
        return inputBackpack.map(b -> b.display(this, result)).orElseGet(super::display);
    }

    @Nonnull
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public RecipeSerializer<ShapedRecipe> getSerializer() {
        // ShapedRecipe fixes the serializer's type; the generic type is erased at runtime
        return (RecipeSerializer) SERIALIZER;
    }
}
