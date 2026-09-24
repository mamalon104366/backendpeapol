package dev.blendemotes.modern.neoforge;

import dev.blendemotes.modern.ModernEmotes;
import dev.blendemotes.modern.net.EmotePayload;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
//#if MC >= 12005
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
//#else
import net.neoforged.neoforge.network.PacketDistributor;
//#endif

/** Client only part of the NeoForge entry point. */
final class ClientNeoForge {
    private ClientNeoForge() {
    }

    static void init(IEventBus modBus) {
        modBus.addListener(ClientNeoForge::onKeys);
        ModernEmotes.setSender(payload -> {
            //#if MC >= 12005
            if (Minecraft.getInstance().getConnection() != null) {
                Minecraft.getInstance().getConnection().send(new ServerboundCustomPayloadPacket(new EmotePayload(payload)));
            }
            //#else
            PacketDistributor.SERVER.noArg().send(new EmotePayload(payload));
            //#endif
        });
    }

    private static void onKeys(RegisterKeyMappingsEvent event) {
        event.register(ModernEmotes.KEY_WHEEL);
        event.register(ModernEmotes.KEY_MENU);
        event.register(ModernEmotes.KEY_STOP);
    }

    static void onPacket(byte[] data) {
        ModernEmotes.onPacket(data);
    }
}
