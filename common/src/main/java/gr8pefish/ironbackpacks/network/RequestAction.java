package gr8pefish.ironbackpacks.network;

import javax.annotation.Nullable;

import gr8pefish.ironbackpacks.api.backpack.IBackpack;
import gr8pefish.ironbackpacks.container.ContainerBackpack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import gr8pefish.ironbackpacks.platform.Services;

public enum RequestAction {

    EQUIP_BACKPACK {
        @Override
        public void handle(ServerPlayer player) {
            ItemStack equipped = Services.PLATFORM.getEquippedBackpack(player);
            ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
            if (!equipped.isEmpty()) {
                // back into the inventory if it fits (1.12 inserted it into the player's item handler)
                if (player.getInventory().getFreeSlot() >= 0 && player.getInventory().add(equipped.copy())) {
                    Services.PLATFORM.sendToPlayer(player, new MessageSetEquippedBackpack(ItemStack.EMPTY));
                    Services.PLATFORM.setEquippedBackpack(player, ItemStack.EMPTY);
                }
            }

            if (!held.isEmpty() && held.getItem() instanceof IBackpack) {
                Services.PLATFORM.setEquippedBackpack(player, held);
                Services.PLATFORM.sendToPlayer(player, new MessageSetEquippedBackpack(Services.PLATFORM.getEquippedBackpack(player)));
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            }
        }
    },
    OPEN_BACKPACK {
        @Override
        public void handle(ServerPlayer player) {
            //get the equipped backpack
            ItemStack equipped = Services.PLATFORM.getEquippedBackpack(player);

            //has a backpack equipped, open that (and update elsewhere)
            if (!equipped.isEmpty()) {
                Services.PLATFORM.sendToPlayer(player, new MessageSetEquippedBackpack(equipped));
                ContainerBackpack.open(player, ContainerBackpack.Mode.EQUIPPED, InteractionHand.OFF_HAND);

            //no backpack equipped, open any pack in the inventory
            } else {
                ContainerBackpack.open(player, ContainerBackpack.Mode.INVENTORY, InteractionHand.OFF_HAND);
            }
        }
    },
    ;

    private static final RequestAction[] VALUES = values();

    public abstract void handle(ServerPlayer player);

    @Nullable
    public static RequestAction getAction(int index) {
        if (index < 0 || index >= VALUES.length)
            return null;

        return VALUES[index];
    }
}
