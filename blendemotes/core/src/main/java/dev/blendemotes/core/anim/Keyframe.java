package dev.blendemotes.core.anim;

import dev.blendemotes.core.anim.molang.Molang;

/**
 * One keyframe of a single animated axis.
 * <p>
 * Like the Bedrock format, {@link #easing} describes the segment that <em>ends</em> at this
 * keyframe. Bezier handles are stored on the keyframe they belong to (as Blender does):
 * the left handle shapes the incoming segment, the right handle the outgoing one.
 * Values are in file units: pixels for position, degrees for rotation/bend, factor for scale.
 */
public final class Keyframe {
    /** Seconds from the start of the animation. */
    public final double time;
    public final Molang.Expr value;
    public final Easing easing;
    /** First easing argument for the parametric easings, NaN if not given. */
    public final double easingArg;

    public final boolean hasLeftHandle;
    /** Left handle time offset in seconds (normally &lt;= 0). */
    public final double leftDt;
    public final double leftDv;
    public final boolean hasRightHandle;
    /** Right handle time offset in seconds (normally &gt;= 0). */
    public final double rightDt;
    public final double rightDv;

    public Keyframe(double time, Molang.Expr value, Easing easing, double easingArg,
                    boolean hasLeftHandle, double leftDt, double leftDv,
                    boolean hasRightHandle, double rightDt, double rightDv) {
        this.time = time;
        this.value = value;
        this.easing = easing;
        this.easingArg = easingArg;
        this.hasLeftHandle = hasLeftHandle;
        this.leftDt = leftDt;
        this.leftDv = leftDv;
        this.hasRightHandle = hasRightHandle;
        this.rightDt = rightDt;
        this.rightDv = rightDv;
    }

    public static Keyframe simple(double time, double value, Easing easing) {
        return new Keyframe(time, Molang.constant(value), easing, Double.NaN, false, 0, 0, false, 0, 0);
    }

    public static Keyframe bezier(double time, double value, double leftDt, double leftDv, double rightDt, double rightDv) {
        return new Keyframe(time, Molang.constant(value), Easing.BEZIER, Double.NaN, true, leftDt, leftDv, true, rightDt, rightDv);
    }

    public Keyframe withTime(double newTime) {
        return new Keyframe(newTime, value, easing, easingArg, hasLeftHandle, leftDt, leftDv, hasRightHandle, rightDt, rightDv);
    }

    public Keyframe withEasing(Easing newEasing) {
        return new Keyframe(time, value, newEasing, easingArg, hasLeftHandle, leftDt, leftDv, hasRightHandle, rightDt, rightDv);
    }

    public boolean isConstant() {
        return value.isConstant();
    }
}
