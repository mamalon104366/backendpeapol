package dev.blendemotes.mc189.forge;

import dev.blendemotes.mc189.net.Payloads189;
import dev.blendemotes.mc189.net.ServerRelay189;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.server.MinecraftServer;
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

/** Forge 1.8.9 entry point. Vanilla clients may join servers running it. */
@Mod(modid = "blendemotes", name = "BlendEmotes", version = "1.0.0",
        acceptedMinecraftVersions = "[1.8.9]", acceptableRemoteVersions = "*")
public class BlendEmotesForge {
    static FMLEventChannel channel;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        channel = NetworkRegistry.INSTANCE.newEventDrivenChannel(Payloads189.CHANNEL);
        channel.register(new ServerPackets());
        FMLCommonHandler.instance().bus().register(new ServerEvents());
        ServerRelay189.setSender(new ServerRelay189.Sender() {
            @Override
            public void send(EntityPlayerMP player, byte[] payload) {
                channel.sendTo(new FMLProxyPacket(Payloads189.buffer(payload), Payloads189.CHANNEL), player);
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
            final EntityPlayerMP player = ((NetHandlerPlayServer) event.handler).playerEntity;
            final byte[] data = Payloads189.bytes(event.packet.payload());
            MinecraftServer.getServer().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    ServerRelay189.onPacket(player, data);
                }
            });
        }
    }

    public static class ServerEvents {
        @SubscribeEvent
        public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
            if (event.player instanceof EntityPlayerMP) {
                ServerRelay189.onLogout((EntityPlayerMP) event.player);
            }
        }
    }
}
