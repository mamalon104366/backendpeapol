package dev.blendemotes.core.net;

import dev.blendemotes.core.TestRunner;
import dev.blendemotes.core.emote.Emote;
import dev.blendemotes.core.emote.EmoteFiles;
import dev.blendemotes.core.emote.EmoteLibrary;
import dev.blendemotes.core.pose.PlayerPose;
import dev.blendemotes.core.pose.PoseEvaluator;
import dev.blendemotes.core.pose.VanillaPose;
import dev.blendemotes.core.rig.PlayerPart;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class EmoteCodecTest {
    static Emote load(String name) {
        return EmoteFiles.parse(TestRunner.resource("/blender/" + name + ".json"), name).get(0);
    }

    public void testRoundTripKeepsAnimation() throws IOException {
        Emote e = load("inchworm");
        byte[] data = EmoteCodec.encode(e);
        System.out.println("    inchworm encoded: " + data.length + " bytes");
        TestRunner.check(data.length < Packets.CHUNK * Packets.MAX_CHUNKS, "fits in the chunk budget");
        Emote d = EmoteCodec.decode(data, "net");
        TestRunner.check(d.id.equals(e.id), "id");
        TestRunner.check(d.info.name.equals(e.info.name), "name");
        TestRunner.check(d.animation.blenderRig, "rig flag");
        for (double t = 0; t < e.animation.length; t += 0.037) {
            PlayerPose a = PoseEvaluator.evaluate(e.animation, t, new VanillaPose(), new PlayerPose());
            PlayerPose b = PoseEvaluator.evaluate(d.animation, t, new VanillaPose(), new PlayerPose());
            for (PlayerPart p : PlayerPart.VALUES) {
                TestRunner.check(a.matrix(p).approxEquals(b.matrix(p), 2e-3), "pose differs after network round trip at " + t + " " + p);
                TestRunner.near(a.bend(p), b.bend(p), 1e-4, "bend");
            }
        }
    }

    public void testRejectsGarbage() {
        byte[][] bad = {new byte[0], new byte[]{1, 2, 3}, EmoteCodec.deflate(new byte[]{9, 9, 9})};
        for (byte[] b : bad) {
            try {
                EmoteCodec.decode(b, "x");
                throw new AssertionError("accepted garbage");
            } catch (IOException expected) {
                // ok
            }
        }
        try {
            Packets.read(new byte[]{(byte) Packets.S_DATA, 1, 0, 0});
            throw new AssertionError("accepted truncated packet");
        } catch (IOException expected) {
            // ok
        }
    }

    /** Two clients and a server wired in memory: B does not have A's emote and downloads it. */
    public void testClientServerRelay() {
        final Emote emote = load("cartwheel");
        final UUID a = UUID.randomUUID();
        final UUID b = UUID.randomUUID();
        final Map<UUID, List<byte[]>> toClient = new HashMap<UUID, List<byte[]>>();
        final Map<UUID, List<byte[]>> toServer = new HashMap<UUID, List<byte[]>>();
        toClient.put(a, new ArrayList<byte[]>());
        toClient.put(b, new ArrayList<byte[]>());
        toServer.put(a, new ArrayList<byte[]>());
        toServer.put(b, new ArrayList<byte[]>());
        final long[] now = {1000};
        EmoteServer server = new EmoteServer(new EmoteServer.Bridge() {
            public void send(UUID player, byte[] packet) {
                toClient.get(player).add(packet);
            }

            public Iterable<UUID> watchers(UUID player) {
                Set<UUID> s = new LinkedHashSet<UUID>();
                s.add(a);
                s.add(b);
                s.remove(player);
                return s;
            }

            public long nowMillis() {
                return now[0];
            }

            public void log(String message) {
            }
        });
        final Map<UUID, Emote> started = new HashMap<UUID, Emote>();
        EmoteLibrary libA = new EmoteLibrary();
        libA.addRemote(emote); // A "has" the emote
        EmoteLibrary libB = new EmoteLibrary();
        EmoteClient clientA = new EmoteClient(bridge(toServer.get(a), started), libA);
        EmoteClient clientB = new EmoteClient(bridge(toServer.get(b), started), libB);
        clientA.onJoin();
        clientB.onJoin();
        pump(server, clientA, clientB, a, b, toClient, toServer);
        TestRunner.check(clientA.isServerSupported() && clientB.isServerSupported(), "handshake");
        clientA.sendPlay(emote, 0);
        pump(server, clientA, clientB, a, b, toClient, toServer);
        TestRunner.check(emote.id.equals(started.get(a) == null ? null : started.get(a).id), "B started A's emote after downloading it");
        TestRunner.check(libB.get(emote.id) != null, "B cached the emote");
        TestRunner.check(server.cachedEmotes() == 1, "server cached the emote");
    }

    private static EmoteClient.Bridge bridge(final List<byte[]> out, final Map<UUID, Emote> started) {
        return new EmoteClient.Bridge() {
            public void send(byte[] packet) {
                out.add(packet);
            }

            public double now() {
                return 1;
            }

            public void startEmote(UUID player, Emote emote, double elapsedSeconds) {
                started.put(player, emote);
            }

            public void stopEmote(UUID player) {
                started.remove(player);
            }

            public void log(String message) {
            }
        };
    }

    private static void pump(EmoteServer server, EmoteClient ca, EmoteClient cb, UUID a, UUID b,
                             Map<UUID, List<byte[]>> toClient, Map<UUID, List<byte[]>> toServer) {
        for (int round = 0; round < 20; round++) {
            boolean any = false;
            for (UUID id : new UUID[]{a, b}) {
                List<byte[]> q = new ArrayList<byte[]>(toServer.get(id));
                toServer.get(id).clear();
                for (byte[] p : q) {
                    server.onPacket(id, p);
                    any = true;
                }
            }
            List<byte[]> qa = new ArrayList<byte[]>(toClient.get(a));
            toClient.get(a).clear();
            for (byte[] p : qa) {
                ca.onPacket(p);
                any = true;
            }
            List<byte[]> qb = new ArrayList<byte[]>(toClient.get(b));
            toClient.get(b).clear();
            for (byte[] p : qb) {
                cb.onPacket(p);
                any = true;
            }
            if (!any) {
                return;
            }
        }
    }
}
