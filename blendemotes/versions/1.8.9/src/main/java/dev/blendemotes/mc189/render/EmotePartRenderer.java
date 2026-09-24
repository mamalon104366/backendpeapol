package dev.blendemotes.mc189.render;

import dev.blendemotes.core.bend.BendMesh;
import dev.blendemotes.core.bend.CubeGeometry;
import dev.blendemotes.core.math.Vec3;
import dev.blendemotes.core.rig.BendProfile;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

/**
 * A single-cube model part that can be scaled and bent (elbow, knee, waist) by emotes. When
 * no emote affects it, it renders exactly like the vanilla {@link ModelRenderer}.
 */
public class EmotePartRenderer extends ModelRenderer {
    private final BendMesh mesh;
    private final BendMesh.Output buffer = new BendMesh.Output();
    private Vec3 joint = Vec3.ZERO;
    /** Bend angle (radians), 0 = straight. */
    public double bend;
    public float scaleX = 1;
    public float scaleY = 1;
    public float scaleZ = 1;
    /** Set while an emote drives this part. */
    public boolean emote;

    /**
     * Same arguments as {@code new ModelRenderer(model, u, v).addBox(...)} plus the bend profile
     * (null when the part cannot bend).
     */
    public EmotePartRenderer(ModelBase model, int u, int v, float x, float y, float z, int w, int h, int d,
                             float inflate, boolean mirror, BendProfile profile) {
        super(model, u, v);
        this.mirror = mirror;
        addBox(x, y, z, w, h, d, inflate);
        CubeGeometry cube = CubeGeometry.box(u, v, x, y, z, w, h, d, inflate, mirror, textureWidth, textureHeight);
        this.mesh = BendMesh.build(cube.positions, cube.uvs, cube.normals,
                profile != null ? profile : BendProfile.limb(1000, 0));
    }

    public void setJoint(Vec3 joint) {
        this.joint = joint;
    }

    public void resetEmote() {
        emote = false;
        bend = 0;
        scaleX = 1;
        scaleY = 1;
        scaleZ = 1;
    }

    public void copyEmote(EmotePartRenderer from) {
        emote = from.emote;
        bend = from.bend;
        joint = from.joint;
        scaleX = from.scaleX;
        scaleY = from.scaleY;
        scaleZ = from.scaleZ;
    }

    private boolean custom() {
        return emote && (bend != 0 || scaleX != 1 || scaleY != 1 || scaleZ != 1);
    }

    @Override
    public void render(float scale) {
        if (!custom()) {
            super.render(scale);
            return;
        }
        if (isHidden || !showModel) {
            return;
        }
        GlStateManager.pushMatrix();
        GlStateManager.translate(offsetX, offsetY, offsetZ);
        GlStateManager.translate(rotationPointX * scale, rotationPointY * scale, rotationPointZ * scale);
        if (rotateAngleZ != 0) {
            GlStateManager.rotate(rotateAngleZ * (180F / (float) Math.PI), 0, 0, 1);
        }
        if (rotateAngleY != 0) {
            GlStateManager.rotate(rotateAngleY * (180F / (float) Math.PI), 0, 1, 0);
        }
        if (rotateAngleX != 0) {
            GlStateManager.rotate(rotateAngleX * (180F / (float) Math.PI), 1, 0, 0);
        }
        GlStateManager.scale(scaleX, scaleY, scaleZ);
        draw(scale);
        if (childModels != null) {
            for (ModelRenderer child : childModels) {
                child.render(scale);
            }
        }
        GlStateManager.popMatrix();
    }

    /** Draws the (bent) cube in the current part space. */
    public void draw(float scale) {
        BendMesh.Output out = mesh.deform(bend, joint, buffer);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(7, DefaultVertexFormats.OLDMODEL_POSITION_TEX_NORMAL);
        for (int q = 0; q < out.quadCount; q++) {
            float nx = out.normals[q * 3];
            float ny = out.normals[q * 3 + 1];
            float nz = out.normals[q * 3 + 2];
            for (int v = 0; v < 4; v++) {
                int i = q * 4 + v;
                wr.pos(out.positions[i * 3] * scale, out.positions[i * 3 + 1] * scale, out.positions[i * 3 + 2] * scale)
                        .tex(out.uvs[i * 2], out.uvs[i * 2 + 1])
                        .normal(nx, ny, nz)
                        .endVertex();
            }
        }
        tessellator.draw();
    }
}
