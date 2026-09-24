package dev.blendemotes.modern.fabric;

import dev.blendemotes.modern.net.ServerRelay;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
//#if MC >= 12005
import dev.blendemotes.modern.net.EmotePayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
//#else
import dev.blendemotes.modern.net.Payloads;
//#endif

/** Fabric entry point (both sides): the emote relay. Logouts come from PlayerListMixin. */
public class BlendEmotesFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        //#if MC >= 12005
        PayloadTypeRegistry.playC2S().register(EmotePayload.TYPE, EmotePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(EmotePayload.TYPE, EmotePayload.CODEC);
        ServerRelay.setSender((player, payload) -> {
            if (ServerPlayNetworking.canSend(player, EmotePayload.TYPE)) {
                ServerPlayNetworking.send(player, new EmotePayload(payload));
            }
        });
        // handlers of the payload API run on the server thread
        ServerPlayNetworking.registerGlobalReceiver(EmotePayload.TYPE,
                (payload, context) -> ServerRelay.onPacket(context.player(), payload.data()));
        //#else
        ServerRelay.setSender((player, payload) -> {
            if (ServerPlayNetworking.canSend(player, Payloads.ID)) {
                ServerPlayNetworking.send(player, Payloads.ID, Payloads.buffer(payload));
            }
        });
        ServerPlayNetworking.registerGlobalReceiver(Payloads.ID, (server, player, handler, buf, responseSender) -> {
            byte[] data = Payloads.bytes(buf);
            server.execute(() -> ServerRelay.onPacket(player, data));
        });
        //#endif
    }
}
