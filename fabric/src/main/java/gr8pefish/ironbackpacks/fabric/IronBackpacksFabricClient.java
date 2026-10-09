package gr8pefish.ironbackpacks.fabric;

import gr8pefish.ironbackpacks.IronBackpacks;
import gr8pefish.ironbackpacks.client.ClientEventHandler;
import gr8pefish.ironbackpacks.client.gui.GuiBackpack;
import gr8pefish.ironbackpacks.core.RegistrarIronBackpacks;
import gr8pefish.ironbackpacks.network.MessageSetEquippedBackpack;
import gr8pefish.ironbackpacks.util.ColorUtil;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.object.builder.v1.client.model.FabricModelPredicateProviderRegistry;
import net.minecraft.client.gui.screens.MenuScreens;

/** Registers the common client parts (client/ClientEventHandler) with Fabric's client API. */
public class IronBackpacksFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(ClientEventHandler.KEY_EQUIP);
        KeyBindingHelper.registerKeyBinding(ClientEventHandler.KEY_OPEN);
        ClientTickEvents.END_CLIENT_TICK.register(ClientEventHandler::onClientTick);
        MenuScreens.register(RegistrarIronBackpacks.BACKPACK_MENU.get(), GuiBackpack::new);
        ColorProviderRegistry.ITEM.register(ColorUtil::getBackpackColor, RegistrarIronBackpacks.BACKPACK.get());
        FabricModelPredicateProviderRegistry.register(RegistrarIronBackpacks.BACKPACK.get(), ClientEventHandler.VARIANT_PROPERTY, ClientEventHandler.VARIANT);
        FabricModelPredicateProviderRegistry.register(RegistrarIronBackpacks.UPGRADE.get(), ClientEventHandler.UPGRADE_PROPERTY, ClientEventHandler.UPGRADE);
        ClientPlayNetworking.registerGlobalReceiver(MessageSetEquippedBackpack.TYPE, (message, context) -> MessageSetEquippedBackpack.handle(message));
        // the blacklist with every mod's items (1.12 CommonProxy.init)
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> IronBackpacks.setup());
    }
}
