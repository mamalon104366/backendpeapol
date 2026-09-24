package dev.blendemotes.modern.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.blendemotes.core.math.Mat4;
import dev.blendemotes.modern.ModernEmotes;
import dev.blendemotes.modern.render.PoseMath;
import dev.blendemotes.modern.render.RenderContext;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Unique
    private final Mat4 blendemotes$root = new Mat4();
    @Unique
    private Entity blendemotes$previous;

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"))
    private void blendemotes$begin(LivingEntity entity, float yaw, float partialTicks, PoseStack poseStack,
                                   MultiBufferSource buffers, int light, CallbackInfo ci) {
        blendemotes$previous = RenderContext.entity;
        if (entity instanceof AbstractClientPlayer) {
            RenderContext.entity = entity;
            RenderContext.pose = null;
            RenderContext.slim = "slim".equals(((AbstractClientPlayer) entity).getModelName());
        }
    }

    /** Model space is ready here (after the flip and the 1.501 offset): apply the rig's body bone. */
    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;prepareMobModel(Lnet/minecraft/world/entity/Entity;FFF)V"))
    private void blendemotes$root(LivingEntity entity, float yaw, float partialTicks, PoseStack poseStack,
                                  MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (entity == RenderContext.entity && ModernEmotes.client() != null
                && ModernEmotes.client().root(entity.getUUID(), RenderContext.slim, blendemotes$root)) {
            PoseMath.mul(poseStack, blendemotes$root);
        }
    }

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("RETURN"))
    private void blendemotes$end(LivingEntity entity, float yaw, float partialTicks, PoseStack poseStack,
                                 MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (entity instanceof AbstractClientPlayer) {
            RenderContext.entity = blendemotes$previous;
            RenderContext.pose = null;
        }
    }
}
