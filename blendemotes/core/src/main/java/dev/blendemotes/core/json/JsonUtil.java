package dev.blendemotes.core.json;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Typed accessors over the plain-Java tree produced by {@link Json}. */
public final class JsonUtil {
    private JsonUtil() {
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> asObject(Object value, String what) {
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        throw new JsonException("Expected an object for " + what + " but found " + describe(value));
    }

    @SuppressWarnings("unchecked")
    public static List<Object> asArray(Object value, String what) {
        if (value instanceof List) {
            return (List<Object>) value;
        }
        throw new JsonException("Expected an array for " + what + " but found " + describe(value));
    }

    public static Map<String, Object> getObject(Map<String, Object> obj, String key) {
        Object v = obj.get(key);
        if (v == null || v == Json.NULL) {
            return Collections.emptyMap();
        }
        return asObject(v, key);
    }

    public static double getDouble(Map<String, Object> obj, String key, double def) {
        Object v = obj.get(key);
        if (v instanceof Number) {
            return ((Number) v).doubleValue();
        }
        if (v instanceof String) {
            try {
                return Double.parseDouble(((String) v).trim());
            } catch (NumberFormatException ignored) {
                return def;
            }
        }
        if (v instanceof Boolean) {
            return ((Boolean) v) ? 1 : 0;
        }
        return def;
    }

    public static int getInt(Map<String, Object> obj, String key, int def) {
        Object v = obj.get(key);
        if (v instanceof Number) {
            return (int) Math.round(((Number) v).doubleValue());
        }
        return def;
    }

    public static boolean getBoolean(Map<String, Object> obj, String key, boolean def) {
        Object v = obj.get(key);
        if (v instanceof Boolean) {
            return (Boolean) v;
        }
        if (v instanceof Number) {
            return ((Number) v).doubleValue() != 0;
        }
        if (v instanceof String) {
            return Boolean.parseBoolean((String) v);
        }
        return def;
    }

    public static String getString(Map<String, Object> obj, String key, String def) {
        Object v = obj.get(key);
        if (v instanceof String) {
            return (String) v;
        }
        if (v instanceof Number || v instanceof Boolean) {
            return v.toString();
        }
        return def;
    }

    public static String describe(Object value) {
        if (value == null) {
            return "nothing";
        }
        if (value == Json.NULL) {
            return "null";
        }
        if (value instanceof Map) {
            return "an object";
        }
        if (value instanceof List) {
            return "an array";
        }
        if (value instanceof String) {
            return "string \"" + value + "\"";
        }
        return value.toString();
    }
}
