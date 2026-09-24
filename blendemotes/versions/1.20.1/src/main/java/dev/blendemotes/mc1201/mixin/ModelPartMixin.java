package dev.blendemotes.mc1201.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.blendemotes.core.bend.BendMesh;
import dev.blendemotes.core.math.Vec3;
import dev.blendemotes.mc1201.render.BendablePart;
import dev.blendemotes.mc1201.render.MeshEmitter;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lets model parts bend (elbow, knee, waist) and scale while an emote drives them. */
@Mixin(ModelPart.class)
public abstract class ModelPartMixin implements BendablePart {
    @Unique
    private BendMesh blendemotes$mesh;
    @Unique
    private final BendMesh.Output blendemotes$buffer = new BendMesh.Output();
    @Unique
    private boolean blendemotes$active;
    @Unique
    private boolean blendemotes$dirty;
    @Unique
    private double blendemotes$bend;
    @Unique
    private Vec3 blendemotes$joint = Vec3.ZERO;
    @Unique
    private float blendemotes$sx = 1;
    @Unique
    private float blendemotes$sy = 1;
    @Unique
    private float blendemotes$sz = 1;

    @Override
    public void blendemotes$setMesh(BendMesh mesh) {
        blendemotes$mesh = mesh;
    }

    @Override
    public BendMesh blendemotes$mesh() {
        return blendemotes$mesh;
    }

    @Override
    public void blendemotes$setEmote(boolean active, double bend, Vec3 joint, float sx, float sy, float sz) {
        blendemotes$active = active;
        blendemotes$dirty |= active;
        blendemotes$bend = bend;
        blendemotes$joint = joint;
        blendemotes$sx = sx;
        blendemotes$sy = sy;
        blendemotes$sz = sz;
    }

    @Override
    public boolean blendemotes$active() {
        return blendemotes$active;
    }

    @Override
    public double blendemotes$bend() {
        return blendemotes$bend;
    }

    @Override
    public Vec3 blendemotes$joint() {
        return blendemotes$joint;
    }

    @Override
    public float[] blendemotes$scale() {
        return new float[]{blendemotes$sx, blendemotes$sy, blendemotes$sz};
    }

    @Override
    public boolean blendemotes$dirty() {
        return blendemotes$dirty;
    }

    @Override
    public void blendemotes$clear() {
        blendemotes$active = false;
        blendemotes$dirty = false;
        blendemotes$bend = 0;
        blendemotes$sx = 1;
        blendemotes$sy = 1;
        blendemotes$sz = 1;
    }

    @Inject(method = "translateAndRotate", at = @At("TAIL"))
    private void blendemotes$scale(PoseStack poseStack, CallbackInfo ci) {
        if (blendemotes$active && (blendemotes$sx != 1 || blendemotes$sy != 1 || blendemotes$sz != 1)) {
            poseStack.scale(blendemotes$sx, blendemotes$sy, blendemotes$sz);
        }
    }

    @Inject(method = "compile", at = @At("HEAD"), cancellable = true)
    private void blendemotes$compile(PoseStack.Pose pose, VertexConsumer consumer, int light, int overlay,
                                     float r, float g, float b, float a, CallbackInfo ci) {
        if (blendemotes$active && blendemotes$bend != 0 && blendemotes$mesh != null) {
            MeshEmitter.emit(blendemotes$mesh.deform(blendemotes$bend, blendemotes$joint, blendemotes$buffer),
                    pose, consumer, light, overlay, r, g, b, a);
            ci.cancel();
        }
    }

    /** Armour and the second skin layer copy the pose of the base parts: copy the emote state too. */
    @Inject(method = "copyFrom", at = @At("TAIL"))
    private void blendemotes$copy(ModelPart other, CallbackInfo ci) {
        BendablePart src = (BendablePart) (Object) other;
        float[] s = src.blendemotes$scale();
        blendemotes$active = src.blendemotes$active();
        blendemotes$dirty |= blendemotes$active;
        blendemotes$bend = src.blendemotes$bend();
        blendemotes$joint = src.blendemotes$joint();
        blendemotes$sx = s[0];
        blendemotes$sy = s[1];
        blendemotes$sz = s[2];
    }
}
