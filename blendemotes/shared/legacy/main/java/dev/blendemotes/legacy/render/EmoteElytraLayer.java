package dev.blendemotes.legacy.render;
//#if MC >= 11202

import dev.blendemotes.core.pose.PlayerPose;
import dev.blendemotes.core.rig.PlayerPart;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.layers.LayerElytra;
import net.minecraft.entity.EntityLivingBase;

/** Elytra wings follow the emoting torso (1.9+). */
public class EmoteElytraLayer extends LayerElytra {
    public EmoteElytraLayer(RenderLivingBase<?> renderer) {
        super(renderer);
    }

    @Override
    public void doRenderLayer(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks,
                              float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        PlayerPose pose = entity == RenderContext.entity ? RenderContext.pose : null;
        if (pose == null) {
            super.doRenderLayer(entity, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch, scale);
            return;
        }
        GlStateManager.pushMatrix();
        GlMatrix.mult(pose.matrix(PlayerPart.TORSO), scale);
        super.doRenderLayer(entity, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch, scale);
        GlStateManager.popMatrix();
    }
}
//#endif
