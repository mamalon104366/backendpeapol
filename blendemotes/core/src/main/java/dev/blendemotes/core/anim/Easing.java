package dev.blendemotes.core.anim;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Interpolation types understood by the Bedrock / GeckoLib / PlayerAnimationLibrary emote
 * format (which is what the Blender rig exports).
 * <p>
 * Naming follows the file format: for the "dynamic" easings (back, elastic, bounce) the
 * format historically names the curves the opposite way round to Robert Penner / Blender
 * (e.g. {@code easeinbounce} bounces at the <em>end</em>). The Blender exporter compensates
 * for that, so the functions below intentionally keep the file-format meaning.
 */
public enum Easing {
    LINEAR("linear"),
    CONSTANT("constant"),
    STEP("step"),
    BEZIER("bezier"),
    CATMULLROM("catmullrom"),

    EASE_IN_SINE("easeinsine"), EASE_OUT_SINE("easeoutsine"), EASE_IN_OUT_SINE("easeinoutsine"),
    EASE_IN_QUAD("easeinquad"), EASE_OUT_QUAD("easeoutquad"), EASE_IN_OUT_QUAD("easeinoutquad"),
    EASE_IN_CUBIC("easeincubic"), EASE_OUT_CUBIC("easeoutcubic"), EASE_IN_OUT_CUBIC("easeinoutcubic"),
    EASE_IN_QUART("easeinquart"), EASE_OUT_QUART("easeoutquart"), EASE_IN_OUT_QUART("easeinoutquart"),
    EASE_IN_QUINT("easeinquint"), EASE_OUT_QUINT("easeoutquint"), EASE_IN_OUT_QUINT("easeinoutquint"),
    EASE_IN_EXPO("easeinexpo"), EASE_OUT_EXPO("easeoutexpo"), EASE_IN_OUT_EXPO("easeinoutexpo"),
    EASE_IN_CIRC("easeincirc"), EASE_OUT_CIRC("easeoutcirc"), EASE_IN_OUT_CIRC("easeinoutcirc"),
    EASE_IN_BACK("easeinback"), EASE_OUT_BACK("easeoutback"), EASE_IN_OUT_BACK("easeinoutback"),
    EASE_IN_ELASTIC("easeinelastic"), EASE_OUT_ELASTIC("easeoutelastic"), EASE_IN_OUT_ELASTIC("easeinoutelastic"),
    EASE_IN_BOUNCE("easeinbounce"), EASE_OUT_BOUNCE("easeoutbounce"), EASE_IN_OUT_BOUNCE("easeinoutbounce");

    private static final Map<String, Easing> BY_NAME = new HashMap<String, Easing>();

    static {
        for (Easing e : values()) {
            BY_NAME.put(e.id, e);
        }
        // aliases seen in the wild (legacy Emotecraft / playerAnimator / Blockbench)
        BY_NAME.put("lerp", LINEAR);
        BY_NAME.put("smooth", CATMULLROM);
        BY_NAME.put("instant", CONSTANT);
        BY_NAME.put("hold", CONSTANT);
    }

    /** Lower case identifier used in the json files. */
    public final String id;

    Easing(String id) {
        this.id = id;
    }

    /** Parses an easing name; unknown names fall back to {@link #LINEAR}. */
    public static Easing fromName(String name) {
        if (name == null) {
            return LINEAR;
        }
        String key = name.trim().toLowerCase(Locale.ROOT).replace("_", "").replace(" ", "");
        Easing e = BY_NAME.get(key);
        return e != null ? e : LINEAR;
    }

    /** Legacy numeric ids used by the binary formats of emote mods. */
    public static Easing fromLegacyId(int id) {
        switch (id) {
            case 0: return LINEAR;
            case 1: return CONSTANT;
            case 6: return EASE_IN_SINE;
            case 7: return EASE_OUT_SINE;
            case 8: return EASE_IN_OUT_SINE;
            case 9: return EASE_IN_CUBIC;
            case 10: return EASE_OUT_CUBIC;
            case 11: return EASE_IN_OUT_CUBIC;
            case 12: return EASE_IN_QUAD;
            case 13: return EASE_OUT_QUAD;
            case 14: return EASE_IN_OUT_QUAD;
            case 15: return EASE_IN_QUART;
            case 16: return EASE_OUT_QUART;
            case 17: return EASE_IN_OUT_QUART;
            case 18: return EASE_IN_QUINT;
            case 19: return EASE_OUT_QUINT;
            case 20: return EASE_IN_OUT_QUINT;
            case 21: return EASE_IN_EXPO;
            case 22: return EASE_OUT_EXPO;
            case 23: return EASE_IN_OUT_EXPO;
            case 24: return EASE_IN_CIRC;
            case 25: return EASE_OUT_CIRC;
            case 26: return EASE_IN_OUT_CIRC;
            case 27: return EASE_IN_BACK;
            case 28: return EASE_OUT_BACK;
            case 29: return EASE_IN_OUT_BACK;
            case 30: return EASE_IN_ELASTIC;
            case 31: return EASE_OUT_ELASTIC;
            case 32: return EASE_IN_OUT_ELASTIC;
            case 33: return EASE_IN_BOUNCE;
            case 34: return EASE_OUT_BOUNCE;
            case 35: return EASE_IN_OUT_BOUNCE;
            case 36: return CATMULLROM;
            case 37: return STEP;
            case 38: return BEZIER;
            default: return LINEAR;
        }
    }

    /**
     * Maps the progress {@code t} in [0, 1] of a segment to the interpolation factor.
     * Not used for {@link #BEZIER} and {@link #CATMULLROM}, which need the neighbouring keys.
     *
     * @param arg optional easing argument (NaN when absent)
     */
    public double apply(double t, double arg) {
        if (t <= 0) {
            return this == CONSTANT ? 0 : shape(0, arg);
        }
        if (t >= 1) {
            return this == CONSTANT ? 0 : 1;
        }
        return shape(t, arg);
    }

    private double shape(double t, double arg) {
        switch (this) {
            case LINEAR:
            case BEZIER:
            case CATMULLROM:
                return t;
            case CONSTANT:
                return 0;
            case STEP: {
                int steps = Double.isNaN(arg) ? 2 : Math.max(1, (int) arg);
                if (t >= 1) {
                    return 1;
                }
                return Math.floor(t * steps) / steps;
            }
            case EASE_IN_SINE: return sine(t);
            case EASE_OUT_SINE: return out(t, SINE, arg);
            case EASE_IN_OUT_SINE: return inOut(t, SINE, arg);
            case EASE_IN_QUAD: return t * t;
            case EASE_OUT_QUAD: return out(t, QUAD, arg);
            case EASE_IN_OUT_QUAD: return inOut(t, QUAD, arg);
            case EASE_IN_CUBIC: return t * t * t;
            case EASE_OUT_CUBIC: return out(t, CUBIC, arg);
            case EASE_IN_OUT_CUBIC: return inOut(t, CUBIC, arg);
            case EASE_IN_QUART: return Math.pow(t, 4);
            case EASE_OUT_QUART: return out(t, QUART, arg);
            case EASE_IN_OUT_QUART: return inOut(t, QUART, arg);
            case EASE_IN_QUINT: return Math.pow(t, 5);
            case EASE_OUT_QUINT: return out(t, QUINT, arg);
            case EASE_IN_OUT_QUINT: return inOut(t, QUINT, arg);
            case EASE_IN_EXPO: return expo(t);
            case EASE_OUT_EXPO: return out(t, EXPO, arg);
            case EASE_IN_OUT_EXPO: return inOut(t, EXPO, arg);
            case EASE_IN_CIRC: return circ(t);
            case EASE_OUT_CIRC: return out(t, CIRC, arg);
            case EASE_IN_OUT_CIRC: return inOut(t, CIRC, arg);
            case EASE_IN_BACK: return back(t, arg);
            case EASE_OUT_BACK: return out(t, BACK, arg);
            case EASE_IN_OUT_BACK: return inOut(t, BACK, arg);
            case EASE_IN_ELASTIC: return elastic(t, arg);
            case EASE_OUT_ELASTIC: return out(t, ELASTIC, arg);
            case EASE_IN_OUT_ELASTIC: return inOut(t, ELASTIC, arg);
            case EASE_IN_BOUNCE: return bounce(t, arg);
            case EASE_OUT_BOUNCE: return out(t, BOUNCE, arg);
            case EASE_IN_OUT_BOUNCE: return inOut(t, BOUNCE, arg);
            default:
                return t;
        }
    }

    // ---- base "ease in" shapes, f(0)=0, f(1)=1 -------------------------------------------

    private static final int SINE = 0, QUAD = 1, CUBIC = 2, QUART = 3, QUINT = 4, EXPO = 5, CIRC = 6,
            BACK = 7, ELASTIC = 8, BOUNCE = 9;

    private static double base(int kind, double t, double arg) {
        switch (kind) {
            case SINE: return sine(t);
            case QUAD: return t * t;
            case CUBIC: return t * t * t;
            case QUART: return Math.pow(t, 4);
            case QUINT: return Math.pow(t, 5);
            case EXPO: return expo(t);
            case CIRC: return circ(t);
            case BACK: return back(t, arg);
            case ELASTIC: return elastic(t, arg);
            case BOUNCE: return bounce(t, arg);
            default: return t;
        }
    }

    private static double out(double t, int kind, double arg) {
        return 1 - base(kind, 1 - t, arg);
    }

    private static double inOut(double t, int kind, double arg) {
        if (t < 0.5) {
            return base(kind, t * 2, arg) / 2;
        }
        return 1 - base(kind, (1 - t) * 2, arg) / 2;
    }

    private static double sine(double t) {
        return 1 - Math.cos(t * Math.PI / 2);
    }

    private static double expo(double t) {
        return t <= 0 ? 0 : Math.pow(2, 10 * (t - 1));
    }

    private static double circ(double t) {
        return 1 - Math.sqrt(Math.max(0, 1 - t * t));
    }

    private static double back(double t, double arg) {
        double s = Double.isNaN(arg) ? 1.70158 : arg * 1.70158;
        return t * t * ((s + 1) * t - s);
    }

    private static double elastic(double t, double arg) {
        double n = Double.isNaN(arg) ? 1 : arg;
        double c = Math.cos(t * Math.PI / 2);
        return 1 - c * c * c * Math.cos(t * n * Math.PI);
    }

    private static double bounce(double t, double arg) {
        double n = Double.isNaN(arg) ? 0.5 : arg;
        double one = 121.0 / 16.0 * t * t;
        double two = 121.0 / 4.0 * n * sq(t - 6.0 / 11.0) + 1 - n;
        double three = 121.0 * n * n * sq(t - 9.0 / 11.0) + 1 - n * n;
        double four = 484.0 * n * n * n * sq(t - 10.5 / 11.0) + 1 - n * n * n;
        return Math.min(Math.min(one, two), Math.min(three, four));
    }

    private static double sq(double v) {
        return v * v;
    }
}
