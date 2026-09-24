package dev.blendemotes.legacy.render;

import dev.blendemotes.core.pose.PlayerPose;
import net.minecraft.client.entity.AbstractClientPlayer;

/**
 * What is being rendered right now. Vanilla calls the model's setRotationAngles from several
 * places (world rendering, first person hand, armour layers); the emote pose is only applied
 * while a player is rendered in the world.
 */
public final class RenderContext {
    /** Player currently rendered in the world, null otherwise. */
    public static AbstractClientPlayer entity;
    /** Pose of that player for this frame, null when not emoting. */
    public static PlayerPose pose;
    public static boolean slim;
    /** Partial tick of the frame being rendered. */
    public static float partialTicks;

    private RenderContext() {
    }

    public static boolean active() {
        return entity != null && pose != null;
    }
}
