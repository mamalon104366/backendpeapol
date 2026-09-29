package dev.blendemotes.legacy.api;

import dev.blendemotes.core.anim.LoopMode;
import dev.blendemotes.core.client.ClientEmotes;
import dev.blendemotes.core.emote.Emote;
import dev.blendemotes.legacy.LegacyEmotes;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.Entity;

import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

/**
 * For other client mods (such as a client with its own emote wheel) that want to show and play
 * BlendEmotes emotes. Every signature uses only JDK and vanilla types, so it can be called through
 * reflection without a compile-time dependency on BlendEmotes.
 */
public final class BlendEmotesApi {
    /** Bumped when a method changes. */
    public static final int VERSION = 1;

    private static final List<BiConsumer<Object, Object>> POSE_HOOKS = new CopyOnWriteArrayList<BiConsumer<Object, Object>>();
    private static volatile boolean ownKeys = true;

    private BlendEmotesApi() {
    }

    /** True once the client side is running. */
    public static boolean ready() {
        return LegacyEmotes.client() != null;
    }

    /**
     * The local emotes (built-in and the emotes folder) as a JSON array of
     * {@code {"id", "name", "author", "description", "loop", "icon"}}; {@code icon} is a PNG in
     * base64 or null.
     */
    public static String emotesJson() {
        ClientEmotes client = LegacyEmotes.client();
        StringBuilder sb = new StringBuilder("[");
        if (client != null) {
            boolean first = true;
            for (Emote e : client.emotes()) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                sb.append("{\"id\":").append(quote(e.id.toString()));
                sb.append(",\"name\":").append(quote(e.info.name));
                sb.append(",\"author\":").append(quote(e.info.author));
                sb.append(",\"description\":").append(quote(e.info.description));
                sb.append(",\"loop\":").append(e.animation.loopMode != LoopMode.PLAY_ONCE);
                sb.append(",\"icon\":").append(e.info.icon == null ? "null" : quote(Base64.getEncoder().encodeToString(e.info.icon)));
                sb.append('}');
            }
        }
        return sb.append(']').toString();
    }

    /** Plays an emote of the list on the local player. */
    public static boolean play(String id) {
        ClientEmotes client = LegacyEmotes.client();
        if (client == null || id == null) {
            return false;
        }
        try {
            Emote e = client.library.get(UUID.fromString(id));
            if (e == null) {
                return false;
            }
            client.playLocal(e);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public static void stop() {
        ClientEmotes client = LegacyEmotes.client();
        if (client != null) {
            client.stopLocal();
        }
    }

    public static boolean isPlaying() {
        ClientEmotes client = LegacyEmotes.client();
        return client != null && client.isLocalPlaying();
    }

    /** Reads the emotes folder again. */
    public static void reload() {
        ClientEmotes client = LegacyEmotes.client();
        if (client != null) {
            client.reload();
        }
    }

    /** Absolute path of the emotes folder (created if missing), or null before start-up. */
    public static String folder() {
        ClientEmotes client = LegacyEmotes.client();
        if (client == null) {
            return null;
        }
        java.io.File f = client.emotesFolder();
        if (!f.isDirectory()) {
            //noinspection ResultOfMethodCallIgnored
            f.mkdirs();
        }
        return f.getAbsolutePath();
    }

    /**
     * A mod that shows BlendEmotes emotes in its own wheel turns BlendEmotes' own wheel and menu keys
     * off, so the two do not open at once (they would share the B key).
     */
    public static void setOwnKeysEnabled(boolean enabled) {
        ownKeys = enabled;
        LegacyEmotes.applyOwnKeys(enabled);
    }

    public static boolean ownKeysEnabled() {
        return ownKeys;
    }

    /**
     * Extra pose for the player and armour models: called with {@code (ModelBiped, Entity)} after the
     * vanilla animation and before a BlendEmotes emote is applied (which wins while it plays). Lets
     * another mod keep its own simple emotes without replacing BlendEmotes' models.
     */
    public static void addPoseHook(BiConsumer<Object, Object> hook) {
        if (hook != null) {
            POSE_HOOKS.add(hook);
        }
    }

    /** Runs the pose hooks on a model (BlendEmotes' own models call this). */
    public static void runPoseHooks(ModelBiped model, Entity entity) {
        for (BiConsumer<Object, Object> hook : POSE_HOOKS) {
            try {
                hook.accept(model, entity);
            } catch (Throwable t) {
                LegacyEmotes.LOGGER.warn("A pose hook failed; it is removed", t);
                POSE_HOOKS.remove(hook);
            }
        }
    }

    private static String quote(String s) {
        if (s == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.append('"').toString();
    }
}
