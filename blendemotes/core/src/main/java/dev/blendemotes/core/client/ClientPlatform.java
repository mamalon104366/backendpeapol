package dev.blendemotes.core.client;

import dev.blendemotes.core.emote.EmoteLibrary;

import java.io.File;
import java.util.List;
import java.util.UUID;

/** What the version specific code provides to {@link ClientEmotes}. */
public interface ClientPlatform {
    /** Sends a payload on the BlendEmotes channel (no-op when not connected). */
    void sendPacket(byte[] payload);

    /** Monotonic emote clock in seconds, normally {@code (ticks + partialTick) / 20}. */
    double clock();

    /** UUID of the local player, or null when not in a world. */
    UUID localPlayer();

    /** 0 = first person, 1 = third person back, 2 = third person front. */
    int perspective();

    void setPerspective(int perspective);

    File gameDirectory();

    List<EmoteLibrary.Resource> builtinEmotes();

    void log(String message);
}
