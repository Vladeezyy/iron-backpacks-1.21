package gr8pefish.ironbackpacks.network;

import gr8pefish.ironbackpacks.core.RegistrarIronBackpacks;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record MessageRequestAction(int action) implements CustomPacketPayload {

    public static final Type<MessageRequestAction> TYPE = new Type<>(RegistrarIronBackpacks.id("request_action"));
    public static final StreamCodec<ByteBuf, MessageRequestAction> STREAM_CODEC = ByteBufCodecs.INT.map(MessageRequestAction::new, MessageRequestAction::action);

    public MessageRequestAction(RequestAction action) {
        this(action.ordinal());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MessageRequestAction message, IPayloadContext context) {
        RequestAction action = RequestAction.getAction(message.action());
        if (action == null)
            return;

        context.enqueueWork(() -> action.handle((ServerPlayer) context.player()));
    }
}
