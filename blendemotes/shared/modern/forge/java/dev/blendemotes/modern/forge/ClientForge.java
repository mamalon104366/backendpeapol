package dev.blendemotes.modern.forge;

import dev.blendemotes.modern.ModernEmotes;
import dev.blendemotes.modern.net.Payloads;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.event.EventNetworkChannel;

/** Client only part of the Forge entry point. */
final class ClientForge {
    private static boolean initialized;

    private ClientForge() {
    }

    static void init(IEventBus modBus, EventNetworkChannel channel) {
        modBus.addListener(ClientForge::onKeys);
        MinecraftForge.EVENT_BUS.addListener(ClientForge::onTick);
        channel.addListener(ClientForge::onClientPayload);
    }

    private static void onKeys(RegisterKeyMappingsEvent event) {
        event.register(ModernEmotes.KEY_WHEEL);
        event.register(ModernEmotes.KEY_MENU);
        event.register(ModernEmotes.KEY_STOP);
    }

    private static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!initialized) {
            initialized = true;
            ModernEmotes.initClient(payload -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.getConnection() != null) {
                    mc.getConnection().send(new ServerboundCustomPayloadPacket(Payloads.ID, Payloads.buffer(payload)));
                }
            });
        }
        ModernEmotes.tick(Minecraft.getInstance());
    }

    private static void onClientPayload(NetworkEvent.ClientCustomPayloadEvent event) {
        NetworkEvent.Context ctx = event.getSource().get();
        byte[] data = Payloads.bytes(event.getPayload());
        ctx.enqueueWork(() -> ModernEmotes.onPacket(data));
        ctx.setPacketHandled(true);
    }
}
