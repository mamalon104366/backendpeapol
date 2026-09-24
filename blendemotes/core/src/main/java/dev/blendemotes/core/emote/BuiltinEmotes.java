package dev.blendemotes.core.emote;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Emotes shipped inside the mod jar ({@code assets/blendemotes/emotes/index.txt}). */
public final class BuiltinEmotes {
    public static final String ROOT = "/assets/blendemotes/emotes/";

    private BuiltinEmotes() {
    }

    public static List<EmoteLibrary.Resource> list() {
        List<EmoteLibrary.Resource> out = new ArrayList<EmoteLibrary.Resource>();
        InputStream index = BuiltinEmotes.class.getResourceAsStream(ROOT + "index.txt");
        if (index == null) {
            return out;
        }
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(index, StandardCharsets.UTF_8));
            String line;
            while ((line = r.readLine()) != null) {
                final String name = line.trim();
                if (name.isEmpty() || name.startsWith("#")) {
                    continue;
                }
                out.add(new EmoteLibrary.Resource() {
                    @Override
                    public String name() {
                        return name;
                    }

                    @Override
                    public InputStream open() throws IOException {
                        InputStream in = BuiltinEmotes.class.getResourceAsStream(ROOT + name);
                        if (in == null) {
                            throw new IOException("missing builtin emote " + name);
                        }
                        return in;
                    }
                });
            }
            r.close();
        } catch (IOException ignored) {
            // no builtin emotes
        }
        return out;
    }
}
