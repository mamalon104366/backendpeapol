package dev.blendemotes.modern.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.blendemotes.core.math.Mat4;
import dev.blendemotes.modern.ModernEmotes;
import dev.blendemotes.modern.render.PoseMath;
import dev.blendemotes.modern.render.RenderContext;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//#if MC >= 12102
import dev.blendemotes.modern.render.EmoteRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
//#else
import dev.blendemotes.modern.Compat;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
//#endif

/** Marks the player being rendered and applies the rig's body bone to the whole model. */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Unique
    private final Mat4 blendemotes$root = new Mat4();

    /** Model space is ready here (after the flip and the 1.501 offset): apply the rig's body bone. */
    @Unique
    private void blendemotes$applyRoot(PoseStack poseStack) {
        if (RenderContext.player != null && ModernEmotes.client() != null
                && ModernEmotes.client().root(RenderContext.player, RenderContext.slim, blendemotes$root)) {
            PoseMath.mul(poseStack, blendemotes$root);
        }
    }

    //#if MC >= 12102
    @Inject(method = "render(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"))
    private void blendemotes$begin(LivingEntityRenderState state, PoseStack poseStack, MultiBufferSource buffers,
                                   int light, CallbackInfo ci) {
        if (state instanceof EmoteRenderState) {
            EmoteRenderState s = (EmoteRenderState) state;
            RenderContext.begin(state, s.blendemotes$player(), s.blendemotes$slim());
        } else {
            RenderContext.begin(null, null, false);
        }
    }

    @Inject(method = "render(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;setupAnim(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;)V"))
    private void blendemotes$root(LivingEntityRenderState state, PoseStack poseStack, MultiBufferSource buffers,
                                  int light, CallbackInfo ci) {
        if (RenderContext.isTarget(state)) {
            blendemotes$applyRoot(poseStack);
        }
    }

    @Inject(method = "render(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("RETURN"))
    private void blendemotes$end(LivingEntityRenderState state, PoseStack poseStack, MultiBufferSource buffers,
                                 int light, CallbackInfo ci) {
        RenderContext.end();
    }
    //#else
    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"))
    private void blendemotes$begin(LivingEntity entity, float yaw, float partialTicks, PoseStack poseStack,
                                   MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (entity instanceof AbstractClientPlayer) {
            RenderContext.begin(entity, entity.getUUID(), Compat.slim((AbstractClientPlayer) entity));
        } else {
            RenderContext.begin(null, null, false);
        }
    }

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;prepareMobModel(Lnet/minecraft/world/entity/Entity;FFF)V"))
    private void blendemotes$root(LivingEntity entity, float yaw, float partialTicks, PoseStack poseStack,
                                  MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (RenderContext.isTarget(entity)) {
            blendemotes$applyRoot(poseStack);
        }
    }

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("RETURN"))
    private void blendemotes$end(LivingEntity entity, float yaw, float partialTicks, PoseStack poseStack,
                                 MultiBufferSource buffers, int light, CallbackInfo ci) {
        RenderContext.end();
    }
    //#endif
}
