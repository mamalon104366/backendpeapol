package dev.blendemotes.mc1201.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.blendemotes.core.bend.BendMesh;
import dev.blendemotes.core.math.Mat4;
import dev.blendemotes.core.math.Vec3;
import dev.blendemotes.core.pose.PlayerPose;
import dev.blendemotes.core.rig.PlayerPart;
import dev.blendemotes.mc1201.ModernEmotes;
import dev.blendemotes.mc1201.render.MeshEmitter;
import dev.blendemotes.mc1201.render.PartMeshes;
import dev.blendemotes.mc1201.render.PoseMath;
import dev.blendemotes.mc1201.render.RenderContext;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** While emoting, the cape follows the rig's cape bone (attached to the upper torso) and bends. */
@Mixin(CapeLayer.class)
public abstract class CapeLayerMixin {
    @Unique
    private final BendMesh.Output blendemotes$buffer = new BendMesh.Output();

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;FFFFFF)V",
            at = @At("HEAD"), cancellable = true)
    private void blendemotes$cape(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                                  float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                                  float netHeadYaw, float headPitch, CallbackInfo ci) {
        PlayerPose pose = player == RenderContext.entity ? RenderContext.pose : null;
        if (pose == null) {
            return;
        }
        ci.cancel();
        if (!player.isCapeLoaded() || player.isInvisible() || !player.isModelPartShown(PlayerModelPart.CAPE)
                || player.getCloakTextureLocation() == null || player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) {
            return;
        }
        poseStack.pushPose();
        Vec3 pivot = pose.rig().pivot(PlayerPart.CAPE);
        PoseMath.mul(poseStack, pose.matrix(PlayerPart.CAPE).mul(Mat4.translation(pivot.x, pivot.y, pivot.z)));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        double bend = ModernEmotes.bendsEnabled() ? -pose.bend(PlayerPart.CAPE) : 0;
        VertexConsumer consumer = buffers.getBuffer(RenderType.entitySolid(player.getCloakTextureLocation()));
        MeshEmitter.emit(PartMeshes.cape().deform(bend, pose.joint(PlayerPart.CAPE), blendemotes$buffer),
                poseStack.last(), consumer, light, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
        poseStack.popPose();
    }
}
