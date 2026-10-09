package gr8pefish.ironbackpacks.core;

import java.util.List;

import javax.annotation.Nonnull;

import gr8pefish.ironbackpacks.api.IronBackpacksAPI;
import gr8pefish.ironbackpacks.api.backpack.BackpackInfo;
import gr8pefish.ironbackpacks.api.backpack.IBackpack;
import gr8pefish.ironbackpacks.api.upgrade.BackpackUpgrade;
import gr8pefish.ironbackpacks.api.upgrade.IUpgrade;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.entity.player.AnvilRepairEvent;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/** Anvil: backpack + upgrade installs it; backpack + shears takes the last upgrade off. */
public class EventHandler {

    @SubscribeEvent
    public static void onAnvil(AnvilUpdateEvent event) {
        ItemStack upgraded = tryUpgradeBackpack(event.getLeft(), event.getRight());
        if (!upgraded.isEmpty()) {
            event.setOutput(upgraded);
            event.setCost(1);
            event.setMaterialCost(1);
        } else if (event.getRight().getItem() instanceof ShearsItem) {
            ItemStack removed = tryRemoveUpgrade(event.getLeft());
            if (!removed.isEmpty()) {
                event.setOutput(removed);
                event.setCost(1);
                event.setMaterialCost(0);
            }
        }
    }

    @SubscribeEvent
    public static void onAnvilPost(AnvilRepairEvent event) {
        if (event.getOutput().isEmpty() || !(event.getOutput().getItem() instanceof IUpgrade))
            return;

        event.setBreakChance(0.0F); // If we're working with backpacks, let's not damage the anvil

        if (event.getLeft().isEmpty() || !(event.getLeft().getItem() instanceof IBackpack))
            return;

        if (event.getRight().isEmpty() || !(event.getRight().getItem() instanceof ShearsItem))
            return;

        BackpackUpgrade upgrade = ((IUpgrade) event.getOutput().getItem()).getUpgrade(event.getOutput());
        IBackpack backpack = ((IBackpack) event.getLeft().getItem());
        BackpackInfo info = backpack.getBackpackInfo(event.getLeft());

        info.removeUpgrade(upgrade);
        backpack.updateBackpack(event.getLeft(), info);

        ItemHandlerHelper.giveItemToPlayer(event.getEntity(), event.getLeft());
        ItemHandlerHelper.giveItemToPlayer(event.getEntity(), event.getRight());
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
