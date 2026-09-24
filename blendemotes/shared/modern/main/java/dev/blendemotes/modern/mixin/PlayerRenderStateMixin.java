//#if MC >= 12102
package dev.blendemotes.modern.mixin;

import dev.blendemotes.modern.render.EmoteRenderState;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.UUID;

@Mixin(PlayerRenderState.class)
public abstract class PlayerRenderStateMixin implements EmoteRenderState {
    @Unique
    private UUID blendemotes$player;
    @Unique
    private boolean blendemotes$slim;

    @Override
    public void blendemotes$set(UUID player, boolean slim) {
        blendemotes$player = player;
        blendemotes$slim = slim;
    }

    @Override
    public UUID blendemotes$player() {
        return blendemotes$player;
    }

    @Override
    public boolean blendemotes$slim() {
        return blendemotes$slim;
    }
}
//#endif
