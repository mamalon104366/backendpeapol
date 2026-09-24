package dev.blendemotes.modern.fabric;

import dev.blendemotes.modern.ModernEmotes;
import dev.blendemotes.modern.net.Payloads;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Fabric client entry point. */
public class BlendEmotesFabricClient implements ClientModInitializer {
    private static boolean initialized;

    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(ModernEmotes.KEY_WHEEL);
        KeyBindingHelper.registerKeyBinding(ModernEmotes.KEY_MENU);
        KeyBindingHelper.registerKeyBinding(ModernEmotes.KEY_STOP);
        ClientPlayNetworking.registerGlobalReceiver(Payloads.ID, (client, handler, buf, responseSender) -> {
            byte[] data = Payloads.bytes(buf);
            client.execute(() -> ModernEmotes.onPacket(data));
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!initialized) {
                initialized = true;
                ModernEmotes.initClient(payload -> ClientPlayNetworking.send(Payloads.ID, Payloads.buffer(payload)));
            }
            ModernEmotes.tick(client);
        });
    }
}
