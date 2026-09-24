package dev.blendemotes.modern.neoforge;

import dev.blendemotes.modern.net.EmotePayload;
import dev.blendemotes.modern.net.ServerRelay;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
//#if MC >= 12005
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
//#else
import dev.blendemotes.modern.net.Payloads;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
//#endif

/**
 * NeoForge entry point. The payload is optional: clients and servers without the mod can still
 * connect. Client ticks and logouts come from mixins (MinecraftMixin, PlayerListMixin).
 */
@Mod("blendemotes")
public class BlendEmotesNeoForge {
    public BlendEmotesNeoForge(IEventBus modBus) {
        modBus.addListener(BlendEmotesNeoForge::onRegisterPayloads);
        ServerRelay.setSender(BlendEmotesNeoForge::send);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientNeoForge.init(modBus);
        }
    }

    private static void send(ServerPlayer player, byte[] payload) {
        //#if MC >= 12005
        PacketDistributor.sendToPlayer(player, new EmotePayload(payload));
        //#else
        PacketDistributor.PLAYER.with(player).send(new EmotePayload(payload));
        //#endif
    }

    //#if MC >= 12005
    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playBidirectional(EmotePayload.TYPE, EmotePayload.CODEC, (payload, context) -> {
            if (context.flow() == PacketFlow.SERVERBOUND) {
                if (context.player() instanceof ServerPlayer) {
                    ServerPlayer player = (ServerPlayer) context.player();
                    context.enqueueWork(() -> ServerRelay.onPacket(player, payload.data()));
                }
            } else if (FMLEnvironment.dist == Dist.CLIENT) {
                context.enqueueWork(() -> ClientNeoForge.onPacket(payload.data()));
            }
        });
    }
    //#else
    private static void onRegisterPayloads(RegisterPayloadHandlerEvent event) {
        event.registrar("blendemotes").versioned("1").optional().play(Payloads.ID, EmotePayload::read, (payload, context) -> {
            if (context.flow() == PacketFlow.SERVERBOUND) {
                context.player().ifPresent(p -> {
                    if (p instanceof ServerPlayer) {
                        context.workHandler().execute(() -> ServerRelay.onPacket((ServerPlayer) p, payload.data()));
                    }
                });
            } else if (FMLEnvironment.dist == Dist.CLIENT) {
                context.workHandler().execute(() -> ClientNeoForge.onPacket(payload.data()));
            }
        });
    }
    //#endif
}
