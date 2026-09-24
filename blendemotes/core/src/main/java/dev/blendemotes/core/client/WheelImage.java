package dev.blendemotes.core.client;

/**
 * Pixels of the emote wheel (a ring of slots), so every Minecraft version can show it as a
 * plain texture instead of version specific drawing code. Anti-aliased by supersampling.
 */
public final class WheelImage {
    /** Inner radius of the ring relative to the outer radius (the centre area selects nothing). */
    public static final double INNER = 0.32;
    /** Gap between slots, radians. */
    private static final double GAP = 0.04;
    private static final int SAMPLES = 4;

    private WheelImage() {
    }

    /**
     * Renders the wheel.
     *
     * @param size    width and height in pixels (the ring touches the edges)
     * @param slots   number of slots; slot 0 is at the top, clockwise
     * @param hovered highlighted slot or -1
     * @param color   ARGB colour of the slots
     * @param hover   ARGB colour of the highlighted slot
     * @return ARGB pixels, row by row, not premultiplied
     */
    public static int[] render(int size, int slots, int hovered, int color, int hover) {
        int[] out = new int[size * size];
        double c = size / 2.0;
        double outer = size / 2.0 - 0.5;
        double inner = outer * INNER;
        double step = Math.PI * 2 / slots;
        double half = step / 2 - GAP / 2;
        for (int py = 0; py < size; py++) {
            for (int px = 0; px < size; px++) {
                int hits = 0;
                int hoverHits = 0;
                for (int sy = 0; sy < SAMPLES; sy++) {
                    for (int sx = 0; sx < SAMPLES; sx++) {
                        double x = px + (sx + 0.5) / SAMPLES - c;
                        double y = py + (sy + 0.5) / SAMPLES - c;
                        double r = Math.sqrt(x * x + y * y);
                        if (r < inner || r > outer) {
                            continue;
                        }
                        double angle = Math.atan2(x, -y);
                        if (angle < 0) {
                            angle += Math.PI * 2;
                        }
                        int slot = (int) Math.floor((angle + step / 2) / step) % slots;
                        double centre = slot * step;
                        double d = Math.abs(angle - centre);
                        d = Math.min(d, Math.PI * 2 - d);
                        if (d > half) {
                            continue; // gap between slots
                        }
                        if (slot == hovered) {
                            hoverHits++;
                        } else {
                            hits++;
                        }
                    }
                }
                int total = SAMPLES * SAMPLES;
                if (hits + hoverHits == 0) {
                    continue;
                }
                out[py * size + px] = hoverHits > hits
                        ? scaleAlpha(hover, (hits + hoverHits) / (double) total)
                        : scaleAlpha(color, (hits + hoverHits) / (double) total);
            }
        }
        return out;
    }

    private static int scaleAlpha(int argb, double coverage) {
        int a = (int) Math.round((argb >>> 24) * coverage);
        return (a << 24) | (argb & 0xFFFFFF);
    }

    /** ARGB to the ABGR layout of Minecraft's NativeImage. */
    public static int toAbgr(int argb) {
        return (argb & 0xFF00FF00) | ((argb & 0xFF) << 16) | ((argb >> 16) & 0xFF);
    }
}
