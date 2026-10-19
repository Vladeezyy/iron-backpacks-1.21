package gr8pefish.ironbackpacks.fabric;

import gr8pefish.ironbackpacks.IronBackpacks;
import gr8pefish.ironbackpacks.client.ClientEventHandler;
import gr8pefish.ironbackpacks.client.gui.GuiBackpack;
import gr8pefish.ironbackpacks.core.RegistrarIronBackpacks;
import gr8pefish.ironbackpacks.network.MessageSetEquippedBackpack;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.recipe.v1.sync.ClientRecipeSynchronizedEvent;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import gr8pefish.ironbackpacks.client.ClientRecipes;
import gr8pefish.ironbackpacks.client.ItemModelProperties;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperties;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.client.gui.screens.MenuScreens;

/** Registers the common client parts (client/ClientEventHandler) with Fabric's client API. */
public class IronBackpacksFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // 26.x key categories are registered ids (the same record as ClientEventHandler.CATEGORY)
        KeyMapping.Category.register(ClientEventHandler.CATEGORY.id());
        KeyMappingHelper.registerKeyMapping(ClientEventHandler.KEY_EQUIP);
        KeyMappingHelper.registerKeyMapping(ClientEventHandler.KEY_OPEN);
        ClientTickEvents.END_CLIENT_TICK.register(ClientEventHandler::onClientTick);
        MenuScreens.register(RegistrarIronBackpacks.BACKPACK_MENU.get(), GuiBackpack::new);
        // the item model properties and the dye tint of assets/ironbackpacks/items/*.json (the mappers are opened by Fabric API)
        SelectItemModelProperties.ID_MAPPER.put(ItemModelProperties.VARIANT_ID, ItemModelProperties.Variant.TYPE);
        SelectItemModelProperties.ID_MAPPER.put(ItemModelProperties.UPGRADE_ID, ItemModelProperties.Upgrade.TYPE);
        ItemTintSources.ID_MAPPER.put(ItemModelProperties.COLOR_ID, ItemModelProperties.BackpackColor.MAP_CODEC);
        ClientRecipeSynchronizedEvent.EVENT.register((client, recipes) -> ClientRecipes.received(recipes.recipes().stream().map(RecipeHolder::value).toList()));
        ClientPlayNetworking.registerGlobalReceiver(MessageSetEquippedBackpack.TYPE, (message, context) -> MessageSetEquippedBackpack.handle(message));
        // the blacklist with every mod's items (1.12 CommonProxy.init)
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> IronBackpacks.setup());
    }
}
