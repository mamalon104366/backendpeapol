package dev.blendemotes.modern.render;

import java.util.UUID;

/** Emote data carried by the player render state (Minecraft 1.21.2 and newer). */
public interface EmoteRenderState {
    void blendemotes$set(UUID player, boolean slim);

    /** The rendered player, null for render states of other entities. */
    UUID blendemotes$player();

    boolean blendemotes$slim();
}
