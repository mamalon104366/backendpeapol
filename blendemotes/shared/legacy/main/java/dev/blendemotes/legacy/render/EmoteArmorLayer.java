package dev.blendemotes.legacy.render;

//#if MC >= 11202
import net.minecraft.client.renderer.entity.RenderLivingBase;
//#else
import net.minecraft.client.renderer.entity.RendererLivingEntity;
//#endif
import net.minecraft.client.renderer.entity.layers.LayerBipedArmor;

/** Armour layer whose models copy the emote pose (and bends) of the player. */
public class EmoteArmorLayer extends LayerBipedArmor {
    //#if MC >= 11202
    public EmoteArmorLayer(RenderLivingBase<?> renderer) {
    //#else
    public EmoteArmorLayer(RendererLivingEntity<?> renderer) {
    //#endif
        super(renderer);
    }

    @Override
    protected void initArmor() {
        this.modelLeggings = new EmoteArmorModel(0.5F);
        this.modelArmor = new EmoteArmorModel(1.0F);
    }
}
