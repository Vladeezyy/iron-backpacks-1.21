package gr8pefish.ironbackpacks.network;

import javax.annotation.Nullable;

import gr8pefish.ironbackpacks.api.backpack.IBackpack;
import gr8pefish.ironbackpacks.container.ContainerBackpack;
import gr8pefish.ironbackpacks.core.RegistrarIronBackpacks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.wrapper.PlayerInvWrapper;
import net.neoforged.neoforge.network.PacketDistributor;

public enum RequestAction {

    EQUIP_BACKPACK {
        @Override
        public void handle(ServerPlayer player) {
            ItemStack equipped = player.getData(RegistrarIronBackpacks.EQUIPPED_BACKPACK);
            ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
            if (!equipped.isEmpty()) {
                ItemStack remainder = ItemHandlerHelper.insertItem(new PlayerInvWrapper(player.getInventory()), equipped, false);
                if (remainder.isEmpty()) {
                    PacketDistributor.sendToPlayer(player, new MessageSetEquippedBackpack(ItemStack.EMPTY));
                    player.setData(RegistrarIronBackpacks.EQUIPPED_BACKPACK, ItemStack.EMPTY);
                }
            }

            if (!held.isEmpty() && held.getItem() instanceof IBackpack) {
                player.setData(RegistrarIronBackpacks.EQUIPPED_BACKPACK, held);
                PacketDistributor.sendToPlayer(player, new MessageSetEquippedBackpack(player.getData(RegistrarIronBackpacks.EQUIPPED_BACKPACK)));
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            }
        }
    },
    OPEN_BACKPACK {
        @Override
        public void handle(ServerPlayer player) {
            //get the equipped backpack
            ItemStack equipped = player.getData(RegistrarIronBackpacks.EQUIPPED_BACKPACK);

            //has a backpack equipped, open that (and update elsewhere)
            if (!equipped.isEmpty()) {
                PacketDistributor.sendToPlayer(player, new MessageSetEquippedBackpack(equipped));
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
