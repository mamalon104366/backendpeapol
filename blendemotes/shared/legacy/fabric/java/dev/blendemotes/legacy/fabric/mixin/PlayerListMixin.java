package dev.blendemotes.legacy.fabric.mixin;

import dev.blendemotes.legacy.net.ServerRelay;
import net.minecraft.entity.player.EntityPlayerMP;
//#if MC >= 11202
import net.minecraft.server.management.PlayerList;
//#else
import net.minecraft.server.management.ServerConfigurationManager;
//#endif
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC >= 11202
@Mixin(PlayerList.class)
//#else
@Mixin(ServerConfigurationManager.class)
//#endif
public abstract class PlayerListMixin {
    @Inject(method = "playerLoggedOut", at = @At("HEAD"))
    private void blendemotes$logout(EntityPlayerMP player, CallbackInfo ci) {
        ServerRelay.onLogout(player);
    }
}
