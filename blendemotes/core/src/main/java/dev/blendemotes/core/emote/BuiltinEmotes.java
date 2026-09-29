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
    /** Emotes only the in-game self-test plays (not listed in the menu). */
    public static final String SELFTEST_ROOT = "/assets/blendemotes/selftest/";

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

    /**
     * A self-test emote by name ("Manos y pies" -> selftest/manos_y_pies.json), or null. They show
     * hands, feet and the rest of the rig in the CI screenshots without appearing in the menu.
     */
    public static Emote selfTest(String name) {
        String file = SELFTEST_ROOT + name.toLowerCase(java.util.Locale.ROOT).replace(' ', '_') + ".json";
        InputStream in = BuiltinEmotes.class.getResourceAsStream(file);
        if (in == null) {
            return null;
        }
        try {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            in.close();
            List<Emote> emotes = EmoteFiles.parse(new String(out.toByteArray(), StandardCharsets.UTF_8), "selftest:" + file);
            return emotes.isEmpty() ? null : emotes.get(0);
        } catch (Exception e) {
            return null;
        }
    }
}
