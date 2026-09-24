package dev.blendemotes.mc189;

import dev.blendemotes.core.client.ClientEmotes;
import dev.blendemotes.core.emote.Emote;
import dev.blendemotes.core.pose.PlayerPose;
import dev.blendemotes.core.pose.VanillaPose;
import dev.blendemotes.core.rig.PlayerPart;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

/**
 * Automated in-game check used by CI ({@code -Dblendemotes.selftest=true}): creates a flat
 * world, plays the built-in emotes in front of the camera, saves screenshots and quits.
 */
final class SelfTest189 {
    private static final boolean ENABLED = Boolean.getBoolean("blendemotes.selftest");
    private static final String[] EMOTES = {"Inchworm", "Cartwheel"};
    private static final int[] SHOTS = {6, 12, 18, 24};
    private static int state;
    private static int timer;
    private static int emoteIndex;

    private SelfTest189() {
    }

    static void tick(Minecraft mc) {
        if (!ENABLED) {
            return;
        }
        timer++;
        switch (state) {
            case 0:
                if (mc.currentScreen instanceof GuiMainMenu && timer > 40) {
                    BlendEmotes189.LOGGER.info("[selftest] creating world");
                    mc.launchIntegratedServer("blendemotes_selftest", "BlendEmotes selftest",
                            new WorldSettings(1L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
                    next();
                }
                break;
            case 1:
                if (mc.thePlayer != null && mc.theWorld != null && timer > 100) {
                    if (mc.getIntegratedServer() != null) {
                        mc.getIntegratedServer().worldServers[0].setWorldTime(6000);
                    }
                    mc.gameSettings.thirdPersonView = 2;
                    mc.gameSettings.hideGUI = true;
                    mc.thePlayer.rotationYaw = 0;
                    mc.thePlayer.rotationPitch = 10;
                    mc.thePlayer.rotationYawHead = 0;
                    mc.thePlayer.renderYawOffset = 0;
                    next();
                }
                break;
            case 2:
                if (timer == 10) {
                    Emote e = BlendEmotes189.client().library.findByName(EMOTES[emoteIndex]);
                    BlendEmotes189.LOGGER.info("[selftest] playing " + EMOTES[emoteIndex] + " -> " + e);
                    if (e != null) {
                        BlendEmotes189.client().playLocal(e);
                    }
                }
                for (int i = 0; i < SHOTS.length; i++) {
                    if (timer == 10 + SHOTS[i]) {
                        shot(mc, EMOTES[emoteIndex].toLowerCase() + "_" + i);
                    }
                }
                if (timer > 10 + SHOTS[SHOTS.length - 1] + 2) {
                    BlendEmotes189.client().stopLocal();
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
                    BlendEmotes189.LOGGER.info("[selftest] BLENDEMOTES_SELFTEST_DONE");
                    next();
                    mc.shutdown();
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
        ScreenShotHelper.saveScreenshot(mc.mcDataDir, "blendemotes_" + name + ".png", mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        ClientEmotes client = BlendEmotes189.client();
        PlayerPose pose = client.pose(mc.thePlayer.getUniqueID(), new VanillaPose(), false, 0, new PlayerPose());
        if (pose != null) {
            BlendEmotes189.LOGGER.info("[selftest] " + name + " right_arm " + pose.transform(PlayerPart.RIGHT_ARM)
                    + " bend " + Math.toDegrees(pose.bend(PlayerPart.RIGHT_ARM)));
        } else {
            BlendEmotes189.LOGGER.info("[selftest] " + name + " (no emote)");
        }
    }
}
