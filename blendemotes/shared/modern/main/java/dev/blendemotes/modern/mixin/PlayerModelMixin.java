package dev.blendemotes.modern.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.blendemotes.core.math.Mat4;
import dev.blendemotes.core.math.Vec3;
import dev.blendemotes.core.pose.PlayerPose;
import dev.blendemotes.core.rig.PlayerPart;
import dev.blendemotes.modern.render.PartMeshes;
import dev.blendemotes.modern.render.PoseMath;
import dev.blendemotes.modern.render.RenderContext;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//#if MC >= 12109
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
//#endif
//#if MC < 12102
import dev.blendemotes.core.pose.VanillaPose;
import dev.blendemotes.modern.ModernEmotes;
import dev.blendemotes.modern.render.PoseApplier;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Unique;
//#endif

/** Runs the emote after vanilla animated the player model and moves held items with the rig. */
@SuppressWarnings("rawtypes")
@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {
    @Shadow
    @Final
    private boolean slim;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void blendemotes$init(CallbackInfo ci) {
        PartMeshes.player((PlayerModel) (Object) this, slim);
    }

    //#if MC < 12102
    @Unique
    private final VanillaPose blendemotes$vanilla = new VanillaPose();
    @Unique
    private final PlayerPose blendemotes$pose = new PlayerPose();

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("HEAD"))
    private void blendemotes$reset(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                                   float netHeadYaw, float headPitch, CallbackInfo ci) {
        PoseApplier.resetDirty((PlayerModel) (Object) this, slim);
    }

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void blendemotes$emote(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                                   float netHeadYaw, float headPitch, CallbackInfo ci) {
        PlayerModel model = (PlayerModel) (Object) this;
        if (!RenderContext.isTarget(entity) || ModernEmotes.client() == null) {
            return;
        }
        PoseApplier.capture(model, blendemotes$vanilla);
        PlayerPose pose = ModernEmotes.client().pose(RenderContext.player, blendemotes$vanilla, slim, ageInTicks / 20.0,
                blendemotes$pose);
        RenderContext.pose = pose;
        if (pose != null) {
            PoseApplier.apply(model, pose);
        }
    }
    //#endif
    // 1.21.2 and newer: see HumanoidModelMixin

    /** Held items follow the bent forearm and the rig's item bones. */
    //#if MC >= 12109
    @Inject(method = "translateToHand(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lnet/minecraft/world/entity/HumanoidArm;Lcom/mojang/blaze3d/vertex/PoseStack;)V",
            at = @At("HEAD"), cancellable = true)
    private void blendemotes$hand(AvatarRenderState state, HumanoidArm arm, PoseStack poseStack, CallbackInfo ci) {
    //#else
    @Inject(method = "translateToHand", at = @At("HEAD"), cancellable = true)
    private void blendemotes$hand(HumanoidArm arm, PoseStack poseStack, CallbackInfo ci) {
    //#endif
        PlayerPose pose = RenderContext.pose;
        if (pose == null || RenderContext.target == null) {
            return;
        }
        boolean right = arm == HumanoidArm.RIGHT;
        PlayerPart item = right ? PlayerPart.RIGHT_ITEM : PlayerPart.LEFT_ITEM;
        Vec3 pivot = pose.rig().pivot(right ? PlayerPart.RIGHT_ARM : PlayerPart.LEFT_ARM);
        double slimOffset = slim ? (right ? 0.5 : -0.5) : 0;
        Mat4 m = pose.matrix(item).mul(Mat4.translation(pivot.x + slimOffset, pivot.y, pivot.z));
        PoseMath.mul(poseStack, m);
        ci.cancel();
    }
}
