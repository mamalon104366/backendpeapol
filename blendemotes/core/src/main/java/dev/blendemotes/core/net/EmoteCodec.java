package dev.blendemotes.core.net;

import dev.blendemotes.core.anim.Animation;
import dev.blendemotes.core.anim.BoneAnimation;
import dev.blendemotes.core.anim.Easing;
import dev.blendemotes.core.anim.Keyframe;
import dev.blendemotes.core.anim.LoopMode;
import dev.blendemotes.core.anim.Track;
import dev.blendemotes.core.anim.molang.Molang;
import dev.blendemotes.core.emote.Emote;
import dev.blendemotes.core.emote.EmoteInfo;
import dev.blendemotes.core.math.Vec3;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Compact binary form of an emote, sent to other players who do not have the file.
 * Deflate compressed. Decoding validates every size so a malicious packet cannot allocate
 * unbounded memory.
 */
public final class EmoteCodec {
    public static final int FORMAT = 1;
    /** Upper bound of the decompressed size. */
    public static final int MAX_RAW_BYTES = 2 * 1024 * 1024;
    public static final int MAX_BONES = 128;
    public static final int MAX_KEYS = 20000;
    public static final int MAX_STRING = 1024;
    /** Icons bigger than this are not sent (receivers show a generic icon). */
    public static final int MAX_ICON_BYTES = 48 * 1024;

    private EmoteCodec() {
    }

    public static byte[] encode(Emote emote) {
        try {
            ByteArrayOutputStream raw = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(raw);
            out.writeByte(FORMAT);
            out.writeLong(emote.id.getMostSignificantBits());
            out.writeLong(emote.id.getLeastSignificantBits());
            writeInfo(out, emote.info);
            writeAnimation(out, emote.animation);
            out.flush();
            return deflate(raw.toByteArray());
        } catch (IOException ex) {
            throw new IllegalStateException(ex); // cannot happen with byte arrays
        }
    }

    public static Emote decode(byte[] data, String source) throws IOException {
        byte[] raw = inflate(data, MAX_RAW_BYTES);
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(raw));
        int format = in.readUnsignedByte();
        if (format != FORMAT) {
            throw new IOException("unsupported emote data format " + format);
        }
        UUID id = new UUID(in.readLong(), in.readLong());
        EmoteInfo info = readInfo(in);
        Animation animation = readAnimation(in);
        return new Emote(id, info, animation, source);
    }

    // ------------------------------------------------------------------ info

    private static void writeInfo(DataOutputStream out, EmoteInfo info) throws IOException {
        writeString(out, info.name);
        writeString(out, info.author);
        writeString(out, info.description);
        out.writeByte(Math.min(info.badges.size(), 16));
        for (int i = 0; i < info.badges.size() && i < 16; i++) {
            writeString(out, info.badges.get(i).text);
            out.writeInt(info.badges.get(i).color);
        }
        byte[] icon = info.icon != null && info.icon.length <= MAX_ICON_BYTES ? info.icon : new byte[0];
        out.writeInt(icon.length);
        out.write(icon);
    }

    private static EmoteInfo readInfo(DataInputStream in) throws IOException {
        String name = readString(in);
        String author = readString(in);
        String description = readString(in);
        int badgeCount = in.readUnsignedByte();
        if (badgeCount > 16) {
            throw new IOException("too many badges");
        }
        List<EmoteInfo.Badge> badges = new ArrayList<EmoteInfo.Badge>();
        for (int i = 0; i < badgeCount; i++) {
            badges.add(new EmoteInfo.Badge(readString(in), in.readInt()));
        }
        int iconLength = in.readInt();
        if (iconLength < 0 || iconLength > MAX_ICON_BYTES) {
            throw new IOException("bad icon size");
        }
        byte[] icon = null;
        if (iconLength > 0) {
            icon = new byte[iconLength];
            in.readFully(icon);
        }
        return new EmoteInfo(name, author, description, badges, icon);
    }

    // ------------------------------------------------------------------ animation

    private static void writeAnimation(DataOutputStream out, Animation a) throws IOException {
        out.writeDouble(a.length);
        out.writeByte(a.loopMode.ordinal());
        out.writeDouble(a.loopStart);
        out.writeBoolean(a.applyBendToOtherBones);
        out.writeBoolean(a.blenderRig);
        out.writeShort(a.pivots.size());
        for (Map.Entry<String, Vec3> e : a.pivots.entrySet()) {
            writeString(out, e.getKey());
            out.writeDouble(e.getValue().x);
            out.writeDouble(e.getValue().y);
            out.writeDouble(e.getValue().z);
        }
        out.writeShort(a.parents.size());
        for (Map.Entry<String, String> e : a.parents.entrySet()) {
            writeString(out, e.getKey());
            writeString(out, e.getValue());
        }
        out.writeShort(a.bones.size());
        for (Map.Entry<String, BoneAnimation> e : a.bones.entrySet()) {
            writeString(out, e.getKey());
            BoneAnimation b = e.getValue();
            for (int i = 0; i < 3; i++) {
                writeTrack(out, b.position[i]);
            }
            for (int i = 0; i < 3; i++) {
                writeTrack(out, b.rotation[i]);
            }
            for (int i = 0; i < 3; i++) {
                writeTrack(out, b.scale[i]);
            }
            writeTrack(out, b.bend);
        }
    }

    private static Animation readAnimation(DataInputStream in) throws IOException {
        double length = finite(in.readDouble());
        int loop = in.readUnsignedByte();
        if (loop >= LoopMode.values().length) {
            throw new IOException("bad loop mode");
        }
        double loopStart = finite(in.readDouble());
        boolean applyBend = in.readBoolean();
        boolean blenderRig = in.readBoolean();
        int pivotCount = in.readUnsignedShort();
        if (pivotCount > MAX_BONES) {
            throw new IOException("too many pivots");
        }
        Map<String, Vec3> pivots = new LinkedHashMap<String, Vec3>();
        for (int i = 0; i < pivotCount; i++) {
            pivots.put(readString(in), new Vec3(finite(in.readDouble()), finite(in.readDouble()), finite(in.readDouble())));
        }
        int parentCount = in.readUnsignedShort();
        if (parentCount > MAX_BONES) {
            throw new IOException("too many parents");
        }
        Map<String, String> parents = new LinkedHashMap<String, String>();
        for (int i = 0; i < parentCount; i++) {
            parents.put(readString(in), readString(in));
        }
        int boneCount = in.readUnsignedShort();
        if (boneCount > MAX_BONES) {
            throw new IOException("too many bones");
        }
        Map<String, BoneAnimation> bones = new LinkedHashMap<String, BoneAnimation>();
        int[] budget = {MAX_KEYS};
        for (int i = 0; i < boneCount; i++) {
            String name = readString(in);
            Track[] pos = {readTrack(in, budget), readTrack(in, budget), readTrack(in, budget)};
            Track[] rot = {readTrack(in, budget), readTrack(in, budget), readTrack(in, budget)};
            Track[] scale = {readTrack(in, budget), readTrack(in, budget), readTrack(in, budget)};
            Track bend = readTrack(in, budget);
            bones.put(name, new BoneAnimation(pos, rot, scale, bend));
        }
        return new Animation(length, LoopMode.values()[loop], loopStart, bones, pivots, parents, applyBend, blenderRig);
    }

    private static final int F_EXPR = 1;
    private static final int F_ARG = 2;
    private static final int F_LEFT = 4;
    private static final int F_RIGHT = 8;

    private static void writeTrack(DataOutputStream out, Track t) throws IOException {
        out.writeShort(t.size());
        for (int i = 0; i < t.size(); i++) {
            Keyframe k = t.get(i);
            int flags = 0;
            String expr = k.value instanceof Molang.Sourced ? ((Molang.Sourced) k.value).source : null;
            if (!k.isConstant() && expr != null) {
                flags |= F_EXPR;
            }
            if (!Double.isNaN(k.easingArg)) {
                flags |= F_ARG;
            }
            if (k.hasLeftHandle) {
                flags |= F_LEFT;
            }
            if (k.hasRightHandle) {
                flags |= F_RIGHT;
            }
            out.writeByte(flags);
            out.writeFloat((float) k.time);
            if ((flags & F_EXPR) != 0) {
                writeString(out, expr);
            } else {
                out.writeFloat((float) k.value.eval(Molang.ZERO_CONTEXT));
            }
            out.writeByte(k.easing.ordinal());
            if ((flags & F_ARG) != 0) {
                out.writeFloat((float) k.easingArg);
            }
            if ((flags & F_LEFT) != 0) {
                out.writeFloat((float) k.leftDt);
                out.writeFloat((float) k.leftDv);
            }
            if ((flags & F_RIGHT) != 0) {
                out.writeFloat((float) k.rightDt);
                out.writeFloat((float) k.rightDv);
            }
        }
    }

    private static Track readTrack(DataInputStream in, int[] budget) throws IOException {
        int n = in.readUnsignedShort();
        budget[0] -= n;
        if (budget[0] < 0) {
            throw new IOException("too many keyframes");
        }
        if (n == 0) {
            return Track.EMPTY;
        }
        List<Keyframe> keys = new ArrayList<Keyframe>(n);
        Easing[] easings = Easing.values();
        for (int i = 0; i < n; i++) {
            int flags = in.readUnsignedByte();
            double time = finite(in.readFloat());
            Molang.Expr value;
            if ((flags & F_EXPR) != 0) {
                String src = readString(in);
                try {
                    value = Molang.parse(src);
                } catch (IllegalArgumentException ex) {
                    throw new IOException("bad molang: " + ex.getMessage());
                }
            } else {
                value = Molang.constant(finite(in.readFloat()));
            }
            int easing = in.readUnsignedByte();
            if (easing >= easings.length) {
                throw new IOException("bad easing");
            }
            double arg = (flags & F_ARG) != 0 ? finite(in.readFloat()) : Double.NaN;
            boolean left = (flags & F_LEFT) != 0;
            double ldt = left ? finite(in.readFloat()) : 0;
            double ldv = left ? finite(in.readFloat()) : 0;
            boolean right = (flags & F_RIGHT) != 0;
            double rdt = right ? finite(in.readFloat()) : 0;
            double rdv = right ? finite(in.readFloat()) : 0;
            keys.add(new Keyframe(time, value, easings[easing], arg, left, ldt, ldv, right, rdt, rdv));
        }
        return new Track(keys);
    }

    private static double finite(double d) throws IOException {
        if (Double.isNaN(d) || Double.isInfinite(d)) {
            throw new IOException("non finite number");
        }
        return d;
    }

    private static void writeString(DataOutputStream out, String s) throws IOException {
        String v = s == null ? "" : s;
        if (v.length() > MAX_STRING) {
            v = v.substring(0, MAX_STRING);
        }
        out.writeUTF(v);
    }

    private static String readString(DataInputStream in) throws IOException {
        String s = in.readUTF();
        if (s.length() > MAX_STRING) {
            throw new IOException("string too long");
        }
        return s;
    }

    // ------------------------------------------------------------------ compression

    public static byte[] deflate(byte[] raw) {
        Deflater d = new Deflater(Deflater.BEST_COMPRESSION);
        try {
            d.setInput(raw);
            d.finish();
            ByteArrayOutputStream out = new ByteArrayOutputStream(raw.length / 2 + 64);
            byte[] buf = new byte[8192];
            while (!d.finished()) {
                int n = d.deflate(buf);
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } finally {
            d.end();
        }
    }

    public static byte[] inflate(byte[] data, int maxBytes) throws IOException {
        Inflater inf = new Inflater();
        try {
            inf.setInput(data);
            ByteArrayOutputStream out = new ByteArrayOutputStream(Math.min(maxBytes, data.length * 4 + 64));
            byte[] buf = new byte[8192];
            while (!inf.finished()) {
                int n;
                try {
                    n = inf.inflate(buf);
                } catch (DataFormatException ex) {
                    throw new IOException("corrupt emote data", ex);
                }
                if (n == 0) {
                    if (inf.needsInput() || inf.needsDictionary()) {
                        throw new IOException("truncated emote data");
                    }
                }
                out.write(buf, 0, n);
                if (out.size() > maxBytes) {
                    throw new IOException("emote data too large");
                }
            }
            return out.toByteArray();
        } finally {
            inf.end();
        }
    }
}
