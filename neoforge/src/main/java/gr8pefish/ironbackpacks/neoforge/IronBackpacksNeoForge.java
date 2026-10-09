package gr8pefish.ironbackpacks.neoforge;

import gr8pefish.ironbackpacks.IronBackpacks;
import gr8pefish.ironbackpacks.core.EventHandler;
import gr8pefish.ironbackpacks.core.RegistrarIronBackpacks;
import gr8pefish.ironbackpacks.core.UpgradeEventHandler;
import gr8pefish.ironbackpacks.gametest.IBGameTests;
import gr8pefish.ironbackpacks.network.MessageRequestAction;
import gr8pefish.ironbackpacks.network.MessageSetEquippedBackpack;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.entity.item.ItemExpireEvent;
import net.neoforged.neoforge.event.entity.player.AnvilRepairEvent;
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

        modBus.addListener(NewRegistryEvent.class, NeoForgePlatform::onNewRegistry);
        modBus.addListener(RegisterEvent.class, IronBackpacksNeoForge::registerBackpacks);
        modBus.addListener(FMLCommonSetupEvent.class, event -> event.enqueueWork(IronBackpacks::setup));
        modBus.addListener(RegisterPayloadHandlersEvent.class, IronBackpacksNeoForge::registerPayloads);
        modBus.addListener(RegisterGameTestsEvent.class, event -> event.register(IBGameTests.class));

        NeoForge.EVENT_BUS.addListener(AnvilUpdateEvent.class, IronBackpacksNeoForge::onAnvil);
        NeoForge.EVENT_BUS.addListener(AnvilRepairEvent.class, IronBackpacksNeoForge::onAnvilPost);
        NeoForge.EVENT_BUS.addListener(ItemExpireEvent.class, IronBackpacksNeoForge::onItemExpire);
    }

    private static void registerBackpacks(RegisterEvent event) {
        event.register(RegistrarIronBackpacks.TYPES_KEY, helper -> RegistrarIronBackpacks.registerBackpackTypes(helper::register));
        event.register(RegistrarIronBackpacks.UPGRADES_KEY, helper -> RegistrarIronBackpacks.registerUpgrades(helper::register));
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
            event.setCost(result.cost());
            event.setMaterialCost(result.materialCost());
        }
    }

    private static void onAnvilPost(AnvilRepairEvent event) {
        if (EventHandler.onAnvilTake(event.getEntity(), event.getLeft(), event.getRight(), event.getOutput()))
            event.setBreakChance(0.0F);
    }

    /** Everlasting: 1.12 extra life + setNoDespawn (the expiry can't be cancelled any more). */
    private static void onItemExpire(ItemExpireEvent event) {
        if (UpgradeEventHandler.keepsAlive(event.getEntity().getItem())) {
            event.setExtraLife(UpgradeEventHandler.EXTRA_LIFE);
            event.getEntity().setUnlimitedLifetime();
        }
    }
}
