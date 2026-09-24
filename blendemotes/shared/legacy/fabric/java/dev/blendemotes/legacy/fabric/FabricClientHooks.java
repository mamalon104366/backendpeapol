package dev.blendemotes.legacy.fabric;

import dev.blendemotes.legacy.LegacyEmotes;
import dev.blendemotes.legacy.net.Payloads;
import net.minecraft.client.Minecraft;
//#if MC >= 11202
import net.minecraft.network.play.client.CPacketCustomPayload;
//#else
import net.minecraft.network.play.client.C17PacketCustomPayload;
//#endif

/** Called by the client mixins. */
public final class FabricClientHooks {
    private static boolean initialized;

    private FabricClientHooks() {
    }

    public static void tick() {
        if (!initialized) {
            initialized = true;
            LegacyEmotes.initClient(new LegacyEmotes.Sender() {
                @Override
                public void send(byte[] payload) {
                    Minecraft mc = Minecraft.getMinecraft();
                    //#if MC >= 11202
                    if (mc.getConnection() != null) {
                        mc.getConnection().sendPacket(new CPacketCustomPayload(Payloads.CHANNEL, Payloads.buffer(payload)));
                    }
                    //#else
                    if (mc.getNetHandler() != null) {
                        mc.getNetHandler().addToSendQueue(new C17PacketCustomPayload(Payloads.CHANNEL, Payloads.buffer(payload)));
                    }
                    //#endif
                }
            });
        }
        LegacyEmotes.tick();
    }
}
