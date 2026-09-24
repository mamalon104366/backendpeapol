package dev.blendemotes.mc189.fabric.mixin;

import dev.blendemotes.mc189.BlendEmotes189;
import dev.blendemotes.mc189.net.Payloads189;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.S3FPacketCustomPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayClient.class)
public abstract class NetHandlerPlayClientMixin {
    @Inject(method = "handleCustomPayload", at = @At("HEAD"), cancellable = true)
    private void blendemotes$payload(S3FPacketCustomPayload packet, CallbackInfo ci) {
        // the first call happens on the network thread and re-queues the packet: wait for the main thread
        if (Payloads189.CHANNEL.equals(packet.getChannelName()) && Minecraft.getMinecraft().isCallingFromMinecraftThread()) {
            BlendEmotes189.onPacket(Payloads189.bytes(packet.getBufferData()));
            ci.cancel();
        }
    }
}
