package dev.blendemotes.mc189.render;

import dev.blendemotes.mc189.BlendEmotes189;

/** Render related settings read from the core config. */
public final class EmoteSettings {
    private EmoteSettings() {
    }

    public static boolean bends() {
        return BlendEmotes189.client() == null || BlendEmotes189.client().config().bends;
    }
}
