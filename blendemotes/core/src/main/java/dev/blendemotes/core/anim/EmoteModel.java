package dev.blendemotes.core.anim;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * A model that is part of an emote and moves with it: a microphone in the hand, a guitar, a
 * horse under the player... Any mesh the animator parents to a bone of the rig in Blender.
 * <p>
 * The geometry is a list of textured triangles in the rig's rest pose, in Minecraft model space
 * (pixels, Y pointing down, origin at the neck, the player's left on +X, face towards -Z), so it
 * is drawn by applying the matrix of its {@link #bone} for the frame, like a player part.
 */
public final class EmoteModel {
    /** Largest model the mod accepts (triangles), per model. */
    public static final int MAX_TRIANGLES = 20000;
    /** Largest texture (PNG bytes), per model. */
    public static final int MAX_TEXTURE_BYTES = 1024 * 1024;

    public final String name;
    /**
     * Bone the model follows: a player part ({@code right_item}, {@code head}, {@code body}...),
     * the lower half of a bendable part ({@code right_arm_bend}...) or a custom bone of the emote.
     */
    public final String bone;
    /** 3 vertices per triangle, x y z each. */
    public final float[] positions;
    /** 3 vertices per triangle, u v each (0..1, v down like Minecraft textures). */
    public final float[] uvs;
    /** 3 vertices per triangle, x y z each (unit length). */
    public final float[] normals;
    /** PNG image (never null). */
    public final byte[] texture;
    /** Hash of the texture bytes: identical textures are uploaded once. */
    public final String textureId;
    /** Draw both faces of every triangle. */
    public final boolean doubleSided;

    public EmoteModel(String name, String bone, float[] positions, float[] uvs, float[] normals, byte[] texture,
                      boolean doubleSided) {
        int n = positions.length / 9;
        if (positions.length != n * 9 || uvs.length != n * 6 || normals.length != n * 9) {
            throw new IllegalArgumentException("model '" + name + "': inconsistent triangle arrays");
        }
        if (n > MAX_TRIANGLES) {
            throw new IllegalArgumentException("model '" + name + "' has " + n + " triangles (max " + MAX_TRIANGLES + ")");
        }
        if (texture == null || texture.length == 0 || texture.length > MAX_TEXTURE_BYTES) {
            throw new IllegalArgumentException("model '" + name + "': missing or too large texture");
        }
        this.name = name;
        this.bone = bone;
        this.positions = positions;
        this.uvs = uvs;
        this.normals = normals;
        this.texture = texture;
        this.textureId = hash(texture);
        this.doubleSided = doubleSided;
    }

    public int triangleCount() {
        return positions.length / 9;
    }

    private static String hash(byte[] data) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-1").digest(data);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 10; i++) {
                sb.append(String.format("%02x", d[i] & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            return Integer.toHexString(java.util.Arrays.hashCode(data));
        }
    }

    /** A 1x1 white PNG, for models without a picture (the vertex colour is white too). */
    public static byte[] whitePixel() {
        return java.util.Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg==");
    }
}
