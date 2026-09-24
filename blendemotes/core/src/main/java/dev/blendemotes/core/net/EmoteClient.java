package dev.blendemotes.core.net;

import dev.blendemotes.core.emote.Emote;
import dev.blendemotes.core.emote.EmoteLibrary;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Client side of the emote protocol: tells the server what the local player plays, starts
 * the emotes of other players, downloads emotes this client does not have.
 * Call it from the client (render) thread.
 */
public final class EmoteClient {
    /** Platform glue. */
    public interface Bridge {
        void send(byte[] packet);

        /** Seconds of the emote clock (same clock as {@code EmotePlayer}). */
        double now();

        void startEmote(UUID player, Emote emote, double elapsedSeconds);

        void stopEmote(UUID player);

        void log(String message);
    }

    private final Bridge bridge;
    private final EmoteLibrary library;
    private boolean serverSupported;
    private final Map<UUID, Packets.Assembler> downloads = new HashMap<UUID, Packets.Assembler>();
    private final Map<UUID, List<Pending>> pending = new HashMap<UUID, List<Pending>>();
    private final Set<UUID> requested = new HashSet<UUID>();
    private final Map<UUID, byte[]> encoded = new HashMap<UUID, byte[]>();

    private static final class Pending {
        final UUID player;
        final double receivedAt;
        final double elapsed;

        Pending(UUID player, double receivedAt, double elapsed) {
            this.player = player;
            this.receivedAt = receivedAt;
            this.elapsed = elapsed;
        }
    }

    public EmoteClient(Bridge bridge, EmoteLibrary library) {
        this.bridge = bridge;
        this.library = library;
    }

    /** Call when joining a world/server. */
    public void onJoin() {
        reset();
        bridge.send(Packets.hello(false));
    }

    public void reset() {
        serverSupported = false;
        downloads.clear();
        pending.clear();
        requested.clear();
    }

    public boolean isServerSupported() {
        return serverSupported;
    }

    /** The local player started an emote. */
    public void sendPlay(Emote emote, double elapsedSeconds) {
        if (serverSupported) {
            bridge.send(Packets.play(emote.id, (int) Math.round(elapsedSeconds * 1000)));
        }
    }

    /** The local player stopped its emote. */
    public void sendStop() {
        if (serverSupported) {
            bridge.send(Packets.stop());
        }
    }

    public void onPacket(byte[] bytes) {
        Packets.Packet p;
        try {
            p = Packets.read(bytes);
        } catch (IOException ex) {
            bridge.log("Ignoring malformed emote packet: " + ex.getMessage());
            return;
        }
        switch (p.type) {
            case Packets.S_HELLO:
                serverSupported = true;
                break;
            case Packets.S_PLAY:
                onPlay(p.player, p.emote, p.elapsedMs / 1000.0);
                break;
            case Packets.S_STOP:
                removePending(p.player);
                bridge.stopEmote(p.player);
                break;
            case Packets.S_NEED:
                upload(p.emote);
                break;
            case Packets.S_DATA:
                onData(p);
                break;
            default:
                break;
        }
    }

    private void onPlay(UUID player, UUID emoteId, double elapsed) {
        removePending(player);
        Emote emote = library.get(emoteId);
        if (emote != null) {
            bridge.startEmote(player, emote, elapsed);
            return;
        }
        List<Pending> list = pending.get(emoteId);
        if (list == null) {
            list = new ArrayList<Pending>();
            pending.put(emoteId, list);
        }
        list.add(new Pending(player, bridge.now(), elapsed));
        if (requested.add(emoteId)) {
            bridge.send(Packets.request(emoteId));
        }
    }

    private void removePending(UUID player) {
        for (List<Pending> list : pending.values()) {
            for (int i = list.size() - 1; i >= 0; i--) {
                if (list.get(i).player.equals(player)) {
                    list.remove(i);
                }
            }
        }
    }

    private void upload(UUID emoteId) {
        Emote emote = library.get(emoteId);
        if (emote == null) {
            return;
        }
        byte[] data = encoded.get(emoteId);
        if (data == null) {
            data = EmoteCodec.encode(emote);
            encoded.put(emoteId, data);
        }
        byte[][] chunks = Packets.split(data);
        if (chunks.length > Packets.MAX_CHUNKS) {
            bridge.log("Emote " + emote.info.name + " is too large to share");
            return;
        }
        for (int i = 0; i < chunks.length; i++) {
            bridge.send(Packets.upload(emoteId, i, chunks.length, chunks[i]));
        }
    }

    private void onData(Packets.Packet p) {
        Packets.Assembler a = downloads.get(p.emote);
        if (a == null) {
            a = new Packets.Assembler(p.chunkCount, System.currentTimeMillis());
            downloads.put(p.emote, a);
        }
        byte[] full;
        try {
            full = a.add(p.chunk, p.chunkCount, p.data);
        } catch (IOException ex) {
            downloads.remove(p.emote);
            return;
        }
        if (full == null) {
            return;
        }
        downloads.remove(p.emote);
        Emote emote;
        try {
            emote = EmoteCodec.decode(full, "network");
        } catch (IOException ex) {
            bridge.log("Could not decode emote from server: " + ex.getMessage());
            return;
        }
        if (!emote.id.equals(p.emote)) {
            return;
        }
        library.addRemote(emote);
        List<Pending> list = pending.remove(p.emote);
        if (list != null) {
            double now = bridge.now();
            for (Pending pd : list) {
                bridge.startEmote(pd.player, emote, pd.elapsed + (now - pd.receivedAt));
            }
        }
    }
}
