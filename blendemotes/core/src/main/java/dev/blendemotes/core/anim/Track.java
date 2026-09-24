package dev.blendemotes.core.anim;

import dev.blendemotes.core.anim.molang.Molang;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Keyframes of a single scalar channel (e.g. rotation X of the head).
 * <p>
 * Bezier segments are evaluated exactly like Blender's f-curves: the handles are corrected
 * to avoid loops ({@code BKE_fcurve_correct_bezpart}) and the curve is solved for the time
 * coordinate, so the in-game motion matches the Graph Editor.
 */
public final class Track {
    public static final Track EMPTY = new Track(Collections.<Keyframe>emptyList());

    private final Keyframe[] keys;
    private final double[] times;

    public Track(List<Keyframe> keyframes) {
        List<Keyframe> sorted = new ArrayList<Keyframe>(keyframes);
        Collections.sort(sorted, new Comparator<Keyframe>() {
            @Override
            public int compare(Keyframe a, Keyframe b) {
                return Double.compare(a.time, b.time);
            }
        });
        this.keys = sorted.toArray(new Keyframe[0]);
        this.times = new double[keys.length];
        for (int i = 0; i < keys.length; i++) {
            times[i] = keys[i].time;
        }
    }

    public boolean isEmpty() {
        return keys.length == 0;
    }

    public int size() {
        return keys.length;
    }

    public Keyframe get(int i) {
        return keys[i];
    }

    public double lastTime() {
        return keys.length == 0 ? 0 : times[keys.length - 1];
    }

    public double evaluate(double t) {
        return evaluate(t, Molang.ZERO_CONTEXT);
    }

    /** Value at time {@code t} (seconds). Before the first / after the last key the value is held. */
    public double evaluate(double t, Molang.Context ctx) {
        int n = keys.length;
        if (n == 0) {
            return 0;
        }
        if (t <= times[0] || n == 1) {
            return keys[0].value.eval(ctx);
        }
        if (t >= times[n - 1]) {
            return keys[n - 1].value.eval(ctx);
        }
        // first key with time > t
        int lo = 0;
        int hi = n - 1;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (times[mid] > t) {
                hi = mid;
            } else {
                lo = mid + 1;
            }
        }
        int b = lo;
        int a = b - 1;
        return evaluateSegment(a, b, t, ctx);
    }

    private double evaluateSegment(int ia, int ib, double t, Molang.Context ctx) {
        Keyframe ka = keys[ia];
        Keyframe kb = keys[ib];
        double va = ka.value.eval(ctx);
        double vb = kb.value.eval(ctx);
        double len = kb.time - ka.time;
        if (len <= 1e-9) {
            return vb;
        }
        double u = (t - ka.time) / len;
        Easing easing = kb.easing;
        if (easing == Easing.BEZIER || ka.easing == Easing.BEZIER) {
            return bezier(ka, kb, va, vb, t);
        }
        switch (easing) {
            case CONSTANT:
                return va;
            case CATMULLROM: {
                double p0 = ia > 0 ? keys[ia - 1].value.eval(ctx) : va;
                double p3 = ib + 1 < keys.length ? keys[ib + 1].value.eval(ctx) : vb;
                return catmullRom(p0, va, vb, p3, u);
            }
            default:
                return va + (vb - va) * easing.apply(u, kb.easingArg);
        }
    }

    static double catmullRom(double p0, double p1, double p2, double p3, double t) {
        double t2 = t * t;
        double t3 = t2 * t;
        return 0.5 * ((2 * p1) + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2 + (-p0 + 3 * p1 - 3 * p2 + p3) * t3);
    }

    private static double bezier(Keyframe ka, Keyframe kb, double va, double vb, double t) {
        double x1 = ka.time;
        double y1 = va;
        double x4 = kb.time;
        double y4 = vb;
        double x2;
        double y2;
        if (ka.hasRightHandle) {
            x2 = x1 + ka.rightDt;
            y2 = y1 + ka.rightDv;
        } else {
            // same default as the format's reference player: flat 0.1s handle
            x2 = x1 + Math.min(0.1, x4 - x1);
            y2 = y1;
        }
        double x3;
        double y3;
        if (kb.easing == Easing.BEZIER && kb.hasLeftHandle) {
            x3 = x4 + kb.leftDt;
            y3 = y4 + kb.leftDv;
        } else {
            x3 = x4;
            y3 = y4;
        }
        return evalBezierSegment(x1, y1, x2, y2, x3, y3, x4, y4, t);
    }

    /**
     * Blender compatible evaluation of one Bezier f-curve segment at time {@code t}.
     * (x1,y1) and (x4,y4) are the keys, (x2,y2) the right handle of the first key and
     * (x3,y3) the left handle of the second key.
     */
    public static double evalBezierSegment(double x1, double y1, double x2, double y2,
                                           double x3, double y3, double x4, double y4, double t) {
        // flat segment
        if (Math.abs(y1 - y4) < 1e-7 && Math.abs(y2 - y3) < 1e-7 && Math.abs(y3 - y4) < 1e-7) {
            return y1;
        }
        // handles may not point backwards in time
        if (x2 < x1) {
            x2 = x1;
        }
        if (x3 > x4) {
            x3 = x4;
        }
        // BKE_fcurve_correct_bezpart: scale both handles when they overlap
        double h1x = x1 - x2;
        double h1y = y1 - y2;
        double h2x = x4 - x3;
        double h2y = y4 - y3;
        double len = x4 - x1;
        double len1 = Math.abs(h1x);
        double len2 = Math.abs(h2x);
        if (len1 + len2 > 0 && len1 + len2 > len) {
            double fac = len / (len1 + len2);
            x2 = x1 - fac * h1x;
            y2 = y1 - fac * h1y;
            x3 = x4 - fac * h2x;
            y3 = y4 - fac * h2y;
        }
        double s = solveBezierParameter(x1, x2, x3, x4, t);
        double ms = 1 - s;
        return ms * ms * ms * y1 + 3 * ms * ms * s * y2 + 3 * ms * s * s * y3 + s * s * s * y4;
    }

    /** Finds s in [0,1] with bezierX(s) == x (the x curve is monotonic after correction). */
    static double solveBezierParameter(double x1, double x2, double x3, double x4, double x) {
        if (x <= x1) {
            return 0;
        }
        if (x >= x4) {
            return 1;
        }
        double lo = 0;
        double hi = 1;
        double s = (x - x1) / (x4 - x1);
        for (int i = 0; i < 64; i++) {
            double ms = 1 - s;
            double bx = ms * ms * ms * x1 + 3 * ms * ms * s * x2 + 3 * ms * s * s * x3 + s * s * s * x4;
            double err = bx - x;
            if (Math.abs(err) < 1e-12) {
                return s;
            }
            if (err > 0) {
                hi = s;
            } else {
                lo = s;
            }
            double d = 3 * ms * ms * (x2 - x1) + 6 * ms * s * (x3 - x2) + 3 * s * s * (x4 - x3);
            double next = d > 1e-12 ? s - err / d : Double.NaN;
            if (Double.isNaN(next) || next <= lo || next >= hi) {
                next = (lo + hi) / 2; // fall back to bisection when Newton leaves the bracket
            }
            s = next;
        }
        return s;
    }
}
