package dev.blendemotes.modern.mixin;

import dev.blendemotes.modern.ModernEmotes;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** End of every client tick, the same on every loader. */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void blendemotes$tick(CallbackInfo ci) {
        ModernEmotes.tick((Minecraft) (Object) this);
    }
}
