package dev.blendemotes.legacy.render;

import dev.blendemotes.core.math.Mat4;
import dev.blendemotes.core.math.Vec3;
import dev.blendemotes.core.pose.PlayerPose;
import dev.blendemotes.core.rig.BendProfile;
import dev.blendemotes.core.rig.PlayerPart;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerCape;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.player.EnumPlayerModelParts;
//#if MC >= 11202
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
//#endif

/**
 * Cape: vanilla physics normally; while emoting it follows the rig's cape bone (attached to
 * the upper torso) and bends.
 */
public class EmoteCapeLayer implements LayerRenderer<AbstractClientPlayer> {
    private final RenderPlayer renderer;
    private final LayerCape vanilla;
    private final EmotePartRenderer cape;

    public EmoteCapeLayer(RenderPlayer renderer) {
        this.renderer = renderer;
        this.vanilla = new LayerCape(renderer);
        ModelBase model = new ModelBase() {
        };
        model.textureWidth = 64;
        model.textureHeight = 32;
        this.cape = new EmotePartRenderer(model, 0, 0, -5, 0, -1, 10, 16, 1, 0, false, BendProfile.CAPE);
    }

    @Override
    public void doRenderLayer(AbstractClientPlayer entity, float limbSwing, float limbSwingAmount, float partialTicks,
                              float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        PlayerPose pose = entity == RenderContext.entity ? RenderContext.pose : null;
        if (pose == null) {
            vanilla.doRenderLayer(entity, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch, scale);
            return;
        }
        if (!entity.hasPlayerInfo() || entity.isInvisible() || !entity.isWearing(EnumPlayerModelParts.CAPE)
                || entity.getLocationCape() == null) {
            return;
        }
        //#if MC >= 11202
        if (entity.getItemStackFromSlot(EntityEquipmentSlot.CHEST).getItem() == Items.ELYTRA) {
            return; // the elytra replaces the cape
        }
        //#endif
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        renderer.bindTexture(entity.getLocationCape());
        GlStateManager.pushMatrix();
        Vec3 pivot = pose.rig().pivot(PlayerPart.CAPE);
        Mat4 m = pose.matrix(PlayerPart.CAPE).mul(Mat4.translation(pivot.x, pivot.y, pivot.z));
        GlMatrix.mult(m, scale);
        // the cape model is drawn turned around (like vanilla), so the bend axis flips
        GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
        cape.emote = true;
        cape.bend = EmoteSettings.bends() ? -pose.bend(PlayerPart.CAPE) : 0;
        cape.setJoint(pose.joint(PlayerPart.CAPE));
        cape.draw(scale);
        GlStateManager.popMatrix();
    }

    @Override
    public boolean shouldCombineTextures() {
        return false;
    }
}
