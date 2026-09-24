package dev.blendemotes.mc189.render;

import dev.blendemotes.core.math.Mat4;
import dev.blendemotes.core.math.Vec3;
import dev.blendemotes.core.pose.PlayerPose;
import dev.blendemotes.core.pose.VanillaPose;
import dev.blendemotes.core.rig.BendProfile;
import dev.blendemotes.core.rig.PlayerPart;
import dev.blendemotes.mc189.BlendEmotes189;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.entity.Entity;

/**
 * The vanilla player model with rig-driven parts. Limbs, body and the second skin layer are
 * {@link EmotePartRenderer}s so they can bend; everything else is untouched.
 */
public class EmoteModelPlayer extends ModelPlayer {
    private final boolean slim;
    private final VanillaPose vanilla = new VanillaPose();
    private final PlayerPose pose = new PlayerPose();

    public EmoteModelPlayer(float size, boolean slim) {
        super(size, slim);
        this.slim = slim;
        float armY = slim ? 2.5F : 2.0F;
        int armW = slim ? 3 : 4;
        bipedHead = part(0, 0, -4, -8, -4, 8, 8, 8, size, false, null, 0, 0, 0);
        bipedHeadwear = part(32, 0, -4, -8, -4, 8, 8, 8, size + 0.5F, false, null, 0, 0, 0);
        bipedBody = part(16, 16, -4, 0, -2, 8, 12, 4, size, false, BendProfile.TORSO, 0, 0, 0);
        bipedBodyWear = part(16, 32, -4, 0, -2, 8, 12, 4, size + 0.25F, false, BendProfile.TORSO, 0, 0, 0);
        bipedRightArm = part(40, 16, slim ? -2 : -3, -2, -2, armW, 12, 4, size, false, BendProfile.ARM, -5, armY, 0);
        bipedRightArmwear = part(40, 32, slim ? -2 : -3, -2, -2, armW, 12, 4, size + 0.25F, false, BendProfile.ARM, -5, armY, 0);
        bipedLeftArm = part(32, 48, -1, -2, -2, armW, 12, 4, size, false, BendProfile.ARM, 5, armY, 0);
        bipedLeftArmwear = part(48, 48, -1, -2, -2, armW, 12, 4, size + 0.25F, false, BendProfile.ARM, 5, armY, 0);
        bipedRightLeg = part(0, 16, -2, 0, -2, 4, 12, 4, size, false, BendProfile.LEG, -1.9F, 12, 0);
        bipedRightLegwear = part(0, 32, -2, 0, -2, 4, 12, 4, size + 0.25F, false, BendProfile.LEG, -1.9F, 12, 0);
        bipedLeftLeg = part(16, 48, -2, 0, -2, 4, 12, 4, size, false, BendProfile.LEG, 1.9F, 12, 0);
        bipedLeftLegwear = part(0, 48, -2, 0, -2, 4, 12, 4, size + 0.25F, false, BendProfile.LEG, 1.9F, 12, 0);
    }

    private EmotePartRenderer part(int u, int v, float x, float y, float z, int w, int h, int d, float inflate,
                                   boolean mirror, BendProfile profile, float px, float py, float pz) {
        EmotePartRenderer r = new EmotePartRenderer(this, u, v, x, y, z, w, h, d, inflate, mirror, profile);
        r.setRotationPoint(px, py, pz);
        return r;
    }

    public boolean isSlim() {
        return slim;
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                                  float headPitch, float scale, Entity entity) {
        PoseWriter.clear(this);
        PoseWriter.resetRest(this, slim ? 2.5F : 2.0F);
        super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        if (entity != RenderContext.entity || !(entity instanceof AbstractClientPlayer) || BlendEmotes189.client() == null) {
            return;
        }
        PoseWriter.capture(this, vanilla);
        PlayerPose p = BlendEmotes189.client().pose(entity.getUniqueID(), vanilla, slim, ageInTicks / 20.0, pose);
        RenderContext.pose = p;
        if (p == null) {
            return;
        }
        PoseWriter.apply(this, p);
        PoseWriter.copy(bipedLeftLeg, bipedLeftLegwear);
        PoseWriter.copy(bipedRightLeg, bipedRightLegwear);
        PoseWriter.copy(bipedLeftArm, bipedLeftArmwear);
        PoseWriter.copy(bipedRightArm, bipedRightArmwear);
        PoseWriter.copy(bipedBody, bipedBodyWear);
    }

    /** Held item: follow the (bent) forearm and the rig's item bone. */
    @Override
    public void postRenderArm(float scale) {
        PlayerPose p = RenderContext.active() && RenderContext.pose != null ? RenderContext.pose : null;
        if (p == null) {
            super.postRenderArm(scale);
            return;
        }
        Vec3 pivot = p.rig().pivot(PlayerPart.RIGHT_ARM);
        Mat4 m = p.matrix(PlayerPart.RIGHT_ITEM).mul(Mat4.translation(pivot.x + (slim ? 1 : 0), pivot.y, pivot.z));
        GlMatrix.mult(m, scale);
    }
}
