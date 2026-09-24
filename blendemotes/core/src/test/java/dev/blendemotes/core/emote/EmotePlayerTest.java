package dev.blendemotes.core.emote;

import dev.blendemotes.core.TestRunner;
import dev.blendemotes.core.pose.PlayerPose;
import dev.blendemotes.core.pose.VanillaPose;
import dev.blendemotes.core.rig.RigDefinition;

public class EmotePlayerTest {
    public void testFadesAndLifecycle() {
        Emote e = EmoteFiles.parse(dev.blendemotes.core.TestRunner.resource("/blender/inchworm.json"), "x").get(0);
        EmotePlayer p = new EmotePlayer();
        p.setFades(0.2, 0.2);
        p.play(e, 10, 0);
        TestRunner.near(0, p.weight(10), 1e-12, "fade starts at 0");
        TestRunner.near(1, p.weight(10.2), 1e-12, "fully in");
        TestRunner.check(p.evaluate(10.5, new VanillaPose(), RigDefinition.MINECRAFT, 0, new PlayerPose()) != null, "playing");
        p.stop(11);
        TestRunner.check(p.isStopping(), "stopping");
        TestRunner.near(0.5, p.weight(11.1), 1e-12, "half faded out");
        TestRunner.check(!p.update(11.21), "finished after fade out");
        TestRunner.check(p.current() == null, "cleared");
        // looping emote keeps playing past its length
        p.play(e, 0, 0);
        TestRunner.check(p.update(e.animation.length * 3), "loop continues");
    }
}
