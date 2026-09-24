package dev.blendemotes.core.client;

import dev.blendemotes.core.TestRunner;

public class WheelTest {
    public void testImageMatchesHitTesting() {
        int size = 128;
        int slots = 8;
        int[] px = WheelImage.render(size, slots, 3, 0x80000000, 0xFF00FF00);
        double c = size / 2.0;
        double outer = c - 0.5;
        double mid = outer * (1 + WheelImage.INNER) / 2;
        for (int slot = 0; slot < slots; slot++) {
            double[] p = EmoteWheel.slotCenter(slot, slots, mid);
            int x = (int) (c + p[0]);
            int y = (int) (c + p[1]);
            int argb = px[y * size + x];
            TestRunner.check(EmoteWheel.slotAt(x + 0.5 - c, y + 0.5 - c, outer * WheelImage.INNER, slots) == slot,
                    "hit test disagrees for slot " + slot);
            if (slot == 3) {
                TestRunner.check(argb == 0xFF00FF00, "hovered slot colour " + Integer.toHexString(argb));
            } else {
                TestRunner.check(argb == 0x80000000, "slot " + slot + " colour " + Integer.toHexString(argb));
            }
        }
        TestRunner.check(px[(size / 2) * size + size / 2] == 0, "centre must be transparent");
        TestRunner.check(px[0] == 0, "corner must be transparent");
    }

    public void testAbgr() {
        TestRunner.check(WheelImage.toAbgr(0x80112233) == 0x80332211, "ABGR conversion");
    }
}
