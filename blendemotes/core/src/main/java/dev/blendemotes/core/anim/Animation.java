package dev.blendemotes.core.anim;

import dev.blendemotes.core.math.Vec3;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A loaded animation: per-bone keyframes plus timing information.
 * Bone names are normalised to snake_case ({@code rightArm -> right_arm}).
 */
public final class Animation {
    /** Length in seconds. */
    public final double length;
    public final LoopMode loopMode;
    /** Where a {@link LoopMode#LOOP} jumps back to, in seconds. */
    public final double loopStart;
    public final Map<String, BoneAnimation> bones;
    /** Pivots of custom bones, Bedrock model coordinates (pixels, Y up, origin at the feet). */
    public final Map<String, Vec3> pivots;
    /** Custom hierarchy: child -> parent. */
    public final Map<String, String> parents;
    /** Legacy flag: bending the torso also moves head and arms. */
    public final boolean applyBendToOtherBones;
    /**
     * True when the file was exported from the Blender rig ({@code emote_creator.blend}): the
     * values are then interpreted in the rig's exact bone frames (arms and legs are slightly
     * tilted, item bones rotate X-Z-Y), which reproduces Blender instead of approximating it.
     */
    public final boolean blenderRig;

    public Animation(double length, LoopMode loopMode, double loopStart, Map<String, BoneAnimation> bones,
                     Map<String, Vec3> pivots, Map<String, String> parents, boolean applyBendToOtherBones) {
        this(length, loopMode, loopStart, bones, pivots, parents, applyBendToOtherBones, false);
    }

    public Animation(double length, LoopMode loopMode, double loopStart, Map<String, BoneAnimation> bones,
                     Map<String, Vec3> pivots, Map<String, String> parents, boolean applyBendToOtherBones,
                     boolean blenderRig) {
        this.length = Math.max(0, length);
        this.loopMode = loopMode;
        this.loopStart = Math.max(0, Math.min(loopStart, this.length));
        this.bones = Collections.unmodifiableMap(new LinkedHashMap<String, BoneAnimation>(bones));
        this.pivots = Collections.unmodifiableMap(new LinkedHashMap<String, Vec3>(pivots));
        this.parents = Collections.unmodifiableMap(new LinkedHashMap<String, String>(parents));
        this.applyBendToOtherBones = applyBendToOtherBones;
        this.blenderRig = blenderRig;
    }

    public BoneAnimation bone(String name) {
        return bones.get(name);
    }

    /**
     * Converts the time elapsed since the emote started into the time inside the animation,
     * applying the loop mode.
     */
    public double animationTime(double elapsed) {
        if (elapsed <= 0) {
            return 0;
        }
        switch (loopMode) {
            case LOOP: {
                if (elapsed <= length) {
                    return elapsed;
                }
                double period = length - loopStart;
                if (period <= 1e-6) {
                    return length;
                }
                double into = (elapsed - length) % period;
                return loopStart + into;
            }
            case HOLD_ON_LAST_FRAME:
            case PLAY_ONCE:
            default:
                return Math.min(elapsed, length);
        }
    }

    /** True when a {@link LoopMode#PLAY_ONCE} animation is over. */
    public boolean isFinished(double elapsed) {
        return loopMode == LoopMode.PLAY_ONCE && elapsed >= length;
    }
}
