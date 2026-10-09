package gr8pefish.ironbackpacks.fabric.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import gr8pefish.ironbackpacks.core.EventHandler;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Fabric has no anvil events: the backpack + upgrade / shears results of core/EventHandler (1.12 AnvilUpdateEvent and
 * AnvilRepairEvent, NeoForge's events) are put into AnvilMenu here.
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin extends ItemCombinerMenu {
    @Shadow
    @Final
    private DataSlot cost;
    @Shadow
    private int repairItemCountCost;

    private AnvilMenuMixin(MenuType<?> type, int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(type, containerId, inventory, access);
    }

    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    private void ironbackpacks$createResult(CallbackInfo ci) {
        EventHandler.AnvilResult result = EventHandler.onAnvil(inputSlots.getItem(0), inputSlots.getItem(1));
        if (result != null) {
            resultSlots.setItem(0, result.output());
            cost.set(result.cost());
            repairItemCountCost = result.materialCost();
            broadcastChanges();
            ci.cancel();
        }
    }

    /** The result is taken: backpack and shears back, and the anvil isn't damaged (vanilla's 12% damage roll skipped). */
    @Inject(method = "onTake", at = @At("HEAD"), cancellable = true)
    private void ironbackpacks$onTake(Player player, ItemStack output, CallbackInfo ci) {
        ItemStack left = inputSlots.getItem(0);
        ItemStack right = inputSlots.getItem(1);
        if (!EventHandler.onAnvilTake(player, left, right, output))
            return;

        // vanilla onTake without the anvil damage
        if (!player.getAbilities().instabuild)
            player.giveExperienceLevels(-cost.get());
        inputSlots.setItem(0, ItemStack.EMPTY);
        if (repairItemCountCost > 0) {
            ItemStack material = inputSlots.getItem(1);
            if (!material.isEmpty() && material.getCount() > repairItemCountCost) {
                material.shrink(repairItemCountCost);
                inputSlots.setItem(1, material);
            } else {
                inputSlots.setItem(1, ItemStack.EMPTY);
            }
        } else {
            inputSlots.setItem(1, ItemStack.EMPTY);
        }
        cost.set(0);
        access.execute((level, pos) -> level.levelEvent(1030, pos, 0));
        ci.cancel();
    }
}
