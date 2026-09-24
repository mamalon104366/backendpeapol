//#if MC >= 12002
package dev.blendemotes.modern.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//#if MC >= 12005
import net.minecraft.network.codec.StreamCodec;
//#else
import net.minecraft.resources.ResourceLocation;
//#endif

/**
 * The emote channel's payload: the raw bytes of a core packet, without a length prefix, so every
 * loader (Fabric, Forge's event channels, NeoForge) reads and writes the same thing.
 */
public record EmotePayload(byte[] data) implements CustomPacketPayload {
    public static EmotePayload read(FriendlyByteBuf buf) {
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        return new EmotePayload(data);
    }

    //#if MC >= 12005
    public static final CustomPacketPayload.Type<EmotePayload> TYPE = new CustomPacketPayload.Type<>(Payloads.ID);
    public static final StreamCodec<FriendlyByteBuf, EmotePayload> CODEC =
            StreamCodec.of((buf, payload) -> buf.writeBytes(payload.data()), EmotePayload::read);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    //#else
    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBytes(data);
    }

    @Override
    public ResourceLocation id() {
        return Payloads.ID;
    }
    //#endif
}
//#endif
