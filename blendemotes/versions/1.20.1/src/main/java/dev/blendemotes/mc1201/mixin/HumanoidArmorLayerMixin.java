package dev.blendemotes.mc1201.mixin;

import dev.blendemotes.mc1201.render.PartMeshes;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.resources.model.ModelManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Armour gets bendable geometry so it follows bent elbows and knees. */
@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void blendemotes$init(RenderLayerParent<?, ?> renderer, HumanoidModel<?> inner, HumanoidModel<?> outer,
                                  ModelManager models, CallbackInfo ci) {
        PartMeshes.armor(inner, 0.5F);
        PartMeshes.armor(outer, 1.0F);
    }
}
