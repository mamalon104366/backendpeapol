package dev.blendemotes.modern.forge;

import dev.blendemotes.modern.ModernEmotes;
import dev.blendemotes.modern.net.Payloads;
//#if MC >= 12002
import net.minecraftforge.network.EventNetworkChannel;
import net.minecraftforge.network.PacketDistributor;
//#else
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
//#endif
//#if MC >= 12106
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
//#elseif MC >= 11900
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
//#else
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
//#endif
//#if MC >= 12002
//#elseif MC >= 11700
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.event.EventNetworkChannel;
//#else
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.event.EventNetworkChannel;
//#endif
//#if MC >= 11700 && MC < 11900
import net.minecraftforge.client.ClientRegistry;
//#elseif MC < 11700
import net.minecraftforge.fml.client.registry.ClientRegistry;
//#endif

/** Client only part of the Forge entry point. */
final class ClientForge {
    private ClientForge() {
    }

    static void init(EventNetworkChannel channel) {
        //#if MC >= 12106
        RegisterKeyMappingsEvent.getBus(FMLJavaModLoadingContext.get().getModBusGroup()).addListener(ClientForge::onKeys);
        //#elseif MC >= 11900
        FMLJavaModLoadingContext.get().getModEventBus().addListener(ClientForge::onKeys);
        //#else
        FMLJavaModLoadingContext.get().getModEventBus().addListener(ClientForge::onSetup);
        //#endif
        //#if MC >= 12002
        ModernEmotes.setSender(payload -> channel.send(Payloads.buffer(payload), PacketDistributor.SERVER.noArg()));
        //#else
        channel.addListener(ClientForge::onClientPayload);
        ModernEmotes.setSender(payload -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getConnection() != null) {
                mc.getConnection().send(new ServerboundCustomPayloadPacket(Payloads.ID, Payloads.buffer(payload)));
            }
        });
        //#endif
    }

    //#if MC >= 11900
    private static void onKeys(RegisterKeyMappingsEvent event) {
        event.register(ModernEmotes.KEY_WHEEL);
        event.register(ModernEmotes.KEY_MENU);
        event.register(ModernEmotes.KEY_STOP);
    }
    //#else
    private static void onSetup(FMLClientSetupEvent event) {
        ClientRegistry.registerKeyBinding(ModernEmotes.KEY_WHEEL);
        ClientRegistry.registerKeyBinding(ModernEmotes.KEY_MENU);
        ClientRegistry.registerKeyBinding(ModernEmotes.KEY_STOP);
    }
    //#endif

    static void onPacket(byte[] data) {
        ModernEmotes.onPacket(data);
    }

    //#if MC < 12002
    private static void onClientPayload(NetworkEvent.ClientCustomPayloadEvent event) {
        NetworkEvent.Context ctx = event.getSource().get();
        byte[] data = Payloads.bytes(event.getPayload());
        ctx.enqueueWork(() -> ModernEmotes.onPacket(data));
        ctx.setPacketHandled(true);
    }
    //#endif
}
