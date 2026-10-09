package gr8pefish.ironbackpacks.network;

import gr8pefish.ironbackpacks.core.RegistrarIronBackpacks;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record MessageSetEquippedBackpack(ItemStack backpack) implements CustomPacketPayload {

    public static final Type<MessageSetEquippedBackpack> TYPE = new Type<>(RegistrarIronBackpacks.id("set_equipped_backpack"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MessageSetEquippedBackpack> STREAM_CODEC =
            ItemStack.OPTIONAL_STREAM_CODEC.map(MessageSetEquippedBackpack::new, MessageSetEquippedBackpack::backpack);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MessageSetEquippedBackpack message, IPayloadContext context) {
        context.enqueueWork(() -> EquippedBackpackClient.equipped = message.backpack());
    }
}
