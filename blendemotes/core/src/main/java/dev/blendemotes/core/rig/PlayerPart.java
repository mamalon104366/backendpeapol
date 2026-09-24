package dev.blendemotes.core.rig;

import dev.blendemotes.core.math.Vec3;

/**
 * Parts of the Minecraft player model driven by the rig, with the bone name used in the
 * animation files. Coordinates are Minecraft model space: pixels, Y pointing down, origin at
 * the neck, the player's left side on +X and its face towards -Z.
 */
public enum PlayerPart {
    HEAD("head", new Vec3(0, 0, 0), null),
    TORSO("torso", new Vec3(0, 0, 0), BendProfile.TORSO),
    RIGHT_ARM("right_arm", new Vec3(-5, 2, 0), BendProfile.ARM),
    LEFT_ARM("left_arm", new Vec3(5, 2, 0), BendProfile.ARM),
    RIGHT_LEG("right_leg", new Vec3(-1.9, 12, 0), BendProfile.LEG),
    LEFT_LEG("left_leg", new Vec3(1.9, 12, 0), BendProfile.LEG),
    CAPE("cape", new Vec3(0, 0, 2), BendProfile.CAPE),
    RIGHT_ITEM("right_item", new Vec3(-6, 12, -2), null),
    LEFT_ITEM("left_item", new Vec3(6, 12, -2), null);

    public static final PlayerPart[] VALUES = values();

    public final String bone;
    /** Vanilla (standing) pivot of the part. */
    public final Vec3 defaultPivot;
    /** How the part bends, or null when it cannot bend. */
    public final BendProfile bend;

    PlayerPart(String bone, Vec3 defaultPivot, BendProfile bend) {
        this.bone = bone;
        this.defaultPivot = defaultPivot;
        this.bend = bend;
    }

    public boolean isItem() {
        return this == RIGHT_ITEM || this == LEFT_ITEM;
    }

    /** The part an item is held by. */
    public PlayerPart holder() {
        switch (this) {
            case RIGHT_ITEM:
                return RIGHT_ARM;
            case LEFT_ITEM:
                return LEFT_ARM;
            case CAPE:
                return TORSO;
            default:
                return null;
        }
    }

    public static PlayerPart byBone(String bone) {
        for (PlayerPart p : VALUES) {
            if (p.bone.equals(bone)) {
                return p;
            }
        }
        return null;
    }
}
