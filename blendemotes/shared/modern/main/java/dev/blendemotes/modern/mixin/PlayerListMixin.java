package dev.blendemotes.modern.mixin;

import dev.blendemotes.modern.net.ServerRelay;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A player left the server (every loader): stop relaying their emote. */
@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    @Inject(method = "remove", at = @At("HEAD"))
    private void blendemotes$remove(ServerPlayer player, CallbackInfo ci) {
        ServerRelay.onLogout(player);
    }
}
