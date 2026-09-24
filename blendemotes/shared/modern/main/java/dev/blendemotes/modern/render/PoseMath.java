package dev.blendemotes.modern.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.blendemotes.core.math.Mat4;
//#if MC >= 11903
import org.joml.Quaternionf;
//#else
import com.mojang.math.Quaternion;
//#endif

/** Applies core matrices (model pixels) to a {@link PoseStack}. */
public final class PoseMath {
    private PoseMath() {
    }

    /** Multiplies the stack by {@code m}; translations are in pixels (1/16 block). */
    public static void mul(PoseStack stack, Mat4 m) {
        double[] d = m.decompose();
        stack.translate(d[0] / 16.0, d[1] / 16.0, d[2] / 16.0);
        //#if MC >= 11903
        stack.mulPose(new Quaternionf((float) d[3], (float) d[4], (float) d[5], (float) d[6]));
        //#else
        stack.mulPose(new Quaternion((float) d[3], (float) d[4], (float) d[5], (float) d[6]));
        //#endif
        if (d[7] != 1 || d[8] != 1 || d[9] != 1) {
            stack.scale((float) d[7], (float) d[8], (float) d[9]);
        }
    }
}
