package dev.blendemotes.mc1201.net;

import dev.blendemotes.core.net.EmoteServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Server side emote relay (dedicated servers and the singleplayer/LAN server). */
public final class ServerRelay {
    private static final Logger LOGGER = LoggerFactory.getLogger("BlendEmotes");

    public interface Sender {
        void send(ServerPlayer player, byte[] payload);
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
                    ServerPlayer p = mc.getPlayerList().getPlayer(player);
                    if (p != null && sender != null) {
                        sender.send(p, packet);
                    }
                }

                @Override
                public Iterable<UUID> watchers(UUID player) {
                    List<UUID> out = new ArrayList<>();
                    for (ServerPlayer p : mc.getPlayerList().getPlayers()) {
                        if (!p.getUUID().equals(player)) {
                            out.add(p.getUUID());
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

    public static void onPacket(ServerPlayer player, byte[] payload) {
        get(player.server).onPacket(player.getUUID(), payload);
    }

    public static void onLogout(ServerPlayer player) {
        if (server != null && owner == player.server) {
            server.onLeave(player.getUUID());
        }
    }
}
