package dev.blendemotes.core.emote;

import dev.blendemotes.core.anim.Animation;

import java.util.UUID;

/** An emote: an animation plus its metadata and a stable identifier. */
public final class Emote {
    /** Content based id; identical files produce identical ids on every client. */
    public final UUID id;
    public final EmoteInfo info;
    public final Animation animation;
    /** Where it was loaded from (file name, "builtin:...", "network"). */
    public final String source;

    public Emote(UUID id, EmoteInfo info, Animation animation, String source) {
        this.id = id;
        this.info = info;
        this.animation = animation;
        this.source = source == null ? "" : source;
    }

    @Override
    public String toString() {
        return "Emote[" + info.name + " / " + id + " from " + source + "]";
    }
}
