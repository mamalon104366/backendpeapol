package dev.blendemotes.core.client;

/** Geometry of the radial emote menu, shared by every version's screen implementation. */
public final class EmoteWheel {
    private EmoteWheel() {
    }

    /**
     * Slot under the mouse.
     *
     * @param dx         mouse x minus wheel centre (screen pixels, right positive)
     * @param dy         mouse y minus wheel centre (screen pixels, down positive)
     * @param deadZone   radius of the centre area that selects nothing
     * @param slots      number of slots, slot 0 is at the top and they go clockwise
     * @return the slot index or -1
     */
    public static int slotAt(double dx, double dy, double deadZone, int slots) {
        if (dx * dx + dy * dy < deadZone * deadZone || slots <= 0) {
            return -1;
        }
        double angle = Math.atan2(dx, -dy); // 0 at the top, clockwise
        if (angle < 0) {
            angle += Math.PI * 2;
        }
        double step = Math.PI * 2 / slots;
        int slot = (int) Math.floor((angle + step / 2) / step) % slots;
        return slot;
    }

    /** Centre of a slot on a circle of the given radius: {x, y}. */
    public static double[] slotCenter(int slot, int slots, double radius) {
        double angle = Math.PI * 2 * slot / slots;
        return new double[]{Math.sin(angle) * radius, -Math.cos(angle) * radius};
    }
}
