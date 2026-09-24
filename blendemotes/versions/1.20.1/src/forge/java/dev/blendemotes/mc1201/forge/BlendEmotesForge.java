package dev.blendemotes.mc1201.forge;

import dev.blendemotes.mc1201.net.Payloads;
import dev.blendemotes.mc1201.net.ServerRelay;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.event.EventNetworkChannel;

/** Forge entry point. Clients and servers without the mod can still connect. */
@Mod("blendemotes")
public class BlendEmotesForge {
    static EventNetworkChannel channel;

    public BlendEmotesForge() {
        channel = NetworkRegistry.newEventChannel(Payloads.ID, () -> "1",
                NetworkRegistry.acceptMissingOr("1"), NetworkRegistry.acceptMissingOr("1"));
        channel.addListener(BlendEmotesForge::onServerPayload);
        MinecraftForge.EVENT_BUS.addListener(BlendEmotesForge::onLogout);
        ServerRelay.setSender((player, payload) -> player.connection.send(new ClientboundCustomPayloadPacket(Payloads.ID, Payloads.buffer(payload))));
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientForge.init(FMLJavaModLoadingContext.get().getModEventBus(), channel);
        }
    }

    private static void onServerPayload(NetworkEvent.ServerCustomPayloadEvent event) {
        NetworkEvent.Context ctx = event.getSource().get();
        ServerPlayer player = ctx.getSender();
        byte[] data = Payloads.bytes(event.getPayload());
        if (player != null) {
            ctx.enqueueWork(() -> ServerRelay.onPacket(player, data));
        }
        ctx.setPacketHandled(true);
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer) {
            ServerRelay.onLogout((ServerPlayer) event.getEntity());
        }
    }
}
