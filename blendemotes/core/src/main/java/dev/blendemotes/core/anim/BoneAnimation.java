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

    public BoneAnimation(Track[] position, Track[] rotation, Track[] scale, Track bend) {
        this(position, rotation, scale, bend, null, null);
    }

    /** @param bendTwist bend about the lower half's own length; @param bendSide sideways bend */
    public BoneAnimation(Track[] position, Track[] rotation, Track[] scale, Track bend, Track bendTwist, Track bendSide) {
        this.position = check(position);
        this.rotation = check(rotation);
        this.scale = check(scale);
        this.bendAxes = check(new Track[]{bend, bendTwist, bendSide});
        this.bend = this.bendAxes[X];
    }

    /** Same channels with every bend axis (index X, Y, Z). */
    public static BoneAnimation withBendAxes(Track[] position, Track[] rotation, Track[] scale, Track[] bendAxes) {
        Track[] b = bendAxes == null ? new Track[3] : bendAxes;
        return new BoneAnimation(position, rotation, scale, b.length > 0 ? b[0] : null, b.length > 1 ? b[1] : null, b.length > 2 ? b[2] : null);
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

    public boolean isEmpty() {
        for (int i = 0; i < 3; i++) {
            if (!position[i].isEmpty() || !rotation[i].isEmpty() || !scale[i].isEmpty() || !bendAxes[i].isEmpty()) {
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
        }
        return t;
    }
}
