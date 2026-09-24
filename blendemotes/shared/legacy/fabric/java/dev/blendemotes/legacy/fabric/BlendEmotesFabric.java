package dev.blendemotes.legacy.fabric;

import dev.blendemotes.legacy.net.Payloads;
import dev.blendemotes.legacy.net.ServerRelay;
import net.fabricmc.api.ModInitializer;
import net.minecraft.entity.player.EntityPlayerMP;
//#if MC >= 11202
import net.minecraft.network.play.server.SPacketCustomPayload;
//#else
import net.minecraft.network.play.server.S3FPacketCustomPayload;
//#endif

/** Legacy Fabric entry point (both sides). */
public class BlendEmotesFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ServerRelay.setSender(new ServerRelay.Sender() {
            @Override
            public void send(EntityPlayerMP player, byte[] payload) {
                //#if MC >= 11202
                player.connection.sendPacket(new SPacketCustomPayload(Payloads.CHANNEL, Payloads.buffer(payload)));
                //#else
                player.playerNetServerHandler.sendPacket(new S3FPacketCustomPayload(Payloads.CHANNEL, Payloads.buffer(payload)));
                //#endif
            }
        });
    }
}
