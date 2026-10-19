package gr8pefish.ironbackpacks.neoforge;

import java.util.List;
import java.util.function.Consumer;

import gr8pefish.ironbackpacks.IronBackpacks;
import gr8pefish.ironbackpacks.core.EventHandler;
import gr8pefish.ironbackpacks.core.RegistrarIronBackpacks;
import gr8pefish.ironbackpacks.core.UpgradeEventHandler;
import gr8pefish.ironbackpacks.gametest.IBGameTests;
import gr8pefish.ironbackpacks.network.MessageRequestAction;
import gr8pefish.ironbackpacks.network.MessageSetEquippedBackpack;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.entity.item.ItemExpireEvent;
import net.neoforged.neoforge.event.entity.player.AnvilCraftEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(IronBackpacks.MODID)
public class IronBackpacksNeoForge {

    public IronBackpacksNeoForge(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.STARTUP, ConfigHandler.SPEC);
        NeoForgePlatform.init(modBus);
        IronBackpacks.init();
        NeoForgePlatform.deferred(NeoForgeRegistries.Keys.CONDITION_CODECS).register("upgrade_enabled", () -> UpgradeEnabledCondition.CODEC);
        // 26.x: game tests are registry entries (the functions) and test instances (registered below)
        IBGameTests.tests().forEach((name, body) -> NeoForgePlatform.<Consumer<GameTestHelper>>deferred(Registries.TEST_FUNCTION).register(name, () -> body));

        modBus.addListener(NewRegistryEvent.class, NeoForgePlatform::onNewRegistry);
        modBus.addListener(RegisterEvent.class, IronBackpacksNeoForge::registerBackpacks);
        modBus.addListener(FMLCommonSetupEvent.class, event -> event.enqueueWork(IronBackpacks::setup));
        modBus.addListener(RegisterPayloadHandlersEvent.class, IronBackpacksNeoForge::registerPayloads);
        modBus.addListener(RegisterGameTestsEvent.class, IronBackpacksNeoForge::registerTests);

        NeoForge.EVENT_BUS.addListener(AnvilUpdateEvent.class, IronBackpacksNeoForge::onAnvil);
        NeoForge.EVENT_BUS.addListener(AnvilCraftEvent.Pre.class, IronBackpacksNeoForge::onAnvilTake);
        NeoForge.EVENT_BUS.addListener(ItemExpireEvent.class, IronBackpacksNeoForge::onItemExpire);
        // 26.x clients get no recipes unless asked: the crafting ones for the JEI tier category
        NeoForge.EVENT_BUS.addListener(OnDatapackSyncEvent.class, event -> event.sendRecipes(RecipeType.CRAFTING));
    }

    private static void registerBackpacks(RegisterEvent event) {
        event.register(RegistrarIronBackpacks.TYPES_KEY, helper -> RegistrarIronBackpacks.registerBackpackTypes(helper::register));
        event.register(RegistrarIronBackpacks.UPGRADES_KEY, helper -> RegistrarIronBackpacks.registerUpgrades(helper::register));
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(RegistrarIronBackpacks.id("default"), new TestEnvironmentDefinition.AllOf(List.of()));
        IBGameTests.tests().keySet().forEach(name -> event.registerTest(RegistrarIronBackpacks.id(name), new FunctionGameTestInstance(
                ResourceKey.create(Registries.TEST_FUNCTION, RegistrarIronBackpacks.id(name)),
                new TestData<>(environment, IBGameTests.structure(), 100, 0, true, Rotation.NONE))));
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(MessageRequestAction.TYPE, MessageRequestAction.STREAM_CODEC,
                (message, context) -> context.enqueueWork(() -> MessageRequestAction.handle(message, (ServerPlayer) context.player())));
        registrar.playToClient(MessageSetEquippedBackpack.TYPE, MessageSetEquippedBackpack.STREAM_CODEC,
                (message, context) -> context.enqueueWork(() -> MessageSetEquippedBackpack.handle(message)));
    }

    private static void onAnvil(AnvilUpdateEvent event) {
        EventHandler.AnvilResult result = EventHandler.onAnvil(event.getLeft(), event.getRight());
        if (result != null) {
            event.setOutput(result.output());
            event.setXpCost(result.cost());
            event.setMaterialCost(result.materialCost());
        }
    }

    /**
     * The result is taken: backpack and shears back, and no anvil damage (1.12 AnvilRepairEvent break chance 0; 26.x has
     * no break chance, so the vanilla take is done here without the damage roll, as on Fabric).
     */
    private static void onAnvilTake(AnvilCraftEvent.Pre event) {
        AnvilMenu menu = event.getMenu();
        Player player = event.getEntity();
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();
        EventHandler.AnvilResult result = EventHandler.onAnvil(left, right);
        if (!EventHandler.onAnvilTake(player, left, right, event.getOutput()))
            return;

        event.setCanceled(true);
        if (!player.hasInfiniteMaterials())
            player.giveExperienceLevels(-menu.getCost());
        int materialCost = result == null ? 0 : result.materialCost();
        ItemStack material = menu.getSlot(1).getItem();
        if (materialCost > 0 && !material.isEmpty() && material.getCount() > materialCost) {
            material.shrink(materialCost);
            menu.getSlot(1).set(material);
        } else {
            menu.getSlot(1).set(ItemStack.EMPTY);
        }
        menu.setCost(0);
        menu.getSlot(0).set(ItemStack.EMPTY);
        // the anvil use sound (the menu keeps the anvil position to itself)
        player.level().levelEvent(1030, player.blockPosition(), 0);
    }

    /** Everlasting: 1.12 extra life + setNoDespawn (the expiry can't be cancelled any more). */
    private static void onItemExpire(ItemExpireEvent event) {
        if (UpgradeEventHandler.keepsAlive(event.getEntity().getItem())) {
            event.setExtraLife(UpgradeEventHandler.EXTRA_LIFE);
            event.getEntity().setUnlimitedLifetime();
        }
    }
}
