package dev.blendemotes.core.json;

/** Thrown for malformed JSON or JSON that does not match the expected emote structure. */
public class JsonException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public JsonException(String message) {
        super(message);
    }

    public JsonException(String message, Throwable cause) {
        super(message, cause);
    }
}
