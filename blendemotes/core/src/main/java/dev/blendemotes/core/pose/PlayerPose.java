package dev.blendemotes.core.pose;

import dev.blendemotes.core.anim.EmoteModel;
import dev.blendemotes.core.math.Mat4;
import dev.blendemotes.core.math.Quat;
import dev.blendemotes.core.math.Vec3;
import dev.blendemotes.core.rig.PlayerPart;
import dev.blendemotes.core.rig.RigDefinition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
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
 *     <li>{@link #bendVector(PlayerPart)}: the bend of the part's lower half about its joint, as
 *     a rotation vector (forwards/backwards, sideways and twist).</li>
 *     <li>{@link #boneMatrix(String)}: the same kind of matrix for other bones of the emote
 *     (the ones models are attached to).</li>
 *     <li>{@link #models()}: the models the emote attaches to those bones (a microphone, a
 *     horse...), drawn with {@code root * boneMatrix(model.bone)}.</li>
 * </ul>
 */
public final class PlayerPose {
    public final Mat4 root = new Mat4();
    private final Map<PlayerPart, Mat4> matrices = new EnumMap<PlayerPart, Mat4>(PlayerPart.class);
    private final Map<PlayerPart, PartTransform> transforms = new EnumMap<PlayerPart, PartTransform>(PlayerPart.class);
    private final Vec3[] bends = new Vec3[PlayerPart.VALUES.length];
    private final Vec3[] tips = new Vec3[PlayerPart.VALUES.length];
    /** Matrices of the bones models hang from, by bone name (rest model -> posed, like parts). */
    private final Map<String, Mat4> bones = new LinkedHashMap<String, Mat4>();
    /** Rest pivot of each of those bones (model space), used to blend them smoothly. */
    private final Map<String, Vec3> bonePivots = new LinkedHashMap<String, Vec3>();
    /** Models of the emote(s) in this pose. */
    private final List<EmoteModel> models = new ArrayList<EmoteModel>();
    private RigDefinition rig = RigDefinition.MINECRAFT;

    public PlayerPose() {
        for (PlayerPart p : PlayerPart.VALUES) {
            matrices.put(p, new Mat4());
            transforms.put(p, new PartTransform(p.defaultPivot.x, p.defaultPivot.y, p.defaultPivot.z, 0, 0, 0));
            bends[p.ordinal()] = Vec3.ZERO;
            tips[p.ordinal()] = Vec3.ZERO;
        }
    }

    public Mat4 matrix(PlayerPart part) {
        return matrices.get(part);
    }

    public PartTransform transform(PlayerPart part) {
        return transforms.get(part);
    }

    /**
     * Bend of the part as a rotation vector (radians; the direction is the axis through the
     * joint, the length the angle), part-local. {@code (a, 0, 0)} is the classic
     * forwards/backwards bend by {@code a}.
     */
    public Vec3 bendVector(PlayerPart part) {
        return bends[part.ordinal()];
    }

    /**
     * Turn of the hand (arms) or foot (legs) about the wrist/ankle as a rotation vector (radians,
     * part-local, rest pose); zero for other parts.
     */
    public Vec3 tipVector(PlayerPart part) {
        return tips[part.ordinal()];
    }

    /** Wrist/ankle (part-local, rest pose). */
    public Vec3 tipJoint(PlayerPart part) {
        return rig.tipJoint(part);
    }

    /** True when the part is bent at all. */
    public boolean isBent(PlayerPart part) {
        Vec3 b = bends[part.ordinal()];
        return b.x != 0 || b.y != 0 || b.z != 0;
    }

    /**
     * Bend angle in radians, signed like a forwards/backwards bend (for logs and single-axis
     * parts such as the cape). Use {@link #bendVector} to draw.
     */
    public double bend(PlayerPart part) {
        Vec3 b = bends[part.ordinal()];
        if (b.y == 0 && b.z == 0) {
            return b.x;
        }
        return b.x < 0 ? -b.length() : b.length();
    }

    /**
     * Matrix of another bone of the emote (a custom bone a model hangs from), or null when the
     * emote has no such bone.
     */
    public Mat4 boneMatrix(String bone) {
        return bones.get(bone);
    }

    public Map<String, Mat4> boneMatrices() {
        return bones;
    }

    /** Models to draw with this pose (each on {@code boneMatrix(model.bone)}); empty for most emotes. */
    public List<EmoteModel> models() {
        return Collections.unmodifiableList(models);
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

    void setPart(PlayerPart part, Mat4 m, Vec3 bend) {
        setPart(part, m, bend, Vec3.ZERO);
    }

    void setPart(PlayerPart part, Mat4 m, Vec3 bend, Vec3 tip) {
        matrices.get(part).set(m);
        bends[part.ordinal()] = bend == null ? Vec3.ZERO : bend;
        tips[part.ordinal()] = tip == null ? Vec3.ZERO : tip;
        updateTransform(part);
    }

    void setBone(String bone, Mat4 m, Vec3 pivot) {
        bonePivots.put(bone, pivot);
        Mat4 dst = bones.get(bone);
        if (dst == null) {
            bones.put(bone, new Mat4(m));
        } else {
            dst.set(m);
        }
    }

    void clearBones() {
        bones.clear();
        bonePivots.clear();
        models.clear();
    }

    void addModels(Collection<EmoteModel> list) {
        for (EmoteModel m : list) {
            if (!models.contains(m)) {
                models.add(m);
            }
        }
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
            setPart(p, other.matrix(p), other.bendVector(p), other.tipVector(p));
        }
        bones.clear();
        for (Map.Entry<String, Mat4> e : other.bones.entrySet()) {
            bones.put(e.getKey(), new Mat4(e.getValue()));
        }
        bonePivots.clear();
        bonePivots.putAll(other.bonePivots);
        models.clear();
        models.addAll(other.models);
        return this;
    }

    /**
     * Blends two poses: {@code weight = 0} gives {@code a}, {@code 1} gives {@code b}.
     * Rotations are interpolated on the shortest path (quaternions), so fades never flip.
     * Bones only one of the poses has (models of an emote) keep that pose's matrix.
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
            out.setPart(p, m, a.bendVector(p).lerp(b.bendVector(p), weight), a.tipVector(p).lerp(b.tipVector(p), weight));
        }
        Map<String, Mat4> blended = new LinkedHashMap<String, Mat4>();
        for (Map.Entry<String, Mat4> e : b.bones.entrySet()) {
            Mat4 from = a.bones.get(e.getKey());
            blended.put(e.getKey(), from == null ? new Mat4(e.getValue())
                    : blendMatrix(from, e.getValue(), pivotOf(b, e.getKey()), weight));
        }
        for (Map.Entry<String, Mat4> e : a.bones.entrySet()) {
            if (!blended.containsKey(e.getKey())) {
                blended.put(e.getKey(), new Mat4(e.getValue()));
            }
        }
        Map<String, Vec3> pivots = new LinkedHashMap<String, Vec3>(a.bonePivots);
        pivots.putAll(b.bonePivots);
        out.bones.clear();
        out.bones.putAll(blended);
        out.bonePivots.clear();
        out.bonePivots.putAll(pivots);
        // models stay while either emote is visible (a fade out keeps them until it ends)
        List<EmoteModel> both = new ArrayList<EmoteModel>(b.models);
        for (EmoteModel m : a.models) {
            if (!both.contains(m)) {
                both.add(m);
            }
        }
        out.models.clear();
        out.models.addAll(both);
        return out;
    }

    private static Vec3 pivotOf(PlayerPose pose, String bone) {
        Vec3 p = pose.bonePivots.get(bone);
        return p == null ? Vec3.ZERO : p;
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
