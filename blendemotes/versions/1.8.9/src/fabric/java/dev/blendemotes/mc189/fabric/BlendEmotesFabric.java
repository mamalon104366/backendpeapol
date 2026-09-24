package dev.blendemotes.mc189.fabric;

import dev.blendemotes.mc189.net.Payloads189;
import dev.blendemotes.mc189.net.ServerRelay189;
import net.fabricmc.api.ModInitializer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.S3FPacketCustomPayload;

/** Legacy Fabric entry point (both sides). */
public class BlendEmotesFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ServerRelay189.setSender(new ServerRelay189.Sender() {
            @Override
            public void send(EntityPlayerMP player, byte[] payload) {
                player.playerNetServerHandler.sendPacket(new S3FPacketCustomPayload(Payloads189.CHANNEL, Payloads189.buffer(payload)));
            }
        });
    }
}
