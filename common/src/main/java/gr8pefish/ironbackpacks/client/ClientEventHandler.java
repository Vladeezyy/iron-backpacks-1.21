package gr8pefish.ironbackpacks.client;

import java.util.List;
import java.util.Map;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import gr8pefish.ironbackpacks.IronBackpacks;
import gr8pefish.ironbackpacks.api.backpack.BackpackInfo;
import gr8pefish.ironbackpacks.core.RegistrarIronBackpacks;
import gr8pefish.ironbackpacks.network.MessageRequestAction;
import gr8pefish.ironbackpacks.network.RequestAction;
import gr8pefish.ironbackpacks.platform.Services;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

/**
 * 1.12 ClientEventHandler + ClientProxy + the client half of RegistrarIronBackpacks.registerModels: key bindings, the
 * open key, layer colours (util/ColorUtil) and the item model choice (1.12 custom mesh definitions; here the overrides
 * of models/item/backpack.json and upgrade.json, picked by the properties below). The loaders register these with their
 * client events.
 */
public class ClientEventHandler {

    public static final String CATEGORY = "key.categories." + IronBackpacks.MODID;
    /** Disabled in 1.12.2 (does nothing); unbound by default here, 1.12's H is Accessories' "open accessories" key. */
    public static final KeyMapping KEY_EQUIP = new KeyMapping("key." + IronBackpacks.MODID + ".equip", InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(), CATEGORY);
    public static final KeyMapping KEY_OPEN = new KeyMapping("key." + IronBackpacks.MODID + ".open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_I, CATEGORY);

    /** Model override index of each backpack variant ("type/specialty"), 0 = models/item/backpack/null; the property is index / 10. */
    private static final Map<String, Integer> VARIANT_MODELS = Map.of("ironbackpacks:basic/none", 1, "ironbackpacks:iron/storage", 2,
            "ironbackpacks:iron/upgrade", 3, "ironbackpacks:gold/storage", 4, "ironbackpacks:gold/upgrade", 5, "ironbackpacks:diamond/storage", 6,
            "ironbackpacks:diamond/upgrade", 7);
    /** Model override index of each upgrade, 0 = models/item/upgrade/null (blank); the property is index / 10. */
    private static final Map<String, Integer> UPGRADE_MODELS = Map.of("ironbackpacks:damage_bar", 1, "ironbackpacks:lock", 2,
            "ironbackpacks:extra_upgrade", 3, "ironbackpacks:everlasting", 4);

    /** End of every client tick (1.12 KeyInputEvent). */
    public static void onClientTick(Minecraft mc) {
        if (KEY_OPEN.consumeClick())
            Services.PLATFORM.sendToServer(new MessageRequestAction(RequestAction.OPEN_BACKPACK));
        else if (KEY_EQUIP.consumeClick() && false) // TODO - Use once backpack equipping has been fully implemented.
            Services.PLATFORM.sendToServer(new MessageRequestAction(RequestAction.EQUIP_BACKPACK));
        DevScene.onTick(mc);
    }

    /** Item property {@code ironbackpacks:variant}: which backpack model (the loaders register it at client setup; clamped to 0..1). */
    public static final ResourceLocation VARIANT_PROPERTY = RegistrarIronBackpacks.id("variant");
    public static final ClampedItemPropertyFunction VARIANT = (stack, level, entity, seed) -> {
        BackpackInfo.Data data = stack.get(RegistrarIronBackpacks.PACK_INFO.get());
        return data == null ? 0 : VARIANT_MODELS.getOrDefault(data.type() + "/" + data.spec().getName(), 0) / 10f;
    };
    /** Item property {@code ironbackpacks:upgrade}: which upgrade model. */
    public static final ResourceLocation UPGRADE_PROPERTY = RegistrarIronBackpacks.id("upgrade");
    public static final ClampedItemPropertyFunction UPGRADE = (stack, level, entity, seed) -> {
        ResourceLocation upgrade = stack.get(RegistrarIronBackpacks.UPGRADE_ID.get());
        return upgrade == null ? 0 : UPGRADE_MODELS.getOrDefault(upgrade.toString(), 0) / 10f;
    };

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
