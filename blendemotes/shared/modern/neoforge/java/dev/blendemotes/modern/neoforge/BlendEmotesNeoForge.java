package dev.blendemotes.modern.neoforge;

import dev.blendemotes.modern.net.EmotePayload;
import dev.blendemotes.modern.net.ServerRelay;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.function.Consumer;
//#if MC >= 12005
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
//#else
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import dev.blendemotes.modern.net.Payloads;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
//#endif

/**
 * NeoForge entry point. The payload is optional: clients and servers without the mod can still
 * connect. Client ticks and logouts come from mixins (MinecraftMixin, PlayerListMixin).
 */
@Mod("blendemotes")
public class BlendEmotesNeoForge {
    /** Set by the client entry point; null on dedicated servers. */
    static volatile Consumer<byte[]> clientReceiver;

    public BlendEmotesNeoForge(IEventBus modBus) {
        modBus.addListener(BlendEmotesNeoForge::onRegisterPayloads);
        ServerRelay.setSender(BlendEmotesNeoForge::send);
        //#if MC < 12005
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientNeoForge.init(modBus);
        }
        //#endif
    }

    private static void toClient(byte[] data) {
        Consumer<byte[]> receiver = clientReceiver;
        if (receiver != null) {
            receiver.accept(data);
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
        IPayloadHandler<EmotePayload> handler = (payload, context) -> {
            if (context.flow() == PacketFlow.SERVERBOUND) {
                if (context.player() instanceof ServerPlayer) {
                    ServerPlayer player = (ServerPlayer) context.player();
                    context.enqueueWork(() -> ServerRelay.onPacket(player, payload.data()));
                }
            } else {
                context.enqueueWork(() -> toClient(payload.data()));
            }
        };
        //#if MC >= 12106
        // the client side handler is registered separately (the handler checks the direction)
        event.registrar("1").optional().playBidirectional(EmotePayload.TYPE, EmotePayload.CODEC, handler, handler);
        //#else
        event.registrar("1").optional().playBidirectional(EmotePayload.TYPE, EmotePayload.CODEC, handler);
        //#endif
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
            } else {
                context.workHandler().execute(() -> toClient(payload.data()));
            }
        });
    }
    //#endif
}
