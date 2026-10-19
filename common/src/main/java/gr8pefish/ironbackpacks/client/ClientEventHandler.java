package gr8pefish.ironbackpacks.client;

import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;

import gr8pefish.ironbackpacks.IronBackpacks;
import gr8pefish.ironbackpacks.core.RegistrarIronBackpacks;
import gr8pefish.ironbackpacks.network.MessageRequestAction;
import gr8pefish.ironbackpacks.network.RequestAction;
import gr8pefish.ironbackpacks.platform.Services;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

/**
 * 1.12 ClientEventHandler + ClientProxy + the client half of RegistrarIronBackpacks.registerModels: key bindings, the
 * open key and the tooltips; the item models and colours are in {@link ItemModelProperties}. The loaders register these
 * with their client events.
 */
public class ClientEventHandler {

    /** 26.x key categories are ids (translation key.category.ironbackpacks.main); the loaders register it. */
    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(RegistrarIronBackpacks.id("main"));
    /** Disabled in 1.12.2 (does nothing); unbound by default here, 1.12's H is Accessories' "open accessories" key. */
    public static final KeyMapping KEY_EQUIP = new KeyMapping("key." + IronBackpacks.MODID + ".equip", InputConstants.Type.KEYBOARD,
            InputConstants.UNKNOWN.getValue(), CATEGORY);
    public static final KeyMapping KEY_OPEN = new KeyMapping("key." + IronBackpacks.MODID + ".open", InputConstants.Type.KEYBOARD, InputConstants.KEY_I, CATEGORY);

    /** End of every client tick (1.12 KeyInputEvent). */
    public static void onClientTick(Minecraft mc) {
        if (KEY_OPEN.consumeClick())
            Services.PLATFORM.sendToServer(new MessageRequestAction(RequestAction.OPEN_BACKPACK));
        else if (KEY_EQUIP.consumeClick() && false) // TODO - Use once backpack equipping has been fully implemented.
            Services.PLATFORM.sendToServer(new MessageRequestAction(RequestAction.EQUIP_BACKPACK));
        DevScene.onTick(mc);
        PerfScene.onTick(mc);
    }

    /** Set by the DevScene to screenshot the shift tooltips. */
    static boolean sceneShift;

    /** 1.12 Keyboard.isKeyDown(Keyboard.KEY_LSHIFT). */
    public static boolean isLeftShiftDown() {
        return sceneShift || InputConstants.isKeyDown(InputConstants.KEY_LSHIFT);
    }

    /** 1.12 FontRenderer.listFormattedStringToWidth. */
    public static List<Component> wrap(Component text, int width) {
        return Minecraft.getInstance().font.getSplitter().splitLines(text, width, Style.EMPTY).stream()
                .map(line -> (Component) Component.literal(line.getString())).toList();
    }
}
