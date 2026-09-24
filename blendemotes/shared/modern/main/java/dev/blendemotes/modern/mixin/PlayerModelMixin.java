package dev.blendemotes.modern.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.blendemotes.core.math.Mat4;
import dev.blendemotes.core.math.Vec3;
import dev.blendemotes.core.pose.PlayerPose;
import dev.blendemotes.core.pose.VanillaPose;
import dev.blendemotes.core.rig.PlayerPart;
import dev.blendemotes.modern.ModernEmotes;
import dev.blendemotes.modern.render.PartMeshes;
import dev.blendemotes.modern.render.PoseApplier;
import dev.blendemotes.modern.render.PoseMath;
import dev.blendemotes.modern.render.RenderContext;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {
    @Shadow
    @Final
    private boolean slim;

    @Unique
    private final VanillaPose blendemotes$vanilla = new VanillaPose();
    @Unique
    private final PlayerPose blendemotes$pose = new PlayerPose();

    @Inject(method = "<init>", at = @At("TAIL"))
    private void blendemotes$init(ModelPart root, boolean slim, CallbackInfo ci) {
        PartMeshes.player((PlayerModel<?>) (Object) this, slim);
    }

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("HEAD"))
    private void blendemotes$reset(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                                   float netHeadYaw, float headPitch, CallbackInfo ci) {
        PoseApplier.resetDirty((PlayerModel<?>) (Object) this);
    }

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void blendemotes$emote(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                                   float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (entity != RenderContext.entity || ModernEmotes.client() == null) {
            return;
        }
        PlayerModel<?> model = (PlayerModel<?>) (Object) this;
        PoseApplier.capture(model, blendemotes$vanilla);
        PlayerPose pose = ModernEmotes.client().pose(entity.getUUID(), blendemotes$vanilla, slim, ageInTicks / 20.0, blendemotes$pose);
        RenderContext.pose = pose;
        if (pose != null) {
            PoseApplier.apply(model, pose);
        }
    }

    /** Held items follow the bent forearm and the rig's item bones. */
    @Inject(method = "translateToHand", at = @At("HEAD"), cancellable = true)
    private void blendemotes$hand(HumanoidArm arm, PoseStack poseStack, CallbackInfo ci) {
        PlayerPose pose = RenderContext.pose;
        if (pose == null || RenderContext.entity == null) {
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
