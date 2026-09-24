package dev.blendemotes.legacy.fabric.mixin;

import dev.blendemotes.legacy.LegacyEmotes;
import dev.blendemotes.legacy.net.Payloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
//#if MC >= 11202
import net.minecraft.network.play.server.SPacketCustomPayload;
//#else
import net.minecraft.network.play.server.S3FPacketCustomPayload;
//#endif
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayClient.class)
public abstract class NetHandlerPlayClientMixin {
    @Inject(method = "handleCustomPayload", at = @At("HEAD"), cancellable = true)
    //#if MC >= 11202
    private void blendemotes$payload(SPacketCustomPayload packet, CallbackInfo ci) {
    //#else
    private void blendemotes$payload(S3FPacketCustomPayload packet, CallbackInfo ci) {
    //#endif
        // the first call happens on the network thread and re-queues the packet: wait for the main thread
        if (Payloads.CHANNEL.equals(packet.getChannelName()) && Minecraft.getMinecraft().isCallingFromMinecraftThread()) {
            LegacyEmotes.onPacket(Payloads.bytes(packet.getBufferData()));
            ci.cancel();
        }
    }
}
