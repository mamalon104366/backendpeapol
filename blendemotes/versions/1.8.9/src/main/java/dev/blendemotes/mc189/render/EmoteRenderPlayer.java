package dev.blendemotes.mc189.render;

import dev.blendemotes.core.math.Mat4;
import dev.blendemotes.mc189.BlendEmotes189;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.client.renderer.entity.layers.LayerArrow;
import net.minecraft.client.renderer.entity.layers.LayerBipedArmor;
import net.minecraft.client.renderer.entity.layers.LayerCape;
import net.minecraft.client.renderer.entity.layers.LayerCustomHead;
import net.minecraft.client.renderer.entity.layers.LayerDeadmau5Head;
import net.minecraft.client.renderer.entity.layers.LayerHeldItem;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;

import java.util.ArrayList;
import java.util.List;

/** Player renderer using {@link EmoteModelPlayer} and emote aware layers. */
public class EmoteRenderPlayer extends RenderPlayer {
    private final boolean slim;
    private final Mat4 root = new Mat4();

    public EmoteRenderPlayer(RenderManager renderManager, boolean slim, RenderPlayer previous) {
        super(renderManager, slim);
        this.slim = slim;
        this.mainModel = new EmoteModelPlayer(0.0F, slim);
        this.layerRenderers.clear();
        this.addLayer(new EmoteArmorLayer(this));
        this.addLayer(new LayerHeldItem(this));
        this.addLayer(new LayerArrow(this));
        this.addLayer(new LayerDeadmau5Head(this));
        this.addLayer(new EmoteCapeLayer(this));
        this.addLayer(new LayerCustomHead(this.getMainModel().bipedHead));
        if (previous != null) {
            keepForeignLayers(previous);
        }
    }

    /** Layers other mods added to the vanilla renderer are kept. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void keepForeignLayers(RenderPlayer previous) {
        List<LayerRenderer<AbstractClientPlayer>> foreign = new ArrayList<LayerRenderer<AbstractClientPlayer>>();
        for (Object o : layersOf(previous)) {
            if (o instanceof LayerBipedArmor || o instanceof LayerHeldItem || o instanceof LayerArrow
                    || o instanceof LayerDeadmau5Head || o instanceof LayerCape || o instanceof LayerCustomHead) {
                continue;
            }
            foreign.add((LayerRenderer) o);
        }
        for (LayerRenderer<AbstractClientPlayer> l : foreign) {
            this.addLayer(l);
        }
    }

    /**
     * The layer list of another renderer (protected field; found by type because its name
     * differs between Forge and Fabric at runtime).
     */
    private static List<?> layersOf(RenderPlayer renderer) {
        for (java.lang.reflect.Field f : RendererLivingEntity.class.getDeclaredFields()) {
            if (List.class.isAssignableFrom(f.getType())) {
                try {
                    f.setAccessible(true);
                    Object value = f.get(renderer);
                    if (value instanceof List) {
                        return (List<?>) value;
                    }
                } catch (Exception ignored) {
                    // fall through
                }
            }
        }
        return new ArrayList<Object>();
    }

    @Override
    public void doRender(AbstractClientPlayer entity, double x, double y, double z, float entityYaw, float partialTicks) {
        AbstractClientPlayer previous = RenderContext.entity;
        RenderContext.entity = entity;
        RenderContext.pose = null;
        RenderContext.slim = slim;
        RenderContext.partialTicks = partialTicks;
        BlendEmotes189.setPartialTicks(partialTicks);
        try {
            super.doRender(entity, x, y, z, entityYaw, partialTicks);
        } finally {
            RenderContext.entity = previous;
            RenderContext.pose = null;
        }
    }

    /**
     * Runs after the entity rotation and the model flip, just before the model is drawn: the
     * rig's "body" bone is applied here so armour, items, cape and every other layer follow.
     */
    @Override
    protected void preRenderCallback(AbstractClientPlayer entity, float partialTickTime) {
        super.preRenderCallback(entity, partialTickTime);
        if (BlendEmotes189.client() != null && entity == RenderContext.entity
                && BlendEmotes189.client().root(entity.getUniqueID(), slim, root)) {
            // vanilla translates by -1.5078125 after this callback; model space starts there
            GlStateManager.translate(0.0F, -1.5078125F, 0.0F);
            GlMatrix.mult(root, 0.0625F);
            GlStateManager.translate(0.0F, 1.5078125F, 0.0F);
        }
    }
}
