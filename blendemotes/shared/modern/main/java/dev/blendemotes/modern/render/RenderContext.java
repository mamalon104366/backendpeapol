package dev.blendemotes.modern.render;

import dev.blendemotes.core.pose.PlayerPose;

import java.util.ArrayDeque;
import java.util.UUID;

/** Player being rendered in the world right now and its emote pose. */
public final class RenderContext {
    /** The player entity (up to 1.21.1) or its render state (1.21.2 and newer); null: not a player. */
    public static Object target;
    public static UUID player;
    public static boolean slim;
    /** Emote pose of {@link #target} this frame, null while it does not emote. */
    public static PlayerPose pose;

    /** Outer renders interrupted by a nested one (e.g. an entity rendered inside a GUI). */
    private static final ArrayDeque<Object[]> STACK = new ArrayDeque<>();

    private RenderContext() {
    }

    /** Starts rendering a player (uuid null: not a player). Every call must be paired with {@link #end()}. */
    public static void begin(Object newTarget, UUID uuid, boolean isSlim) {
        STACK.push(new Object[]{target, player, slim, pose});
        target = uuid == null ? null : newTarget;
        player = uuid;
        slim = isSlim;
        pose = null;
    }

    public static void end() {
        Object[] previous = STACK.poll();
        if (previous == null) {
            target = null;
            player = null;
            pose = null;
            return;
        }
        target = previous[0];
        player = (UUID) previous[1];
        slim = (Boolean) previous[2];
        pose = (PlayerPose) previous[3];
    }

    public static boolean isTarget(Object o) {
        return o != null && o == target;
    }
}
