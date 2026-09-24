//#if MC >= 12102
package dev.blendemotes.modern.mixin;

import dev.blendemotes.core.pose.PlayerPose;
import dev.blendemotes.core.pose.VanillaPose;
import dev.blendemotes.modern.ModernEmotes;
import dev.blendemotes.modern.render.EmoteRenderState;
import dev.blendemotes.modern.render.PoseApplier;
import dev.blendemotes.modern.render.RenderContext;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * Minecraft 1.21.2 and newer: the emote pose is applied after vanilla animated a humanoid model
 * from a player's render state. That covers the player model and the armour models (which get
 * the player's render state too), and works when the drawing is deferred (1.21.9 and newer).
 */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {
    @Unique
    private final VanillaPose blendemotes$vanilla = new VanillaPose();
    @Unique
    private final PlayerPose blendemotes$pose = new PlayerPose();

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("HEAD"))
    private void blendemotes$reset(HumanoidRenderState state, CallbackInfo ci) {
        PoseApplier.resetDirty((HumanoidModel<?>) (Object) this, false);
    }

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("TAIL"))
    private void blendemotes$emote(HumanoidRenderState state, CallbackInfo ci) {
        if (!(state instanceof EmoteRenderState) || ModernEmotes.client() == null) {
            return;
        }
        EmoteRenderState s = (EmoteRenderState) state;
        UUID player = s.blendemotes$player();
        if (player == null) {
            return;
        }
        HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;
        PoseApplier.capture(model, blendemotes$vanilla);
        PlayerPose pose = ModernEmotes.client().pose(player, blendemotes$vanilla, s.blendemotes$slim(),
                state.ageInTicks / 20.0, blendemotes$pose);
        if (pose != null) {
            PoseApplier.apply(model, pose);
        }
        if (RenderContext.isTarget(state)) {
            // held items and the cape are placed right after this (same frame, same thread)
            RenderContext.pose = pose;
        }
    }
}
//#endif
