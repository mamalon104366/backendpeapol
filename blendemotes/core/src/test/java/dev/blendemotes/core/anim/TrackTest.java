package dev.blendemotes.core.anim;

import dev.blendemotes.core.TestRunner;

import java.util.Arrays;

public class TrackTest {
    public void testLinearAndHold() {
        Track t = new Track(Arrays.asList(Keyframe.simple(1, 10, Easing.LINEAR), Keyframe.simple(3, 30, Easing.LINEAR)));
        TestRunner.near(10, t.evaluate(0), 1e-12, "hold before first key");
        TestRunner.near(20, t.evaluate(2), 1e-12, "linear middle");
        TestRunner.near(30, t.evaluate(5), 1e-12, "hold after last key");
    }

    public void testConstantAndEasings() {
        Track c = new Track(Arrays.asList(Keyframe.simple(0, 0, Easing.LINEAR), Keyframe.simple(1, 10, Easing.CONSTANT)));
        TestRunner.near(0, c.evaluate(0.99), 1e-12, "constant holds");
        TestRunner.near(10, c.evaluate(1), 1e-12, "constant jumps at key");
        for (Easing e : Easing.values()) {
            if (e == Easing.BEZIER || e == Easing.CATMULLROM || e == Easing.CONSTANT) {
                continue;
            }
            TestRunner.near(0, e.apply(0, Double.NaN), 1e-9, e + " starts at 0");
            TestRunner.near(1, e.apply(1, Double.NaN), 1e-9, e + " ends at 1");
        }
        TestRunner.near(0.5, Easing.EASE_IN_OUT_QUAD.apply(0.5, Double.NaN), 1e-12, "in-out symmetric");
        TestRunner.check(Easing.EASE_IN_QUAD.apply(0.3, Double.NaN) < 0.3, "ease in is slow at start");
        TestRunner.check(Easing.EASE_OUT_QUAD.apply(0.3, Double.NaN) > 0.3, "ease out is fast at start");
        TestRunner.check(Easing.fromName("EaseInOutSine") == Easing.EASE_IN_OUT_SINE, "name lookup");
        TestRunner.check(Easing.fromName("nonsense") == Easing.LINEAR, "unknown -> linear");
    }

    public void testBezierMatchesBlender() {
        // auto clamped handles (1/3 of the neighbour spacing, flat at extremes) give an
        // ease in/out curve; value at 25% of the segment known from Blender: 0.15625 (smoothstep-like)
        Track t = new Track(Arrays.asList(
                Keyframe.bezier(0, 0, -1.0 / 3, 0, 1.0 / 3, 0),
                Keyframe.bezier(1, 1, -1.0 / 3, 0, 1.0 / 3, 0)));
        TestRunner.near(0.5, t.evaluate(0.5), 1e-9, "symmetric midpoint");
        // x(s)=s for these handles, so y = 3s^2 - 2s^3
        TestRunner.near(3 * 0.0625 - 2 * 0.015625, t.evaluate(0.25), 1e-9, "cubic value");
        // overlapping handles are scaled down like BKE_fcurve_correct_bezpart
        double v = Track.evalBezierSegment(0, 0, 2, 1, -1, 1, 1, 1, 0.5);
        TestRunner.check(v >= 0 && v <= 1.0001, "corrected handles stay in range: " + v);
    }

    public void testCatmullRomPassesThroughKeys() {
        Track t = new Track(Arrays.asList(Keyframe.simple(0, 0, Easing.LINEAR), Keyframe.simple(1, 5, Easing.CATMULLROM),
                Keyframe.simple(2, 3, Easing.CATMULLROM)));
        TestRunner.near(5, t.evaluate(1), 1e-12, "key 1");
        TestRunner.near(3, t.evaluate(2), 1e-12, "key 2");
        double mid = t.evaluate(1.5);
        TestRunner.check(mid > 3 && mid < 5.5, "smooth between keys " + mid);
    }
}
