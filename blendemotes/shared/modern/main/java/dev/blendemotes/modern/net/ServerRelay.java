package dev.blendemotes.modern.net;

import dev.blendemotes.core.net.EmoteServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
//#if MC >= 11800
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//#else
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//#endif

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Server side emote relay (dedicated servers and the singleplayer/LAN server). */
public final class ServerRelay {
    //#if MC >= 11800
    private static final Logger LOGGER = LoggerFactory.getLogger("BlendEmotes");
    //#else
    private static final Logger LOGGER = LogManager.getLogger("BlendEmotes");
    //#endif

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
                        try {
                            sender.send(p, packet);
                        } catch (RuntimeException e) {
                            // a client without the mod: some loaders refuse channels it did not announce
                            LOGGER.debug("Emote packet not sent to " + player + ": " + e);
                        }
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

    private static MinecraftServer serverOf(ServerPlayer player) {
        //#if MC >= 12105
        return player.level().getServer();
        //#else
        return player.server;
        //#endif
    }

    public static void onPacket(ServerPlayer player, byte[] payload) {
        MinecraftServer mc = serverOf(player);
        if (mc != null) {
            get(mc).onPacket(player.getUUID(), payload);
        }
    }

    public static void onLogout(ServerPlayer player) {
        if (server != null && owner == serverOf(player)) {
            server.onLeave(player.getUUID());
        }
    }
}
