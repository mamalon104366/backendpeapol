package dev.blendemotes.modern.forge;

import dev.blendemotes.modern.net.Payloads;
import dev.blendemotes.modern.net.ServerRelay;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
//#if MC >= 12002
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.EventNetworkChannel;
import net.minecraftforge.network.PacketDistributor;
//#elseif MC >= 11700
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.event.EventNetworkChannel;
//#else
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.event.EventNetworkChannel;
//#endif

/**
 * Forge entry point. The channel is optional: clients and servers without the mod can still
 * connect. Client ticks and logouts come from mixins (MinecraftMixin, PlayerListMixin).
 */
@Mod("blendemotes")
public class BlendEmotesForge {
    static EventNetworkChannel channel;

    public BlendEmotesForge() {
        //#if MC >= 12002
        channel = ChannelBuilder.named(Payloads.ID).optional().eventNetworkChannel();
        channel.addListener(BlendEmotesForge::onPayload);
        ServerRelay.setSender((player, payload) -> channel.send(Payloads.buffer(payload), PacketDistributor.PLAYER.with(player)));
        //#else
        channel = NetworkRegistry.newEventChannel(Payloads.ID, () -> "1", version -> true, version -> true);
        channel.addListener(BlendEmotesForge::onServerPayload);
        ServerRelay.setSender((player, payload) -> player.connection.send(new ClientboundCustomPayloadPacket(Payloads.ID, Payloads.buffer(payload))));
        //#endif
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientForge.init(channel);
        }
    }

    //#if MC >= 12002
    private static void onPayload(CustomPayloadEvent event) {
        CustomPayloadEvent.Context ctx = event.getSource();
        byte[] data = Payloads.bytes(event.getPayload());
        if (ctx.isServerSide()) {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                ctx.enqueueWork(() -> ServerRelay.onPacket(player, data));
            }
        } else if (FMLEnvironment.dist == Dist.CLIENT) {
            ctx.enqueueWork(() -> ClientForge.onPacket(data));
        }
        ctx.setPacketHandled(true);
    }
    //#else
    private static void onServerPayload(NetworkEvent.ServerCustomPayloadEvent event) {
        NetworkEvent.Context ctx = event.getSource().get();
        ServerPlayer player = ctx.getSender();
        byte[] data = Payloads.bytes(event.getPayload());
        if (player != null) {
            ctx.enqueueWork(() -> ServerRelay.onPacket(player, data));
        }
        ctx.setPacketHandled(true);
    }
    //#endif
}
