package dev.blendemotes.core.anim.io;

import dev.blendemotes.core.anim.Animation;
import dev.blendemotes.core.anim.BoneAnimation;
import dev.blendemotes.core.anim.Easing;
import dev.blendemotes.core.anim.Keyframe;
import dev.blendemotes.core.anim.LoopMode;
import dev.blendemotes.core.anim.Track;
import dev.blendemotes.core.anim.molang.Molang;
import dev.blendemotes.core.emote.EmoteInfo;
import dev.blendemotes.core.json.Json;
import dev.blendemotes.core.json.JsonException;
import dev.blendemotes.core.json.JsonUtil;
import dev.blendemotes.core.math.Vec3;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Reads Bedrock style animation files: the format exported by the Blender rig
 * ({@code emote_creator.blend}), by Blockbench/GeckoLib and used by PlayerAnimationLibrary.
 */
public final class BedrockAnimationLoader {
    /** Name of the extension block written by the Blender rig. */
    public static final String PAL_KEY = "player_animation_library";
    /** Our own optional extension block. */
    public static final String OWN_KEY = "blendemotes";

    private static final Pattern UPPERCASE = Pattern.compile("([A-Z])");
    /** Time offset used for Bedrock "pre" keyframes. */
    private static final double PRE_EPSILON = 1e-4;

    private BedrockAnimationLoader() {
    }

    /** One animation from a file together with its metadata. */
    public static final class Entry {
        public final String key;
        public final EmoteInfo info;
        public final Animation animation;
        /** Canonical JSON of the animation, used to derive a stable id. */
        public final String canonical;

        Entry(String key, EmoteInfo info, Animation animation, String canonical) {
            this.key = key;
            this.info = info;
            this.animation = animation;
            this.canonical = canonical;
        }
    }

    public static boolean looksLikeBedrock(Map<String, Object> root) {
        return root.get("animations") instanceof Map;
    }

    public static List<Entry> load(Map<String, Object> root) {
        Map<String, Object> animations = JsonUtil.asObject(root.get("animations"), "animations");
        Map<String, Object> fileModel = JsonUtil.getObject(root, "model");
        Map<String, Object> fileParents = JsonUtil.getObject(root, "parents");
        List<Entry> out = new ArrayList<Entry>();
        for (Map.Entry<String, Object> e : animations.entrySet()) {
            Map<String, Object> anim = JsonUtil.asObject(e.getValue(), "animation '" + e.getKey() + "'");
            try {
                out.add(loadAnimation(e.getKey(), anim, fileModel, fileParents));
            } catch (RuntimeException ex) {
                throw new JsonException("Animation '" + e.getKey() + "': " + ex.getMessage(), ex);
            }
        }
        return out;
    }

    /** Converts camelCase bone names to the snake_case names used internally. */
    public static String normalizeBoneName(String name) {
        return UPPERCASE.matcher(name.trim()).replaceAll("_$1").toLowerCase(Locale.ROOT);
    }

    private static Entry loadAnimation(String key, Map<String, Object> anim,
                                       Map<String, Object> fileModel, Map<String, Object> fileParents) {
        Map<String, BoneAnimation> bones = new LinkedHashMap<String, BoneAnimation>();
        Map<String, Object> bonesObj = JsonUtil.getObject(anim, "bones");
        for (Map.Entry<String, Object> b : bonesObj.entrySet()) {
            Map<String, Object> boneObj = JsonUtil.asObject(b.getValue(), "bone '" + b.getKey() + "'");
            BoneAnimation bone = new BoneAnimation(
                    readVectorChannel(boneObj.get("position"), false),
                    readVectorChannel(boneObj.get("rotation"), false),
                    readVectorChannel(boneObj.get("scale"), true),
                    readBendChannel(boneObj.get("bend")));
            if (!bone.isEmpty()) {
                bones.put(normalizeBoneName(b.getKey()), bone);
            }
        }

        double length;
        if (anim.get("animation_length") instanceof Number) {
            length = JsonUtil.getDouble(anim, "animation_length", 0);
        } else {
            length = 0;
            for (BoneAnimation bone : bones.values()) {
                length = Math.max(length, bone.lastKeyTime());
            }
        }

        LoopMode loopMode;
        double loopStart = 0;
        Object loop = anim.get("loop");
        if (anim.get("loopTick") instanceof Number) {
            loopMode = LoopMode.LOOP;
            loopStart = JsonUtil.getDouble(anim, "loopTick", 0);
            if (loopStart < 0 || loopStart > length) {
                loopStart = 0;
            }
        } else if (loop instanceof Boolean) {
            loopMode = ((Boolean) loop) ? LoopMode.LOOP : LoopMode.PLAY_ONCE;
        } else if (loop instanceof String) {
            String s = ((String) loop).toLowerCase(Locale.ROOT);
            if (s.equals("hold_on_last_frame")) {
                loopMode = LoopMode.HOLD_ON_LAST_FRAME;
            } else if (s.equals("true") || s.equals("loop")) {
                loopMode = LoopMode.LOOP;
            } else {
                loopMode = LoopMode.PLAY_ONCE;
            }
        } else {
            loopMode = LoopMode.PLAY_ONCE;
        }

        Map<String, Vec3> pivots = new LinkedHashMap<String, Vec3>();
        readModel(fileModel, pivots);
        readModel(JsonUtil.getObject(anim, "model"), pivots);

        Map<String, String> parents = new LinkedHashMap<String, String>();
        readParents(fileParents, parents);
        readParents(JsonUtil.getObject(anim, "parents"), parents);

        Map<String, Object> pal = JsonUtil.getObject(anim, PAL_KEY);
        Map<String, Object> own = JsonUtil.getObject(anim, OWN_KEY);
        boolean applyBend = JsonUtil.getBoolean(pal, "applyBendToOtherBones", false);

        boolean blenderRig = isBlenderRigExport(pal, own, pivots, parents);
        if (blenderRig) {
            if (!JsonUtil.getBoolean(own, "exactHandles", false)) {
                RigExportRepair.Report report = new RigExportRepair.Report();
                for (Map.Entry<String, BoneAnimation> b : bones.entrySet()) {
                    b.setValue(RigExportRepair.repair(b.getValue(), report));
                }
            }
            double fps = own.get("fps") instanceof Number ? JsonUtil.getDouble(own, "fps", 0)
                    : RigExportRepair.detectFrameRate(bones.values());
            if (fps > 0) {
                for (Map.Entry<String, BoneAnimation> b : bones.entrySet()) {
                    b.setValue(RigExportRepair.snapTimes(b.getValue(), fps));
                }
                length = RigExportRepair.snap(length, fps);
                loopStart = RigExportRepair.snap(loopStart, fps);
            }
        }

        Animation animation = new Animation(length, loopMode, loopStart, bones, pivots, parents, applyBend, blenderRig);
        EmoteInfo info = readInfo(key, pal);

        Map<String, Object> canonicalObj = new LinkedHashMap<String, Object>(anim);
        Map<String, Object> palCopy = new LinkedHashMap<String, Object>(pal);
        palCopy.remove("iconData");
        canonicalObj.put(PAL_KEY, palCopy);
        return new Entry(key, info, animation, key + "\n" + Json.write(canonicalObj));
    }

    /**
     * Recognises files written by the Blender rig: our own exporter marks them, the rig's
     * built-in exporter leaves recognisable traces (its custom pivot bones and its "bages" key).
     */
    static boolean isBlenderRigExport(Map<String, Object> pal, Map<String, Object> own,
                                      Map<String, Vec3> pivots, Map<String, String> parents) {
        if (own.containsKey("rig")) {
            return "emote_creator".equals(JsonUtil.getString(own, "rig", ""));
        }
        if (pal.isEmpty()) {
            return false;
        }
        return pal.containsKey("bages") || pivots.containsKey("body_control") || pivots.containsKey("waist")
                || parents.containsValue("body_control") || parents.containsValue("waist");
    }

    private static void readModel(Map<String, Object> model, Map<String, Vec3> out) {
        for (Map.Entry<String, Object> e : model.entrySet()) {
            if (!(e.getValue() instanceof Map)) {
                continue;
            }
            Object pivot = JsonUtil.asObject(e.getValue(), "model").get("pivot");
            if (pivot instanceof List) {
                List<Object> p = JsonUtil.asArray(pivot, "pivot");
                out.put(normalizeBoneName(e.getKey()), new Vec3(num(p, 0), num(p, 1), num(p, 2)));
            }
        }
    }

    private static void readParents(Map<String, Object> parents, Map<String, String> out) {
        for (Map.Entry<String, Object> e : parents.entrySet()) {
            if (e.getValue() instanceof String) {
                out.put(normalizeBoneName(e.getKey()), normalizeBoneName((String) e.getValue()));
            }
        }
    }

    private static double num(List<Object> list, int i) {
        if (i < list.size() && list.get(i) instanceof Number) {
            return ((Number) list.get(i)).doubleValue();
        }
        return 0;
    }

    private static EmoteInfo readInfo(String key, Map<String, Object> pal) {
        String name = key;
        Object nameObj = pal.get("name");
        if (nameObj instanceof String && !((String) nameObj).isEmpty()) {
            name = (String) nameObj;
        } else if (nameObj instanceof Map) {
            name = JsonUtil.getString(JsonUtil.asObject(nameObj, "name"), "fallback", key);
        }
        String author = JsonUtil.getString(pal, "author", "");
        String description = "";
        Object descObj = pal.get("description");
        if (descObj instanceof String) {
            description = (String) descObj;
        } else if (descObj instanceof Map) {
            description = JsonUtil.getString(JsonUtil.asObject(descObj, "description"), "fallback", "");
        }
        List<EmoteInfo.Badge> badges = new ArrayList<EmoteInfo.Badge>();
        Object badgesObj = pal.containsKey("badges") ? pal.get("badges") : pal.get("bages");
        if (badgesObj instanceof List) {
            for (Object o : JsonUtil.asArray(badgesObj, "badges")) {
                if (o instanceof Map) {
                    Map<String, Object> b = JsonUtil.asObject(o, "badge");
                    badges.add(new EmoteInfo.Badge(JsonUtil.getString(b, "text", ""), parseColor(JsonUtil.getString(b, "color", "#FFFFFF"))));
                } else if (o instanceof String) {
                    badges.add(new EmoteInfo.Badge((String) o, 0xFFFFFF));
                }
            }
        }
        byte[] icon = null;
        Object iconObj = pal.get("iconData");
        if (iconObj instanceof String) {
            try {
                icon = Base64.getDecoder().decode(((String) iconObj).replaceAll("\\s", ""));
            } catch (IllegalArgumentException ignored) {
                icon = null;
            }
        }
        return new EmoteInfo(name, author, description, badges, icon);
    }

    static int parseColor(String s) {
        String c = s.trim();
        if (c.startsWith("#")) {
            c = c.substring(1);
        }
        try {
            return (int) Long.parseLong(c, 16) & 0xFFFFFF;
        } catch (NumberFormatException ex) {
            return 0xFFFFFF;
        }
    }

    // ---------------------------------------------------------------- channels

    /** Raw keyframe before being split into axes. */
    private static final class RawKey {
        final double time;
        final Object[] values; // Number, String or null (= disabled)
        final Map<String, Object> props; // easing fields, may be empty

        RawKey(double time, Object[] values, Map<String, Object> props) {
            this.time = time;
            this.values = values;
            this.props = props;
        }
    }

    private static Track[] readVectorChannel(Object channel, boolean isScale) {
        List<RawKey> raw = readRawKeys(channel, false);
        Track[] tracks = new Track[3];
        for (int axis = 0; axis < 3; axis++) {
            tracks[axis] = buildTrack(raw, axis, "XYZ".charAt(axis) + "");
        }
        return tracks;
    }

    private static Track readBendChannel(Object channel) {
        List<RawKey> raw = readRawKeys(channel, true);
        return buildTrack(raw, 0, "");
    }

    private static List<RawKey> readRawKeys(Object channel, boolean bend) {
        List<RawKey> keys = new ArrayList<RawKey>();
        if (channel == null || channel == Json.NULL) {
            return keys;
        }
        if (channel instanceof Number || channel instanceof String || channel instanceof List) {
            keys.add(new RawKey(0, vectorOf(channel, bend), new HashMap<String, Object>()));
            return keys;
        }
        Map<String, Object> obj = JsonUtil.asObject(channel, "channel");
        if (obj.containsKey("vector") || obj.containsKey("value")) {
            keys.add(new RawKey(0, vectorFromKeyObject(obj, bend), obj));
            return keys;
        }
        if (obj.containsKey("pre") || obj.containsKey("post")) {
            addPrePost(0, obj, bend, keys);
            return keys;
        }
        for (Map.Entry<String, Object> e : obj.entrySet()) {
            double time;
            try {
                time = Double.parseDouble(e.getKey().trim());
            } catch (NumberFormatException ex) {
                continue; // not a timestamp
            }
            Object v = e.getValue();
            if (v instanceof Map) {
                Map<String, Object> kobj = JsonUtil.asObject(v, "keyframe");
                if (kobj.containsKey("vector") || kobj.containsKey("value")) {
                    keys.add(new RawKey(time, vectorFromKeyObject(kobj, bend), kobj));
                } else {
                    addPrePost(time, kobj, bend, keys);
                }
            } else {
                keys.add(new RawKey(time, vectorOf(v, bend), new HashMap<String, Object>()));
            }
        }
        return keys;
    }

    private static void addPrePost(double time, Map<String, Object> kobj, boolean bend, List<RawKey> keys) {
        boolean added = false;
        if (kobj.containsKey("pre")) {
            Map<String, Object> props = new HashMap<String, Object>();
            if (kobj.containsKey("easing")) {
                props.put("easing", kobj.get("easing"));
            }
            if (kobj.containsKey("easingArgs")) {
                props.put("easingArgs", kobj.get("easingArgs"));
            }
            if (kobj.containsKey("lerp_mode") && !kobj.containsKey("post")) {
                props.put("easing", kobj.get("lerp_mode"));
            }
            keys.add(new RawKey(time == 0 ? 0 : time - PRE_EPSILON, extractBedrock(kobj.get("pre"), bend), props));
            added = true;
        }
        if (kobj.containsKey("post")) {
            Map<String, Object> props = new HashMap<String, Object>();
            if (kobj.containsKey("lerp_mode")) {
                props.put("easing", kobj.get("lerp_mode"));
            } else if (!added && kobj.containsKey("easing")) {
                props.put("easing", kobj.get("easing"));
                if (kobj.containsKey("easingArgs")) {
                    props.put("easingArgs", kobj.get("easingArgs"));
                }
            }
            if (added) {
                // the "post" value starts the next segment instantly
                props.put("easing", props.containsKey("easing") ? props.get("easing") : "linear");
            }
            keys.add(new RawKey(time, extractBedrock(kobj.get("post"), bend), props));
            added = true;
        }
        if (!added) {
            throw new JsonException("Invalid keyframe: expected a value, 'vector', 'pre' or 'post'");
        }
    }

    private static Object[] extractBedrock(Object v, boolean bend) {
        if (v instanceof Map) {
            Map<String, Object> m = JsonUtil.asObject(v, "keyframe");
            if (m.containsKey("vector") || m.containsKey("value")) {
                return vectorFromKeyObject(m, bend);
            }
        }
        return vectorOf(v, bend);
    }

    private static Object[] vectorFromKeyObject(Map<String, Object> kobj, boolean bend) {
        if (kobj.containsKey("value")) {
            Object v = kobj.get("value");
            return new Object[]{v, bend ? null : v, bend ? null : v};
        }
        return vectorOf(kobj.get("vector"), bend);
    }

    private static Object[] vectorOf(Object v, boolean bend) {
        if (v instanceof List) {
            List<Object> list = JsonUtil.asArray(v, "vector");
            Object[] r = new Object[3];
            for (int i = 0; i < 3; i++) {
                r[i] = i < list.size() ? list.get(i) : null;
            }
            if (bend) {
                r[1] = null;
                r[2] = null;
            }
            return r;
        }
        if (v instanceof Number || v instanceof String) {
            return bend ? new Object[]{v, null, null} : new Object[]{v, v, v};
        }
        throw new JsonException("Invalid keyframe value: " + JsonUtil.describe(v));
    }

    private static boolean isDisabled(Object v) {
        if (v == null || v == Json.NULL) {
            return true;
        }
        if (v instanceof String) {
            String s = ((String) v).trim().toLowerCase(Locale.ROOT);
            return s.equals("pal.disabled") || s.equals("pal.skip");
        }
        return false;
    }

    private static Molang.Expr toExpr(Object v) {
        if (v instanceof Number) {
            return Molang.constant(((Number) v).doubleValue());
        }
        if (v instanceof Boolean) {
            return Molang.constant(((Boolean) v) ? 1 : 0);
        }
        String s = ((String) v).trim();
        if (s.isEmpty()) {
            return Molang.constant(0);
        }
        try {
            return Molang.constant(Double.parseDouble(s));
        } catch (NumberFormatException ignored) {
            // molang expression
        }
        try {
            return Molang.parse(s);
        } catch (IllegalArgumentException ex) {
            throw new JsonException(ex.getMessage());
        }
    }

    private static Track buildTrack(List<RawKey> raw, int axis, String axisName) {
        List<Keyframe> keys = new ArrayList<Keyframe>();
        for (RawKey rk : raw) {
            Object v = rk.values[axis];
            if (isDisabled(v)) {
                continue;
            }
            Object easingObj = rk.props.containsKey("easing" + axisName) ? rk.props.get("easing" + axisName) : rk.props.get("easing");
            Easing easing = easingObj instanceof String ? Easing.fromName((String) easingObj)
                    : easingObj instanceof Number ? Easing.fromLegacyId(((Number) easingObj).intValue()) : Easing.LINEAR;
            Object argsObj = rk.props.containsKey("easingArgs" + axisName) ? rk.props.get("easingArgs" + axisName) : rk.props.get("easingArgs");
            double[] args = argsObj instanceof List ? toDoubles(JsonUtil.asArray(argsObj, "easingArgs"))
                    : argsObj instanceof Number ? new double[]{((Number) argsObj).doubleValue()} : new double[0];
            if (easing == Easing.BEZIER) {
                boolean hasLeft = args.length >= 2;
                boolean hasRight = args.length >= 4;
                keys.add(new Keyframe(rk.time, toExpr(v), easing, Double.NaN,
                        hasLeft, hasLeft ? args[1] : 0, hasLeft ? args[0] : 0,
                        hasRight, hasRight ? args[3] : 0, hasRight ? args[2] : 0));
            } else {
                keys.add(new Keyframe(rk.time, toExpr(v), easing, args.length > 0 ? args[0] : Double.NaN,
                        false, 0, 0, false, 0, 0));
            }
        }
        if (keys.isEmpty()) {
            return Track.EMPTY;
        }
        return new Track(keys);
    }

    private static double[] toDoubles(List<Object> list) {
        double[] r = new double[list.size()];
        for (int i = 0; i < r.length; i++) {
            Object o = list.get(i);
            r[i] = o instanceof Number ? ((Number) o).doubleValue() : 0;
        }
        return r;
    }
}
