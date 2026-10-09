package gr8pefish.ironbackpacks.neoforge;

import gr8pefish.ironbackpacks.IronBackpacks;
import gr8pefish.ironbackpacks.client.ClientEventHandler;
import gr8pefish.ironbackpacks.client.gui.GuiBackpack;
import gr8pefish.ironbackpacks.core.RegistrarIronBackpacks;
import gr8pefish.ironbackpacks.util.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

/** Registers the common client parts (client/ClientEventHandler) with NeoForge's client events. */
@EventBusSubscriber(modid = IronBackpacks.MODID, value = Dist.CLIENT)
public final class IronBackpacksNeoForgeClient {

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        ClientEventHandler.onClientTick(Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        ClientEventHandler.KEY_EQUIP.setKeyConflictContext(KeyConflictContext.IN_GAME);
        ClientEventHandler.KEY_OPEN.setKeyConflictContext(KeyConflictContext.IN_GAME);
        event.register(ClientEventHandler.KEY_EQUIP);
        event.register(ClientEventHandler.KEY_OPEN);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(RegistrarIronBackpacks.BACKPACK_MENU.get(), GuiBackpack::new);
    }

    // Register colors for backpacks
    @SubscribeEvent
    public static void registerColors(RegisterColorHandlersEvent.Item event) {
        event.register(ColorUtil::getBackpackColor, RegistrarIronBackpacks.BACKPACK.get());
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(RegistrarIronBackpacks.BACKPACK.get(), ClientEventHandler.VARIANT_PROPERTY, ClientEventHandler.VARIANT);
            ItemProperties.register(RegistrarIronBackpacks.UPGRADE.get(), ClientEventHandler.UPGRADE_PROPERTY, ClientEventHandler.UPGRADE);
        });
    }

    private IronBackpacksNeoForgeClient() {}
}
