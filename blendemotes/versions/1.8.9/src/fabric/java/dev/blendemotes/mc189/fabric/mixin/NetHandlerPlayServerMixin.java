package dev.blendemotes.mc189.fabric.mixin;

import dev.blendemotes.mc189.net.Payloads189;
import dev.blendemotes.mc189.net.ServerRelay189;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayServer.class)
public abstract class NetHandlerPlayServerMixin {
    @Shadow
    public EntityPlayerMP playerEntity;

    @Inject(method = "processVanilla250Packet", at = @At("HEAD"), cancellable = true)
    private void blendemotes$payload(C17PacketCustomPayload packet, CallbackInfo ci) {
        if (Payloads189.CHANNEL.equals(packet.getChannelName()) && MinecraftServer.getServer().isCallingFromMinecraftThread()) {
            ServerRelay189.onPacket(playerEntity, Payloads189.bytes(packet.getBufferData()));
            ci.cancel();
        }
    }
}
