package dev.blendemotes.core.pose;

import dev.blendemotes.core.math.Mat4;
import dev.blendemotes.core.math.Quat;
import dev.blendemotes.core.math.Vec3;
import dev.blendemotes.core.rig.PlayerPart;
import dev.blendemotes.core.rig.RigDefinition;

import java.util.EnumMap;
import java.util.Map;

/**
 * Result of evaluating an emote for one frame, ready to be written to the player model.
 * <ul>
 *     <li>{@link #root}: transform of the whole player (the rig's {@code body} bone), in model
 *     space. Apply it before rendering the model so armour, items and layers follow.</li>
 *     <li>{@link #matrix(PlayerPart)}: maps the rest-pose model to the posed model for a
 *     part (relative to the root).</li>
 *     <li>{@link #transform(PlayerPart)}: the same, decomposed into {@code ModelPart} values
 *     for the vanilla part geometry.</li>
 *     <li>{@link #bend(PlayerPart)}: bend angle in radians.</li>
 * </ul>
 */
public final class PlayerPose {
    public final Mat4 root = new Mat4();
    private final Map<PlayerPart, Mat4> matrices = new EnumMap<PlayerPart, Mat4>(PlayerPart.class);
    private final Map<PlayerPart, PartTransform> transforms = new EnumMap<PlayerPart, PartTransform>(PlayerPart.class);
    private final double[] bends = new double[PlayerPart.VALUES.length];
    private RigDefinition rig = RigDefinition.MINECRAFT;

    public PlayerPose() {
        for (PlayerPart p : PlayerPart.VALUES) {
            matrices.put(p, new Mat4());
            transforms.put(p, new PartTransform(p.defaultPivot.x, p.defaultPivot.y, p.defaultPivot.z, 0, 0, 0));
        }
    }

    public Mat4 matrix(PlayerPart part) {
        return matrices.get(part);
    }

    public PartTransform transform(PlayerPart part) {
        return transforms.get(part);
    }

    public double bend(PlayerPart part) {
        return bends[part.ordinal()];
    }

    public RigDefinition rig() {
        return rig;
    }

    /** Joint (part-local, rest pose) of a bendable part. */
    public Vec3 joint(PlayerPart part) {
        return rig.joint(part);
    }

    void setRig(RigDefinition rig) {
        this.rig = rig;
    }

    void setPart(PlayerPart part, Mat4 m, double bend) {
        matrices.get(part).set(m);
        bends[part.ordinal()] = bend;
        updateTransform(part);
    }

    private void updateTransform(PlayerPart part) {
        Vec3 p = rig.pivot(part);
        PartTransform t = PartTransform.fromMatrix(matrices.get(part).mul(Mat4.translation(p.x, p.y, p.z)));
        PartTransform dst = transforms.get(part);
        dst.set(t.x, t.y, t.z, t.pitch, t.yaw, t.roll).setScale(t.scaleX, t.scaleY, t.scaleZ);
    }

    /** Copies {@code other} into this pose. */
    public PlayerPose set(PlayerPose other) {
        root.set(other.root);
        rig = other.rig;
        for (PlayerPart p : PlayerPart.VALUES) {
            setPart(p, other.matrix(p), other.bend(p));
        }
        return this;
    }

    /**
     * Blends two poses: {@code weight = 0} gives {@code a}, {@code 1} gives {@code b}.
     * Rotations are interpolated on the shortest path (quaternions), so fades never flip.
     */
    public static PlayerPose blend(PlayerPose a, PlayerPose b, double weight, PlayerPose out) {
        if (weight <= 0) {
            return out.set(a);
        }
        if (weight >= 1) {
            return out.set(b);
        }
        out.rig = b.rig;
        out.root.set(blendMatrix(a.root, b.root, b.rig.bodyPivot, weight));
        for (PlayerPart p : PlayerPart.VALUES) {
            Mat4 m = blendMatrix(a.matrix(p), b.matrix(p), b.rig.pivot(p), weight);
            out.setPart(p, m, a.bend(p) + (b.bend(p) - a.bend(p)) * weight);
        }
        return out;
    }

    /** Blends two affine transforms around a reference point (pivot). */
    public static Mat4 blendMatrix(Mat4 a, Mat4 b, Vec3 pivot, double t) {
        Vec3 pa = a.transformPoint(pivot);
        Vec3 pb = b.transformPoint(pivot);
        Vec3 sa = a.getScale();
        Vec3 sb = b.getScale();
        Quat q = Quat.fromMatrix(a).slerp(Quat.fromMatrix(b), t);
        Vec3 pos = pa.lerp(pb, t);
        Vec3 s = sa.lerp(sb, t);
        Mat4 m = Mat4.translation(pos.x, pos.y, pos.z);
        m.mulLocal(q.toMatrix());
        m.mulLocal(Mat4.scaling(s.x, s.y, s.z));
        m.mulLocal(Mat4.translation(-pivot.x, -pivot.y, -pivot.z));
        return m;
    }
}
