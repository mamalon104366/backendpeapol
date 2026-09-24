package dev.blendemotes.legacy.render;

import dev.blendemotes.core.math.Mat4;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;

/** Multiplies the OpenGL matrix stack by a core {@link Mat4}. */
public final class GlMatrix {
    private static final FloatBuffer BUFFER = BufferUtils.createFloatBuffer(16);

    private GlMatrix() {
    }

    /**
     * @param m     transform in model pixels
     * @param scale pixels to OpenGL units (0.0625 for entity models)
     */
    public static void mult(Mat4 m, float scale) {
        BUFFER.clear();
        for (int col = 0; col < 4; col++) {
            for (int row = 0; row < 4; row++) {
                double v = m.m[row * 4 + col];
                if (col == 3 && row < 3) {
                    v *= scale;
                }
                BUFFER.put((float) v);
            }
        }
        BUFFER.flip();
        GL11.glMultMatrix(BUFFER);
    }
}
