package dev.blendemotes.mc189.fabric;

import dev.blendemotes.mc189.BlendEmotes189;
import dev.blendemotes.mc189.net.Payloads189;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.C17PacketCustomPayload;

/** Called by the client mixins. */
public final class FabricClientHooks {
    private static boolean initialized;

    private FabricClientHooks() {
    }

    public static void tick() {
        if (!initialized) {
            initialized = true;
            BlendEmotes189.initClient(new BlendEmotes189.Sender() {
                @Override
                public void send(byte[] payload) {
                    Minecraft mc = Minecraft.getMinecraft();
                    if (mc.getNetHandler() != null) {
                        mc.getNetHandler().addToSendQueue(new C17PacketCustomPayload(Payloads189.CHANNEL, Payloads189.buffer(payload)));
                    }
                }
            });
        }
        BlendEmotes189.tick();
    }
}
