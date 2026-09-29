package dev.blendemotes.core.anim;

/**
 * Animated channels of one bone. Each axis is an independent {@link Track}; an empty track
 * means "not animated", in which case the player's vanilla pose (or the rest pose for custom
 * bones) is used for that axis.
 */
public final class BoneAnimation {
    public static final int X = 0;
    public static final int Y = 1;
    public static final int Z = 2;

    /** Offset in pixels, Bedrock axes (Y up). */
    public final Track[] position;
    /** Degrees, applied X then Y then Z. */
    public final Track[] rotation;
    /** Scale factors. */
    public final Track[] scale;
    /**
     * Bend (elbows, knees, waist...) in degrees, as the rotation of the rig's bend bone about its
     * own axes: X bends forwards/backwards (the classic single "bend" value), Y twists the lower
     * half, Z bends it sideways. Applied X, then Y, then Z like the bone's Euler rotation.
     */
    public final Track[] bendAxes;
    /** Forwards/backwards bend: {@code bendAxes[X]}. */
    public final Track bend;
    /**
     * Hand (arms) or foot (legs) in degrees: the rotation of the rig's hand/foot bone about its own
     * axes (the same axes as the bend bone), turning the last 3 px of the limb about the wrist or
     * the ankle. Applied X, then Y, then Z. Empty for other bones.
     */
    public final Track[] tipAxes;

    public BoneAnimation(Track[] position, Track[] rotation, Track[] scale, Track bend) {
        this(position, rotation, scale, bend, null, null);
    }

    /** @param bendTwist bend about the lower half's own length; @param bendSide sideways bend */
    public BoneAnimation(Track[] position, Track[] rotation, Track[] scale, Track bend, Track bendTwist, Track bendSide) {
        this(position, rotation, scale, new Track[]{bend, bendTwist, bendSide}, null);
    }

    private BoneAnimation(Track[] position, Track[] rotation, Track[] scale, Track[] bendAxes, Track[] tipAxes) {
        this.position = check(position);
        this.rotation = check(rotation);
        this.scale = check(scale);
        this.bendAxes = check(bendAxes);
        this.bend = this.bendAxes[X];
        this.tipAxes = check(tipAxes);
    }

    /** Every channel, hand/foot included (null arrays or entries: not animated). */
    public static BoneAnimation of(Track[] position, Track[] rotation, Track[] scale, Track[] bendAxes, Track[] tipAxes) {
        return new BoneAnimation(position, rotation, scale, bendAxes, tipAxes);
    }

    /** Same channels with every bend axis (index X, Y, Z). */
    public static BoneAnimation withBendAxes(Track[] position, Track[] rotation, Track[] scale, Track[] bendAxes) {
        return new BoneAnimation(position, rotation, scale, bendAxes, null);
    }

    private static Track[] check(Track[] tracks) {
        Track[] r = new Track[3];
        for (int i = 0; i < 3; i++) {
            r[i] = tracks != null && i < tracks.length && tracks[i] != null ? tracks[i] : Track.EMPTY;
        }
        return r;
    }

    /** True when the bend has a twist or sideways part (not just forwards/backwards). */
    public boolean hasBendOffAxis() {
        return !bendAxes[Y].isEmpty() || !bendAxes[Z].isEmpty();
    }

    public boolean hasBend() {
        return !bendAxes[X].isEmpty() || hasBendOffAxis();
    }

    /** True when the hand (arms) or the foot (legs) is animated. */
    public boolean hasTip() {
        return !tipAxes[X].isEmpty() || !tipAxes[Y].isEmpty() || !tipAxes[Z].isEmpty();
    }

    public boolean isEmpty() {
        for (int i = 0; i < 3; i++) {
            if (!position[i].isEmpty() || !rotation[i].isEmpty() || !scale[i].isEmpty() || !bendAxes[i].isEmpty()
                    || !tipAxes[i].isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public double lastKeyTime() {
        double t = bend.lastTime();
        for (int i = 0; i < 3; i++) {
            t = Math.max(t, position[i].lastTime());
            t = Math.max(t, rotation[i].lastTime());
            t = Math.max(t, scale[i].lastTime());
            t = Math.max(t, bendAxes[i].lastTime());
            t = Math.max(t, tipAxes[i].lastTime());
        }
        return t;
    }
}
