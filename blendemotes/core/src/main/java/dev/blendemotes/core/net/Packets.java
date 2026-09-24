package dev.blendemotes.core.net;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * Wire format of the BlendEmotes plugin channel. The same bytes are used on every Minecraft
 * version; only the channel name differs (1.8-1.12 channels cannot contain ':').
 * <p>
 * Emotes are content addressed: players send the emote id, the full data is only transferred
 * (compressed, in chunks) to clients that do not have that emote file.
 */
public final class Packets {
    public static final int PROTOCOL = 1;
    /** Channel for Minecraft 1.13+. */
    public static final String CHANNEL = "blendemotes:main";
    /** Channel for Minecraft 1.8 - 1.12 (max 20 chars, no namespace). */
    public static final String LEGACY_CHANNEL = "BlendEmotes";
    /** Chunk size, well below the 32 KiB client-to-server payload limit of old versions. */
    public static final int CHUNK = 24 * 1024;
    public static final int MAX_CHUNKS = 64;

    // client -> server
    public static final int C_HELLO = 1;
    public static final int C_PLAY = 2;
    public static final int C_STOP = 3;
    public static final int C_UPLOAD = 4;
    public static final int C_REQUEST = 5;
    // server -> client
    public static final int S_HELLO = 101;
    public static final int S_PLAY = 102;
    public static final int S_STOP = 103;
    public static final int S_NEED = 104;
    public static final int S_DATA = 105;

    private Packets() {
    }

    /** Decoded packet (fields used depend on {@link #type}). */
    public static final class Packet {
        public int type;
        public int protocol;
        public UUID player;
        public UUID emote;
        /** Milliseconds the emote has already been playing. */
        public int elapsedMs;
        public int chunk;
        public int chunkCount;
        public byte[] data;
    }

    public static byte[] hello(boolean fromServer) {
        return write(fromServer ? S_HELLO : C_HELLO, null, null, 0, 0, 0, null);
    }

    public static byte[] play(UUID emote, int elapsedMs) {
        return write(C_PLAY, null, emote, elapsedMs, 0, 0, null);
    }

    public static byte[] stop() {
        return write(C_STOP, null, null, 0, 0, 0, null);
    }

    public static byte[] upload(UUID emote, int chunk, int count, byte[] data) {
        return write(C_UPLOAD, null, emote, 0, chunk, count, data);
    }

    public static byte[] request(UUID emote) {
        return write(C_REQUEST, null, emote, 0, 0, 0, null);
    }

    public static byte[] playOf(UUID player, UUID emote, int elapsedMs) {
        return write(S_PLAY, player, emote, elapsedMs, 0, 0, null);
    }

    public static byte[] stopOf(UUID player) {
        return write(S_STOP, player, null, 0, 0, 0, null);
    }

    public static byte[] need(UUID emote) {
        return write(S_NEED, null, emote, 0, 0, 0, null);
    }

    public static byte[] data(UUID emote, int chunk, int count, byte[] bytes) {
        return write(S_DATA, null, emote, 0, chunk, count, bytes);
    }

    private static byte[] write(int type, UUID player, UUID emote, int elapsed, int chunk, int count, byte[] data) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(data == null ? 48 : data.length + 48);
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeByte(type);
            out.writeByte(PROTOCOL);
            switch (type) {
                case C_PLAY:
                    uuid(out, emote);
                    out.writeInt(elapsed);
                    break;
                case S_PLAY:
                    uuid(out, player);
                    uuid(out, emote);
                    out.writeInt(elapsed);
                    break;
                case S_STOP:
                    uuid(out, player);
                    break;
                case C_REQUEST:
                case S_NEED:
                    uuid(out, emote);
                    break;
                case C_UPLOAD:
                case S_DATA:
                    uuid(out, emote);
                    out.writeShort(chunk);
                    out.writeShort(count);
                    out.writeInt(data.length);
                    out.write(data);
                    break;
                default:
                    break;
            }
            out.flush();
            return bytes.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static void uuid(DataOutputStream out, UUID id) throws IOException {
        out.writeLong(id.getMostSignificantBits());
        out.writeLong(id.getLeastSignificantBits());
    }

    private static UUID uuid(DataInputStream in) throws IOException {
        return new UUID(in.readLong(), in.readLong());
    }

    /** Parses a packet; throws {@link IOException} for malformed or oversized data. */
    public static Packet read(byte[] bytes) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
        Packet p = new Packet();
        p.type = in.readUnsignedByte();
        p.protocol = in.readUnsignedByte();
        switch (p.type) {
            case C_HELLO:
            case S_HELLO:
            case C_STOP:
                break;
            case C_PLAY:
                p.emote = uuid(in);
                p.elapsedMs = Math.max(0, in.readInt());
                break;
            case S_PLAY:
                p.player = uuid(in);
                p.emote = uuid(in);
                p.elapsedMs = Math.max(0, in.readInt());
                break;
            case S_STOP:
                p.player = uuid(in);
                break;
            case C_REQUEST:
            case S_NEED:
                p.emote = uuid(in);
                break;
            case C_UPLOAD:
            case S_DATA: {
                p.emote = uuid(in);
                p.chunk = in.readUnsignedShort();
                p.chunkCount = in.readUnsignedShort();
                int len = in.readInt();
                if (p.chunkCount == 0 || p.chunkCount > MAX_CHUNKS || p.chunk >= p.chunkCount || len < 0 || len > CHUNK) {
                    throw new IOException("bad chunk header");
                }
                p.data = new byte[len];
                in.readFully(p.data);
                break;
            }
            default:
                throw new IOException("unknown packet type " + p.type);
        }
        return p;
    }

    /** Splits encoded emote data into chunks. */
    public static byte[][] split(byte[] data) {
        int count = Math.max(1, (data.length + CHUNK - 1) / CHUNK);
        byte[][] chunks = new byte[count][];
        for (int i = 0; i < count; i++) {
            int from = i * CHUNK;
            int to = Math.min(data.length, from + CHUNK);
            chunks[i] = java.util.Arrays.copyOfRange(data, from, to);
        }
        return chunks;
    }

    /** Collects the chunks of one emote. */
    public static final class Assembler {
        private final byte[][] parts;
        private int received;
        public final long createdAt;

        public Assembler(int count, long now) {
            parts = new byte[count][];
            createdAt = now;
        }

        /** Returns the full data once every chunk arrived, otherwise null. */
        public byte[] add(int index, int count, byte[] data) throws IOException {
            if (count != parts.length || index < 0 || index >= parts.length) {
                throw new IOException("inconsistent chunks");
            }
            if (parts[index] == null) {
                parts[index] = data;
                received++;
            }
            if (received < parts.length) {
                return null;
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            for (byte[] p : parts) {
                out.write(p);
            }
            return out.toByteArray();
        }
    }
}
