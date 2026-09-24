package dev.blendemotes.mc1201.render;

import dev.blendemotes.core.pose.PlayerPose;
import net.minecraft.world.entity.Entity;

/** Player being rendered in the world right now and its emote pose. */
public final class RenderContext {
    public static Entity entity;
    public static PlayerPose pose;
    public static boolean slim;

    private RenderContext() {
    }
}
