package dev.blendemotes.core.emote;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * All emotes known on this client: built-in ones, the ones in the emotes folder and the ones
 * received from other players. Thread safe (files can be loaded off-thread).
 */
public final class EmoteLibrary {
    /** A built-in emote bundled in the mod jar. */
    public interface Resource {
        String name();

        InputStream open() throws IOException;
    }

    private final Map<UUID, Emote> local = new LinkedHashMap<UUID, Emote>();
    private final Map<UUID, Emote> remote = new LinkedHashMap<UUID, Emote>();
    private final Map<String, String> errors = new LinkedHashMap<String, String>();
    private List<Emote> sorted = Collections.emptyList();

    /** Reloads built-in and folder emotes. Remote emotes are kept. */
    public synchronized void reload(List<? extends Resource> builtin, File folder) {
        local.clear();
        errors.clear();
        if (builtin != null) {
            for (Resource r : builtin) {
                try {
                    addAll(EmoteFiles.read(r.open(), "builtin:" + r.name()));
                } catch (Exception ex) {
                    errors.put("builtin:" + r.name(), describe(ex));
                }
            }
        }
        if (folder != null) {
            if (!folder.isDirectory()) {
                //noinspection ResultOfMethodCallIgnored
                folder.mkdirs();
            }
            List<File> files = new ArrayList<File>();
            collect(folder, files, 0);
            Collections.sort(files);
            for (File f : files) {
                try {
                    addAll(EmoteFiles.read(new FileInputStream(f), f.getName()));
                } catch (Exception ex) {
                    errors.put(f.getName(), describe(ex));
                }
            }
        }
        rebuild();
    }

    private static void collect(File dir, List<File> out, int depth) {
        File[] list = dir.listFiles();
        if (list == null || depth > 4) {
            return;
        }
        for (File f : list) {
            if (f.isDirectory()) {
                collect(f, out, depth + 1);
            } else if (f.getName().toLowerCase(Locale.ROOT).endsWith(".json")) {
                out.add(f);
            }
        }
    }

    private static String describe(Exception ex) {
        String msg = ex.getMessage();
        return msg == null ? ex.getClass().getSimpleName() : msg;
    }

    private void addAll(List<Emote> emotes) {
        for (Emote e : emotes) {
            local.put(e.id, e);
        }
    }

    /** Adds an emote received from the network (not saved to disk). */
    public synchronized void addRemote(Emote emote) {
        if (!local.containsKey(emote.id)) {
            remote.put(emote.id, emote);
            // keep memory bounded
            if (remote.size() > 256) {
                UUID first = remote.keySet().iterator().next();
                remote.remove(first);
            }
        }
    }

    public synchronized Emote get(UUID id) {
        Emote e = local.get(id);
        return e != null ? e : remote.get(id);
    }

    /** True when the emote is available locally (built-in or in the folder). */
    public synchronized boolean isLocal(UUID id) {
        return local.containsKey(id);
    }

    public synchronized Emote findByName(String name) {
        for (Emote e : local.values()) {
            if (e.info.name.equalsIgnoreCase(name)) {
                return e;
            }
        }
        return null;
    }

    /** Local emotes sorted by name. */
    public synchronized List<Emote> all() {
        return sorted;
    }

    public synchronized Map<String, String> errors() {
        return new LinkedHashMap<String, String>(errors);
    }

    private void rebuild() {
        List<Emote> list = new ArrayList<Emote>(local.values());
        Collections.sort(list, new Comparator<Emote>() {
            @Override
            public int compare(Emote a, Emote b) {
                int c = a.info.name.compareToIgnoreCase(b.info.name);
                return c != 0 ? c : a.id.compareTo(b.id);
            }
        });
        sorted = Collections.unmodifiableList(list);
    }
}
