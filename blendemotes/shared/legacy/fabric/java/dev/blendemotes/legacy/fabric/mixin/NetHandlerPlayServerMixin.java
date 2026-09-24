package dev.blendemotes.legacy.fabric.mixin;

import dev.blendemotes.legacy.net.Payloads;
import dev.blendemotes.legacy.net.ServerRelay;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
//#if MC >= 11202
import net.minecraft.network.play.client.CPacketCustomPayload;
//#else
import net.minecraft.network.play.client.C17PacketCustomPayload;
//#endif
import dev.blendemotes.legacy.Compat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayServer.class)
public abstract class NetHandlerPlayServerMixin {
    //#if MC >= 11202
    @Shadow
    public EntityPlayerMP player;

    @Inject(method = "processCustomPayload", at = @At("HEAD"), cancellable = true)
    private void blendemotes$payload(CPacketCustomPayload packet, CallbackInfo ci) {
        if (Payloads.CHANNEL.equals(packet.getChannelName()) && Compat.server(player).isCallingFromMinecraftThread()) {
            ServerRelay.onPacket(player, Payloads.bytes(packet.getBufferData()));
            ci.cancel();
        }
    }
    //#else
    @Shadow
    public EntityPlayerMP playerEntity;

    @Inject(method = "processVanilla250Packet", at = @At("HEAD"), cancellable = true)
    private void blendemotes$payload(C17PacketCustomPayload packet, CallbackInfo ci) {
        if (Payloads.CHANNEL.equals(packet.getChannelName()) && Compat.server(playerEntity).isCallingFromMinecraftThread()) {
            ServerRelay.onPacket(playerEntity, Payloads.bytes(packet.getBufferData()));
            ci.cancel();
        }
    }
    //#endif
}
