package dev.blendemotes.legacy.net;

import dev.blendemotes.core.net.Packets;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketBuffer;

/** Byte helpers for the plugin channel. */
public final class Payloads {
    /** Wire name of the channel (at most 20 characters on 1.8). */
    public static final String CHANNEL = Packets.CHANNEL;

    private Payloads() {
    }

    public static byte[] bytes(ByteBuf buf) {
        byte[] data = new byte[buf.readableBytes()];
        buf.getBytes(buf.readerIndex(), data);
        return data;
    }

    public static PacketBuffer buffer(byte[] data) {
        return new PacketBuffer(Unpooled.wrappedBuffer(data));
    }
}
