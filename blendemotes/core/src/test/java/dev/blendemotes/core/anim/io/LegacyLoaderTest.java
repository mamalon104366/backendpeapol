package dev.blendemotes.core.anim.io;

import dev.blendemotes.core.TestRunner;
import dev.blendemotes.core.anim.LoopMode;
import dev.blendemotes.core.emote.Emote;
import dev.blendemotes.core.emote.EmoteFiles;
import dev.blendemotes.core.pose.PlayerPose;
import dev.blendemotes.core.pose.PoseEvaluator;
import dev.blendemotes.core.pose.VanillaPose;
import dev.blendemotes.core.rig.PlayerPart;

public class LegacyLoaderTest {
    private static final String WAVE = "{\"version\": 3, \"name\": \"Wave\", \"author\": \"me\", \"emote\": {"
            + "\"beginTick\": 0, \"endTick\": 20, \"stopTick\": 25, \"isLoop\": false, \"returnTick\": 0, \"degrees\": true,"
            + "\"moves\": ["
            + "{\"tick\": 0, \"easing\": \"LINEAR\", \"rightArm\": {\"pitch\": 0}},"
            + "{\"tick\": 10, \"easing\": \"EASEINOUTQUAD\", \"rightArm\": {\"pitch\": -120, \"bend\": 1.0, \"y\": 1}},"
            + "{\"tick\": 20, \"easing\": \"LINEAR\", \"rightArm\": {\"pitch\": -90}}"
            + "]}}";

    public void testLegacyWave() {
        Emote e = EmoteFiles.parse(WAVE, "wave.json").get(0);
        TestRunner.check("Wave".equals(e.info.name), "name");
        TestRunner.check(e.animation.loopMode == LoopMode.PLAY_ONCE, "play once");
        TestRunner.near(25 / 20.0, e.animation.length, 1e-9, "length = stopTick");
        PlayerPose pose = PoseEvaluator.evaluate(e.animation, 0.5, new VanillaPose(), new PlayerPose());
        TestRunner.near(Math.toRadians(-120), pose.transform(PlayerPart.RIGHT_ARM).pitch, 1e-9, "pitch at key");
        TestRunner.near(1.0, pose.bend(PlayerPart.RIGHT_ARM), 1e-9, "bend (radians in legacy files)");
        // legacy y is absolute model space (y down): 1 means 1 px below the default pivot? no:
        // it is the absolute pivot, default 2 -> offset -1 -> moved up by 1
        TestRunner.near(1, pose.transform(PlayerPart.RIGHT_ARM).y, 1e-9, "absolute legacy y");
        // easing after the key (legacy default): 0 -> 10 is linear
        PlayerPose quarter = PoseEvaluator.evaluate(e.animation, 0.25, new VanillaPose(), new PlayerPose());
        TestRunner.near(Math.toRadians(-60), quarter.transform(PlayerPart.RIGHT_ARM).pitch, 1e-9, "linear first half");
    }
}
