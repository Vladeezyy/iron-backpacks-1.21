package gr8pefish.ironbackpacks.util;

import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.Set;

import javax.annotation.Nonnull;

import com.google.common.collect.Sets;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.reflect.TypeToken;

import gr8pefish.ironbackpacks.IronBackpacks;
import gr8pefish.ironbackpacks.api.IronBackpacksAPI;
import gr8pefish.ironbackpacks.api.blacklist.IInventoryBlacklist;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.fml.loading.FMLPaths;

/**
 * Items that can't go into a backpack, read from config/ironbackpacks/blacklist.json (written empty on first start,
 * like 1.12). 1.21 has no item metadata, so "itemBlacklist" lists item ids; "nbtBlacklist" keys (and "foo.bar" sub
 * keys) are looked up in the stack's custom data, which holds the free-form NBT stacks had in 1.12.
 */
public class InventoryBlacklist implements IInventoryBlacklist {

    public static final InventoryBlacklist INSTANCE = new InventoryBlacklist();

    private final Set<ItemEntry> itemBlacklist;
    private final Set<String> nbtBlacklist;

    private InventoryBlacklist() {
        this.itemBlacklist = Sets.newHashSet();
        this.nbtBlacklist = Sets.newHashSet();
    }

    @Override
    public void blacklist(@Nonnull ItemStack stack) {
        ItemEntry entry = new ItemEntry(stack.getItem());
        if (!itemBlacklist.contains(entry))
            itemBlacklist.add(entry);
    }

    @Override
    public void blacklist(@Nonnull String tagKey) {
        if (!nbtBlacklist.contains(tagKey))
            nbtBlacklist.add(tagKey);
    }

    @Override
    public boolean isBlacklisted(@Nonnull ItemStack stack) {
        for (ItemEntry entry : itemBlacklist)
            if (entry.item() == stack.getItem())
                return true;

        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.isEmpty())
            return false;

        for (String key : nbtBlacklist) {
            if (tag.contains(key))
                return true; // Short circuit if main tag has key

            String[] subTags = key.split("\\."); // Allow sub tags defined as `foo.bar` to look for a
            for (String subTag : subTags) {
                if (tag.isEmpty() || tag.getTagType(subTag) != Tag.TAG_COMPOUND)
                    return tag.contains(subTag);

                tag = tag.getCompound(subTag);
            }
        }

        return false;
    }

    public static void initBlacklist() {
        Path jsonConfig = FMLPaths.CONFIGDIR.get().resolve(IronBackpacks.MODID).resolve("blacklist.json");
        InventoryBlacklist blacklist = JsonUtil.fromJson(TypeToken.get(InventoryBlacklist.class), jsonConfig.toFile(), new InventoryBlacklist());

        INSTANCE.itemBlacklist.clear();
        INSTANCE.nbtBlacklist.clear();
        INSTANCE.itemBlacklist.addAll(blacklist.itemBlacklist);
        INSTANCE.nbtBlacklist.addAll(blacklist.nbtBlacklist);
        INSTANCE.itemBlacklist.removeIf(entry -> entry.item() == null);

        IronBackpacksAPI.setInventoryBlacklist(INSTANCE);
    }

    @JsonAdapter(ItemEntry.Serializer.class)
    public record ItemEntry(Item item) {
        public static class Serializer implements JsonSerializer<ItemEntry>, JsonDeserializer<ItemEntry> {
            @Override
            public ItemEntry deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                ResourceLocation id = ResourceLocation.tryParse(json.getAsString());
                return new ItemEntry(id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null));
            }

            @Override
            public JsonElement serialize(ItemEntry src, Type typeOfSrc, JsonSerializationContext context) {
                return new JsonPrimitive(BuiltInRegistries.ITEM.getKey(src.item()).toString());
            }
        }
    }
}
