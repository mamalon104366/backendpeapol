package dev.blendemotes.core.pose;

import dev.blendemotes.core.anim.Animation;
import dev.blendemotes.core.anim.BoneAnimation;
import dev.blendemotes.core.anim.EmoteModel;
import dev.blendemotes.core.anim.Track;
import dev.blendemotes.core.anim.molang.Molang;
import dev.blendemotes.core.math.Mat4;
import dev.blendemotes.core.math.Quat;
import dev.blendemotes.core.math.Vec3;
import dev.blendemotes.core.rig.PlayerPart;
import dev.blendemotes.core.rig.RigDefinition;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Evaluates an {@link Animation} on the player rig, reproducing Blender's armature
 * hierarchy: {@code body -> body_control -> waist -> torso/head/arms}, {@code torso_bend ->
 * cape}, {@code arm_bend -> item}...
 * <p>
 * Channel conventions (as written by the Blender exporter, Bedrock style):
 * rotation in degrees applied X then Y then Z around Minecraft model axes; position in pixels
 * with Y up; scale factors; bend in degrees around the part's X axis.
 */
public final class PoseEvaluator {
    public static final String BODY = "body";

    private final Animation anim;
    private final double time;
    private final VanillaPose vanilla;
    private final RigDefinition rig;
    private final Molang.Context ctx;
    private final Map<String, Mat4> world = new HashMap<String, Mat4>();
    private final Set<String> visiting = new HashSet<String>();

    private PoseEvaluator(Animation anim, double time, VanillaPose vanilla, RigDefinition rig, Molang.Context ctx) {
        this.anim = anim;
        this.time = time;
        this.vanilla = vanilla;
        this.rig = rig;
        this.ctx = ctx;
    }

    /**
     * @param anim    animation, or null for the plain vanilla pose
     * @param time    seconds inside the animation (loop already applied)
     * @param vanilla the vanilla pose of this frame (used for axes the emote does not animate)
     */
    public static PlayerPose evaluate(Animation anim, double time, VanillaPose vanilla, RigDefinition rig,
                                      Molang.Context ctx, PlayerPose out) {
        PoseEvaluator e = new PoseEvaluator(anim, time, vanilla == null ? new VanillaPose() : vanilla, rig,
                ctx == null ? Molang.ZERO_CONTEXT : ctx);
        out.setRig(rig);
        out.root.set(e.local(BODY));
        for (PlayerPart p : PlayerPart.VALUES) {
            out.setPart(p, e.world(p.bone), e.bendVector(p.bone), e.tipVector(p.bone));
        }
        out.clearBones();
        if (anim != null) {
            out.addModels(anim.models);
            for (EmoteModel m : anim.models) {
                if (out.boneMatrix(m.bone) == null) {
                    out.setBone(m.bone, e.modelBone(m.bone), e.modelBonePivot(m.bone));
                }
            }
        }
        return out;
    }

    /** Transform of the "body" root bone only. */
    public static Mat4 rootTransform(Animation anim, double time, RigDefinition rig) {
        return new PoseEvaluator(anim, time, new VanillaPose(), rig, Molang.ZERO_CONTEXT).local(BODY);
    }

    /** For tests and tools: access to intermediate bone transforms. */
    static PoseEvaluator create(Animation anim, double time, VanillaPose vanilla, RigDefinition rig) {
        return new PoseEvaluator(anim, time, vanilla, rig, Molang.ZERO_CONTEXT);
    }

    public static PlayerPose evaluate(Animation anim, double time, VanillaPose vanilla, PlayerPose out) {
        return evaluate(anim, time, vanilla, RigDefinition.MINECRAFT, Molang.ZERO_CONTEXT, out);
    }

    // ------------------------------------------------------------------ hierarchy

    private String parentOf(String bone) {
        if (anim != null) {
            String explicit = anim.parents.get(bone);
            if (explicit != null) {
                if (explicit.equals(BODY) || explicit.equals(bone)) {
                    return null;
                }
                return explicit;
            }
        }
        PlayerPart part = PlayerPart.byBone(bone);
        if (part != null && part.holder() != null) {
            return part.holder().bone;
        }
        return null;
    }

    /** Items hang from the forearm and the cape from the upper torso, like in the Blender rig. */
    private boolean attachesToBendSegment(String bone, String parent) {
        if (anim != null && anim.parents.containsKey(bone)) {
            return false;
        }
        PlayerPart part = PlayerPart.byBone(bone);
        return part != null && part.holder() != null && part.holder().bone.equals(parent);
    }

    Mat4 world(String bone) {
        Mat4 cached = world.get(bone);
        if (cached != null) {
            return cached;
        }
        if (!visiting.add(bone)) {
            return new Mat4(); // cycle in "parents": break it
        }
        Mat4 result;
        String parent = parentOf(bone);
        if (parent == null) {
            result = new Mat4();
        } else {
            result = new Mat4(world(parent));
            if (attachesToBendSegment(bone, parent)) {
                // items hang from the hand: the forearm's bend, then the hand's own turn
                result.mulLocal(bendSegment(parent)).mulLocal(tipSegment(parent));
            }
        }
        result.mulLocal(local(bone));
        if (anim != null && anim.applyBendToOtherBones && isUpperBodyFollower(bone)) {
            Mat4 torso = world(PlayerPart.TORSO.bone);
            Mat4 follow = torso.mul(bendSegment(PlayerPart.TORSO.bone)).mul(torso.invertAffine());
            result = follow.mul(result);
        }
        visiting.remove(bone);
        world.put(bone, result);
        return result;
    }

    private static boolean isUpperBodyFollower(String bone) {
        return bone.equals(PlayerPart.HEAD.bone) || bone.equals(PlayerPart.RIGHT_ARM.bone) || bone.equals(PlayerPart.LEFT_ARM.bone);
    }

    /**
     * Matrix (rest model -> posed, relative to the root) of the bone a model hangs from: a player
     * part, the lower half of a bendable part ("right_arm_bend"), the root ("body") or a
     * custom bone of the emote.
     */
    Mat4 modelBone(String bone) {
        if (bone.equals(BODY)) {
            return new Mat4();
        }
        PlayerPart limb = PlayerPart.byTipBone(bone);
        if (limb != null) {
            return new Mat4(world(limb.bone)).mulLocal(bendSegment(limb.bone)).mulLocal(tipSegment(limb.bone));
        }
        PlayerPart lower = bendBase(bone);
        if (lower != null) {
            return new Mat4(world(lower.bone)).mulLocal(bendSegment(lower.bone));
        }
        return new Mat4(world(bone));
    }

    Vec3 modelBonePivot(String bone) {
        PlayerPart limb = PlayerPart.byTipBone(bone);
        if (limb != null) {
            return rig.pivot(limb).add(rig.tipJoint(limb));
        }
        PlayerPart lower = bendBase(bone);
        if (lower != null) {
            return rig.pivot(lower).add(rig.joint(lower));
        }
        return bone.equals(BODY) ? rig.bodyPivot : pivotOf(bone, PlayerPart.byBone(bone));
    }

    /** The part whose lower half a "<part>_bend" bone is, or null. */
    private static PlayerPart bendBase(String bone) {
        if (!bone.endsWith("_bend")) {
            return null;
        }
        PlayerPart part = PlayerPart.byBone(bone.substring(0, bone.length() - 5));
        return part != null && part.bend != null ? part : null;
    }

    /** Rotation of the moving half of a bendable part, in the part's rest model space. */
    Mat4 bendSegment(String bone) {
        PlayerPart part = PlayerPart.byBone(bone);
        if (part == null || part.bend == null) {
            return new Mat4();
        }
        Mat4 r = bendRotation(bone);
        if (r == null) {
            return new Mat4();
        }
        Vec3 j = rig.pivot(part).add(rig.joint(part));
        return Mat4.translation(j.x, j.y, j.z).mulLocal(r).mulLocal(Mat4.translation(-j.x, -j.y, -j.z));
    }

    /**
     * The bend as a rotation about the joint (part-local, rest pose), or null when straight.
     * Forwards/backwards only is a plain rotation about X (the classic bend); with a sideways
     * or twist part it is the bend bone's X-Y-Z Euler rotation about that bone's own axes.
     */
    Mat4 bendRotation(String bone) {
        PlayerPart part = PlayerPart.byBone(bone);
        if (anim == null || part == null || part.bend == null) {
            return null;
        }
        BoneAnimation ba = anim.bone(bone);
        if (ba == null || !ba.hasBend()) {
            return null;
        }
        double x = axis(ba, BoneAnimation.X);
        double y = axis(ba, BoneAnimation.Y);
        double z = axis(ba, BoneAnimation.Z);
        if (y == 0 && z == 0) {
            return x == 0 ? null : Mat4.rotationX(x);
        }
        Mat4 r = Mat4.rotationZYX(x, y, z);
        double tilt = anim.blenderRig ? rig.blenderBendTilt(part) : 0;
        if (tilt != 0) {
            r = Mat4.rotationX(tilt).mul(r).mul(Mat4.rotationX(-tilt));
        }
        return r;
    }

    private double axis(BoneAnimation ba, int axis) {
        return value(ba.bendAxes[axis]);
    }

    private double value(Track t) {
        return t.isEmpty() ? 0 : Math.toRadians(t.evaluate(time, ctx));
    }

    /**
     * The hand/foot turn as a rotation about the wrist/ankle (part-local, rest pose), or null. The
     * rig's hand and foot bones have the same axes as the bend bones, so the same tilt applies.
     */
    Mat4 tipRotation(String bone) {
        PlayerPart part = PlayerPart.byBone(bone);
        if (anim == null || part == null || part.bend == null || part.bend.tip == null) {
            return null;
        }
        BoneAnimation ba = anim.bone(bone);
        if (ba == null || !ba.hasTip()) {
            return null;
        }
        double x = value(ba.tipAxes[BoneAnimation.X]);
        double y = value(ba.tipAxes[BoneAnimation.Y]);
        double z = value(ba.tipAxes[BoneAnimation.Z]);
        if (y == 0 && z == 0) {
            return x == 0 ? null : Mat4.rotationX(x);
        }
        Mat4 r = Mat4.rotationZYX(x, y, z);
        double tilt = anim.blenderRig ? rig.blenderBendTilt(part) : 0;
        if (tilt != 0) {
            r = Mat4.rotationX(tilt).mul(r).mul(Mat4.rotationX(-tilt));
        }
        return r;
    }

    /** Rotation of the hand/foot about the wrist/ankle, in the part's rest model space. */
    Mat4 tipSegment(String bone) {
        Mat4 r = tipRotation(bone);
        if (r == null) {
            return new Mat4();
        }
        PlayerPart part = PlayerPart.byBone(bone);
        Vec3 j = rig.pivot(part).add(rig.tipJoint(part));
        return Mat4.translation(j.x, j.y, j.z).mulLocal(r).mulLocal(Mat4.translation(-j.x, -j.y, -j.z));
    }

    /** The hand/foot turn as a rotation vector (radians, part-local); X alone keeps its exact angle. */
    Vec3 tipVector(String bone) {
        PlayerPart part = PlayerPart.byBone(bone);
        if (anim == null || part == null || part.bend == null || part.bend.tip == null) {
            return Vec3.ZERO;
        }
        BoneAnimation ba = anim.bone(bone);
        if (ba == null || !ba.hasTip()) {
            return Vec3.ZERO;
        }
        if (ba.tipAxes[BoneAnimation.Y].isEmpty() && ba.tipAxes[BoneAnimation.Z].isEmpty()) {
            return new Vec3(value(ba.tipAxes[BoneAnimation.X]), 0, 0);
        }
        Mat4 r = tipRotation(bone);
        return r == null ? Vec3.ZERO : rotationVector(r);
    }

    /**
     * The bend as a rotation vector (axis times angle, radians, part-local). A forwards/backwards
     * bend keeps its exact angle on X (even past half a turn); see {@link PlayerPose#bendVector}.
     */
    Vec3 bendVector(String bone) {
        PlayerPart part = PlayerPart.byBone(bone);
        if (anim == null || part == null || part.bend == null) {
            return Vec3.ZERO;
        }
        BoneAnimation ba = anim.bone(bone);
        if (ba == null || !ba.hasBend()) {
            return Vec3.ZERO;
        }
        if (!ba.hasBendOffAxis()) {
            return new Vec3(axis(ba, BoneAnimation.X), 0, 0);
        }
        Mat4 r = bendRotation(bone);
        return r == null ? Vec3.ZERO : rotationVector(r);
    }

    /** Signed forwards/backwards bend angle (radians); the X part of {@link #bendVector}. */
    double bendAngle(String bone) {
        return bendVector(bone).x;
    }

    /** Axis times angle of a rotation matrix, the angle in [0, pi]. */
    static Vec3 rotationVector(Mat4 r) {
        Quat q = Quat.fromMatrix(r);
        double x = q.x, y = q.y, z = q.z, w = q.w;
        if (w < 0) {
            x = -x;
            y = -y;
            z = -z;
            w = -w;
        }
        double s = Math.sqrt(x * x + y * y + z * z);
        if (s < 1e-12) {
            return Vec3.ZERO;
        }
        double angle = 2 * Math.atan2(s, w);
        return new Vec3(x / s * angle, y / s * angle, z / s * angle);
    }

    private Vec3 pivotOf(String bone, PlayerPart part) {
        if (part != null) {
            return rig.pivot(part);
        }
        if (bone.equals(BODY)) {
            return rig.bodyPivot;
        }
        if (anim != null) {
            Vec3 p = anim.pivots.get(bone);
            if (p != null) {
                return RigDefinition.bedrockPivotToModel(p);
            }
        }
        return new Vec3(0, 24, 0);
    }

    /** Local delta of one bone: T(offset) * T(pivot) * Rz Ry Rx * S * T(-pivot). */
    Mat4 local(String bone) {
        PlayerPart part = PlayerPart.byBone(bone);
        Vec3 pivot = pivotOf(bone, part);
        BoneAnimation ba = anim == null ? null : anim.bone(bone);

        double[] offset = new double[3];
        double[] rot = new double[3];
        double[] scale = {1, 1, 1};
        if (part != null && !part.isItem()) {
            PartTransform v = vanilla.get(part);
            offset[0] = v.x - pivot.x;
            offset[1] = v.y - pivot.y;
            offset[2] = v.z - pivot.z;
            rot[0] = v.pitch;
            rot[1] = v.yaw;
            rot[2] = v.roll;
            scale[0] = v.scaleX;
            scale[1] = v.scaleY;
            scale[2] = v.scaleZ;
        }
        // Blender rig files: values are relative to the rig's (slightly tilted) bone frame
        double tilt = anim != null && anim.blenderRig && part != null ? rig.blenderTilt(part) : 0;
        boolean xzy = anim != null && anim.blenderRig && part != null && rig.blenderXzyOrder(part);
        double axisScale = tilt != 0 ? 1.0 / Math.cos(tilt) : 1.0;
        boolean animatedOffset = false;
        boolean animatedRot = false;
        boolean animatedScale = false;
        if (ba != null) {
            for (int a = 0; a < 3; a++) {
                double k = a == 0 ? 1 : axisScale;
                Track pos = ba.position[a];
                if (!pos.isEmpty()) {
                    double v = pos.evaluate(time, ctx) * k;
                    offset[a] = a == 1 ? -v : v; // Bedrock Y up -> model Y down
                    animatedOffset = true;
                }
                Track r = ba.rotation[a];
                if (!r.isEmpty()) {
                    rot[a] = Math.toRadians(r.evaluate(time, ctx)) * k;
                    animatedRot = true;
                }
                Track s = ba.scale[a];
                if (!s.isEmpty()) {
                    scale[a] = s.evaluate(time, ctx) * k;
                    animatedScale = true;
                }
            }
        }
        Mat4 tiltM = tilt != 0 ? Mat4.rotationX(tilt) : null;
        Mat4 tiltInv = tilt != 0 ? Mat4.rotationX(-tilt) : null;

        double ox = offset[0];
        double oy = offset[1];
        double oz = offset[2];
        if (tiltM != null && animatedOffset) {
            Vec3 o = tiltM.transformDirection(ox, oy, oz);
            ox = o.x;
            oy = o.y;
            oz = o.z;
        }
        Mat4 m = Mat4.translation(ox + pivot.x, oy + pivot.y, oz + pivot.z);
        boolean frame = tiltM != null && (animatedRot || animatedScale);
        if (frame) {
            m.mulLocal(tiltM);
        }
        if (xzy) {
            m.mulLocal(Mat4.rotationY(rot[1])).mulLocal(Mat4.rotationZ(rot[2])).mulLocal(Mat4.rotationX(rot[0]));
        } else {
            m.mulLocal(Mat4.rotationZYX(rot[0], rot[1], rot[2]));
        }
        if (scale[0] != 1 || scale[1] != 1 || scale[2] != 1) {
            m.mulLocal(Mat4.scaling(scale[0], scale[1], scale[2]));
        }
        if (frame) {
            m.mulLocal(tiltInv);
        }
        m.mulLocal(Mat4.translation(-pivot.x, -pivot.y, -pivot.z));
        return m;
    }
}
