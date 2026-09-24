package dev.blendemotes.modern.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.blendemotes.core.bend.BendMesh;
import dev.blendemotes.core.math.Mat4;
import dev.blendemotes.core.math.Vec3;
import dev.blendemotes.core.pose.PlayerPose;
import dev.blendemotes.core.rig.PlayerPart;
import dev.blendemotes.modern.ModernEmotes;
import dev.blendemotes.modern.render.MeshEmitter;
import dev.blendemotes.modern.render.PartMeshes;
import dev.blendemotes.modern.render.PoseMath;
import dev.blendemotes.modern.render.RenderContext;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
//#if MC >= 12111
import net.minecraft.client.renderer.rendertype.RenderTypes;
//#endif
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//#if MC >= 12109
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Shadow;
//#elseif MC >= 12102
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Shadow;
//#else
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.Items;
//#endif

/** While emoting, the cape follows the rig's cape bone (attached to the upper torso) and bends. */
@Mixin(CapeLayer.class)
public abstract class CapeLayerMixin {
    @Unique
    private final BendMesh.Output blendemotes$buffer = new BendMesh.Output();

    @Unique
    private void blendemotes$draw(PoseStack poseStack, MultiBufferSource buffers, int light, PlayerPose pose,
                                  ResourceLocation texture) {
        poseStack.pushPose();
        Vec3 pivot = pose.rig().pivot(PlayerPart.CAPE);
        PoseMath.mul(poseStack, pose.matrix(PlayerPart.CAPE).mul(Mat4.translation(pivot.x, pivot.y, pivot.z))
                .mul(Mat4.rotationY(Math.PI)));
        double bend = ModernEmotes.bendsEnabled() ? -pose.bend(PlayerPart.CAPE) : 0;
        VertexConsumer consumer = buffers.getBuffer(RenderType.entitySolid(texture));
        MeshEmitter.emit(PartMeshes.cape().deform(bend, pose.joint(PlayerPart.CAPE), blendemotes$buffer),
                poseStack.last(), consumer, light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        poseStack.popPose();
    }

    //#if MC >= 12109
    @Shadow
    private boolean hasLayer(ItemStack stack, EquipmentClientInfo.LayerType layer) {
        throw new AssertionError();
    }

    /** Drawing is deferred: the deformed cape is computed now and emitted later. */
    @Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V",
            at = @At("HEAD"), cancellable = true)
    private void blendemotes$cape(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state,
                                  float yRot, float xRot, CallbackInfo ci) {
        PlayerPose pose = RenderContext.isTarget(state) ? RenderContext.pose : null;
        if (pose == null) {
            return;
        }
        ci.cancel();
        if (state.isInvisible || !state.showCape || state.skin.cape() == null
                || hasLayer(state.chestEquipment, EquipmentClientInfo.LayerType.WINGS)) {
            return;
        }
        ResourceLocation texture = state.skin.cape().texturePath();
        poseStack.pushPose();
        Vec3 pivot = pose.rig().pivot(PlayerPart.CAPE);
        PoseMath.mul(poseStack, pose.matrix(PlayerPart.CAPE).mul(Mat4.translation(pivot.x, pivot.y, pivot.z))
                .mul(Mat4.rotationY(Math.PI)));
        double bend = ModernEmotes.bendsEnabled() ? -pose.bend(PlayerPart.CAPE) : 0;
        final BendMesh.Output mesh = PartMeshes.cape().deform(bend, pose.joint(PlayerPart.CAPE), new BendMesh.Output());
        collector.submitCustomGeometry(poseStack, entitySolid(texture),
                (p, consumer) -> MeshEmitter.emit(mesh, p, consumer, light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF));
        poseStack.popPose();
    }

    @Unique
    private static RenderType entitySolid(ResourceLocation texture) {
        //#if MC >= 12111
        return RenderTypes.entitySolid(texture);
        //#else
        return RenderType.entitySolid(texture);
        //#endif
    }
    //#elseif MC >= 12102
    @Shadow
    private boolean hasLayer(ItemStack stack, EquipmentClientInfo.LayerType layer) {
        throw new AssertionError();
    }

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/PlayerRenderState;FF)V",
            at = @At("HEAD"), cancellable = true)
    private void blendemotes$cape(PoseStack poseStack, MultiBufferSource buffers, int light, PlayerRenderState state,
                                  float yRot, float xRot, CallbackInfo ci) {
        PlayerPose pose = RenderContext.isTarget(state) ? RenderContext.pose : null;
        if (pose == null) {
            return;
        }
        ci.cancel();
        ResourceLocation texture = state.skin.capeTexture();
        if (state.isInvisible || !state.showCape || texture == null
                || hasLayer(state.chestEquipment, EquipmentClientInfo.LayerType.WINGS)) {
            return;
        }
        blendemotes$draw(poseStack, buffers, light, pose, texture);
    }
    //#else
    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;FFFFFF)V",
            at = @At("HEAD"), cancellable = true)
    private void blendemotes$cape(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                                  float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                                  float netHeadYaw, float headPitch, CallbackInfo ci) {
        PlayerPose pose = RenderContext.isTarget(player) ? RenderContext.pose : null;
        if (pose == null) {
            return;
        }
        ci.cancel();
        //#if MC >= 12002
        ResourceLocation texture = player.getSkin().capeTexture();
        //#else
        ResourceLocation texture = player.isCapeLoaded() ? player.getCloakTextureLocation() : null;
        //#endif
        if (texture == null || player.isInvisible() || !player.isModelPartShown(PlayerModelPart.CAPE)
                || player.getItemBySlot(EquipmentSlot.CHEST).getItem() == Items.ELYTRA) {
            return;
        }
        blendemotes$draw(poseStack, buffers, light, pose, texture);
    }
    //#endif
}
