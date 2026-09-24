package dev.blendemotes.legacy.net;

import dev.blendemotes.core.net.EmoteServer;
import dev.blendemotes.legacy.Compat;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Server side emote relay (dedicated servers and the singleplayer/LAN server). */
public final class ServerRelay {
    private static final Logger LOGGER = LogManager.getLogger("BlendEmotes");

    /** Sends a payload to one player (loader specific). */
    public interface Sender {
        void send(EntityPlayerMP player, byte[] payload);
    }

    private static EmoteServer server;
    private static MinecraftServer owner;
    private static Sender sender;

    private ServerRelay() {
    }

    public static void setSender(Sender s) {
        sender = s;
    }

    private static EmoteServer get(final MinecraftServer mc) {
        if (server == null || owner != mc) {
            owner = mc;
            server = new EmoteServer(new EmoteServer.Bridge() {
                @Override
                public void send(UUID player, byte[] packet) {
                    EntityPlayerMP p = find(player);
                    if (p != null && sender != null) {
                        sender.send(p, packet);
                    }
                }

                @Override
                public Iterable<UUID> watchers(UUID player) {
                    List<UUID> out = new ArrayList<UUID>();
                    if (mc == null) {
                        return out;
                    }
                    for (EntityPlayerMP p : players(mc)) {
                        if (!p.getUniqueID().equals(player)) {
                            out.add(p.getUniqueID());
                        }
                    }
                    return out;
                }

                @Override
                public long nowMillis() {
                    return System.currentTimeMillis();
                }

                @Override
                public void log(String message) {
                    LOGGER.info(message);
                }
            });
        }
        return server;
    }

    private static List<EntityPlayerMP> players(MinecraftServer mc) {
        //#if MC >= 11202
        return mc.getPlayerList() == null ? new ArrayList<EntityPlayerMP>() : mc.getPlayerList().getPlayers();
        //#else
        return mc.getConfigurationManager() == null ? new ArrayList<EntityPlayerMP>() : mc.getConfigurationManager().getPlayerList();
        //#endif
    }

    private static EntityPlayerMP find(UUID id) {
        MinecraftServer mc = owner;
        if (mc == null) {
            return null;
        }
        for (EntityPlayerMP p : players(mc)) {
            if (p.getUniqueID().equals(id)) {
                return p;
            }
        }
        return null;
    }

    /** Payload received from a player (server thread). */
    public static void onPacket(EntityPlayerMP player, byte[] payload) {
        get(Compat.server(player)).onPacket(player.getUniqueID(), payload);
    }

    public static void onLogout(EntityPlayerMP player) {
        if (server != null) {
            server.onLeave(player.getUniqueID());
        }
    }
}
