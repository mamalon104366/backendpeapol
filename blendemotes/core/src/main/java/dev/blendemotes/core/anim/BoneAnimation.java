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
    /** Bend angle in degrees (elbows, knees, waist...). */
    public final Track bend;

    public BoneAnimation(Track[] position, Track[] rotation, Track[] scale, Track bend) {
        this.position = check(position);
        this.rotation = check(rotation);
        this.scale = check(scale);
        this.bend = bend == null ? Track.EMPTY : bend;
    }

    private static Track[] check(Track[] tracks) {
        Track[] r = new Track[3];
        for (int i = 0; i < 3; i++) {
            r[i] = tracks != null && i < tracks.length && tracks[i] != null ? tracks[i] : Track.EMPTY;
        }
        return r;
    }

    public boolean isEmpty() {
        for (int i = 0; i < 3; i++) {
            if (!position[i].isEmpty() || !rotation[i].isEmpty() || !scale[i].isEmpty()) {
                return false;
            }
        }
        return bend.isEmpty();
    }

    public double lastKeyTime() {
        double t = bend.lastTime();
        for (int i = 0; i < 3; i++) {
            t = Math.max(t, position[i].lastTime());
            t = Math.max(t, rotation[i].lastTime());
            t = Math.max(t, scale[i].lastTime());
        }
        return t;
    }
}
