package dev.blendemotes.core.bend;

/**
 * Generates the quads of a Minecraft model cube exactly like the game does
 * ({@code ModelBox}/{@code TexturedQuad} up to 1.16, {@code ModelPart.Cube}/{@code Polygon}
 * since 1.17 - the layout has not changed since the 64x64 skins of 1.8): same vertex order,
 * same texture mapping, same mirroring. Used to build bendable versions of the player parts.
 */
public final class CubeGeometry {
    /** Face order used here: down, up, west, north, east, south. */
    public static final int FACES = 6;

    public final float[] positions; // 6 * 4 * 3
    public final float[] uvs;       // 6 * 4 * 2 (normalised 0..1)
    public final float[] normals;   // 6 * 3

    private CubeGeometry(float[] positions, float[] uvs, float[] normals) {
        this.positions = positions;
        this.uvs = uvs;
        this.normals = normals;
    }

    /**
     * @param texU    texture offset U (pixels)
     * @param texV    texture offset V (pixels)
     * @param x       min corner (pixels, part-local)
     * @param w       size (pixels)
     * @param inflate cube deformation ("modelSize"), e.g. 0.25 for sleeves
     * @param mirror  mirrored texture (left limbs of the old 64x32 skins)
     * @param texW    texture width in pixels
     * @param texH    texture height in pixels
     */
    public static CubeGeometry box(int texU, int texV, float x, float y, float z, float w, float h, float d,
                                   float inflate, boolean mirror, float texW, float texH) {
        float x2 = x + w;
        float y2 = y + h;
        float z2 = z + d;
        float x1 = x - inflate;
        float y1 = y - inflate;
        float z1 = z - inflate;
        x2 += inflate;
        y2 += inflate;
        z2 += inflate;
        if (mirror) {
            float t = x2;
            x2 = x1;
            x1 = t;
        }
        float[][] v = {
                {x1, y1, z1}, // 0
                {x2, y1, z1}, // 1
                {x2, y2, z1}, // 2
                {x1, y2, z1}, // 3
                {x1, y1, z2}, // 4
                {x2, y1, z2}, // 5
                {x2, y2, z2}, // 6
                {x1, y2, z2}, // 7
        };
        float j = texU;
        float k = texU + d;
        float l = texU + d + w;
        float m = texU + d + w + w;
        float n = texU + d + w + d;
        float o = texU + d + w + d + w;
        float p = texV;
        float q = texV + d;
        float r = texV + d + h;

        float[] pos = new float[FACES * 12];
        float[] uv = new float[FACES * 8];
        float[] nrm = new float[FACES * 3];
        int f = 0;
        f = face(f, pos, uv, nrm, v, new int[]{5, 4, 0, 1}, k, p, l, q, texW, texH, mirror, 0, -1, 0);  // down
        f = face(f, pos, uv, nrm, v, new int[]{2, 3, 7, 6}, l, q, m, p, texW, texH, mirror, 0, 1, 0);   // up
        f = face(f, pos, uv, nrm, v, new int[]{0, 4, 7, 3}, j, q, k, r, texW, texH, mirror, -1, 0, 0);  // west
        f = face(f, pos, uv, nrm, v, new int[]{1, 0, 3, 2}, k, q, l, r, texW, texH, mirror, 0, 0, -1);  // north
        f = face(f, pos, uv, nrm, v, new int[]{5, 1, 2, 6}, l, q, n, r, texW, texH, mirror, 1, 0, 0);   // east
        face(f, pos, uv, nrm, v, new int[]{4, 5, 6, 7}, n, q, o, r, texW, texH, mirror, 0, 0, 1);       // south
        return new CubeGeometry(pos, uv, nrm);
    }

    private static int face(int f, float[] pos, float[] uv, float[] nrm, float[][] v, int[] idx,
                            float u1, float v1, float u2, float v2, float texW, float texH, boolean mirror,
                            float nx, float ny, float nz) {
        float[][] tex = {
                {u2 / texW, v1 / texH},
                {u1 / texW, v1 / texH},
                {u1 / texW, v2 / texH},
                {u2 / texW, v2 / texH},
        };
        int[] order = mirror ? new int[]{3, 2, 1, 0} : new int[]{0, 1, 2, 3};
        for (int i = 0; i < 4; i++) {
            int src = order[i];
            float[] p = v[idx[src]];
            pos[f * 12 + i * 3] = p[0];
            pos[f * 12 + i * 3 + 1] = p[1];
            pos[f * 12 + i * 3 + 2] = p[2];
            uv[f * 8 + i * 2] = tex[src][0];
            uv[f * 8 + i * 2 + 1] = tex[src][1];
        }
        nrm[f * 3] = mirror ? -nx : nx;
        nrm[f * 3 + 1] = ny;
        nrm[f * 3 + 2] = nz;
        return f + 1;
    }

    /** Concatenates several cubes into one quad list. */
    public static float[][] merge(CubeGeometry... cubes) {
        int n = 0;
        for (CubeGeometry c : cubes) {
            n += c.normals.length / 3;
        }
        float[] pos = new float[n * 12];
        float[] uv = new float[n * 8];
        float[] nrm = new float[n * 3];
        int q = 0;
        for (CubeGeometry c : cubes) {
            int cq = c.normals.length / 3;
            System.arraycopy(c.positions, 0, pos, q * 12, cq * 12);
            System.arraycopy(c.uvs, 0, uv, q * 8, cq * 8);
            System.arraycopy(c.normals, 0, nrm, q * 3, cq * 3);
            q += cq;
        }
        return new float[][]{pos, uv, nrm};
    }

    public BendMesh toBendMesh(dev.blendemotes.core.rig.BendProfile profile) {
        return BendMesh.build(positions, uvs, normals, profile);
    }
}
