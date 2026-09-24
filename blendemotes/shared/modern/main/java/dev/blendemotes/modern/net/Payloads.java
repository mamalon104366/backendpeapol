package dev.blendemotes.modern.net;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** Plugin channel helpers. */
public final class Payloads {
    public static final ResourceLocation ID = new ResourceLocation("blendemotes", "main");

    private Payloads() {
    }

    public static byte[] bytes(ByteBuf buf) {
        byte[] data = new byte[buf.readableBytes()];
        buf.getBytes(buf.readerIndex(), data);
        return data;
    }

    public static FriendlyByteBuf buffer(byte[] data) {
        return new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
    }
}
