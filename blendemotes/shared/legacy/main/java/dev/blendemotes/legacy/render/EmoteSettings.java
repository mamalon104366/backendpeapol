package dev.blendemotes.legacy.render;

import dev.blendemotes.legacy.LegacyEmotes;

/** Render related settings read from the core config. */
public final class EmoteSettings {
    private EmoteSettings() {
    }

    public static boolean bends() {
        return LegacyEmotes.client() == null || LegacyEmotes.client().config().bends;
    }
}
