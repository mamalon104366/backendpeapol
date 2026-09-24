package dev.blendemotes.core.net;

import dev.blendemotes.core.emote.Emote;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server side relay, shared by the Fabric/Forge server code (and the integrated server of
 * singleplayer / LAN). It only forwards packets and caches emote data; it never animates.
 * Not thread safe: call it from the server thread.
 */
public final class EmoteServer {
    /** Platform glue. */
    public interface Bridge {
        void send(UUID player, byte[] packet);

        /** Players that currently see {@code player} (not including them). */
        Iterable<UUID> watchers(UUID player);

        long nowMillis();

        void log(String message);
    }

    private static final int MAX_CACHE_BYTES = 8 * 1024 * 1024;
    private static final long MIN_PLAY_INTERVAL_MS = 200;
    private static final long UPLOAD_TIMEOUT_MS = 60000;
    private static final int MAX_ELAPSED_MS = 60 * 60 * 1000;

    private final Bridge bridge;
    private final LinkedHashMap<UUID, byte[]> cache = new LinkedHashMap<UUID, byte[]>(16, 0.75f, true);
    private int cacheBytes;
    private final Map<UUID, Packets.Assembler> uploads = new HashMap<UUID, Packets.Assembler>();
    private final Map<UUID, Set<UUID>> waiting = new HashMap<UUID, Set<UUID>>();
    private final Map<UUID, State> players = new HashMap<UUID, State>();

    private static final class State {
        boolean supported;
        UUID emote;
        long startedAt;
        long lastPlay;
        final Set<UUID> asked = new HashSet<UUID>();
    }

    public EmoteServer(Bridge bridge) {
        this.bridge = bridge;
    }

    private State state(UUID player) {
        State s = players.get(player);
        if (s == null) {
            s = new State();
            players.put(player, s);
        }
        return s;
    }

    public boolean supports(UUID player) {
        State s = players.get(player);
        return s != null && s.supported;
    }

    public void onLeave(UUID player) {
        State s = players.remove(player);
        if (s != null && s.emote != null) {
            broadcast(player, Packets.stopOf(player));
        }
        for (Set<UUID> w : waiting.values()) {
            w.remove(player);
        }
    }

    /** Call when {@code watcher} starts seeing {@code target} so it catches up on its emote. */
    public void onStartTracking(UUID watcher, UUID target) {
        State t = players.get(target);
        if (t != null && t.emote != null && supports(watcher)) {
            bridge.send(watcher, Packets.playOf(target, t.emote, elapsed(t)));
        }
    }

    private int elapsed(State s) {
        long e = bridge.nowMillis() - s.startedAt;
        return (int) Math.max(0, Math.min(MAX_ELAPSED_MS, e));
    }

    public void onPacket(UUID player, byte[] bytes) {
        Packets.Packet p;
        try {
            p = Packets.read(bytes);
        } catch (IOException ex) {
            bridge.log("Ignoring malformed emote packet from " + player + ": " + ex.getMessage());
            return;
        }
        State s = state(player);
        switch (p.type) {
            case Packets.C_HELLO:
                s.supported = true;
                bridge.send(player, Packets.hello(true));
                break;
            case Packets.C_PLAY:
                onPlay(player, s, p);
                break;
            case Packets.C_STOP:
                if (s.emote != null) {
                    s.emote = null;
                    broadcast(player, Packets.stopOf(player));
                }
                break;
            case Packets.C_UPLOAD:
                onUpload(player, p);
                break;
            case Packets.C_REQUEST:
                onRequest(player, p.emote);
                break;
            default:
                break;
        }
    }

    private void onPlay(UUID player, State s, Packets.Packet p) {
        long now = bridge.nowMillis();
        if (now - s.lastPlay < MIN_PLAY_INTERVAL_MS) {
            return; // spam protection
        }
        s.lastPlay = now;
        s.emote = p.emote;
        s.startedAt = now - Math.min(p.elapsedMs, MAX_ELAPSED_MS);
        broadcast(player, Packets.playOf(player, p.emote, elapsed(s)));
        if (!cache.containsKey(p.emote) && s.asked.add(p.emote)) {
            bridge.send(player, Packets.need(p.emote));
        }
    }

    private void onUpload(UUID player, Packets.Packet p) {
        if (cache.containsKey(p.emote)) {
            return;
        }
        long now = bridge.nowMillis();
        expireUploads(now);
        Packets.Assembler a = uploads.get(p.emote);
        if (a == null) {
            a = new Packets.Assembler(p.chunkCount, now);
            uploads.put(p.emote, a);
        }
        byte[] full;
        try {
            full = a.add(p.chunk, p.chunkCount, p.data);
        } catch (IOException ex) {
            uploads.remove(p.emote);
            return;
        }
        if (full == null) {
            return;
        }
        uploads.remove(p.emote);
        Emote decoded;
        try {
            decoded = EmoteCodec.decode(full, "network");
        } catch (IOException ex) {
            bridge.log("Rejected emote upload from " + player + ": " + ex.getMessage());
            return;
        }
        if (!decoded.id.equals(p.emote)) {
            bridge.log("Rejected emote upload from " + player + ": id mismatch");
            return;
        }
        put(p.emote, full);
        Set<UUID> w = waiting.remove(p.emote);
        if (w != null) {
            for (UUID target : w) {
                sendData(target, p.emote, full);
            }
        }
    }

    private void onRequest(UUID player, UUID emote) {
        byte[] data = cache.get(emote);
        if (data != null) {
            sendData(player, emote, data);
            return;
        }
        Set<UUID> w = waiting.get(emote);
        if (w == null) {
            w = new HashSet<UUID>();
            waiting.put(emote, w);
        }
        if (w.size() < 256) {
            w.add(player);
        }
        // ask whoever is playing it
        for (Map.Entry<UUID, State> e : players.entrySet()) {
            State s = e.getValue();
            if (emote.equals(s.emote) && s.asked.add(emote)) {
                bridge.send(e.getKey(), Packets.need(emote));
            }
        }
    }

    private void sendData(UUID player, UUID emote, byte[] data) {
        byte[][] chunks = Packets.split(data);
        for (int i = 0; i < chunks.length; i++) {
            bridge.send(player, Packets.data(emote, i, chunks.length, chunks[i]));
        }
    }

    private void put(UUID emote, byte[] data) {
        cache.put(emote, data);
        cacheBytes += data.length;
        Iterator<Map.Entry<UUID, byte[]>> it = cache.entrySet().iterator();
        while (cacheBytes > MAX_CACHE_BYTES && it.hasNext()) {
            Map.Entry<UUID, byte[]> e = it.next();
            if (e.getKey().equals(emote)) {
                continue;
            }
            cacheBytes -= e.getValue().length;
            it.remove();
        }
    }

    private void expireUploads(long now) {
        Iterator<Map.Entry<UUID, Packets.Assembler>> it = uploads.entrySet().iterator();
        while (it.hasNext()) {
            if (now - it.next().getValue().createdAt > UPLOAD_TIMEOUT_MS) {
                it.remove();
            }
        }
    }

    private void broadcast(UUID player, byte[] packet) {
        for (UUID w : bridge.watchers(player)) {
            if (!w.equals(player) && supports(w)) {
                bridge.send(w, packet);
            }
        }
    }

    /** For tests / debug commands. */
    public int cachedEmotes() {
        return cache.size();
    }

    public Map<UUID, UUID> playing() {
        Map<UUID, UUID> r = new LinkedHashMap<UUID, UUID>();
        for (Map.Entry<UUID, State> e : players.entrySet()) {
            if (e.getValue().emote != null) {
                r.put(e.getKey(), e.getValue().emote);
            }
        }
        return r;
    }
}
