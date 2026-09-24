package dev.blendemotes.core.pose;

import dev.blendemotes.core.math.Vec3;
import dev.blendemotes.core.rig.PlayerPart;
import dev.blendemotes.core.rig.RigDefinition;

import java.util.EnumMap;
import java.util.Map;

/**
 * The pose computed by vanilla Minecraft for this frame (walking, looking around, sneaking...).
 * Animated axes override it, everything else is kept, so an emote that only waves an arm
 * still lets the head follow the mouse.
 */
public final class VanillaPose {
    private final Map<PlayerPart, PartTransform> parts = new EnumMap<PlayerPart, PartTransform>(PlayerPart.class);

    public VanillaPose() {
        reset();
    }

    /** Rest pose: default pivots, no rotation. */
    public VanillaPose reset() {
        return reset(RigDefinition.MINECRAFT);
    }

    /** Rest pose of the given rig. */
    public VanillaPose reset(RigDefinition rig) {
        for (PlayerPart p : PlayerPart.VALUES) {
            PartTransform t = parts.get(p);
            if (t == null) {
                t = new PartTransform();
                parts.put(p, t);
            }
            Vec3 pivot = rig.pivot(p);
            t.set(pivot.x, pivot.y, pivot.z, 0, 0, 0).setScale(1, 1, 1);
        }
        return this;
    }

    public PartTransform get(PlayerPart part) {
        return parts.get(part);
    }

    public VanillaPose set(PlayerPart part, double x, double y, double z, double pitch, double yaw, double roll) {
        parts.get(part).set(x, y, z, pitch, yaw, roll);
        return this;
    }
}
