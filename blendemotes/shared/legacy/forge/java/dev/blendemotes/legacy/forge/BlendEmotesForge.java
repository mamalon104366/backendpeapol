package dev.blendemotes.legacy.forge;

import dev.blendemotes.legacy.net.Payloads;
import dev.blendemotes.legacy.net.ServerRelay;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import dev.blendemotes.legacy.Compat;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.network.FMLEventChannel;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.relauncher.Side;

/** Forge entry point (1.8.9 / 1.12.2). Vanilla clients may join servers running it. */
//#if MC >= 11202
@Mod(modid = "blendemotes", name = "BlendEmotes", version = "1.0.0",
        acceptedMinecraftVersions = "[1.12.2]", acceptableRemoteVersions = "*")
//#else
@Mod(modid = "blendemotes", name = "BlendEmotes", version = "1.0.0",
        acceptedMinecraftVersions = "[1.8.9]", acceptableRemoteVersions = "*")
//#endif
public class BlendEmotesForge {
    static FMLEventChannel channel;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        channel = NetworkRegistry.INSTANCE.newEventDrivenChannel(Payloads.CHANNEL);
        channel.register(new ServerPackets());
        FMLCommonHandler.instance().bus().register(new ServerEvents());
        ServerRelay.setSender(new ServerRelay.Sender() {
            @Override
            public void send(EntityPlayerMP player, byte[] payload) {
                channel.sendTo(new FMLProxyPacket(Payloads.buffer(payload), Payloads.CHANNEL), player);
            }
        });
        if (event.getSide() == Side.CLIENT) {
            ClientForge.init(channel);
        }
    }

    /** Server side packet handler (runs on the network thread, work goes to the server thread). */
    public static class ServerPackets {
        @SubscribeEvent
        public void onServerPacket(FMLNetworkEvent.ServerCustomPacketEvent event) {
            //#if MC >= 11202
            final EntityPlayerMP player = ((NetHandlerPlayServer) event.getHandler()).player;
            final byte[] data = Payloads.bytes(event.getPacket().payload());
            //#else
            final EntityPlayerMP player = ((NetHandlerPlayServer) event.handler).playerEntity;
            final byte[] data = Payloads.bytes(event.packet.payload());
            //#endif
            Compat.server(player).addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    ServerRelay.onPacket(player, data);
                }
            });
        }
    }

    public static class ServerEvents {
        @SubscribeEvent
        public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
            if (event.player instanceof EntityPlayerMP) {
                ServerRelay.onLogout((EntityPlayerMP) event.player);
            }
        }
    }
}
