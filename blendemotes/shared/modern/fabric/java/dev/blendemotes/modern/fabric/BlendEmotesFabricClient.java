package dev.blendemotes.modern.fabric;

import dev.blendemotes.modern.ModernEmotes;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
//#if MC >= 12005
import dev.blendemotes.modern.net.EmotePayload;
//#else
import dev.blendemotes.modern.net.Payloads;
//#endif

/** Fabric client entry point. Client ticks come from MinecraftMixin. */
public class BlendEmotesFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(ModernEmotes.KEY_WHEEL);
        KeyBindingHelper.registerKeyBinding(ModernEmotes.KEY_MENU);
        KeyBindingHelper.registerKeyBinding(ModernEmotes.KEY_STOP);
        //#if MC >= 12005
        // handlers of the payload API run on the client thread
        ClientPlayNetworking.registerGlobalReceiver(EmotePayload.TYPE,
                (payload, context) -> ModernEmotes.onPacket(payload.data()));
        ModernEmotes.setSender(payload -> {
            if (ClientPlayNetworking.canSend(EmotePayload.TYPE)) {
                ClientPlayNetworking.send(new EmotePayload(payload));
            }
        });
        //#else
        ClientPlayNetworking.registerGlobalReceiver(Payloads.ID, (client, handler, buf, responseSender) -> {
            byte[] data = Payloads.bytes(buf);
            client.execute(() -> ModernEmotes.onPacket(data));
        });
        ModernEmotes.setSender(payload -> {
            if (ClientPlayNetworking.canSend(Payloads.ID)) {
                ClientPlayNetworking.send(Payloads.ID, Payloads.buffer(payload));
            }
        });
        //#endif
    }
}
