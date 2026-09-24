package dev.blendemotes.core.emote;

import dev.blendemotes.core.anim.Animation;
import dev.blendemotes.core.anim.molang.Molang;
import dev.blendemotes.core.pose.PlayerPose;
import dev.blendemotes.core.pose.PoseEvaluator;
import dev.blendemotes.core.pose.VanillaPose;
import dev.blendemotes.core.rig.RigDefinition;

/**
 * Playback state of the emote of one player (local or remote). Time is passed in seconds of
 * a monotonic clock, normally {@code (tickCount + partialTick) / 20}.
 */
public final class EmotePlayer {
    public static final double DEFAULT_FADE_IN = 0.15;
    public static final double DEFAULT_FADE_OUT = 0.25;

    private Emote emote;
    private double start;
    private double stopAt = Double.NaN;
    private double fadeIn = DEFAULT_FADE_IN;
    private double fadeOut = DEFAULT_FADE_OUT;

    private final PlayerPose vanillaPose = new PlayerPose();
    private final PlayerPose emotePose = new PlayerPose();

    public void setFades(double fadeIn, double fadeOut) {
        this.fadeIn = Math.max(0, fadeIn);
        this.fadeOut = Math.max(0, fadeOut);
    }

    /**
     * Starts an emote.
     *
     * @param elapsed seconds the emote has already been playing (for players that start
     *                watching late or network latency)
     */
    public void play(Emote emote, double now, double elapsed) {
        this.emote = emote;
        this.start = now - Math.max(0, elapsed);
        this.stopAt = Double.NaN;
    }

    /** Starts fading out. */
    public void stop(double now) {
        if (emote != null && Double.isNaN(stopAt)) {
            stopAt = now;
        }
    }

    /** Removes the emote immediately, without fading. */
    public void clear() {
        emote = null;
        stopAt = Double.NaN;
    }

    public Emote current() {
        return emote;
    }

    public boolean isStopping() {
        return emote != null && !Double.isNaN(stopAt);
    }

    public double elapsed(double now) {
        return emote == null ? 0 : Math.max(0, now - start);
    }

    /** Advances the state; returns false once the emote has completely finished. */
    public boolean update(double now) {
        if (emote == null) {
            return false;
        }
        Animation anim = emote.animation;
        double e = now - start;
        if (Double.isNaN(stopAt) && anim.isFinished(e)) {
            stopAt = start + anim.length;
        }
        if (!Double.isNaN(stopAt) && now - stopAt >= fadeOut) {
            clear();
            return false;
        }
        return true;
    }

    public boolean isActive(double now) {
        return update(now);
    }

    /** Blend weight of the emote at {@code now} (0 = vanilla, 1 = emote). */
    public double weight(double now) {
        if (emote == null) {
            return 0;
        }
        double e = Math.max(0, now - start);
        double in = fadeIn <= 0 ? 1 : smooth(Math.min(1, e / fadeIn));
        double out = 1;
        if (!Double.isNaN(stopAt)) {
            out = fadeOut <= 0 ? 0 : 1 - smooth(Math.min(1, Math.max(0, (now - stopAt) / fadeOut)));
        }
        return in * out;
    }

    private static double smooth(double x) {
        return x * x * (3 - 2 * x);
    }

    /**
     * Computes the pose to render.
     *
     * @param lifeTime seconds the entity has existed (for Molang {@code query.life_time})
     * @return {@code out}, or null when no emote is playing
     */
    public PlayerPose evaluate(double now, VanillaPose vanilla, RigDefinition rig, final double lifeTime, PlayerPose out) {
        if (!update(now)) {
            return null;
        }
        Animation anim = emote.animation;
        double e = Math.max(0, now - start);
        final double t = anim.animationTime(e);
        Molang.Context ctx = new Molang.Context() {
            @Override
            public double animTime() {
                return t;
            }

            @Override
            public double lifeTime() {
                return lifeTime;
            }
        };
        double w = weight(now);
        PoseEvaluator.evaluate(anim, t, vanilla, rig, ctx, emotePose);
        if (w >= 1) {
            return out.set(emotePose);
        }
        PoseEvaluator.evaluate(null, 0, vanilla, rig, ctx, vanillaPose);
        return PlayerPose.blend(vanillaPose, emotePose, w, out);
    }
}
