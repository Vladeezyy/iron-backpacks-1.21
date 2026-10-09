package gr8pefish.ironbackpacks.core.recipe;

import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import gr8pefish.ironbackpacks.api.IronBackpacksAPI;
import gr8pefish.ironbackpacks.api.backpack.BackpackInfo;
import gr8pefish.ironbackpacks.api.backpack.IBackpack;
import gr8pefish.ironbackpacks.api.backpack.variant.BackpackSpecialty;
import gr8pefish.ironbackpacks.api.backpack.variant.BackpackType;
import gr8pefish.ironbackpacks.core.RegistrarIronBackpacks;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

/**
 * Any backpack of the given type's tier with the given specialty (JSON: {@code {"type": "ironbackpacks:backpack",
 * "backpack_type": "ironbackpacks:iron", "specialty": "storage"}}).
 */
public record IngredientBackpack(ResourceLocation backpackType, BackpackSpecialty specialty) implements ICustomIngredient {

    public static final MapCodec<IngredientBackpack> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.fieldOf("backpack_type").forGetter(IngredientBackpack::backpackType),
            BackpackSpecialty.CODEC.fieldOf("specialty").forGetter(IngredientBackpack::specialty)
    ).apply(i, IngredientBackpack::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, IngredientBackpack> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, IngredientBackpack::backpackType,
            BackpackSpecialty.STREAM_CODEC, IngredientBackpack::specialty,
            IngredientBackpack::new);

    public IngredientBackpack(BackpackType type, BackpackSpecialty specialty) {
        this(type.getIdentifier(), specialty);
    }

    private int packTier() {
        return IronBackpacksAPI.getBackpackType(backpackType).getTier();
    }

    @Override
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
    @Override
    @Nonnull
    public Stream<ItemStack> getItems() {
        int packTier = packTier();
        return IronBackpacksAPI.getBackpackTypes().stream()
                .filter(b -> !b.getIdentifier().equals(IronBackpacksAPI.NULL) && b.getTier() == packTier)
                .map(type -> IronBackpacksAPI.getStack(type, specialty));
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    @Nonnull
    public IngredientType<?> getType() {
        return RegistrarIronBackpacks.BACKPACK_INGREDIENT.get();
    }
}
