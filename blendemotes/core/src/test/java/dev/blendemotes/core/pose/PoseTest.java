package dev.blendemotes.core.pose;

import dev.blendemotes.core.TestRunner;
import dev.blendemotes.core.anim.Animation;
import dev.blendemotes.core.anim.BoneAnimation;
import dev.blendemotes.core.anim.Easing;
import dev.blendemotes.core.anim.Keyframe;
import dev.blendemotes.core.anim.LoopMode;
import dev.blendemotes.core.anim.Track;
import dev.blendemotes.core.math.Mat4;
import dev.blendemotes.core.math.Vec3;
import dev.blendemotes.core.rig.PlayerPart;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class PoseTest {
    private static Track constant(double v) {
        return new Track(Collections.singletonList(Keyframe.simple(0, v, Easing.LINEAR)));
    }

    private static Animation single(String bone, BoneAnimation b) {
        Map<String, BoneAnimation> bones = new LinkedHashMap<String, BoneAnimation>();
        bones.put(bone, b);
        return new Animation(1, LoopMode.PLAY_ONCE, 0, bones, Collections.<String, Vec3>emptyMap(),
                Collections.<String, String>emptyMap(), false);
    }

    public void testUnanimatedAxesKeepVanillaPose() {
        // only the right arm's X rotation is animated: head keeps looking where vanilla says
        Animation anim = single("right_arm", new BoneAnimation(null, new Track[]{constant(-90), null, null}, null, null));
        VanillaPose vanilla = new VanillaPose();
        vanilla.set(PlayerPart.HEAD, 0, 0, 0, 0.3, 0.2, 0);
        vanilla.set(PlayerPart.RIGHT_ARM, -5, 2, 0, 0.1, 0.25, 0.05);
        PlayerPose pose = PoseEvaluator.evaluate(anim, 0.5, vanilla, new PlayerPose());
        PartTransform head = pose.transform(PlayerPart.HEAD);
        TestRunner.near(0.3, head.pitch, 1e-9, "head pitch from vanilla");
        TestRunner.near(0.2, head.yaw, 1e-9, "head yaw from vanilla");
        PartTransform arm = pose.transform(PlayerPart.RIGHT_ARM);
        TestRunner.near(Math.toRadians(-90), arm.pitch, 1e-9, "animated arm pitch");
        TestRunner.near(0.25, arm.yaw, 1e-9, "vanilla arm yaw kept");
        TestRunner.near(0.05, arm.roll, 1e-9, "vanilla arm roll kept");
    }

    public void testPositionConvention() {
        // Bedrock Y is up: +2 lifts the head, i.e. model Y decreases
        Animation anim = single("head", new BoneAnimation(new Track[]{constant(1), constant(2), constant(3)}, null, null, null));
        PlayerPose pose = PoseEvaluator.evaluate(anim, 0, new VanillaPose(), new PlayerPose());
        PartTransform head = pose.transform(PlayerPart.HEAD);
        TestRunner.near(1, head.x, 1e-9, "x");
        TestRunner.near(-2, head.y, 1e-9, "y up");
        TestRunner.near(3, head.z, 1e-9, "z");
    }

    public void testBodyRotatesEverythingAroundHips() {
        Animation anim = single("body", new BoneAnimation(null, new Track[]{constant(90), null, null}, null, null));
        PlayerPose pose = PoseEvaluator.evaluate(anim, 0, new VanillaPose(), new PlayerPose());
        // hips (0,12,0) stay, the neck (0,0,0) swings to (0,12,-12) or (0,12,12)
        Vec3 hips = pose.root.transformPoint(0, 12, 0);
        TestRunner.near(12, hips.y, 1e-9, "hips fixed");
        Vec3 neck = pose.root.transformPoint(0, 0, 0);
        TestRunner.near(12, neck.y, 1e-9, "neck at hip height");
        TestRunner.near(12, Math.abs(neck.z), 1e-9, "neck 12px away");
    }

    public void testCustomParentsAndBendSegment() {
        Map<String, BoneAnimation> bones = new LinkedHashMap<String, BoneAnimation>();
        bones.put("waist", new BoneAnimation(null, new Track[]{null, constant(90), null}, null, null));
        bones.put("right_arm", new BoneAnimation(null, null, null, constant(90)));
        Map<String, Vec3> pivots = new LinkedHashMap<String, Vec3>();
        pivots.put("waist", new Vec3(0, 12, 0));
        Map<String, String> parents = new LinkedHashMap<String, String>();
        parents.put("right_arm", "waist");
        Animation anim = new Animation(1, LoopMode.LOOP, 0, bones, pivots, parents, false);
        PlayerPose pose = PoseEvaluator.evaluate(anim, 0, new VanillaPose(), new PlayerPose());
        // waist yaw 90 degrees carries the arm around the spine
        Vec3 shoulder = pose.matrix(PlayerPart.RIGHT_ARM).transformPoint(-5, 2, 0);
        TestRunner.near(0, shoulder.x, 1e-9, "arm swung around");
        TestRunner.near(5, Math.abs(shoulder.z), 1e-9, "arm swung around (z)");
        TestRunner.near(Math.toRadians(90), pose.bend(PlayerPart.RIGHT_ARM), 1e-12, "bend angle");
        // the item hangs from the bent forearm: it moved away from the straight position
        Mat4 item = pose.matrix(PlayerPart.RIGHT_ITEM);
        Mat4 arm = pose.matrix(PlayerPart.RIGHT_ARM);
        TestRunner.check(!item.approxEquals(arm, 1e-6), "item follows the bend segment");
    }

    public void testBlendHalfway() {
        Animation anim = single("head", new BoneAnimation(null, new Track[]{constant(90), null, null}, null, null));
        PlayerPose a = PoseEvaluator.evaluate(null, 0, new VanillaPose(), new PlayerPose());
        PlayerPose b = PoseEvaluator.evaluate(anim, 0, new VanillaPose(), new PlayerPose());
        PlayerPose half = PlayerPose.blend(a, b, 0.5, new PlayerPose());
        TestRunner.near(Math.toRadians(45), half.transform(PlayerPart.HEAD).pitch, 1e-9, "slerp halfway");
    }

    public void testLoopTiming() {
        Animation anim = new Animation(2, LoopMode.LOOP, 0.5, Collections.<String, BoneAnimation>emptyMap(),
                Collections.<String, Vec3>emptyMap(), Collections.<String, String>emptyMap(), false);
        TestRunner.near(1.5, anim.animationTime(1.5), 1e-12, "first pass");
        TestRunner.near(0.75, anim.animationTime(2.25), 1e-12, "wrapped to loop start");
        Animation once = new Animation(2, LoopMode.PLAY_ONCE, 0, Collections.<String, BoneAnimation>emptyMap(),
                Collections.<String, Vec3>emptyMap(), Collections.<String, String>emptyMap(), false);
        TestRunner.near(2, once.animationTime(5), 1e-12, "clamped");
        TestRunner.check(once.isFinished(2.1) && !once.isFinished(1.9), "finished");
    }

    public void testEulerDecompositionRoundTrip() {
        double[][] cases = {{0.3, -1.2, 2.0}, {-2.9, 0.4, -0.1}, {1.0, Math.PI / 2 - 1e-9, 0.5}};
        for (double[] c : cases) {
            Mat4 m = Mat4.rotationZYX(c[0], c[1], c[2]);
            double[] e = m.toEulerZYX();
            TestRunner.check(Mat4.rotationZYX(e[0], e[1], e[2]).approxEquals(m, 1e-6), "euler round trip " + Arrays.toString(c));
        }
    }
}
