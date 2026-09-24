package dev.blendemotes.mc1201;

import dev.blendemotes.core.emote.Emote;
import dev.blendemotes.core.pose.PlayerPose;
import dev.blendemotes.core.pose.VanillaPose;
import dev.blendemotes.core.rig.PlayerPart;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

/**
 * Automated in-game check used by CI ({@code -Dblendemotes.selftest=true}): creates a flat
 * world, plays the built-in emotes in front of the camera, saves screenshots and quits.
 */
final class SelfTest {
    private static final boolean ENABLED = Boolean.getBoolean("blendemotes.selftest")
            || "1".equals(System.getenv("BLENDEMOTES_SELFTEST"));
    private static final String[] EMOTES = {"Inchworm", "Cartwheel"};
    private static final int[] SHOTS = {6, 12, 18, 24};
    private static int state;
    private static int timer;
    private static int emoteIndex;

    private SelfTest() {
    }

    static void tick(Minecraft mc) {
        if (!ENABLED) {
            return;
        }
        timer++;
        switch (state) {
            case 0:
                if (timer > 100 && mc.level == null) {
                    ModernEmotes.LOGGER.info("[selftest] creating world");
                    LevelSettings settings = new LevelSettings("blendemotes_selftest", GameType.CREATIVE, false,
                            Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("blendemotes_selftest", settings, new WorldOptions(1L, false, false),
                            registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
                    next();
                }
                break;
            case 1:
                if (mc.player != null && mc.level != null && timer > 100) {
                    if (mc.getSingleplayerServer() != null) {
                        mc.getSingleplayerServer().overworld().setDayTime(6000);
                    }
                    mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                    mc.options.hideGui = true;
                    mc.player.setYRot(0);
                    mc.player.setXRot(10);
                    mc.player.setYHeadRot(0);
                    mc.player.setYBodyRot(0);
                    next();
                }
                break;
            case 2:
                if (timer == 10) {
                    Emote e = ModernEmotes.client().library.findByName(EMOTES[emoteIndex]);
                    ModernEmotes.LOGGER.info("[selftest] playing " + EMOTES[emoteIndex] + " -> " + e);
                    if (e != null) {
                        ModernEmotes.client().playLocal(e);
                    }
                }
                for (int i = 0; i < SHOTS.length; i++) {
                    if (timer == 10 + SHOTS[i]) {
                        shot(mc, EMOTES[emoteIndex].toLowerCase() + "_" + i);
                    }
                }
                if (timer > 10 + SHOTS[SHOTS.length - 1] + 2) {
                    ModernEmotes.client().stopLocal();
                    emoteIndex++;
                    if (emoteIndex >= EMOTES.length) {
                        next();
                    } else {
                        timer = -20;
                    }
                }
                break;
            case 3:
                if (timer > 20) {
                    shot(mc, "after_stop");
                    ModernEmotes.LOGGER.info("[selftest] BLENDEMOTES_SELFTEST_DONE");
                    next();
                    mc.stop();
                }
                break;
            default:
                break;
        }
    }

    private static void next() {
        state++;
        timer = 0;
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, "blendemotes_" + name + ".png", mc.getMainRenderTarget(), msg -> { });
        PlayerPose pose = ModernEmotes.client().pose(mc.player.getUUID(), new VanillaPose(), false, 0, new PlayerPose());
        if (pose != null) {
            ModernEmotes.LOGGER.info("[selftest] " + name + " right_arm " + pose.transform(PlayerPart.RIGHT_ARM)
                    + " bend " + Math.toDegrees(pose.bend(PlayerPart.RIGHT_ARM)));
        } else {
            ModernEmotes.LOGGER.info("[selftest] " + name + " (no emote)");
        }
    }
}
