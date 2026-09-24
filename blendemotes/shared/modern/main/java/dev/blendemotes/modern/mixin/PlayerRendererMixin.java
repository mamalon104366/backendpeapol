//#if MC >= 12102
package dev.blendemotes.modern.mixin;

import dev.blendemotes.modern.Compat;
import dev.blendemotes.modern.render.EmoteRenderState;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Remembers which player a render state belongs to. */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/client/player/AbstractClientPlayer;Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;F)V",
            at = @At("TAIL"))
    private void blendemotes$extract(AbstractClientPlayer player, PlayerRenderState state, float partialTick,
                                     CallbackInfo ci) {
        ((EmoteRenderState) state).blendemotes$set(player.getUUID(), Compat.slim(player));
    }
}
//#endif
