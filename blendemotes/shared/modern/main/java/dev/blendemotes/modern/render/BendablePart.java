package dev.blendemotes.modern.render;

import dev.blendemotes.core.bend.BendMesh;
import dev.blendemotes.core.math.Vec3;

/** Extra state added to {@code ModelPart} by {@code ModelPartMixin}. */
public interface BendablePart {
    /** Geometry used when the part is bent or scaled (null: the part cannot bend). */
    void blendemotes$setMesh(BendMesh mesh);

    BendMesh blendemotes$mesh();

    /** Emote state for this frame. */
    void blendemotes$setEmote(boolean active, Vec3 bend, Vec3 joint, float sx, float sy, float sz);

    boolean blendemotes$active();

    /** True while an emote changed the part since its last reset. */
    boolean blendemotes$dirty();

    void blendemotes$clear();

    /** Bend as a rotation vector (radians, part-local): X forwards/backwards, Z sideways. */
    Vec3 blendemotes$bend();

    Vec3 blendemotes$joint();

    float[] blendemotes$scale();
}
