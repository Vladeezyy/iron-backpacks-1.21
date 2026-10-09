package gr8pefish.ironbackpacks.core;

import javax.annotation.Nullable;

import gr8pefish.ironbackpacks.ConfigHandler;
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
import gr8pefish.ironbackpacks.core.recipe.IngredientBackpack;
import gr8pefish.ironbackpacks.core.recipe.UpgradeEnabledCondition;
import gr8pefish.ironbackpacks.item.ItemBackpack;
import gr8pefish.ironbackpacks.item.ItemUpgrade;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

/**
 * Everything Iron Backpacks registers. 1.12 used @ObjectHolder fields filled by the registry events; here the
 * backpack types and upgrades are still registered in their {@link RegisterEvent}s (the upgrades depend on the
 * config), the rest through deferred registers.
 */
public final class RegistrarIronBackpacks {

    // Registries
    public static final ResourceKey<Registry<BackpackType>> TYPES_KEY = ResourceKey.createRegistryKey(id("types"));
    public static final ResourceKey<Registry<BackpackUpgrade>> UPGRADES_KEY = ResourceKey.createRegistryKey(id("upgrade"));
    public static final Registry<BackpackType> REGISTRY_TYPES = new RegistryBuilder<>(TYPES_KEY).defaultKey(IronBackpacksAPI.NULL).sync(true).create();
    public static final Registry<BackpackUpgrade> REGISTRY_UPGRADES = new RegistryBuilder<>(UPGRADES_KEY).defaultKey(IronBackpacksAPI.NULL).sync(true).create();

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(IronBackpacks.MODID);
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, IronBackpacks.MODID);
    private static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, IronBackpacks.MODID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, IronBackpacks.MODID);
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, IronBackpacks.MODID);
    private static final DeferredRegister<IngredientType<?>> INGREDIENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.INGREDIENT_TYPES, IronBackpacks.MODID);
    private static final DeferredRegister<com.mojang.serialization.MapCodec<? extends ICondition>> CONDITIONS =
            DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, IronBackpacks.MODID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, IronBackpacks.MODID);
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, IronBackpacks.MODID);

    // Data components (1.12: the "packInfo" / "packInv" NBT tags of the backpack, the "upgrade" tag of the upgrade item)
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BackpackInfo.Data>> PACK_INFO = COMPONENTS.registerComponentType("pack_info",
            b -> b.persistent(BackpackInfo.Data.CODEC).networkSynchronized(BackpackInfo.Data.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> PACK_INVENTORY = COMPONENTS.registerComponentType("pack_inventory",
            b -> b.persistent(ItemContainerContents.CODEC).networkSynchronized(ItemContainerContents.STREAM_CODEC).cacheEncoding());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> UPGRADE_ID = COMPONENTS.registerComponentType("upgrade",
            b -> b.persistent(ResourceLocation.CODEC).networkSynchronized(ResourceLocation.STREAM_CODEC));

    // Items
    public static final DeferredItem<ItemBackpack> BACKPACK = ITEMS.register("backpack", ItemBackpack::new);
    public static final DeferredItem<ItemUpgrade> UPGRADE = ITEMS.register("upgrade", ItemUpgrade::new);

    // Sounds
    public static final DeferredHolder<SoundEvent, SoundEvent> BACKPACK_OPEN = SOUNDS.register("open_backpack", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> BACKPACK_CLOSE = SOUNDS.register("close_backpack", SoundEvent::createVariableRangeEvent);

    // Backpack types
    public static final DeferredHolder<BackpackType, BackpackType> PACK_BASIC = DeferredHolder.create(TYPES_KEY, id("basic"));
    public static final DeferredHolder<BackpackType, BackpackType> PACK_IRON = DeferredHolder.create(TYPES_KEY, id("iron"));
    public static final DeferredHolder<BackpackType, BackpackType> PACK_GOLD = DeferredHolder.create(TYPES_KEY, id("gold"));
    public static final DeferredHolder<BackpackType, BackpackType> PACK_DIAMOND = DeferredHolder.create(TYPES_KEY, id("diamond"));

    // Backpack upgrade (each may be switched off in the config, then the holder stays unbound)
    public static final DeferredHolder<BackpackUpgrade, BackpackUpgrade> UPGRADE_DAMAGE_BAR = DeferredHolder.create(UPGRADES_KEY, id("damage_bar"));
    public static final DeferredHolder<BackpackUpgrade, BackpackUpgrade> UPGRADE_LOCK = DeferredHolder.create(UPGRADES_KEY, id("lock"));
    public static final DeferredHolder<BackpackUpgrade, BackpackUpgrade> UPGRADE_EXTRA_UPGRADE = DeferredHolder.create(UPGRADES_KEY, id("extra_upgrade"));
    public static final DeferredHolder<BackpackUpgrade, BackpackUpgrade> UPGRADE_EVERLASTING = DeferredHolder.create(UPGRADES_KEY, id("everlasting"));

    // Menu (1.12: GuiHandler ids 0-2; the mode and hand travel with the open packet)
    public static final DeferredHolder<MenuType<?>, MenuType<ContainerBackpack>> BACKPACK_MENU = MENUS.register("backpack",
            () -> IMenuTypeExtension.create(ContainerBackpack::fromNetwork));

    // Recipes
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BackpackTierRecipe>> TIER_RECIPE = RECIPE_SERIALIZERS.register("backpack_tier",
            BackpackTierRecipe.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ColorBackpackRecipe>> COLOR_RECIPE = RECIPE_SERIALIZERS.register("backpack_color",
            ColorBackpackRecipe.Serializer::new);
    public static final DeferredHolder<IngredientType<?>, IngredientType<IngredientBackpack>> BACKPACK_INGREDIENT = INGREDIENT_TYPES.register("backpack",
            () -> new IngredientType<>(IngredientBackpack.CODEC, IngredientBackpack.STREAM_CODEC));
    public static final DeferredHolder<com.mojang.serialization.MapCodec<? extends ICondition>, com.mojang.serialization.MapCodec<UpgradeEnabledCondition>> UPGRADE_ENABLED =
            CONDITIONS.register("upgrade_enabled", () -> UpgradeEnabledCondition.CODEC);

    // Player data (1.12 capability "equipped_backpack")
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ItemStack>> EQUIPPED_BACKPACK = ATTACHMENTS.register("equipped_backpack",
            () -> AttachmentType.builder(() -> ItemStack.EMPTY).serialize(ItemStack.OPTIONAL_CODEC).build());

    // Creative tab (1.12 IronBackpacks.TAB_IB)
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB_IB = TABS.register(IronBackpacks.MODID, () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + IronBackpacks.MODID))
            .icon(() -> IronBackpacksAPI.getStack(PACK_IRON.get() /*im not null don't worry*/, BackpackSpecialty.STORAGE))
            .displayItems((params, output) -> {
                ItemBackpack.fillCreativeTab(output);
                ItemUpgrade.fillCreativeTab(output);
            })
            .build());

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        SOUNDS.register(modBus);
        COMPONENTS.register(modBus);
        MENUS.register(modBus);
        RECIPE_SERIALIZERS.register(modBus);
        INGREDIENT_TYPES.register(modBus);
        CONDITIONS.register(modBus);
        TABS.register(modBus);
        ATTACHMENTS.register(modBus);
        modBus.addListener(RegistrarIronBackpacks::createRegistries);
        modBus.addListener(RegistrarIronBackpacks::registerBackpacks);
    }

    private static void createRegistries(NewRegistryEvent event) {
        event.register(REGISTRY_TYPES);
        event.register(REGISTRY_UPGRADES);
    }

    private static void registerBackpacks(RegisterEvent event) {
        event.register(TYPES_KEY, helper -> {
            helper.register(IronBackpacksAPI.NULL, new BackpackType(IronBackpacksAPI.NULL, 0, 0, false, BackpackSize.MIN));

            helper.register(id("basic"), new BackpackType(id("basic"), 0, 4, false, 9, 2));
            helper.register(id("iron"), new BackpackType(id("iron"), 1, 7, true, 9, 3));
            helper.register(id("gold"), new BackpackType(id("gold"), 2, 12, true, 9, 5));
            helper.register(id("diamond"), new BackpackType(id("diamond"), 3, 18, true, 9, 7));
        });

        event.register(UPGRADES_KEY, helper -> {
            helper.register(IronBackpacksAPI.NULL, new BackpackUpgrade(IronBackpacksAPI.NULL, 0, 0));

            if (ConfigHandler.upgrades.enableDamageBar.get())
                helper.register(id("damage_bar"), new BackpackUpgrade(id("damage_bar"), 1, 0));
            if (ConfigHandler.upgrades.enablePackLatch.get())
                helper.register(id("lock"), new BackpackUpgrade(id("lock"), 1, 0));
            if (ConfigHandler.upgrades.enableExtraUpgrade.get())
                helper.register(id("extra_upgrade"), new BackpackUpgrade(id("extra_upgrade"), -1, 0));
            if (ConfigHandler.upgrades.enableEverlasting.get())
                helper.register(id("everlasting"), new BackpackUpgrade(id("everlasting"), 4, 1));
        });
    }

    /** The upgrade of a holder, or null when it is switched off in the config (1.12: a null @ObjectHolder). */
    @Nullable
    public static BackpackUpgrade get(DeferredHolder<BackpackUpgrade, BackpackUpgrade> upgrade) {
        return upgrade.isBound() ? upgrade.get() : null;
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(IronBackpacks.MODID, path);
    }

    private RegistrarIronBackpacks() {}
}
