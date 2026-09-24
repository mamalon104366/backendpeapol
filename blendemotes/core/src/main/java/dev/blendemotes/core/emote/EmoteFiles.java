package dev.blendemotes.core.emote;

import dev.blendemotes.core.anim.io.BedrockAnimationLoader;
import dev.blendemotes.core.anim.io.LegacyEmoteLoader;
import dev.blendemotes.core.json.Json;
import dev.blendemotes.core.json.JsonException;
import dev.blendemotes.core.json.JsonUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Reads emote files of every supported format. */
public final class EmoteFiles {
    /** Refuse absurdly large files (a Blender export with icon is ~0.6 MB). */
    public static final int MAX_FILE_BYTES = 16 * 1024 * 1024;

    private EmoteFiles() {
    }

    /**
     * Parses a file's text. A Bedrock file may contain several animations, each one becomes
     * an emote.
     */
    public static List<Emote> parse(String json, String source) {
        Object rootObj = Json.parse(json);
        Map<String, Object> root = JsonUtil.asObject(rootObj, "file");
        List<BedrockAnimationLoader.Entry> entries;
        if (BedrockAnimationLoader.looksLikeBedrock(root)) {
            entries = BedrockAnimationLoader.load(root);
        } else if (LegacyEmoteLoader.looksLikeLegacy(root)) {
            entries = new ArrayList<BedrockAnimationLoader.Entry>();
            entries.add(LegacyEmoteLoader.load(root));
        } else {
            throw new JsonException("not an emote file (expected \"animations\" or \"emote\")");
        }
        List<Emote> out = new ArrayList<Emote>();
        for (BedrockAnimationLoader.Entry e : entries) {
            UUID id = UUID.nameUUIDFromBytes(e.canonical.getBytes(StandardCharsets.UTF_8));
            String name = e.info.name.isEmpty() ? e.key : e.info.name;
            out.add(new Emote(id, e.info.withName(name), e.animation, source));
        }
        return out;
    }

    public static List<Emote> read(InputStream in, String source) throws IOException {
        return parse(readText(in), source);
    }

    public static String readText(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[65536];
        int n;
        int total = 0;
        try {
            while ((n = in.read(buf)) > 0) {
                total += n;
                if (total > MAX_FILE_BYTES) {
                    throw new IOException("file too large");
                }
                out.write(buf, 0, n);
            }
        } finally {
            in.close();
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
