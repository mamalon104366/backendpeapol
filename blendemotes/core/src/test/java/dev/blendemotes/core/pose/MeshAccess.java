package dev.blendemotes.core.pose;

import dev.blendemotes.core.anim.Animation;
import dev.blendemotes.core.math.Mat4;
import dev.blendemotes.core.rig.RigDefinition;

/** Test helper exposing the evaluator internals to other test packages. */
public final class MeshAccess {
    private final PoseEvaluator e;
    private final Mat4 root;

    public MeshAccess(Animation anim, double time, RigDefinition rig) {
        e = PoseEvaluator.create(anim, time, new VanillaPose().reset(rig), rig);
        root = e.local(PoseEvaluator.BODY);
    }

    /** root * world(bone) */
    public Mat4 world(String bone) {
        return root.mul(e.world(bone));
    }

    public Mat4 bendSegment(String bone) {
        return e.bendSegment(bone);
    }

    public double bendAngle(String bone) {
        return e.bendAngle(bone);
    }
}
