//#if MC >= 12102
package dev.blendemotes.modern.mixin;

import dev.blendemotes.modern.render.EmoteRenderState;
import dev.blendemotes.modern.render.PoseApplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//#if MC >= 12109
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.PlayerModelType;
//#else
import dev.blendemotes.modern.Compat;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.resources.ResourceLocation;
//#endif

/**
 * Remembers which player a render state belongs to, and keeps emote bends away from the
 * first person hand (Minecraft 1.21.2 and newer).
 */
//#if MC >= 12109
@Mixin(AvatarRenderer.class)
public abstract class PlayerRendererMixin extends LivingEntityRenderer<Avatar, AvatarRenderState, PlayerModel> {
    protected PlayerRendererMixin(EntityRendererProvider.Context context, PlayerModel model, float shadow) {
        super(context, model, shadow);
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
            at = @At("TAIL"))
    private void blendemotes$extract(Avatar avatar, AvatarRenderState state, float partialTick, CallbackInfo ci) {
        ((EmoteRenderState) state).blendemotes$set(avatar.getUUID(), state.skin.model() == PlayerModelType.SLIM);
    }

    @Inject(method = "renderHand", at = @At("HEAD"))
    private void blendemotes$hand(PoseStack poseStack, SubmitNodeCollector collector, int light, ResourceLocation skin,
                                  ModelPart arm, boolean sleeve, CallbackInfo ci) {
        PoseApplier.clear(getModel());
    }
}
//#else
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin extends LivingEntityRenderer<AbstractClientPlayer, PlayerRenderState, PlayerModel> {
    protected PlayerRendererMixin(EntityRendererProvider.Context context, PlayerModel model, float shadow) {
        super(context, model, shadow);
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/client/player/AbstractClientPlayer;Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;F)V",
            at = @At("TAIL"))
    private void blendemotes$extract(AbstractClientPlayer player, PlayerRenderState state, float partialTick,
                                     CallbackInfo ci) {
        ((EmoteRenderState) state).blendemotes$set(player.getUUID(), Compat.slim(player));
    }

    @Inject(method = "renderHand", at = @At("HEAD"))
    private void blendemotes$hand(PoseStack poseStack, MultiBufferSource buffers, int light, ResourceLocation skin,
                                  ModelPart arm, boolean sleeve, CallbackInfo ci) {
        PoseApplier.clear(getModel());
    }
}
//#endif
//#endif
