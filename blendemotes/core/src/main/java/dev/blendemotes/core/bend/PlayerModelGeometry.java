package dev.blendemotes.core.bend;

import dev.blendemotes.core.rig.PlayerPart;

import java.util.EnumMap;
import java.util.Map;

/**
 * Cubes of the vanilla player model (identical from 1.8 to today) and their cached bendable
 * meshes. {@code INNER} is the base layer (arm, leg, body), {@code OUTER} the second skin
 * layer (sleeve, pants, jacket), each rendered by its own {@code ModelPart}.
 */
public final class PlayerModelGeometry {
    public enum Layer {
        INNER, OUTER
    }

    private static final Map<PlayerPart, BendMesh[]> WIDE = new EnumMap<PlayerPart, BendMesh[]>(PlayerPart.class);
    private static final Map<PlayerPart, BendMesh[]> SLIM = new EnumMap<PlayerPart, BendMesh[]>(PlayerPart.class);

    private PlayerModelGeometry() {
    }

    /** The cube of a part/layer, or null for parts that do not bend. */
    public static CubeGeometry cube(PlayerPart part, Layer layer, boolean slim) {
        boolean outer = layer == Layer.OUTER;
        float inflate = outer ? 0.25f : 0f;
        switch (part) {
            case TORSO:
                return outer
                        ? CubeGeometry.box(16, 32, -4, 0, -2, 8, 12, 4, inflate, false, 64, 64)
                        : CubeGeometry.box(16, 16, -4, 0, -2, 8, 12, 4, inflate, false, 64, 64);
            case RIGHT_ARM:
                if (slim) {
                    return outer
                            ? CubeGeometry.box(40, 32, -2, -2, -2, 3, 12, 4, inflate, false, 64, 64)
                            : CubeGeometry.box(40, 16, -2, -2, -2, 3, 12, 4, inflate, false, 64, 64);
                }
                return outer
                        ? CubeGeometry.box(40, 32, -3, -2, -2, 4, 12, 4, inflate, false, 64, 64)
                        : CubeGeometry.box(40, 16, -3, -2, -2, 4, 12, 4, inflate, false, 64, 64);
            case LEFT_ARM:
                if (slim) {
                    return outer
                            ? CubeGeometry.box(48, 48, -1, -2, -2, 3, 12, 4, inflate, false, 64, 64)
                            : CubeGeometry.box(32, 48, -1, -2, -2, 3, 12, 4, inflate, false, 64, 64);
                }
                return outer
                        ? CubeGeometry.box(48, 48, -1, -2, -2, 4, 12, 4, inflate, false, 64, 64)
                        : CubeGeometry.box(32, 48, -1, -2, -2, 4, 12, 4, inflate, false, 64, 64);
            case RIGHT_LEG:
                return outer
                        ? CubeGeometry.box(0, 32, -2, 0, -2, 4, 12, 4, inflate, false, 64, 64)
                        : CubeGeometry.box(0, 16, -2, 0, -2, 4, 12, 4, inflate, false, 64, 64);
            case LEFT_LEG:
                return outer
                        ? CubeGeometry.box(0, 48, -2, 0, -2, 4, 12, 4, inflate, false, 64, 64)
                        : CubeGeometry.box(16, 48, -2, 0, -2, 4, 12, 4, inflate, false, 64, 64);
            case CAPE:
                return outer ? null : CubeGeometry.box(0, 0, -5, 0, -1, 10, 16, 1, 0, false, 64, 32);
            default:
                return null;
        }
    }

    /** Cached bendable mesh of a part/layer, or null when the part does not bend. */
    public static synchronized BendMesh mesh(PlayerPart part, Layer layer, boolean slim) {
        if (part.bend == null) {
            return null;
        }
        Map<PlayerPart, BendMesh[]> cache = slim ? SLIM : WIDE;
        BendMesh[] meshes = cache.get(part);
        if (meshes == null) {
            meshes = new BendMesh[2];
            cache.put(part, meshes);
        }
        int i = layer.ordinal();
        if (meshes[i] == null) {
            CubeGeometry cube = cube(part, layer, slim);
            if (cube == null) {
                return null;
            }
            meshes[i] = cube.toBendMesh(part.bend);
        }
        return meshes[i];
    }
}
