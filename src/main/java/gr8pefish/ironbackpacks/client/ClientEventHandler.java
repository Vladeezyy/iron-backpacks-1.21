package gr8pefish.ironbackpacks.client;

import java.util.List;
import java.util.Map;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import gr8pefish.ironbackpacks.IronBackpacks;
import gr8pefish.ironbackpacks.api.backpack.BackpackInfo;
import gr8pefish.ironbackpacks.client.gui.GuiBackpack;
import gr8pefish.ironbackpacks.core.RegistrarIronBackpacks;
import gr8pefish.ironbackpacks.network.MessageRequestAction;
import gr8pefish.ironbackpacks.network.RequestAction;
import gr8pefish.ironbackpacks.util.ColorUtil;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 1.12 ClientEventHandler + ClientProxy + the client half of RegistrarIronBackpacks.registerModels: key bindings,
 * the backpack screen, layer colours and the item model choice (1.12 custom mesh definitions; here the overrides of
 * models/item/backpack.json and upgrade.json, picked by the properties below).
 */
@EventBusSubscriber(modid = IronBackpacks.MODID, value = Dist.CLIENT)
public class ClientEventHandler {

    public static final String CATEGORY = "key.categories." + IronBackpacks.MODID;
    /** Disabled in 1.12.2 (does nothing); unbound by default here, 1.12's H is Accessories' "open accessories" key. */
    public static final KeyMapping KEY_EQUIP = new KeyMapping("key." + IronBackpacks.MODID + ".equip", KeyConflictContext.IN_GAME, KeyModifier.NONE,
            InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), CATEGORY);
    public static final KeyMapping KEY_OPEN = new KeyMapping("key." + IronBackpacks.MODID + ".open", KeyConflictContext.IN_GAME, KeyModifier.NONE,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_I, CATEGORY);

    /** Model override index of each backpack variant ("type/specialty"), 0 = models/item/backpack/null. */
    private static final Map<String, Integer> VARIANT_MODELS = Map.of("ironbackpacks:basic/none", 1, "ironbackpacks:iron/storage", 2,
            "ironbackpacks:iron/upgrade", 3, "ironbackpacks:gold/storage", 4, "ironbackpacks:gold/upgrade", 5, "ironbackpacks:diamond/storage", 6,
            "ironbackpacks:diamond/upgrade", 7);
    /** Model override index of each upgrade, 0 = models/item/upgrade/null (blank). */
    private static final Map<String, Integer> UPGRADE_MODELS = Map.of("ironbackpacks:damage_bar", 1, "ironbackpacks:lock", 2,
            "ironbackpacks:extra_upgrade", 3, "ironbackpacks:everlasting", 4);

    @SubscribeEvent
    public static void onKey(ClientTickEvent.Post event) {
        if (KEY_OPEN.consumeClick())
            PacketDistributor.sendToServer(new MessageRequestAction(RequestAction.OPEN_BACKPACK));
        else if (KEY_EQUIP.consumeClick() && false) // TODO - Use once backpack equipping has been fully implemented.
            PacketDistributor.sendToServer(new MessageRequestAction(RequestAction.EQUIP_BACKPACK));
    }

    @EventBusSubscriber(modid = IronBackpacks.MODID, value = Dist.CLIENT)
    public static class ModEvents {
        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(KEY_EQUIP);
            event.register(KEY_OPEN);
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
                ItemProperties.register(RegistrarIronBackpacks.BACKPACK.get(), RegistrarIronBackpacks.id("variant"), (stack, level, entity, seed) -> {
                    BackpackInfo.Data data = stack.get(RegistrarIronBackpacks.PACK_INFO.get());
                    return data == null ? 0 : VARIANT_MODELS.getOrDefault(data.type() + "/" + data.spec().getName(), 0);
                });
                ItemProperties.register(RegistrarIronBackpacks.UPGRADE.get(), RegistrarIronBackpacks.id("upgrade"), (stack, level, entity, seed) -> {
                    ResourceLocation upgrade = stack.get(RegistrarIronBackpacks.UPGRADE_ID.get());
                    return upgrade == null ? 0 : UPGRADE_MODELS.getOrDefault(upgrade.toString(), 0);
                });
            });
        }
    }

    /** Set by the DevScene to screenshot the shift tooltips. */
    static boolean sceneShift;

    /** 1.12 Keyboard.isKeyDown(Keyboard.KEY_LSHIFT). */
    public static boolean isLeftShiftDown() {
        return sceneShift || InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT);
    }

    /** 1.12 FontRenderer.listFormattedStringToWidth. */
    public static List<Component> wrap(Component text, int width) {
        return Minecraft.getInstance().font.getSplitter().splitLines(text, width, Style.EMPTY).stream()
                .map(line -> (Component) Component.literal(line.getString())).toList();
    }
}
