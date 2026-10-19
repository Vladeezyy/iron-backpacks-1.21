package gr8pefish.ironbackpacks.core.recipe;

import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import gr8pefish.ironbackpacks.api.IronBackpacksAPI;
import gr8pefish.ironbackpacks.api.backpack.BackpackInfo;
import gr8pefish.ironbackpacks.api.backpack.IBackpack;
import gr8pefish.ironbackpacks.api.backpack.variant.BackpackSpecialty;
import gr8pefish.ironbackpacks.api.backpack.variant.BackpackType;
import gr8pefish.ironbackpacks.core.RegistrarIronBackpacks;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;

/**
 * 1.12 IngredientBackpack: any backpack of the given type's tier with the given specialty. The recipes keep the plain
 * backpack item as their ingredient (the same JSON on NeoForge and Fabric) and check this in {@code matches}
 * (JSON: {@code "input_backpack": {"backpack_type": "ironbackpacks:iron", "specialty": "storage"}}).
 */
public record IngredientBackpack(Identifier backpackType, BackpackSpecialty specialty) {

    public static final Codec<IngredientBackpack> CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.fieldOf("backpack_type").forGetter(IngredientBackpack::backpackType),
            BackpackSpecialty.CODEC.fieldOf("specialty").forGetter(IngredientBackpack::specialty)
    ).apply(i, IngredientBackpack::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, IngredientBackpack> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, IngredientBackpack::backpackType,
            BackpackSpecialty.STREAM_CODEC, IngredientBackpack::specialty,
            IngredientBackpack::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, java.util.Optional<IngredientBackpack>> OPTIONAL_STREAM_CODEC =
            net.minecraft.network.codec.ByteBufCodecs.optional(STREAM_CODEC);

    public IngredientBackpack(BackpackType type, BackpackSpecialty specialty) {
        this(type.getIdentifier(), specialty);
    }

    private int packTier() {
        return IronBackpacksAPI.getBackpackType(backpackType).getTier();
    }

    public boolean test(@Nullable ItemStack input) {
        if (input == null || input.isEmpty())
            return false;

        if (!input.has(RegistrarIronBackpacks.PACK_INFO.get()))
            return false;

        if (!(input.getItem() instanceof IBackpack))
            return false;

        BackpackInfo info = ((IBackpack) input.getItem()).getBackpackInfo(input);
        return info.getVariant().getBackpackType().getTier() == packTier() && info.getVariant().getBackpackSpecialty() == specialty;
    }

    /** Gathers all BackpackTypes from the API that match our required tier. */
    @Nonnull
    public Stream<ItemStack> getItems() {
        int packTier = packTier();
        return IronBackpacksAPI.getBackpackTypes().stream()
                .filter(b -> !b.getIdentifier().equals(IronBackpacksAPI.NULL) && b.getTier() == packTier)
                .map(type -> IronBackpacksAPI.getStack(type, specialty));
    }

    /** Every backpack in the grid is one of these. */
    public boolean matches(CraftingInput input) {
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.getItem() instanceof IBackpack && !test(stack))
                return false;
        }
        return true;
    }

    /** The recipe for display (JEI, the recipe book): the backpack slot shows the matching backpacks. */
    public List<RecipeDisplay> display(ShapedRecipe recipe, ItemStackTemplate result) {
        SlotDisplay backpacks = new SlotDisplay.Composite(getItems().map(stack -> (SlotDisplay) new SlotDisplay.ItemStackSlotDisplay(ItemStackTemplate.fromNonEmptyStack(stack))).toList());
        ItemStack plainBackpack = new ItemStack(RegistrarIronBackpacks.BACKPACK.get());
        List<SlotDisplay> slots = recipe.getIngredients().stream()
                .map(ingredient -> ingredient.map(i -> i.test(plainBackpack) ? backpacks : i.display()).orElse(SlotDisplay.Empty.INSTANCE)).toList();
        return List.of(new ShapedCraftingRecipeDisplay(recipe.getWidth(), recipe.getHeight(), slots, new SlotDisplay.ItemStackSlotDisplay(result),
                new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)));
    }
}
