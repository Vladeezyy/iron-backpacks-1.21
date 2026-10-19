package gr8pefish.ironbackpacks.core.recipe;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import gr8pefish.ironbackpacks.api.IronBackpacksAPI;
import gr8pefish.ironbackpacks.api.backpack.BackpackInfo;
import gr8pefish.ironbackpacks.api.backpack.IBackpack;
import gr8pefish.ironbackpacks.api.backpack.variant.BackpackSpecialty;
import gr8pefish.ironbackpacks.api.backpack.variant.BackpackType;
import gr8pefish.ironbackpacks.api.backpack.variant.BackpackVariant;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.display.RecipeDisplay;

/**
 * A shaped recipe making a backpack of the given type and specialty. When the middle slot holds a backpack, the
 * result keeps its owner, upgrades and items (JSON: the shaped recipe fields plus "backpack_type" and "specialty").
 */
public class BackpackTierRecipe extends ShapedRecipe {

    public static final MapCodec<BackpackTierRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Recipe.CommonInfo.MAP_CODEC.forGetter(r -> r.commonInfo),
            CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(r -> r.bookInfo),
            ShapedRecipePattern.MAP_CODEC.forGetter(r -> r.shapedPattern),
            Identifier.CODEC.fieldOf("backpack_type").forGetter(r -> r.resultTypeId),
            BackpackSpecialty.CODEC.fieldOf("specialty").forGetter(r -> r.resultSpecialty),
            IngredientBackpack.CODEC.optionalFieldOf("input_backpack").forGetter(r -> r.inputBackpack)
    ).apply(i, BackpackTierRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, BackpackTierRecipe> STREAM_CODEC = StreamCodec.composite(
            Recipe.CommonInfo.STREAM_CODEC, r -> r.commonInfo,
            CraftingRecipe.CraftingBookInfo.STREAM_CODEC, r -> r.bookInfo,
            ShapedRecipePattern.STREAM_CODEC, r -> r.shapedPattern,
            Identifier.STREAM_CODEC, r -> r.resultTypeId,
            BackpackSpecialty.STREAM_CODEC, r -> r.resultSpecialty,
            IngredientBackpack.OPTIONAL_STREAM_CODEC, r -> r.inputBackpack,
            BackpackTierRecipe::new);
    public static final RecipeSerializer<BackpackTierRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);

    /** The pattern (vanilla keeps ShapedRecipe.pattern private). */
    private final ShapedRecipePattern shapedPattern;

    private final Identifier resultTypeId;
    private final BackpackSpecialty resultSpecialty;
    private final Optional<IngredientBackpack> inputBackpack;
    private final ItemStackTemplate result;

    public BackpackTierRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo, ShapedRecipePattern pattern, Identifier resultType,
                              BackpackSpecialty resultSpecialty, Optional<IngredientBackpack> inputBackpack) {
        this(commonInfo, bookInfo, pattern, resultType, resultSpecialty, inputBackpack,
                IronBackpacksAPI.getTemplate(new BackpackVariant(IronBackpacksAPI.getBackpackType(resultType), resultSpecialty)));
    }

    private BackpackTierRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo, ShapedRecipePattern pattern, Identifier resultType,
                               BackpackSpecialty resultSpecialty, Optional<IngredientBackpack> inputBackpack, ItemStackTemplate result) {
        super(commonInfo, bookInfo, pattern, result);
        this.shapedPattern = pattern;

        this.resultTypeId = resultType;
        this.resultSpecialty = resultSpecialty;
        this.inputBackpack = inputBackpack;
        this.result = result;
    }

    @Nonnull
    @Override
    public ItemStack assemble(@Nonnull CraftingInput matrix) {
        ItemStack backpack = matrix.width() > 1 && matrix.height() > 1 ? matrix.getItem(1, 1) : ItemStack.EMPTY;
        if (!(backpack.getItem() instanceof IBackpack))
            return super.assemble(matrix);

        ItemStack upgraded = result.create();
        BackpackInfo upgradedInfo = BackpackInfo.upgradeTo(((IBackpack) backpack.getItem()).getBackpackInfo(backpack), getResultType(), getResultSpecialty());
        return IronBackpacksAPI.applyPackInfo(upgraded, upgradedInfo);
    }

    public BackpackType getResultType() {
        return IronBackpacksAPI.getBackpackType(resultTypeId);
    }

    public BackpackSpecialty getResultSpecialty() {
        return resultSpecialty;
    }

    /** The result before an input backpack's contents are moved in. */
    public ItemStack getResult() {
        return result.create();
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
