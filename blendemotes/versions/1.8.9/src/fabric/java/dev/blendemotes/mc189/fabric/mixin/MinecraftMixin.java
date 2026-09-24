package dev.blendemotes.mc189.fabric.mixin;

import dev.blendemotes.mc189.fabric.FabricClientHooks;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "runTick", at = @At("TAIL"))
    private void blendemotes$tick(CallbackInfo ci) {
        FabricClientHooks.tick();
    }
}
