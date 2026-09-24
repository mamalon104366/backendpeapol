package dev.blendemotes.mc189.render;

import dev.blendemotes.core.pose.PartTransform;
import dev.blendemotes.core.pose.PlayerPose;
import dev.blendemotes.core.pose.VanillaPose;
import dev.blendemotes.core.rig.PlayerPart;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;

/** Copies poses between Minecraft model parts and the core rig. */
public final class PoseWriter {
    private PoseWriter() {
    }

    static void read(ModelRenderer part, PartTransform t) {
        t.set(part.rotationPointX, part.rotationPointY, part.rotationPointZ,
                part.rotateAngleX, part.rotateAngleY, part.rotateAngleZ).setScale(1, 1, 1);
    }

    /** Vanilla pose computed by {@code ModelBiped.setRotationAngles}. */
    public static void capture(ModelBiped model, VanillaPose out) {
        read(model.bipedHead, out.get(PlayerPart.HEAD));
        read(model.bipedBody, out.get(PlayerPart.TORSO));
        read(model.bipedRightArm, out.get(PlayerPart.RIGHT_ARM));
        read(model.bipedLeftArm, out.get(PlayerPart.LEFT_ARM));
        read(model.bipedRightLeg, out.get(PlayerPart.RIGHT_LEG));
        read(model.bipedLeftLeg, out.get(PlayerPart.LEFT_LEG));
        // cape hangs slightly away from the back like vanilla's resting cape
        out.get(PlayerPart.CAPE).set(0, 0, 2, Math.toRadians(6), 0, 0);
    }

    static void write(ModelRenderer part, PlayerPose pose, PlayerPart p) {
        PartTransform t = pose.transform(p);
        part.rotationPointX = (float) t.x;
        part.rotationPointY = (float) t.y;
        part.rotationPointZ = (float) t.z;
        part.rotateAngleX = (float) t.pitch;
        part.rotateAngleY = (float) t.yaw;
        part.rotateAngleZ = (float) t.roll;
        if (part instanceof EmotePartRenderer) {
            EmotePartRenderer e = (EmotePartRenderer) part;
            e.emote = true;
            e.scaleX = (float) t.scaleX;
            e.scaleY = (float) t.scaleY;
            e.scaleZ = (float) t.scaleZ;
            e.bend = p.bend != null && EmoteSettings.bends() ? pose.bend(p) : 0;
            e.setJoint(pose.joint(p));
        }
    }

    /** Writes an emote pose into the biped parts (also used for armour models). */
    public static void apply(ModelBiped model, PlayerPose pose) {
        write(model.bipedHead, pose, PlayerPart.HEAD);
        write(model.bipedBody, pose, PlayerPart.TORSO);
        write(model.bipedRightArm, pose, PlayerPart.RIGHT_ARM);
        write(model.bipedLeftArm, pose, PlayerPart.LEFT_ARM);
        write(model.bipedRightLeg, pose, PlayerPart.RIGHT_LEG);
        write(model.bipedLeftLeg, pose, PlayerPart.LEFT_LEG);
        copy(model.bipedHead, model.bipedHeadwear);
    }

    /**
     * Puts every biped part back to its rest pose. Vanilla's setRotationAngles does not reset
     * every field (for example the arms' pivot Y or the head roll), so without this an emote
     * would leave parts where it moved them.
     */
    public static void resetRest(ModelBiped model, float armY) {
        rest(model.bipedHead, 0, 0, 0);
        rest(model.bipedHeadwear, 0, 0, 0);
        rest(model.bipedBody, 0, 0, 0);
        rest(model.bipedRightArm, -5, armY, 0);
        rest(model.bipedLeftArm, 5, armY, 0);
        rest(model.bipedRightLeg, -1.9F, 12, 0);
        rest(model.bipedLeftLeg, 1.9F, 12, 0);
    }

    private static void rest(ModelRenderer part, float x, float y, float z) {
        part.setRotationPoint(x, y, z);
        part.rotateAngleX = 0;
        part.rotateAngleY = 0;
        part.rotateAngleZ = 0;
    }

    /** Resets the emote state of every part of a model. */
    public static void clear(ModelBiped model) {
        for (Object o : model.boxList) {
            if (o instanceof EmotePartRenderer) {
                ((EmotePartRenderer) o).resetEmote();
            }
        }
    }

    public static void copy(ModelRenderer from, ModelRenderer to) {
        ModelBase.copyModelAngles(from, to);
        if (from instanceof EmotePartRenderer && to instanceof EmotePartRenderer) {
            ((EmotePartRenderer) to).copyEmote((EmotePartRenderer) from);
        }
    }
}
