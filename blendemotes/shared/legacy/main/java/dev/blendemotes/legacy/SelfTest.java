package dev.blendemotes.legacy;

import dev.blendemotes.core.client.ClientEmotes;
import dev.blendemotes.core.emote.Emote;
import dev.blendemotes.core.pose.PlayerPose;
import dev.blendemotes.core.pose.VanillaPose;
import dev.blendemotes.core.rig.PlayerPart;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
//#if MC >= 11202
import net.minecraft.world.GameType;
//#endif

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
    /** Ticks since the game started; the test gives up (and fails) after about 10 minutes. */
    private static int total;

    private SelfTest() {
    }

    static void tick(Minecraft mc) {
        if (!ENABLED) {
            return;
        }
        timer++;
        total++;
        if (total % 200 == 0) {
            LegacyEmotes.LOGGER.info("[selftest] progress: state " + state + ", timer " + timer + ", screen " + mc.currentScreen);
        }
        if (total > 20 * 60 * 10 && state < 4) {
            LegacyEmotes.LOGGER.error("[selftest] BLENDEMOTES_SELFTEST_TIMEOUT in state " + state);
            state = 4;
            mc.shutdown();
            return;
        }
        switch (state) {
            case 0:
                if (mc.currentScreen instanceof GuiMainMenu && timer > 40) {
                    LegacyEmotes.LOGGER.info("[selftest] creating world");
                    mc.launchIntegratedServer("blendemotes_selftest", "BlendEmotes selftest",
                            //#if MC >= 11202
                            new WorldSettings(1L, GameType.CREATIVE, false, false, WorldType.FLAT));
                            //#else
                            new WorldSettings(1L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
                            //#endif
                    next();
                }
                break;
            case 1:
                EntityPlayerSP player = Compat.player();
                if (player != null && Compat.world() != null && timer > 100) {
                    if (mc.getIntegratedServer() != null) {
                        //#if MC >= 11202
                        mc.getIntegratedServer().worlds[0].setWorldTime(6000);
                        //#else
                        mc.getIntegratedServer().worldServers[0].setWorldTime(6000);
                        //#endif
                    }
                    mc.gameSettings.thirdPersonView = 2;
                    mc.gameSettings.hideGUI = true;
                    player.rotationYaw = 0;
                    player.rotationPitch = 10;
                    player.rotationYawHead = 0;
                    player.renderYawOffset = 0;
                    next();
                }
                break;
            case 2:
                if (timer == 10) {
                    Emote e = LegacyEmotes.client().library.findByName(EMOTES[emoteIndex]);
                    LegacyEmotes.LOGGER.info("[selftest] playing " + EMOTES[emoteIndex] + " -> " + e);
                    if (e != null) {
                        LegacyEmotes.client().playLocal(e);
                    }
                }
                for (int i = 0; i < SHOTS.length; i++) {
                    if (timer == 10 + SHOTS[i]) {
                        shot(mc, EMOTES[emoteIndex].toLowerCase() + "_" + i);
                    }
                }
                if (timer > 10 + SHOTS[SHOTS.length - 1] + 2) {
                    LegacyEmotes.client().stopLocal();
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
                    LegacyEmotes.LOGGER.info("[selftest] BLENDEMOTES_SELFTEST_DONE");
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
        ScreenShotHelper.saveScreenshot(Compat.gameDir(), "blendemotes_" + name + ".png", mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        ClientEmotes client = LegacyEmotes.client();
        PlayerPose pose = client.pose(Compat.player().getUniqueID(), new VanillaPose(), false, 0, new PlayerPose());
        if (pose != null) {
            LegacyEmotes.LOGGER.info("[selftest] " + name + " right_arm " + pose.transform(PlayerPart.RIGHT_ARM)
                    + " bend " + Math.toDegrees(pose.bend(PlayerPart.RIGHT_ARM)));
        } else {
            LegacyEmotes.LOGGER.info("[selftest] " + name + " (no emote)");
        }
    }
}
