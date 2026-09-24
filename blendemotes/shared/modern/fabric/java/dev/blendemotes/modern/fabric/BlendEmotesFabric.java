package dev.blendemotes.modern.fabric;

import dev.blendemotes.modern.net.Payloads;
import dev.blendemotes.modern.net.ServerRelay;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/** Fabric entry point (both sides): the emote relay. */
public class BlendEmotesFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ServerRelay.setSender((player, payload) -> ServerPlayNetworking.send(player, Payloads.ID, Payloads.buffer(payload)));
        ServerPlayNetworking.registerGlobalReceiver(Payloads.ID, (server, player, handler, buf, responseSender) -> {
            byte[] data = Payloads.bytes(buf);
            server.execute(() -> ServerRelay.onPacket(player, data));
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> ServerRelay.onLogout(handler.player));
    }
}
