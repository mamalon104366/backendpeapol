package dev.blendemotes.mc189.fabric.mixin;

import dev.blendemotes.mc189.net.ServerRelay189;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.ServerConfigurationManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerConfigurationManager.class)
public abstract class ServerConfigurationManagerMixin {
    @Inject(method = "playerLoggedOut", at = @At("HEAD"))
    private void blendemotes$logout(EntityPlayerMP player, CallbackInfo ci) {
        ServerRelay189.onLogout(player);
    }
}
