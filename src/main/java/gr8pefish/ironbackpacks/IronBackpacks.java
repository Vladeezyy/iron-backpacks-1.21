package gr8pefish.ironbackpacks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gr8pefish.ironbackpacks.api.IronBackpacksAPI;
import gr8pefish.ironbackpacks.core.EventHandler;
import gr8pefish.ironbackpacks.core.RegistrarIronBackpacks;
import gr8pefish.ironbackpacks.core.UpgradeEventHandler;
import gr8pefish.ironbackpacks.network.MessageRequestAction;
import gr8pefish.ironbackpacks.network.MessageSetEquippedBackpack;
import gr8pefish.ironbackpacks.util.InventoryBlacklist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(IronBackpacks.MODID)
public class IronBackpacks {

    public static final String MODID = "ironbackpacks";
    public static final String NAME = "Iron Backpacks";
    public static final Logger LOGGER = LogManager.getLogger(NAME);

    public IronBackpacks(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.STARTUP, ConfigHandler.SPEC);
        RegistrarIronBackpacks.register(modBus);
        modBus.addListener(this::setup);
        modBus.addListener(this::registerPayloads);
        NeoForge.EVENT_BUS.register(EventHandler.class);
        NeoForge.EVENT_BUS.register(UpgradeEventHandler.class);
        gr8pefish.ironbackpacks.gametest.IBGameTests.register(modBus);
    }

    /** 1.12 CommonProxy.init. */
    private void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            IronBackpacksAPI.initBackpackVariantList();
            InventoryBlacklist.initBlacklist();
        });
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(MessageRequestAction.TYPE, MessageRequestAction.STREAM_CODEC, MessageRequestAction::handle);
        registrar.playToClient(MessageSetEquippedBackpack.TYPE, MessageSetEquippedBackpack.STREAM_CODEC, MessageSetEquippedBackpack::handle);
    }
}
