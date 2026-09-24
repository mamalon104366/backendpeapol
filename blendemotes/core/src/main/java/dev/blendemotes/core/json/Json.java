package dev.blendemotes.core.json;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal, dependency free JSON reader/writer.
 * <p>
 * Minecraft ships a different Gson version in every release (1.8.9 still uses Gson 2.2.4),
 * so the core keeps its own tiny implementation instead of depending on any of them.
 * <p>
 * Values are mapped to plain Java types:
 * object -> {@link LinkedHashMap}, array -> {@link ArrayList}, number -> {@link Double},
 * string -> {@link String}, boolean -> {@link Boolean}, null -> {@link #NULL}.
 */
public final class Json {
    /** Marker for a JSON {@code null}; {@code Map.get} returning Java null means "missing". */
    public static final Object NULL = new Object() {
        @Override
        public String toString() {
            return "null";
        }
    };

    private final String src;
    private int pos;

    private Json(String src) {
        this.src = src;
    }

    public static Object parse(String text) {
        Json parser = new Json(text);
        parser.skipWhitespace();
        if (parser.pos < text.length() && text.charAt(parser.pos) == '﻿') {
            parser.pos++;
            parser.skipWhitespace();
        }
        Object value = parser.readValue();
        parser.skipWhitespace();
        if (parser.pos != text.length()) {
            throw parser.error("Unexpected trailing data");
        }
        return value;
    }

    // ---------------------------------------------------------------- reading

    private JsonException error(String message) {
        int line = 1;
        int col = 1;
        for (int i = 0; i < pos && i < src.length(); i++) {
            if (src.charAt(i) == '\n') {
                line++;
                col = 1;
            } else {
                col++;
            }
        }
        return new JsonException(message + " at line " + line + ", column " + col);
    }

    private void skipWhitespace() {
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                pos++;
            } else if (c == '/' && pos + 1 < src.length() && src.charAt(pos + 1) == '/') {
                // lenient: line comments (some hand written emotes contain them)
                while (pos < src.length() && src.charAt(pos) != '\n') {
                    pos++;
                }
            } else if (c == '/' && pos + 1 < src.length() && src.charAt(pos + 1) == '*') {
                int end = src.indexOf("*/", pos + 2);
                if (end < 0) {
                    throw error("Unterminated comment");
                }
                pos = end + 2;
            } else {
                break;
            }
        }
    }

    private Object readValue() {
        if (pos >= src.length()) {
            throw error("Unexpected end of input");
        }
        char c = src.charAt(pos);
        switch (c) {
            case '{':
                return readObject();
            case '[':
                return readArray();
            case '"':
                return readString();
            case 't':
                expectWord("true");
                return Boolean.TRUE;
            case 'f':
                expectWord("false");
                return Boolean.FALSE;
            case 'n':
                expectWord("null");
                return NULL;
            default:
                if (c == '-' || c == '+' || c == '.' || (c >= '0' && c <= '9') || c == 'N' || c == 'I') {
                    return readNumber();
                }
                throw error("Unexpected character '" + c + "'");
        }
    }

    private void expectWord(String word) {
        if (!src.startsWith(word, pos)) {
            throw error("Expected '" + word + "'");
        }
        pos += word.length();
    }

    private Map<String, Object> readObject() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        pos++; // {
        skipWhitespace();
        if (peek() == '}') {
            pos++;
            return map;
        }
        while (true) {
            skipWhitespace();
            if (peek() == '}') { // lenient trailing comma
                pos++;
                return map;
            }
            if (peek() != '"') {
                throw error("Expected string key");
            }
            String key = readString();
            skipWhitespace();
            if (peek() != ':') {
                throw error("Expected ':'");
            }
            pos++;
            skipWhitespace();
            map.put(key, readValue());
            skipWhitespace();
            char c = peek();
            pos++;
            if (c == ',') {
                continue;
            }
            if (c == '}') {
                return map;
            }
            pos--;
            throw error("Expected ',' or '}'");
        }
    }

    private List<Object> readArray() {
        List<Object> list = new ArrayList<Object>();
        pos++; // [
        skipWhitespace();
        if (peek() == ']') {
            pos++;
            return list;
        }
        while (true) {
            skipWhitespace();
            if (peek() == ']') { // lenient trailing comma
                pos++;
                return list;
            }
            list.add(readValue());
            skipWhitespace();
            char c = peek();
            pos++;
            if (c == ',') {
                continue;
            }
            if (c == ']') {
                return list;
            }
            pos--;
            throw error("Expected ',' or ']'");
        }
    }

    private char peek() {
        if (pos >= src.length()) {
            throw error("Unexpected end of input");
        }
        return src.charAt(pos);
    }

    private String readString() {
        pos++; // opening quote
        StringBuilder sb = null;
        int start = pos;
        while (true) {
            if (pos >= src.length()) {
                throw error("Unterminated string");
            }
            char c = src.charAt(pos);
            if (c == '"') {
                String result = sb == null ? src.substring(start, pos) : sb.append(src, start, pos).toString();
                pos++;
                return result;
            }
            if (c == '\\') {
                if (sb == null) {
                    sb = new StringBuilder();
                }
                sb.append(src, start, pos);
                pos++;
                if (pos >= src.length()) {
                    throw error("Unterminated escape");
                }
                char e = src.charAt(pos);
                switch (e) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'u':
                        if (pos + 4 >= src.length()) {
                            throw error("Bad unicode escape");
                        }
                        try {
                            sb.append((char) Integer.parseInt(src.substring(pos + 1, pos + 5), 16));
                        } catch (NumberFormatException ex) {
                            throw error("Bad unicode escape");
                        }
                        pos += 4;
                        break;
                    default:
                        throw error("Bad escape '\\" + e + "'");
                }
                pos++;
                start = pos;
                continue;
            }
            pos++;
        }
    }

    private Double readNumber() {
        int start = pos;
        if (src.startsWith("NaN", pos)) {
            pos += 3;
            return Double.NaN;
        }
        if (peek() == '-' || peek() == '+') {
            pos++;
        }
        if (src.startsWith("Infinity", pos)) {
            pos += 8;
            return src.charAt(start) == '-' ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
        }
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if ((c >= '0' && c <= '9') || c == '.' || c == 'e' || c == 'E' || c == '-' || c == '+') {
                pos++;
            } else {
                break;
            }
        }
        String text = src.substring(start, pos);
        try {
            return Double.valueOf(text);
        } catch (NumberFormatException ex) {
            pos = start;
            throw error("Bad number '" + text + "'");
        }
    }

    // ---------------------------------------------------------------- writing

    public static String write(Object value) {
        StringBuilder sb = new StringBuilder();
        write(sb, value, -1, 0);
        return sb.toString();
    }

    public static String writePretty(Object value) {
        StringBuilder sb = new StringBuilder();
        write(sb, value, 2, 0);
        return sb.toString();
    }

    private static void newline(StringBuilder sb, int indent, int depth) {
        if (indent < 0) {
            return;
        }
        sb.append('\n');
        for (int i = 0; i < indent * depth; i++) {
            sb.append(' ');
        }
    }

    @SuppressWarnings("unchecked")
    private static void write(StringBuilder sb, Object value, int indent, int depth) {
        if (value == null || value == NULL) {
            sb.append("null");
        } else if (value instanceof String) {
            writeString(sb, (String) value);
        } else if (value instanceof Boolean) {
            sb.append(value.toString());
        } else if (value instanceof Number) {
            double d = ((Number) value).doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                sb.append("0");
            } else if (d == Math.rint(d) && Math.abs(d) < 1e15) {
                sb.append((long) d);
            } else {
                sb.append(d);
            }
        } else if (value instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) value;
            sb.append('{');
            Iterator<Map.Entry<String, Object>> it = map.entrySet().iterator();
            boolean any = false;
            while (it.hasNext()) {
                Map.Entry<String, Object> e = it.next();
                newline(sb, indent, depth + 1);
                writeString(sb, e.getKey());
                sb.append(indent < 0 ? ":" : ": ");
                write(sb, e.getValue(), indent, depth + 1);
                if (it.hasNext()) {
                    sb.append(',');
                }
                any = true;
            }
            if (any) {
                newline(sb, indent, depth);
            }
            sb.append('}');
        } else if (value instanceof Iterable) {
            sb.append('[');
            Iterator<Object> it = ((Iterable<Object>) value).iterator();
            boolean any = false;
            while (it.hasNext()) {
                newline(sb, indent, depth + 1);
                write(sb, it.next(), indent, depth + 1);
                if (it.hasNext()) {
                    sb.append(',');
                }
                any = true;
            }
            if (any) {
                newline(sb, indent, depth);
            }
            sb.append(']');
        } else {
            writeString(sb, value.toString());
        }
    }

    private static void writeString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                default:
                    if (c < 0x20) {
                        String hex = Integer.toHexString(c);
                        sb.append("\\u");
                        for (int k = hex.length(); k < 4; k++) {
                            sb.append('0');
                        }
                        sb.append(hex);
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }
}
