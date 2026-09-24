//#if MC >= 12102
package dev.blendemotes.modern.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.blendemotes.core.pose.PlayerPose;
import dev.blendemotes.core.rig.PlayerPart;
import dev.blendemotes.modern.render.PoseMath;
import dev.blendemotes.modern.render.RenderContext;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Elytra wings follow the emoting torso (Minecraft 1.21.2 and newer). */
@Mixin(WingsLayer.class)
public abstract class WingsLayerMixin {
    @Unique
    private boolean blendemotes$pushed;

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V", at = @At("HEAD"))
    private void blendemotes$begin(PoseStack poseStack, MultiBufferSource buffers, int light, HumanoidRenderState state,
                                   float yRot, float xRot, CallbackInfo ci) {
        PlayerPose pose = RenderContext.isTarget(state) ? RenderContext.pose : null;
        blendemotes$pushed = pose != null;
        if (pose != null) {
            poseStack.pushPose();
            PoseMath.mul(poseStack, pose.matrix(PlayerPart.TORSO));
        }
    }

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V", at = @At("RETURN"))
    private void blendemotes$end(PoseStack poseStack, MultiBufferSource buffers, int light, HumanoidRenderState state,
                                 float yRot, float xRot, CallbackInfo ci) {
        if (blendemotes$pushed) {
            poseStack.popPose();
            blendemotes$pushed = false;
        }
    }
}
//#endif
