package dev.blendemotes.modern.render;

import dev.blendemotes.core.math.Vec3;
import dev.blendemotes.core.pose.PartTransform;
import dev.blendemotes.core.pose.PlayerPose;
import dev.blendemotes.core.pose.VanillaPose;
import dev.blendemotes.core.rig.PlayerPart;
import dev.blendemotes.core.rig.RigDefinition;
import dev.blendemotes.modern.ModernEmotes;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;

/** Moves poses between the vanilla player model and the rig. */
@SuppressWarnings({"rawtypes"})
public final class PoseApplier {
    private PoseApplier() {
    }

    private static BendablePart b(ModelPart part) {
        return (BendablePart) (Object) part;
    }

    private static void read(ModelPart part, PartTransform t) {
        t.set(part.x, part.y, part.z, part.xRot, part.yRot, part.zRot).setScale(1, 1, 1);
    }

    /** Vanilla pose computed by the model's setupAnim. */
    public static void capture(HumanoidModel<?> model, VanillaPose out) {
        read(model.head, out.get(PlayerPart.HEAD));
        read(model.body, out.get(PlayerPart.TORSO));
        read(model.rightArm, out.get(PlayerPart.RIGHT_ARM));
        read(model.leftArm, out.get(PlayerPart.LEFT_ARM));
        read(model.rightLeg, out.get(PlayerPart.RIGHT_LEG));
        read(model.leftLeg, out.get(PlayerPart.LEFT_LEG));
        // cape hangs slightly away from the back like vanilla's resting cape
        out.get(PlayerPart.CAPE).set(0, 0, 2, Math.toRadians(6), 0, 0);
    }

    /**
     * Parts an emote changed last frame go back to their rest pose before vanilla animates them
     * (vanilla does not reset every field, e.g. the arms' pivot height).
     */
    public static void resetDirty(HumanoidModel<?> model, boolean slim) {
        reset(model.head, PlayerPart.HEAD, slim);
        reset(model.hat, PlayerPart.HEAD, slim);
        reset(model.body, PlayerPart.TORSO, slim);
        reset(model.rightArm, PlayerPart.RIGHT_ARM, slim);
        reset(model.leftArm, PlayerPart.LEFT_ARM, slim);
        reset(model.rightLeg, PlayerPart.RIGHT_LEG, slim);
        reset(model.leftLeg, PlayerPart.LEFT_LEG, slim);
        if (model instanceof PlayerModel) {
            PlayerModel pm = (PlayerModel) model;
            reset(pm.jacket, PlayerPart.TORSO, slim);
            reset(pm.rightSleeve, PlayerPart.RIGHT_ARM, slim);
            reset(pm.leftSleeve, PlayerPart.LEFT_ARM, slim);
            reset(pm.rightPants, PlayerPart.RIGHT_LEG, slim);
            reset(pm.leftPants, PlayerPart.LEFT_LEG, slim);
        }
    }

    private static void reset(ModelPart part, PlayerPart p, boolean slim) {
        BendablePart bp = b(part);
        if (bp.blendemotes$dirty()) {
            //#if MC >= 11900
            part.resetPose();
            //#else
            Vec3 pivot = RigDefinition.minecraft(slim).pivot(p);
            part.x = (float) pivot.x;
            part.y = (float) pivot.y;
            part.z = (float) pivot.z;
            part.xRot = 0;
            part.yRot = 0;
            part.zRot = 0;
            //#endif
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
        b(part).blendemotes$setEmote(true, bend(pose, p), pose.joint(p), (float) t.scaleX, (float) t.scaleY, (float) t.scaleZ);
    }

    private static double bend(PlayerPose pose, PlayerPart p) {
        return p.bend != null && ModernEmotes.bendsEnabled() ? pose.bend(p) : 0;
    }

    /** Writes an emote pose into a player or armour model. */
    public static void apply(HumanoidModel<?> model, PlayerPose pose) {
        write(model.head, pose, PlayerPart.HEAD);
        write(model.body, pose, PlayerPart.TORSO);
        write(model.rightArm, pose, PlayerPart.RIGHT_ARM);
        write(model.leftArm, pose, PlayerPart.LEFT_ARM);
        write(model.rightLeg, pose, PlayerPart.RIGHT_LEG);
        write(model.leftLeg, pose, PlayerPart.LEFT_LEG);
        //#if MC >= 12102
        // the second skin layer (and the hat) are children of the base parts: they follow them and
        // only need the bend
        layer(model.hat, pose, PlayerPart.HEAD);
        if (model instanceof PlayerModel) {
            PlayerModel pm = (PlayerModel) model;
            layer(pm.jacket, pose, PlayerPart.TORSO);
            layer(pm.rightSleeve, pose, PlayerPart.RIGHT_ARM);
            layer(pm.leftSleeve, pose, PlayerPart.LEFT_ARM);
            layer(pm.rightPants, pose, PlayerPart.RIGHT_LEG);
            layer(pm.leftPants, pose, PlayerPart.LEFT_LEG);
        }
        //#else
        model.hat.copyFrom(model.head);
        if (model instanceof PlayerModel) {
            PlayerModel pm = (PlayerModel) model;
            pm.jacket.copyFrom(model.body);
            pm.rightSleeve.copyFrom(model.rightArm);
            pm.leftSleeve.copyFrom(model.leftArm);
            pm.rightPants.copyFrom(model.rightLeg);
            pm.leftPants.copyFrom(model.leftLeg);
        }
        //#endif
    }

    //#if MC >= 12102
    private static void layer(ModelPart part, PlayerPose pose, PlayerPart p) {
        b(part).blendemotes$setEmote(true, bend(pose, p), pose.joint(p), 1, 1, 1);
    }
    //#endif

    /** Removes every emote state from a model (for renders that are not the emoting player). */
    public static void clear(HumanoidModel<?> model) {
        b(model.head).blendemotes$clear();
        b(model.hat).blendemotes$clear();
        b(model.body).blendemotes$clear();
        b(model.rightArm).blendemotes$clear();
        b(model.leftArm).blendemotes$clear();
        b(model.rightLeg).blendemotes$clear();
        b(model.leftLeg).blendemotes$clear();
        if (model instanceof PlayerModel) {
            PlayerModel pm = (PlayerModel) model;
            b(pm.jacket).blendemotes$clear();
            b(pm.rightSleeve).blendemotes$clear();
            b(pm.leftSleeve).blendemotes$clear();
            b(pm.rightPants).blendemotes$clear();
            b(pm.leftPants).blendemotes$clear();
        }
    }
}
