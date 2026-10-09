package gr8pefish.ironbackpacks.core;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import gr8pefish.ironbackpacks.IronBackpacks;
import gr8pefish.ironbackpacks.api.IronBackpacksAPI;
import gr8pefish.ironbackpacks.api.backpack.BackpackInfo;
import gr8pefish.ironbackpacks.api.backpack.variant.BackpackSize;
import gr8pefish.ironbackpacks.api.backpack.variant.BackpackSpecialty;
import gr8pefish.ironbackpacks.api.backpack.variant.BackpackType;
import gr8pefish.ironbackpacks.api.upgrade.BackpackUpgrade;
import gr8pefish.ironbackpacks.container.ContainerBackpack;
import gr8pefish.ironbackpacks.core.recipe.BackpackTierRecipe;
import gr8pefish.ironbackpacks.core.recipe.ColorBackpackRecipe;
import gr8pefish.ironbackpacks.item.ItemBackpack;
import gr8pefish.ironbackpacks.item.ItemUpgrade;
import gr8pefish.ironbackpacks.platform.Services;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Everything Iron Backpacks registers. 1.12 used @ObjectHolder fields filled by the registry events; here the loader's
 * {@link gr8pefish.ironbackpacks.platform.IPlatform} registers the entries, and the backpack types / upgrades come from
 * {@link #registerBackpackTypes} / {@link #registerUpgrades} when the loader fills those registries.
 */
public final class RegistrarIronBackpacks {

    // Registries
    public static final ResourceKey<Registry<BackpackType>> TYPES_KEY = ResourceKey.createRegistryKey(id("types"));
    public static final ResourceKey<Registry<BackpackUpgrade>> UPGRADES_KEY = ResourceKey.createRegistryKey(id("upgrade"));
    public static final Registry<BackpackType> REGISTRY_TYPES = Services.PLATFORM.createRegistry(TYPES_KEY, IronBackpacksAPI.NULL);
    public static final Registry<BackpackUpgrade> REGISTRY_UPGRADES = Services.PLATFORM.createRegistry(UPGRADES_KEY, IronBackpacksAPI.NULL);

    // Data components (1.12: the "packInfo" / "packInv" NBT tags of the backpack, the "upgrade" tag of the upgrade item)
    public static final Supplier<DataComponentType<BackpackInfo.Data>> PACK_INFO = Services.PLATFORM.register(Registries.DATA_COMPONENT_TYPE, "pack_info",
            () -> DataComponentType.<BackpackInfo.Data>builder().persistent(BackpackInfo.Data.CODEC).networkSynchronized(BackpackInfo.Data.STREAM_CODEC).build());
    public static final Supplier<DataComponentType<ItemContainerContents>> PACK_INVENTORY = Services.PLATFORM.register(Registries.DATA_COMPONENT_TYPE, "pack_inventory",
            () -> DataComponentType.<ItemContainerContents>builder().persistent(ItemContainerContents.CODEC)
                    .networkSynchronized(ItemContainerContents.STREAM_CODEC).cacheEncoding().build());
    public static final Supplier<DataComponentType<ResourceLocation>> UPGRADE_ID = Services.PLATFORM.register(Registries.DATA_COMPONENT_TYPE, "upgrade",
            () -> DataComponentType.<ResourceLocation>builder().persistent(ResourceLocation.CODEC).networkSynchronized(ResourceLocation.STREAM_CODEC).build());

    // Items
    public static final Supplier<ItemBackpack> BACKPACK = Services.PLATFORM.register(Registries.ITEM, "backpack", ItemBackpack::new);
    public static final Supplier<ItemUpgrade> UPGRADE = Services.PLATFORM.register(Registries.ITEM, "upgrade", ItemUpgrade::new);

    // Sounds
    public static final Supplier<SoundEvent> BACKPACK_OPEN = Services.PLATFORM.register(Registries.SOUND_EVENT, "open_backpack",
            () -> SoundEvent.createVariableRangeEvent(id("open_backpack")));
    public static final Supplier<SoundEvent> BACKPACK_CLOSE = Services.PLATFORM.register(Registries.SOUND_EVENT, "close_backpack",
            () -> SoundEvent.createVariableRangeEvent(id("close_backpack")));

    // Backpack types
    public static final Supplier<BackpackType> PACK_BASIC = type("basic");
    public static final Supplier<BackpackType> PACK_IRON = type("iron");
    public static final Supplier<BackpackType> PACK_GOLD = type("gold");
    public static final Supplier<BackpackType> PACK_DIAMOND = type("diamond");

    // Backpack upgrade (each may be switched off in the config, then it gives null like a 1.12 @ObjectHolder)
    public static final Supplier<BackpackUpgrade> UPGRADE_DAMAGE_BAR = upgrade("damage_bar");
    public static final Supplier<BackpackUpgrade> UPGRADE_LOCK = upgrade("lock");
    public static final Supplier<BackpackUpgrade> UPGRADE_EXTRA_UPGRADE = upgrade("extra_upgrade");
    public static final Supplier<BackpackUpgrade> UPGRADE_EVERLASTING = upgrade("everlasting");

    // Menu (1.12: GuiHandler ids 0-2; the mode and hand travel with the open packet)
    public static final Supplier<MenuType<ContainerBackpack>> BACKPACK_MENU = Services.PLATFORM.register(Registries.MENU, "backpack",
            Services.PLATFORM::createBackpackMenuType);

    // Recipes
    public static final Supplier<RecipeSerializer<BackpackTierRecipe>> TIER_RECIPE = Services.PLATFORM.register(Registries.RECIPE_SERIALIZER, "backpack_tier",
            BackpackTierRecipe.Serializer::new);
    public static final Supplier<RecipeSerializer<ColorBackpackRecipe>> COLOR_RECIPE = Services.PLATFORM.register(Registries.RECIPE_SERIALIZER, "backpack_color",
            ColorBackpackRecipe.Serializer::new);

    // Creative tab (1.12 IronBackpacks.TAB_IB)
    public static final Supplier<CreativeModeTab> TAB_IB = Services.PLATFORM.register(Registries.CREATIVE_MODE_TAB, IronBackpacks.MODID,
            () -> Services.PLATFORM.creativeTabBuilder()
                    .title(Component.translatable("itemGroup." + IronBackpacks.MODID))
                    .icon(() -> IronBackpacksAPI.getStack(PACK_IRON.get() /*im not null don't worry*/, BackpackSpecialty.STORAGE))
                    .displayItems((params, output) -> {
                        ItemBackpack.fillCreativeTab(output);
                        ItemUpgrade.fillCreativeTab(output);
                    })
                    .build());

    /** Loads this class: every entry above is handed to the loader. */
    public static void init() {}

    /** 1.12 registerBackpacks: the loader calls this when the types registry takes entries. */
    public static void registerBackpackTypes(BiConsumer<ResourceLocation, BackpackType> helper) {
        helper.accept(IronBackpacksAPI.NULL, new BackpackType(IronBackpacksAPI.NULL, 0, 0, false, BackpackSize.MIN));

        helper.accept(id("basic"), new BackpackType(id("basic"), 0, 4, false, 9, 2));
        helper.accept(id("iron"), new BackpackType(id("iron"), 1, 7, true, 9, 3));
        helper.accept(id("gold"), new BackpackType(id("gold"), 2, 12, true, 9, 5));
        helper.accept(id("diamond"), new BackpackType(id("diamond"), 3, 18, true, 9, 7));
    }

    /** 1.12 registerUpgrades: the upgrades switched on in the config. */
    public static void registerUpgrades(BiConsumer<ResourceLocation, BackpackUpgrade> helper) {
        helper.accept(IronBackpacksAPI.NULL, new BackpackUpgrade(IronBackpacksAPI.NULL, 0, 0));

        if (Services.PLATFORM.upgradeEnabled("damage_bar"))
            helper.accept(id("damage_bar"), new BackpackUpgrade(id("damage_bar"), 1, 0));
        if (Services.PLATFORM.upgradeEnabled("lock"))
            helper.accept(id("lock"), new BackpackUpgrade(id("lock"), 1, 0));
        if (Services.PLATFORM.upgradeEnabled("extra_upgrade"))
            helper.accept(id("extra_upgrade"), new BackpackUpgrade(id("extra_upgrade"), -1, 0));
        if (Services.PLATFORM.upgradeEnabled("everlasting"))
            helper.accept(id("everlasting"), new BackpackUpgrade(id("everlasting"), 4, 1));
    }

    private static Supplier<BackpackType> type(String name) {
        ResourceLocation id = id(name);
        return () -> REGISTRY_TYPES.get(id);
    }

    private static Supplier<BackpackUpgrade> upgrade(String name) {
        ResourceLocation id = id(name);
        return () -> REGISTRY_UPGRADES.containsKey(id) ? REGISTRY_UPGRADES.get(id) : null;
    }

    /** The upgrade, or null when it is switched off in the config (1.12: a null @ObjectHolder). */
    @Nullable
    public static BackpackUpgrade get(Supplier<BackpackUpgrade> upgrade) {
        return upgrade.get();
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(IronBackpacks.MODID, path);
    }

    private RegistrarIronBackpacks() {}
}
