package gr8pefish.ironbackpacks.core;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import gr8pefish.ironbackpacks.api.IronBackpacksAPI;
import gr8pefish.ironbackpacks.api.backpack.BackpackInfo;
import gr8pefish.ironbackpacks.api.backpack.IBackpack;
import gr8pefish.ironbackpacks.api.upgrade.BackpackUpgrade;
import gr8pefish.ironbackpacks.api.upgrade.IUpgrade;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.entity.player.Player;

/**
 * Anvil: backpack + upgrade installs it; backpack + shears takes the last upgrade off. 1.12 did this in the
 * AnvilUpdateEvent / AnvilRepairEvent; the loaders call {@link #onAnvil} and {@link #onAnvilTake} from theirs (NeoForge
 * events, Fabric mixins into AnvilMenu).
 */
public class EventHandler {

    /** The anvil's result for these inputs: output, level cost, items used from the right slot. */
    public record AnvilResult(ItemStack output, int cost, int materialCost) {}

    @Nullable
    public static AnvilResult onAnvil(ItemStack left, ItemStack right) {
        ItemStack upgraded = tryUpgradeBackpack(left, right);
        if (!upgraded.isEmpty()) {
            return new AnvilResult(upgraded, 1, 1);
        } else if (right.getItem() instanceof ShearsItem) {
            ItemStack removed = tryRemoveUpgrade(left);
            if (!removed.isEmpty()) {
                return new AnvilResult(removed, 1, 0);
            }
        }
        return null;
    }

    /**
     * The result was taken (1.12 AnvilRepairEvent). Returns true when the anvil must not be damaged; when the shears
     * took an upgrade off, the backpack (without it) and the shears go back to the player.
     */
    public static boolean onAnvilTake(Player player, ItemStack left, ItemStack right, ItemStack output) {
        if (output.isEmpty() || !(output.getItem() instanceof IUpgrade))
            return false;

        // If we're working with backpacks, let's not damage the anvil

        if (left.isEmpty() || !(left.getItem() instanceof IBackpack))
            return true;

        if (right.isEmpty() || !(right.getItem() instanceof ShearsItem))
            return true;

        BackpackUpgrade upgrade = ((IUpgrade) output.getItem()).getUpgrade(output);
        IBackpack backpack = ((IBackpack) left.getItem());
        BackpackInfo info = backpack.getBackpackInfo(left);

        info.removeUpgrade(upgrade);
        backpack.updateBackpack(left, info);

        player.getInventory().placeItemBackInInventory(left);
        player.getInventory().placeItemBackInInventory(right);
        return true;
    }

    @Nonnull
    public static ItemStack tryUpgradeBackpack(ItemStack left, ItemStack right) {
        if (left.isEmpty() || !(left.getItem() instanceof IBackpack))
            return ItemStack.EMPTY;

        if (right.isEmpty() || !(right.getItem() instanceof IUpgrade))
            return ItemStack.EMPTY;

        ItemStack output = left.copy();

        IBackpack backpack = (IBackpack) output.getItem();
        BackpackInfo packInfo = backpack.getBackpackInfo(output);
        BackpackUpgrade upgrade = ((IUpgrade) right.getItem()).getUpgrade(right);

        if (upgrade.isNull() || packInfo.getVariant().getBackpackType().isNull())
            return ItemStack.EMPTY;

        if (packInfo.conflicts(upgrade))
            return ItemStack.EMPTY;

        if (packInfo.getMaxPoints() - packInfo.getPointsUsed() < upgrade.getApplicationCost())
            return ItemStack.EMPTY;

        if (packInfo.hasUpgrade(upgrade))
            return ItemStack.EMPTY;

        if (packInfo.getVariant().getBackpackType().getTier() < upgrade.getMinimumTier())
            return ItemStack.EMPTY;

        packInfo.addUpgrade(upgrade);
        backpack.updateBackpack(output, packInfo);

        return output;
    }

    @Nonnull
    public static ItemStack tryRemoveUpgrade(ItemStack left) {
        if (left.isEmpty() || !(left.getItem() instanceof IBackpack))
            return ItemStack.EMPTY;

        BackpackInfo packInfo = ((IBackpack) left.getItem()).getBackpackInfo(left);
        List<BackpackUpgrade> upgrades = packInfo.getUpgrades();
        if (upgrades.isEmpty())
            return ItemStack.EMPTY;

        BackpackUpgrade upgrade = upgrades.get(upgrades.size() - 1);
        return IronBackpacksAPI.getStack(upgrade);
    }
}
