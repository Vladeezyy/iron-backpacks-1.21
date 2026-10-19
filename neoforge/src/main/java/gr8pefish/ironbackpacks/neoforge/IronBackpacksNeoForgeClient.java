package gr8pefish.ironbackpacks.neoforge;

import gr8pefish.ironbackpacks.IronBackpacks;
import gr8pefish.ironbackpacks.client.ClientEventHandler;
import gr8pefish.ironbackpacks.client.ClientRecipes;
import gr8pefish.ironbackpacks.client.ItemModelProperties;
import gr8pefish.ironbackpacks.client.gui.GuiBackpack;
import gr8pefish.ironbackpacks.core.RegistrarIronBackpacks;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterSelectItemModelPropertyEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

/** Registers the common client parts (client/ClientEventHandler, client/ItemModelProperties) with NeoForge's client events. */
@EventBusSubscriber(modid = IronBackpacks.MODID, value = Dist.CLIENT)
public final class IronBackpacksNeoForgeClient {

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        ClientEventHandler.onClientTick(Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.registerCategory(ClientEventHandler.CATEGORY);
        ClientEventHandler.KEY_EQUIP.setKeyConflictContext(KeyConflictContext.IN_GAME);
        ClientEventHandler.KEY_OPEN.setKeyConflictContext(KeyConflictContext.IN_GAME);
        event.register(ClientEventHandler.KEY_EQUIP);
        event.register(ClientEventHandler.KEY_OPEN);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(RegistrarIronBackpacks.BACKPACK_MENU.get(), GuiBackpack::new);
    }

    @SubscribeEvent
    public static void registerTints(RegisterColorHandlersEvent.ItemTintSources event) {
        event.register(ItemModelProperties.COLOR_ID, ItemModelProperties.BackpackColor.MAP_CODEC);
    }

    @SubscribeEvent
    public static void registerProperties(RegisterSelectItemModelPropertyEvent event) {
        event.register(ItemModelProperties.VARIANT_ID, ItemModelProperties.Variant.TYPE);
        event.register(ItemModelProperties.UPGRADE_ID, ItemModelProperties.Upgrade.TYPE);
    }

    /** Before JEI, which reloads on this event too, reads them. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRecipes(RecipesReceivedEvent event) {
        ClientRecipes.received(event.getRecipeMap().values().stream().map(RecipeHolder::value).toList());
    }

    private IronBackpacksNeoForgeClient() {}
}
