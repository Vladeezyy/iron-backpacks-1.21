package gr8pefish.ironbackpacks.core.recipe;

import java.util.Optional;

import javax.annotation.Nonnull;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import gr8pefish.ironbackpacks.api.IronBackpacksAPI;
import gr8pefish.ironbackpacks.api.backpack.BackpackInfo;
import gr8pefish.ironbackpacks.api.backpack.IBackpack;
import gr8pefish.ironbackpacks.api.backpack.variant.BackpackSpecialty;
import gr8pefish.ironbackpacks.api.backpack.variant.BackpackType;
import gr8pefish.ironbackpacks.core.RegistrarIronBackpacks;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

/**
 * A shaped recipe making a backpack of the given type and specialty. When the middle slot holds a backpack, the
 * result keeps its owner, upgrades and items (JSON: the shaped recipe fields plus "backpack_type" and "specialty").
 */
public class BackpackTierRecipe extends ShapedRecipe {

    /** The pattern (vanilla keeps ShapedRecipe.pattern package private). */
    private final ShapedRecipePattern shapedPattern;

    private final ResourceLocation resultTypeId;
    private final BackpackSpecialty resultSpecialty;
    private final Optional<IngredientBackpack> inputBackpack;

    public BackpackTierRecipe(String group, CraftingBookCategory category, ShapedRecipePattern pattern, ResourceLocation resultType,
                              BackpackSpecialty resultSpecialty, Optional<IngredientBackpack> inputBackpack) {
        super(group, category, pattern, IronBackpacksAPI.getStack(IronBackpacksAPI.getBackpackType(resultType), resultSpecialty));
        this.shapedPattern = pattern;

        this.resultTypeId = resultType;
        this.resultSpecialty = resultSpecialty;
        this.inputBackpack = inputBackpack;
    }

    @Nonnull
    @Override
    public ItemStack assemble(@Nonnull CraftingInput matrix, @Nonnull HolderLookup.Provider registries) {
        ItemStack backpack = matrix.width() > 1 && matrix.height() > 1 ? matrix.getItem(1, 1) : ItemStack.EMPTY;
        if (!(backpack.getItem() instanceof IBackpack))
            return super.assemble(matrix, registries);

        ItemStack upgraded = getResultItem(registries).copy();
        BackpackInfo upgradedInfo = BackpackInfo.upgradeTo(((IBackpack) backpack.getItem()).getBackpackInfo(backpack), getResultType(), getResultSpecialty());
        return IronBackpacksAPI.applyPackInfo(upgraded, upgradedInfo);
    }

    public BackpackType getResultType() {
        return IronBackpacksAPI.getBackpackType(resultTypeId);
    }

    public BackpackSpecialty getResultSpecialty() {
        return resultSpecialty;
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
        return RegistrarIronBackpacks.TIER_RECIPE.get();
    }

    public static class Serializer implements RecipeSerializer<BackpackTierRecipe> {
        private static final MapCodec<BackpackTierRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                com.mojang.serialization.Codec.STRING.optionalFieldOf("group", "").forGetter(BackpackTierRecipe::getGroup),
                CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(BackpackTierRecipe::category),
                ShapedRecipePattern.MAP_CODEC.forGetter(r -> r.shapedPattern),
                ResourceLocation.CODEC.fieldOf("backpack_type").forGetter(r -> r.resultTypeId),
                BackpackSpecialty.CODEC.fieldOf("specialty").forGetter(r -> r.resultSpecialty),
                IngredientBackpack.CODEC.optionalFieldOf("input_backpack").forGetter(r -> r.inputBackpack)
        ).apply(i, BackpackTierRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, BackpackTierRecipe> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, BackpackTierRecipe::getGroup,
                CraftingBookCategory.STREAM_CODEC, BackpackTierRecipe::category,
                ShapedRecipePattern.STREAM_CODEC, r -> r.shapedPattern,
                ResourceLocation.STREAM_CODEC, r -> r.resultTypeId,
                BackpackSpecialty.STREAM_CODEC, r -> r.resultSpecialty,
                ByteBufCodecs.optional(IngredientBackpack.STREAM_CODEC), r -> r.inputBackpack,
                BackpackTierRecipe::new);

        @Override
        public MapCodec<BackpackTierRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, BackpackTierRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
