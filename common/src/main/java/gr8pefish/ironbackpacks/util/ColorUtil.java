package gr8pefish.ironbackpacks.util;

import javax.annotation.Nonnull;

import gr8pefish.ironbackpacks.api.backpack.IBackpack;
import net.minecraft.world.item.ItemStack;

public class ColorUtil {

    /**
     * The dye colour of a stack: a dye item, or anything in a {@code c:dyes/<colour>} tag (1.12 DyeUtils.colorFromStack
     * with the ore dictionary; NeoForge's DyeColor.getColor does the same, Fabric has nothing like it). Null if none.
     */
    @javax.annotation.Nullable
    public static net.minecraft.world.item.DyeColor dyeColorOf(@Nonnull ItemStack stack) {
        if (stack.getItem() instanceof net.minecraft.world.item.DyeItem dye)
            return dye.getDyeColor();
        for (net.minecraft.world.item.DyeColor color : net.minecraft.world.item.DyeColor.values()) {
            var tag = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "dyes/" + color.getName()));
            if (stack.is(tag))
                return color;
        }
        return null;
    }

    /**
     * Gets the color for the specified backpack.
     * Does so via a direct call to the backpack's color data to ensure a performant lookup
     *
     * @param backpackStack - the backpack to target
     * @param tintindex - the tintindex, which refers to the layer# in the model json
     * @return - ARGB value as an {@link int} (1.21 item tints are opaque ARGB; -1 = untinted)
     */
    public static int getBackpackColor(ItemStack backpackStack, int tintindex) {
        if (tintindex < 1) return -1;
        int rgb = getBackpackColorFromStack(backpackStack);
        return rgb == -1 ? -1 : 0xFF000000 | rgb;
    }

    /**
     * Gets the RGB color of the backpack.
     *
     * @param stack - the backpack item stack to check
     * @return the RGB color, -1 if none
     */
    public static int getBackpackColorFromStack(@Nonnull ItemStack stack) {
        if (!stack.isEmpty() && stack.getItem() instanceof IBackpack) {
            return ((IBackpack) stack.getItem()).getBackpackColor(stack);
        }
        throw new RuntimeException("Tried to get backpack color from an ItemStack that isn't an IBackpack. Wrong item: "+stack);
    }

}
