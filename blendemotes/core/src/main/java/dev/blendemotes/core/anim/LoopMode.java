package dev.blendemotes.core.anim;

/** What happens when the animation reaches its end. */
public enum LoopMode {
    /** Play once, then the emote finishes (and fades back to the vanilla pose). */
    PLAY_ONCE,
    /** Jump back to {@link Animation#loopStart} and keep playing until stopped. */
    LOOP,
    /** Keep the last frame until the emote is stopped. */
    HOLD_ON_LAST_FRAME
}
