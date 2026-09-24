package dev.blendemotes.core.config;

import dev.blendemotes.core.json.Json;
import dev.blendemotes.core.json.JsonUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** User settings, stored as {@code config/blendemotes.json}. */
public final class EmoteConfig {
    public static final int WHEEL_SLOTS = 8;

    public double fadeIn = 0.15;
    public double fadeOut = 0.25;
    public boolean stopOnMove = true;
    public boolean stopOnJump = true;
    public boolean stopOnSneak = true;
    public boolean stopOnAttack = true;
    public boolean stopOnHurt = false;
    /** Switch to third person while the local player emotes (like Lunar). */
    public boolean autoThirdPerson = true;
    /** Render the rig bends (elbows, knees, torso). */
    public boolean bends = true;
    public boolean showOtherPlayers = true;
    /** Let other players see your emotes (needs the mod/plugin on the server). */
    public boolean shareEmotes = true;
    /** Emote folder, relative to the game directory. */
    public String emotesFolder = "blendemotes/emotes";
    /** Emote wheel: emote ids (or names) per slot, "" for empty. */
    public final List<String> wheel = new ArrayList<String>();

    public EmoteConfig() {
        while (wheel.size() < WHEEL_SLOTS) {
            wheel.add("");
        }
    }

    public static EmoteConfig load(File file) {
        EmoteConfig c = new EmoteConfig();
        if (!file.isFile()) {
            return c;
        }
        try {
            String text = dev.blendemotes.core.emote.EmoteFiles.readText(new FileInputStream(file));
            Map<String, Object> o = JsonUtil.asObject(Json.parse(text), "config");
            c.fadeIn = clamp(JsonUtil.getDouble(o, "fadeIn", c.fadeIn), 0, 5);
            c.fadeOut = clamp(JsonUtil.getDouble(o, "fadeOut", c.fadeOut), 0, 5);
            c.stopOnMove = JsonUtil.getBoolean(o, "stopOnMove", c.stopOnMove);
            c.stopOnJump = JsonUtil.getBoolean(o, "stopOnJump", c.stopOnJump);
            c.stopOnSneak = JsonUtil.getBoolean(o, "stopOnSneak", c.stopOnSneak);
            c.stopOnAttack = JsonUtil.getBoolean(o, "stopOnAttack", c.stopOnAttack);
            c.stopOnHurt = JsonUtil.getBoolean(o, "stopOnHurt", c.stopOnHurt);
            c.autoThirdPerson = JsonUtil.getBoolean(o, "autoThirdPerson", c.autoThirdPerson);
            c.bends = JsonUtil.getBoolean(o, "bends", c.bends);
            c.showOtherPlayers = JsonUtil.getBoolean(o, "showOtherPlayers", c.showOtherPlayers);
            c.shareEmotes = JsonUtil.getBoolean(o, "shareEmotes", c.shareEmotes);
            c.emotesFolder = JsonUtil.getString(o, "emotesFolder", c.emotesFolder);
            Object wheel = o.get("wheel");
            if (wheel instanceof List) {
                List<Object> list = JsonUtil.asArray(wheel, "wheel");
                for (int i = 0; i < WHEEL_SLOTS; i++) {
                    Object v = i < list.size() ? list.get(i) : null;
                    c.wheel.set(i, v instanceof String ? (String) v : "");
                }
            }
        } catch (Exception ignored) {
            // broken config: keep defaults, it will be rewritten on save
        }
        return c;
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    public void save(File file) throws IOException {
        Map<String, Object> o = new LinkedHashMap<String, Object>();
        o.put("fadeIn", fadeIn);
        o.put("fadeOut", fadeOut);
        o.put("stopOnMove", stopOnMove);
        o.put("stopOnJump", stopOnJump);
        o.put("stopOnSneak", stopOnSneak);
        o.put("stopOnAttack", stopOnAttack);
        o.put("stopOnHurt", stopOnHurt);
        o.put("autoThirdPerson", autoThirdPerson);
        o.put("bends", bends);
        o.put("showOtherPlayers", showOtherPlayers);
        o.put("shareEmotes", shareEmotes);
        o.put("emotesFolder", emotesFolder);
        o.put("wheel", new ArrayList<Object>(wheel));
        File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("cannot create " + parent);
        }
        File tmp = new File(file.getPath() + ".tmp");
        Writer w = new OutputStreamWriter(new FileOutputStream(tmp), StandardCharsets.UTF_8);
        try {
            w.write(Json.writePretty(o));
        } finally {
            w.close();
        }
        if (file.exists() && !file.delete()) {
            throw new IOException("cannot replace " + file);
        }
        if (!tmp.renameTo(file)) {
            throw new IOException("cannot write " + file);
        }
    }
}
