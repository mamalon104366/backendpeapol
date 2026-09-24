package dev.blendemotes.mc1201.render;

import dev.blendemotes.core.pose.PartTransform;
import dev.blendemotes.core.pose.PlayerPose;
import dev.blendemotes.core.pose.VanillaPose;
import dev.blendemotes.core.rig.PlayerPart;
import dev.blendemotes.mc1201.ModernEmotes;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;

/** Moves poses between the vanilla player model and the rig. */
public final class PoseApplier {
    private PoseApplier() {
    }

    private static BendablePart b(ModelPart part) {
        return (BendablePart) (Object) part;
    }

    private static void read(ModelPart part, PartTransform t) {
        t.set(part.x, part.y, part.z, part.xRot, part.yRot, part.zRot).setScale(1, 1, 1);
    }

    public static void capture(HumanoidModel<?> model, VanillaPose out) {
        read(model.head, out.get(PlayerPart.HEAD));
        read(model.body, out.get(PlayerPart.TORSO));
        read(model.rightArm, out.get(PlayerPart.RIGHT_ARM));
        read(model.leftArm, out.get(PlayerPart.LEFT_ARM));
        read(model.rightLeg, out.get(PlayerPart.RIGHT_LEG));
        read(model.leftLeg, out.get(PlayerPart.LEFT_LEG));
        out.get(PlayerPart.CAPE).set(0, 0, 2, Math.toRadians(6), 0, 0);
    }

    /** Parts an emote changed last frame go back to their rest pose before vanilla animates. */
    public static void resetDirty(PlayerModel<?> model) {
        reset(model.head);
        reset(model.hat);
        reset(model.body);
        reset(model.jacket);
        reset(model.rightArm);
        reset(model.rightSleeve);
        reset(model.leftArm);
        reset(model.leftSleeve);
        reset(model.rightLeg);
        reset(model.rightPants);
        reset(model.leftLeg);
        reset(model.leftPants);
    }

    private static void reset(ModelPart part) {
        BendablePart bp = b(part);
        if (bp.blendemotes$dirty()) {
            part.resetPose();
        }
        bp.blendemotes$clear();
    }

    private static void write(ModelPart part, PlayerPose pose, PlayerPart p) {
        PartTransform t = pose.transform(p);
        part.x = (float) t.x;
        part.y = (float) t.y;
        part.z = (float) t.z;
        part.xRot = (float) t.pitch;
        part.yRot = (float) t.yaw;
        part.zRot = (float) t.roll;
        double bend = p.bend != null && ModernEmotes.bendsEnabled() ? pose.bend(p) : 0;
        b(part).blendemotes$setEmote(true, bend, pose.joint(p), (float) t.scaleX, (float) t.scaleY, (float) t.scaleZ);
    }

    public static void apply(PlayerModel<?> model, PlayerPose pose) {
        write(model.head, pose, PlayerPart.HEAD);
        write(model.body, pose, PlayerPart.TORSO);
        write(model.rightArm, pose, PlayerPart.RIGHT_ARM);
        write(model.leftArm, pose, PlayerPart.LEFT_ARM);
        write(model.rightLeg, pose, PlayerPart.RIGHT_LEG);
        write(model.leftLeg, pose, PlayerPart.LEFT_LEG);
        model.hat.copyFrom(model.head);
        model.jacket.copyFrom(model.body);
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftSleeve.copyFrom(model.leftArm);
        model.rightPants.copyFrom(model.rightLeg);
        model.leftPants.copyFrom(model.leftLeg);
    }
}
