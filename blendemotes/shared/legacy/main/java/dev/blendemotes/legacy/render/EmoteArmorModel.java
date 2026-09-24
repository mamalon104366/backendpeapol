package dev.blendemotes.legacy.render;

import dev.blendemotes.core.rig.BendProfile;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.Entity;

/** Armour model (64x32 armour texture) with bendable parts. */
public class EmoteArmorModel extends ModelBiped {
    public EmoteArmorModel(float size) {
        super(size, 0.0F, 64, 32);
        bipedHead = part(0, 0, -4, -8, -4, 8, 8, 8, size, false, null, 0, 0, 0);
        bipedHeadwear = part(32, 0, -4, -8, -4, 8, 8, 8, size + 0.5F, false, null, 0, 0, 0);
        bipedBody = part(16, 16, -4, 0, -2, 8, 12, 4, size, false, BendProfile.TORSO, 0, 0, 0);
        bipedRightArm = part(40, 16, -3, -2, -2, 4, 12, 4, size, false, BendProfile.ARM, -5, 2, 0);
        bipedLeftArm = part(40, 16, -1, -2, -2, 4, 12, 4, size, true, BendProfile.ARM, 5, 2, 0);
        bipedRightLeg = part(0, 16, -2, 0, -2, 4, 12, 4, size, false, BendProfile.LEG, -1.9F, 12, 0);
        bipedLeftLeg = part(0, 16, -2, 0, -2, 4, 12, 4, size, true, BendProfile.LEG, 1.9F, 12, 0);
    }

    private EmotePartRenderer part(int u, int v, float x, float y, float z, int w, int h, int d, float inflate,
                                   boolean mirror, BendProfile profile, float px, float py, float pz) {
        EmotePartRenderer r = new EmotePartRenderer(this, u, v, x, y, z, w, h, d, inflate, mirror, profile);
        r.setRotationPoint(px, py, pz);
        return r;
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                                  float headPitch, float scale, Entity entity) {
        PoseWriter.clear(this);
        PoseWriter.resetRest(this, 2.0F);
        super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        if (entity == RenderContext.entity && RenderContext.pose != null) {
            PoseWriter.apply(this, RenderContext.pose);
        }
    }
}
