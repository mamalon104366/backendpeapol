//#if MC >= 12102
package dev.blendemotes.modern.mixin;

import dev.blendemotes.modern.render.EmoteRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
//#if MC >= 12109
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
//#else
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
//#endif

import java.util.UUID;

/** The player's render state remembers who it belongs to (Minecraft 1.21.2 and newer). */
//#if MC >= 12109
@Mixin(AvatarRenderState.class)
//#else
@Mixin(PlayerRenderState.class)
//#endif
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
