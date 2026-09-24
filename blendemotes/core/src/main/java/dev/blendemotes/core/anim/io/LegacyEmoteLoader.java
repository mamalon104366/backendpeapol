package dev.blendemotes.core.anim.io;

import dev.blendemotes.core.anim.Animation;
import dev.blendemotes.core.anim.BoneAnimation;
import dev.blendemotes.core.anim.Easing;
import dev.blendemotes.core.anim.Keyframe;
import dev.blendemotes.core.anim.LoopMode;
import dev.blendemotes.core.anim.Track;
import dev.blendemotes.core.emote.EmoteInfo;
import dev.blendemotes.core.json.Json;
import dev.blendemotes.core.json.JsonException;
import dev.blendemotes.core.json.JsonUtil;
import dev.blendemotes.core.math.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reads the classic Emotecraft / playerAnimator {@code emote.json} format (versions 1-3) so the
 * large existing emote libraries keep working. The keyframes are converted to the same
 * Bedrock conventions used for the Blender rig files.
 */
public final class LegacyEmoteLoader {
    private static final int MAX_VERSION = 3;
    /** Ticks per second of the legacy format. */
    private static final double TPS = 20.0;

    private LegacyEmoteLoader() {
    }

    public static boolean looksLikeLegacy(Map<String, Object> root) {
        return root.get("emote") instanceof Map;
    }

    /** Collects keyframes per axis before building immutable tracks. */
    private static final class AxisBuilder {
        final List<Keyframe> keys = new ArrayList<Keyframe>();
    }

    private static final class BoneBuilder {
        final AxisBuilder[] position = {new AxisBuilder(), new AxisBuilder(), new AxisBuilder()};
        final AxisBuilder[] rotation = {new AxisBuilder(), new AxisBuilder(), new AxisBuilder()};
        final AxisBuilder[] scale = {new AxisBuilder(), new AxisBuilder(), new AxisBuilder()};
        final AxisBuilder bend = new AxisBuilder();
    }

    public static BedrockAnimationLoader.Entry load(Map<String, Object> root) {
        int version = JsonUtil.getInt(root, "version", 1);
        if (version > MAX_VERSION) {
            throw new JsonException("emote format version " + version + " is newer than supported (" + MAX_VERSION + ")");
        }
        Map<String, Object> node = JsonUtil.getObject(root, "emote");
        boolean easeBefore = JsonUtil.getBoolean(node, "easeBeforeKeyframe", false);
        double beginTick = JsonUtil.getDouble(node, "beginTick", 0);
        double endTick = Math.max(JsonUtil.getDouble(node, "endTick", beginTick + 1), beginTick + 1);
        if (endTick <= 0) {
            throw new JsonException("endTick must be bigger than 0");
        }
        LoopMode loop = LoopMode.PLAY_ONCE;
        double loopStartTick = 0;
        if (JsonUtil.getBoolean(node, "isLoop", false) && node.containsKey("returnTick")) {
            double returnTick = Math.max(JsonUtil.getDouble(node, "returnTick", 0) - 1, 0);
            if (returnTick > endTick) {
                throw new JsonException("returnTick has to be smaller than endTick");
            }
            loop = LoopMode.LOOP;
            loopStartTick = returnTick;
        }
        double stopTick = JsonUtil.getDouble(node, "stopTick", 0);
        double lengthTick = endTick;
        if (loop == LoopMode.PLAY_ONCE) {
            lengthTick = stopTick <= endTick ? endTick + 3 : stopTick;
        }
        boolean degrees = JsonUtil.getBoolean(node, "degrees", true);

        List<Object> moves = new ArrayList<Object>(JsonUtil.asArray(node.get("moves") == null ? new ArrayList<Object>() : node.get("moves"), "moves"));
        Collections.sort(moves, new Comparator<Object>() {
            @Override
            public int compare(Object a, Object b) {
                return Double.compare(JsonUtil.getDouble(JsonUtil.asObject(a, "move"), "tick", 0),
                        JsonUtil.getDouble(JsonUtil.asObject(b, "move"), "tick", 0));
            }
        });

        Map<String, BoneBuilder> bones = new LinkedHashMap<String, BoneBuilder>();
        for (Object m : moves) {
            Map<String, Object> move = JsonUtil.asObject(m, "move");
            double tick = JsonUtil.getDouble(move, "tick", 0);
            if (tick > lengthTick) {
                continue;
            }
            Easing easing = legacyEasing(JsonUtil.getString(move, "easing", "linear"));
            int turn = JsonUtil.getInt(move, "turn", 0);
            for (Map.Entry<String, Object> e : move.entrySet()) {
                String key = e.getKey();
                if (key.equals("tick") || key.equals("comment") || key.equals("easing") || key.equals("turn") || !(e.getValue() instanceof Map)) {
                    continue;
                }
                String bone = BedrockAnimationLoader.normalizeBoneName(key);
                if (version < 3 && bone.equals("torso")) {
                    bone = "body";
                }
                BoneBuilder b = bones.get(bone);
                if (b == null) {
                    b = new BoneBuilder();
                    bones.put(bone, b);
                }
                addPart(bone, b, JsonUtil.asObject(e.getValue(), bone), degrees, tick / TPS, easing, turn);
            }
        }
        // legacy files put the waist bend on the whole body
        BoneBuilder body = bones.get("body");
        if (body != null && !body.bend.keys.isEmpty()) {
            BoneBuilder torso = bones.get("torso");
            if (torso == null) {
                torso = new BoneBuilder();
                bones.put("torso", torso);
            }
            torso.bend.keys.addAll(body.bend.keys);
            body.bend.keys.clear();
        }

        Map<String, BoneAnimation> result = new LinkedHashMap<String, BoneAnimation>();
        for (Map.Entry<String, BoneBuilder> e : bones.entrySet()) {
            BoneBuilder b = e.getValue();
            boolean item = e.getKey().equals("right_item") || e.getKey().equals("left_item");
            Track[] pos = tracks(b.position, easeBefore);
            Track[] rot = tracks(b.rotation, easeBefore);
            if (item) {
                pos = new Track[]{pos[0], pos[2], pos[1]};
                rot = new Track[]{rot[0], rot[2], rot[1]};
            }
            BoneAnimation anim = new BoneAnimation(pos, rot, tracks(b.scale, easeBefore), track(b.bend, easeBefore));
            if (!anim.isEmpty()) {
                result.put(e.getKey(), anim);
            }
        }

        Animation animation = new Animation(lengthTick / TPS, loop, loopStartTick / TPS, result,
                Collections.<String, Vec3>emptyMap(), Collections.<String, String>emptyMap(), version < 3);
        EmoteInfo info = readInfo(root);
        return new BedrockAnimationLoader.Entry(info.name, info, animation, "legacy\n" + Json.write(root));
    }

    private static EmoteInfo readInfo(Map<String, Object> root) {
        String name = textOf(root.get("name"), "emote");
        String author = textOf(root.get("author"), "");
        String description = textOf(root.get("description"), "");
        byte[] icon = null;
        Object iconObj = root.get("icon");
        if (iconObj instanceof String) {
            try {
                icon = java.util.Base64.getDecoder().decode(((String) iconObj).replaceAll("\\s", ""));
            } catch (IllegalArgumentException ignored) {
                icon = null;
            }
        }
        return new EmoteInfo(name, author, description, null, icon);
    }

    private static String textOf(Object o, String def) {
        if (o instanceof String) {
            return (String) o;
        }
        if (o instanceof Map) {
            Map<String, Object> m = JsonUtil.asObject(o, "text");
            if (m.get("text") instanceof String) {
                return (String) m.get("text");
            }
            if (m.get("fallback") instanceof String) {
                return (String) m.get("fallback");
            }
        }
        return def;
    }

    /** Legacy easing names ("EASEINOUTQUAD", "INOUTSINE", "CONSTANT"...). */
    static Easing legacyEasing(String name) {
        String n = name.trim().toLowerCase(Locale.ROOT).replace("_", "");
        if (n.equals("linear") || n.isEmpty()) {
            return Easing.LINEAR;
        }
        Easing e = Easing.fromName(n);
        if (e == Easing.LINEAR) {
            e = Easing.fromName("ease" + n);
        }
        return e;
    }

    private static final Map<String, Vec3> DEFAULT_PIVOTS = new HashMap<String, Vec3>();

    static {
        DEFAULT_PIVOTS.put("right_arm", new Vec3(-5, 2, 0));
        DEFAULT_PIVOTS.put("left_arm", new Vec3(5, 2, 0));
        DEFAULT_PIVOTS.put("left_leg", new Vec3(1.9, 12, 0.1));
        DEFAULT_PIVOTS.put("right_leg", new Vec3(-1.9, 12, 0.1));
    }

    private static void addPart(String bone, BoneBuilder b, Map<String, Object> part, boolean degrees, double time, Easing easing, int turn) {
        boolean item = bone.equals("right_item") || bone.equals("left_item");
        boolean cape = bone.equals("cape");
        boolean isBody = bone.equals("body");
        Vec3 def = DEFAULT_PIVOTS.containsKey(bone) ? DEFAULT_PIVOTS.get(bone) : Vec3.ZERO;
        // positions: absolute pivots in model space for parts (Y down), blocks for the body
        add(b.position[0], part, "x", time, easing, isBody ? 0 : def.x, isBody, false, item || cape || isBody, degrees, turn);
        add(b.position[1], part, "y", time, easing, isBody ? 0 : def.y, isBody, false, item || !isBody, degrees, turn);
        add(b.position[2], part, "z", time, easing, isBody ? 0 : def.z, isBody, false, cape, degrees, turn);
        add(b.rotation[0], part, "pitch", time, easing, 0, false, true, item || cape || isBody, degrees, turn);
        add(b.rotation[1], part, "yaw", time, easing, 0, false, true, item || isBody, degrees, turn);
        add(b.rotation[2], part, "roll", time, easing, 0, false, true, item || cape, degrees, turn);
        add(b.scale[0], part, "scaleX", time, easing, 0, false, false, false, degrees, 0);
        add(b.scale[1], part, "scaleY", time, easing, 0, false, false, false, degrees, 0);
        add(b.scale[2], part, "scaleZ", time, easing, 0, false, false, false, degrees, 0);
        // legacy bend values are radians whatever the "degrees" flag says
        Object bend = part.get("bend");
        if (bend instanceof Number) {
            b.bend.keys.add(Keyframe.simple(time, Math.toDegrees(((Number) bend).doubleValue()), easing));
        }
    }

    private static void add(AxisBuilder axis, Map<String, Object> part, String key, double time, Easing easing,
                            double subtract, boolean blocks, boolean rotation, boolean negate, boolean degrees, int turn) {
        Object v = part.get(key);
        if (!(v instanceof Number)) {
            return;
        }
        double value = ((Number) v).doubleValue() - subtract;
        if (negate) {
            value = -value;
        }
        if (rotation) {
            if (!degrees) {
                value = Math.toDegrees(value);
            }
            value += 360.0 * turn;
        }
        if (blocks) {
            value *= 16;
        }
        axis.keys.add(Keyframe.simple(time, value, easing));
    }

    private static Track[] tracks(AxisBuilder[] axes, boolean easeBefore) {
        return new Track[]{track(axes[0], easeBefore), track(axes[1], easeBefore), track(axes[2], easeBefore)};
    }

    /**
     * Legacy files store the easing of the segment <em>after</em> a keyframe unless
     * {@code easeBeforeKeyframe} is set; the internal convention is "segment before".
     */
    private static Track track(AxisBuilder axis, boolean easeBefore) {
        if (axis.keys.isEmpty()) {
            return Track.EMPTY;
        }
        List<Keyframe> keys = new ArrayList<Keyframe>();
        if (easeBefore) {
            keys.addAll(axis.keys);
        } else {
            Easing previous = Easing.EASE_IN_OUT_SINE;
            for (Keyframe k : axis.keys) {
                keys.add(k.withEasing(previous));
                previous = k.easing;
            }
        }
        return new Track(keys);
    }
}
