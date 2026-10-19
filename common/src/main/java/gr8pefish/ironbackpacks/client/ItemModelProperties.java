package gr8pefish.ironbackpacks.client;

import java.util.Map;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import gr8pefish.ironbackpacks.api.backpack.BackpackInfo;
import gr8pefish.ironbackpacks.api.backpack.variant.BackpackSpecialty;
import gr8pefish.ironbackpacks.core.RegistrarIronBackpacks;
import gr8pefish.ironbackpacks.util.ColorUtil;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * The item model choice and the dye colour (1.12 custom mesh definitions and IItemColor; 1.21.1 item properties and
 * overrides). 26.x picks models in {@code assets/ironbackpacks/items/*.json} with these select properties and tints
 * with {@link BackpackColor}; the loaders register them under {@link #VARIANT_ID}, {@link #UPGRADE_ID}, {@link #COLOR_ID}.
 */
public final class ItemModelProperties {

    public static final Identifier VARIANT_ID = RegistrarIronBackpacks.id("variant");
    public static final Identifier UPGRADE_ID = RegistrarIronBackpacks.id("upgrade");
    public static final Identifier COLOR_ID = RegistrarIronBackpacks.id("backpack_color");

    /** "basic/none", "iron/storage" ... by type and specialty: asked every frame per rendered backpack, so built once. */
    private static final Map<Identifier, String[]> VARIANT_NAMES = new java.util.concurrent.ConcurrentHashMap<>();

    /** The backpack's "type/specialty" (the type's path), null without backpack data (models/item/backpack/null). */
    public record Variant() implements SelectItemModelProperty<String> {
        public static final SelectItemModelProperty.Type<Variant, String> TYPE = SelectItemModelProperty.Type.create(MapCodec.unit(new Variant()), Codec.STRING);

        @Nullable
        @Override
        public String get(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner, int seed, ItemDisplayContext displayContext) {
            BackpackInfo.Data data = stack.get(RegistrarIronBackpacks.PACK_INFO.get());
            if (data == null)
                return null;
            String[] bySpec = VARIANT_NAMES.computeIfAbsent(data.type(), type -> {
                String[] names = new String[BackpackSpecialty.values().length];
                for (BackpackSpecialty specialty : BackpackSpecialty.values())
                    names[specialty.ordinal()] = type.getPath() + "/" + specialty.getName();
                return names;
            });
            return bySpec[data.spec().ordinal()];
        }

        @Override
        public Codec<String> valueCodec() {
            return Codec.STRING;
        }

        @Override
        public SelectItemModelProperty.Type<Variant, String> type() {
            return TYPE;
        }
    }

    /** The upgrade an upgrade item holds ("ironbackpacks:lock" ...), null for the blank upgrade. */
    public record Upgrade() implements SelectItemModelProperty<String> {
        public static final SelectItemModelProperty.Type<Upgrade, String> TYPE = SelectItemModelProperty.Type.create(MapCodec.unit(new Upgrade()), Codec.STRING);

        @Nullable
        @Override
        public String get(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner, int seed, ItemDisplayContext displayContext) {
            Identifier upgrade = stack.get(RegistrarIronBackpacks.UPGRADE_ID.get());
            return upgrade == null ? null : upgrade.toString();
        }

        @Override
        public Codec<String> valueCodec() {
            return Codec.STRING;
        }

        @Override
        public SelectItemModelProperty.Type<Upgrade, String> type() {
            return TYPE;
        }
    }

    /** The backpack's dye colour, opaque white (no tint) when it has none. */
    public record BackpackColor() implements ItemTintSource {
        public static final MapCodec<BackpackColor> MAP_CODEC = MapCodec.unit(new BackpackColor());

        @Override
        public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
            return ColorUtil.getBackpackColor(stack, 1);
        }

        @Override
        public MapCodec<BackpackColor> type() {
            return MAP_CODEC;
        }
    }

    private ItemModelProperties() {}
}
