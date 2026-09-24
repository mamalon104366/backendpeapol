package dev.blendemotes.modern.render;

import dev.blendemotes.core.bend.BendMesh;
import dev.blendemotes.core.bend.CubeGeometry;
import dev.blendemotes.core.bend.PlayerModelGeometry;
import dev.blendemotes.core.rig.BendProfile;
import dev.blendemotes.core.rig.PlayerPart;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;

/** Attaches bendable geometry to the parts of player and armour models. */
public final class PartMeshes {
    private PartMeshes() {
    }

    private static void set(ModelPart part, BendMesh mesh) {
        ((BendablePart) (Object) part).blendemotes$setMesh(mesh);
    }

    public static void player(PlayerModel<?> model, boolean slim) {
        set(model.body, PlayerModelGeometry.mesh(PlayerPart.TORSO, PlayerModelGeometry.Layer.INNER, slim));
        set(model.jacket, PlayerModelGeometry.mesh(PlayerPart.TORSO, PlayerModelGeometry.Layer.OUTER, slim));
        set(model.rightArm, PlayerModelGeometry.mesh(PlayerPart.RIGHT_ARM, PlayerModelGeometry.Layer.INNER, slim));
        set(model.rightSleeve, PlayerModelGeometry.mesh(PlayerPart.RIGHT_ARM, PlayerModelGeometry.Layer.OUTER, slim));
        set(model.leftArm, PlayerModelGeometry.mesh(PlayerPart.LEFT_ARM, PlayerModelGeometry.Layer.INNER, slim));
        set(model.leftSleeve, PlayerModelGeometry.mesh(PlayerPart.LEFT_ARM, PlayerModelGeometry.Layer.OUTER, slim));
        set(model.rightLeg, PlayerModelGeometry.mesh(PlayerPart.RIGHT_LEG, PlayerModelGeometry.Layer.INNER, slim));
        set(model.rightPants, PlayerModelGeometry.mesh(PlayerPart.RIGHT_LEG, PlayerModelGeometry.Layer.OUTER, slim));
        set(model.leftLeg, PlayerModelGeometry.mesh(PlayerPart.LEFT_LEG, PlayerModelGeometry.Layer.INNER, slim));
        set(model.leftPants, PlayerModelGeometry.mesh(PlayerPart.LEFT_LEG, PlayerModelGeometry.Layer.OUTER, slim));
    }

    /** Vanilla armour models: 64x32 texture, left limbs mirrored. */
    public static void armor(HumanoidModel<?> model, float inflate) {
        set(model.body, CubeGeometry.box(16, 16, -4, 0, -2, 8, 12, 4, inflate, false, 64, 32).toBendMesh(BendProfile.TORSO));
        set(model.rightArm, CubeGeometry.box(40, 16, -3, -2, -2, 4, 12, 4, inflate, false, 64, 32).toBendMesh(BendProfile.ARM));
        set(model.leftArm, CubeGeometry.box(40, 16, -1, -2, -2, 4, 12, 4, inflate, true, 64, 32).toBendMesh(BendProfile.ARM));
        set(model.rightLeg, CubeGeometry.box(0, 16, -2, 0, -2, 4, 12, 4, inflate, false, 64, 32).toBendMesh(BendProfile.LEG));
        set(model.leftLeg, CubeGeometry.box(0, 16, -2, 0, -2, 4, 12, 4, inflate, true, 64, 32).toBendMesh(BendProfile.LEG));
    }

    private static BendMesh cape;

    public static synchronized BendMesh cape() {
        if (cape == null) {
            cape = PlayerModelGeometry.mesh(PlayerPart.CAPE, PlayerModelGeometry.Layer.INNER, false);
        }
        return cape;
    }
}
