package dev.blendemotes.modern.mixin;

import dev.blendemotes.modern.render.PartMeshes;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Armour gets bendable geometry so it follows bent elbows and knees (the pose itself reaches
 * the armour models through {@code copyPropertiesTo}, see {@code ModelPartMixin}).
 */
@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin {
    @Shadow
    @Final
    private HumanoidModel<?> innerModel;
    @Shadow
    @Final
    private HumanoidModel<?> outerModel;

    //#if MC >= 12102
    // the short constructor calls this one
    @Inject(method = "<init>(Lnet/minecraft/client/renderer/entity/RenderLayerParent;Lnet/minecraft/client/model/HumanoidModel;Lnet/minecraft/client/model/HumanoidModel;Lnet/minecraft/client/model/HumanoidModel;Lnet/minecraft/client/model/HumanoidModel;Lnet/minecraft/client/renderer/entity/layers/EquipmentLayerRenderer;)V",
            at = @At("TAIL"))
    //#else
    @Inject(method = "<init>", at = @At("TAIL"))
    //#endif
    private void blendemotes$init(CallbackInfo ci) {
        PartMeshes.armor(innerModel, 0.5F);
        PartMeshes.armor(outerModel, 1.0F);
    }
}
