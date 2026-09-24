package dev.blendemotes.modern.mixin;

import dev.blendemotes.modern.render.PartMeshes;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
//#if MC >= 12109
import net.minecraft.client.renderer.entity.ArmorModelSet;
//#endif
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Armour gets bendable geometry so it follows bent elbows and knees. The pose reaches the armour
 * models through {@code copyPropertiesTo} (see {@code ModelPartMixin}) up to 1.21.8 and through
 * the player's render state from 1.21.9 (see {@code HumanoidModelMixin}).
 */
@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin {
    //#if MC >= 12109
    @Shadow
    @Final
    private ArmorModelSet<?> modelSet;

    // the short constructor calls this one
    @Inject(method = "<init>(Lnet/minecraft/client/renderer/entity/RenderLayerParent;Lnet/minecraft/client/renderer/entity/ArmorModelSet;Lnet/minecraft/client/renderer/entity/ArmorModelSet;Lnet/minecraft/client/renderer/entity/layers/EquipmentLayerRenderer;)V",
            at = @At("TAIL"))
    private void blendemotes$init(CallbackInfo ci) {
        // one model per armour piece: leggings use the inner (thinner) layer
        PartMeshes.armor((HumanoidModel<?>) modelSet.head(), 1.0F);
        PartMeshes.armor((HumanoidModel<?>) modelSet.chest(), 1.0F);
        PartMeshes.armor((HumanoidModel<?>) modelSet.legs(), 0.5F);
        PartMeshes.armor((HumanoidModel<?>) modelSet.feet(), 1.0F);
    }
    //#else
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
    //#endif
}
