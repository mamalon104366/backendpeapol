package dev.blendemotes.core.emote;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Human facing metadata of an emote (shown in the emote menu). */
public final class EmoteInfo {
    public final String name;
    public final String author;
    public final String description;
    public final List<Badge> badges;
    /** PNG bytes of the icon, or null. */
    public final byte[] icon;

    public EmoteInfo(String name, String author, String description, List<Badge> badges, byte[] icon) {
        this.name = name == null ? "" : name;
        this.author = author == null ? "" : author;
        this.description = description == null ? "" : description;
        this.badges = badges == null ? Collections.<Badge>emptyList() : Collections.unmodifiableList(new ArrayList<Badge>(badges));
        this.icon = icon;
    }

    public EmoteInfo withName(String newName) {
        return new EmoteInfo(newName, author, description, badges, icon);
    }

    /** Small coloured label ("Example", "Dance"...). */
    public static final class Badge {
        public final String text;
        /** 0xRRGGBB */
        public final int color;

        public Badge(String text, int color) {
            this.text = text == null ? "" : text;
            this.color = color & 0xFFFFFF;
        }
    }
}
