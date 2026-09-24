package dev.blendemotes.legacy.forge;

import dev.blendemotes.legacy.LegacyEmotes;
import dev.blendemotes.legacy.net.Payloads;
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
        LegacyEmotes.initClient(new LegacyEmotes.Sender() {
            @Override
            public void send(byte[] payload) {
                channel.sendToServer(new FMLProxyPacket(Payloads.buffer(payload), Payloads.CHANNEL));
            }
        });
        FMLCommonHandler.instance().bus().register(new Events());
        channel.register(new Packets());
    }

    public static class Events {
        @SubscribeEvent
        public void onTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                LegacyEmotes.tick();
            }
        }
    }

    public static class Packets {
        @SubscribeEvent
        public void onClientPacket(FMLNetworkEvent.ClientCustomPacketEvent event) {
            //#if MC >= 11202
            final byte[] data = Payloads.bytes(event.getPacket().payload());
            //#else
            final byte[] data = Payloads.bytes(event.packet.payload());
            //#endif
            Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    LegacyEmotes.onPacket(data);
                }
            });
        }
    }
}
