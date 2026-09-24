package dev.blendemotes.modern.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.blendemotes.core.math.Mat4;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Applies core matrices (model pixels) to a {@link PoseStack}. */
public final class PoseMath {
    private PoseMath() {
    }

    public static void mul(PoseStack stack, Mat4 m) {
        float[] c = new float[16];
        for (int col = 0; col < 4; col++) {
            for (int row = 0; row < 4; row++) {
                double v = m.m[row * 4 + col];
                if (col == 3 && row < 3) {
                    v /= 16.0;
                }
                c[col * 4 + row] = (float) v;
            }
        }
        Matrix4f pose = new Matrix4f().set(c);
        stack.last().pose().mul(pose);
        Matrix3f normal = new Matrix3f();
        pose.get3x3(normal);
        normal.invert().transpose();
        stack.last().normal().mul(normal);
    }
}
