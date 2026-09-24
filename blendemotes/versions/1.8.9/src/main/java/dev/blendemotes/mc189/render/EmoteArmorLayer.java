package dev.blendemotes.mc189.render;

import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.client.renderer.entity.layers.LayerBipedArmor;

/** Armour layer whose models copy the emote pose (and bends) of the player. */
public class EmoteArmorLayer extends LayerBipedArmor {
    public EmoteArmorLayer(RendererLivingEntity<?> renderer) {
        super(renderer);
    }

    @Override
    protected void initArmor() {
        this.modelLeggings = new EmoteArmorModel(0.5F);
        this.modelArmor = new EmoteArmorModel(1.0F);
    }
}
