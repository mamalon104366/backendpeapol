package dev.blendemotes.modern.fabric;

import dev.blendemotes.modern.ModernEmotes;
import net.fabricmc.api.ClientModInitializer;
//#if MC >= 260100
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
//#else
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
//#endif
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
        //#if MC >= 260100
        KeyMappingHelper.registerKeyMapping(ModernEmotes.KEY_WHEEL);
        KeyMappingHelper.registerKeyMapping(ModernEmotes.KEY_MENU);
        KeyMappingHelper.registerKeyMapping(ModernEmotes.KEY_STOP);
        //#else
        KeyBindingHelper.registerKeyBinding(ModernEmotes.KEY_WHEEL);
        KeyBindingHelper.registerKeyBinding(ModernEmotes.KEY_MENU);
        KeyBindingHelper.registerKeyBinding(ModernEmotes.KEY_STOP);
        //#endif
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
