package dev.blendemotes.core.rig;

import dev.blendemotes.core.math.Vec3;

import java.util.EnumMap;
import java.util.Map;

/**
 * Rest pose of the rig: where every standard bone pivots and where the joints are.
 * <p>
 * {@link #MINECRAFT} uses the vanilla player model pivots and is what the game uses.
 * {@link #BLENDER} uses the exact bone heads of the Blender rig (legs at +-2 px instead of
 * +-1.9 px, elbows 0.1 px behind the arm centre...) and is used by the tests to reproduce
 * Blender's numbers exactly.
 */
public final class RigDefinition {
    /** Pivot of the "body" root bone: the hips. */
    public final Vec3 bodyPivot;
    private final Map<PlayerPart, Vec3> pivots;
    private final Map<PlayerPart, Vec3> joints;

    private RigDefinition(Vec3 bodyPivot, Map<PlayerPart, Vec3> pivots, Map<PlayerPart, Vec3> joints) {
        this.bodyPivot = bodyPivot;
        this.pivots = pivots;
        this.joints = joints;
    }

    public static final RigDefinition MINECRAFT;
    /** Player model with slim (3 px) arms: the arms hang from y = 2.5 instead of 2. */
    public static final RigDefinition MINECRAFT_SLIM;
    public static final RigDefinition BLENDER;

    static {
        Map<PlayerPart, Vec3> mc = new EnumMap<PlayerPart, Vec3>(PlayerPart.class);
        Map<PlayerPart, Vec3> mcJoints = new EnumMap<PlayerPart, Vec3>(PlayerPart.class);
        for (PlayerPart p : PlayerPart.VALUES) {
            mc.put(p, p.defaultPivot);
            if (p.bend != null) {
                mcJoints.put(p, new Vec3(0, p.bend.jointY, p.bend.jointZ));
            }
        }
        MINECRAFT = new RigDefinition(new Vec3(0, 12, 0), mc, mcJoints);
        Map<PlayerPart, Vec3> slim = new EnumMap<PlayerPart, Vec3>(mc);
        slim.put(PlayerPart.RIGHT_ARM, new Vec3(-5, 2.5, 0));
        slim.put(PlayerPart.LEFT_ARM, new Vec3(5, 2.5, 0));
        MINECRAFT_SLIM = new RigDefinition(new Vec3(0, 12, 0), slim, mcJoints);

        // bone heads of emote_creator.blend (rig 2.0) converted to Minecraft model space:
        // mc = (4x, 24 - 4z, 4y)
        Map<PlayerPart, Vec3> bl = new EnumMap<PlayerPart, Vec3>(mc);
        bl.put(PlayerPart.RIGHT_LEG, new Vec3(-2, 12, 0));
        bl.put(PlayerPart.LEFT_LEG, new Vec3(2, 12, 0));
        bl.put(PlayerPart.CAPE, new Vec3(0, 0, 2.5));
        Map<PlayerPart, Vec3> blJoints = new EnumMap<PlayerPart, Vec3>(mcJoints);
        blJoints.put(PlayerPart.RIGHT_ARM, new Vec3(0, 4, 0.1));
        blJoints.put(PlayerPart.LEFT_ARM, new Vec3(0, 4, 0.1));
        blJoints.put(PlayerPart.RIGHT_LEG, new Vec3(0, 6, -0.1));
        blJoints.put(PlayerPart.LEFT_LEG, new Vec3(0, 6, -0.1));
        blJoints.put(PlayerPart.CAPE, new Vec3(0, 6, 0));
        BLENDER = new RigDefinition(new Vec3(0, 12, 0), bl, blJoints);
    }

    /**
     * Rest tilt (radians about X) of the Blender rig's bone for this part. The rig tilts the arm
     * bones 1.43 degrees backwards and the leg bones 0.95 degrees forwards so the IK bends the
     * right way; rotations in exported files are relative to those tilted bones.
     */
    public double blenderTilt(PlayerPart part) {
        switch (part) {
            case RIGHT_ARM:
            case LEFT_ARM:
                return Math.toRadians(1.43208);
            case RIGHT_LEG:
            case LEFT_LEG:
                return Math.toRadians(-0.95482);
            default:
                return 0;
        }
    }

    /** Item bones of the Blender rig point forward, so their Euler order maps to X, Z, Y. */
    public boolean blenderXzyOrder(PlayerPart part) {
        return part == PlayerPart.RIGHT_ITEM || part == PlayerPart.LEFT_ITEM;
    }

    public static RigDefinition minecraft(boolean slim) {
        return slim ? MINECRAFT_SLIM : MINECRAFT;
    }

    public Vec3 pivot(PlayerPart part) {
        return pivots.get(part);
    }

    /** Joint offset relative to the part pivot (part-local, rest pose). */
    public Vec3 joint(PlayerPart part) {
        Vec3 j = joints.get(part);
        return j == null ? Vec3.ZERO : j;
    }

    /**
     * Converts a Bedrock model pivot (pixels, Y up, origin at the feet, X mirrored as the
     * Blender exporter writes it) to Minecraft model space.
     */
    public static Vec3 bedrockPivotToModel(Vec3 p) {
        return new Vec3(-p.x, 24 - p.y, p.z);
    }
}
