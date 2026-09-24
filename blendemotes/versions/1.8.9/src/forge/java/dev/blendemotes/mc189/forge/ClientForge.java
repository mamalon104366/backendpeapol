package dev.blendemotes.mc189.forge;

import dev.blendemotes.mc189.BlendEmotes189;
import dev.blendemotes.mc189.net.Payloads189;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLEventChannel;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;

/** Client only part of the Forge entry point (never loaded on dedicated servers). */
final class ClientForge {
    private ClientForge() {
    }

    static void init(final FMLEventChannel channel) {
        BlendEmotes189.initClient(new BlendEmotes189.Sender() {
            @Override
            public void send(byte[] payload) {
                channel.sendToServer(new FMLProxyPacket(Payloads189.buffer(payload), Payloads189.CHANNEL));
            }
        });
        FMLCommonHandler.instance().bus().register(new Events());
        channel.register(new Packets());
    }

    public static class Events {
        @SubscribeEvent
        public void onTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                BlendEmotes189.tick();
            }
        }
    }

    public static class Packets {
        @SubscribeEvent
        public void onClientPacket(FMLNetworkEvent.ClientCustomPacketEvent event) {
            final byte[] data = Payloads189.bytes(event.packet.payload());
            Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    BlendEmotes189.onPacket(data);
                }
            });
        }
    }
}
