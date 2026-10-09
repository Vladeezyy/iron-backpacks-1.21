package gr8pefish.ironbackpacks.util;

import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Lists;

import gr8pefish.ironbackpacks.api.backpack.BackpackInfo;
import gr8pefish.ironbackpacks.api.backpack.IBackpack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

// Because I don't know where else to put things
public class Utils {

    /**
     * Finds an {@link IBackpack} in the player's inventory. Allows filtering.
     *
     * Result definitions:
     *
     * <ul>
     *     <li>{@link InteractionResult#FAIL} - No backpack was found for the player.</li>
     *     <li>{@link InteractionResult#SUCCESS} - A backpack was found and it was on the hotbar.</li>
     *     <li>{@link InteractionResult#PASS} - A backpack was found and it was not on the hotbar.</li>
     * </ul>
     *
     * The slots are searched in the order of the 1.12 player item handler: the 36 main slots, then armor, then the
     * offhand.
     *
     * @param player       - The player to find a backpack for
     * @param requirements - Requirements the backpack must meet, such as having a specific upgrade
     * @return an InteractionResultHolder containing information on the discovered stack
     */
    // TODO - Look for equipped backpack when implemented
    @Nonnull
    public static InteractionResultHolder<ItemStack> getBackpack(@Nonnull Player player, @Nonnull Predicate<Pair<ItemStack, IBackpack>> requirements) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack foundStack = inventory.getItem(slot);
            if (!foundStack.isEmpty() && foundStack.getItem() instanceof IBackpack && requirements.test(Pair.of(foundStack, (IBackpack) foundStack.getItem()))) {
                InteractionResult result = slot < 9 ? InteractionResult.SUCCESS : InteractionResult.PASS;
                return new InteractionResultHolder<>(result, foundStack);
            }
        }

        return InteractionResultHolder.fail(ItemStack.EMPTY);
    }

    /**
     * Finds any {@link IBackpack} in the player's inventory.
     *
     * @see #getBackpack(Player, Predicate)
     */
    @Nonnull
    public static InteractionResultHolder<ItemStack> getBackpack(@Nonnull Player player) {
        return getBackpack(player, Predicates.alwaysTrue());
    }

    @Nonnull
    public static List<InteractionResultHolder<ItemStack>> getAllBackpacks(@Nonnull Player player, @Nonnull Predicate<Pair<ItemStack, IBackpack>> requirements) {
        Inventory inventory = player.getInventory();
        List<InteractionResultHolder<ItemStack>> backpacks = Lists.newArrayList();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack foundStack = inventory.getItem(slot);
            if (!foundStack.isEmpty() && foundStack.getItem() instanceof IBackpack && requirements.test(Pair.of(foundStack, (IBackpack) foundStack.getItem()))) {
                InteractionResult result = slot < 9 ? InteractionResult.SUCCESS : InteractionResult.PASS;
                backpacks.add(new InteractionResultHolder<>(result, foundStack));
            }
        }

        return backpacks;
    }

    @Nonnull
    public static List<InteractionResultHolder<ItemStack>> getAllBackpacks(@Nonnull Player player) {
        return getAllBackpacks(player, Predicates.alwaysTrue());
    }

    /**
     * Gets the {@link BackpackInfo} from a given {@link ItemStack}.
     * NOTE: The ItemStack *must* be a backpack, or it will (intentionally) crash.
     *
     * @param stack - the backpack as an item stack
     * @return - the backpack info
     */
    @Nonnull
    public static BackpackInfo getBackpackInfoFromStack(@Nonnull ItemStack stack) {
        if (!stack.isEmpty() && stack.getItem() instanceof IBackpack) {
            return ((IBackpack) stack.getItem()).getBackpackInfo(stack);
        }
        throw new RuntimeException("Tried to get backpack info from an ItemStack that isn't an IBackpack. Wrong item: "+stack);
    }

    /**
     * Gets the backpack to open from a player's inventory.
     * Checks the selected slot first, then the offhand then the hotbar (left to right) then the main 3 inventory slots (left to right, top to bottom)
     *
     * @param player - the player to check
     * @param hand - the current selected hand
     * @return - an ItemStack containing the backpack, {@link ItemStack#EMPTY} if none found
     */
    @Nonnull
    public static ItemStack getNonequippedBackpackFromInventory(Player player, InteractionHand hand) {
        //check current item first
        ItemStack held = player.getItemInHand(hand);
        if (!held.isEmpty() && held.getItem() instanceof IBackpack) {
            return held;
        //loop through inventory second
        } else {
            //check offhand
            ItemStack offhand = player.getItemInHand(InteractionHand.OFF_HAND);
            if (!offhand.isEmpty() && offhand.getItem() instanceof IBackpack)
                return offhand;

            //check main inventory (hotbar first left to right, then main inventory)
            return getBackpack(player).getObject();
        }
    }
}
