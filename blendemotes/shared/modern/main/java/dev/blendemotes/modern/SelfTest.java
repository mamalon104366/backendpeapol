package dev.blendemotes.modern;

import dev.blendemotes.core.emote.Emote;
import dev.blendemotes.core.pose.PlayerPose;
import dev.blendemotes.core.pose.VanillaPose;
import dev.blendemotes.core.rig.PlayerPart;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
//#if MC >= 11903
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
//#elseif MC >= 11900
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
//#else
import dev.blendemotes.modern.mixin.WorldPresetAccessor;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.levelgen.WorldGenSettings;
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
            ModernEmotes.LOGGER.info("[selftest] progress: state " + state + ", timer " + timer + ", screen " + mc.screen);
        }
        if (total > 20 * 60 * 10 && state < 4) {
            ModernEmotes.LOGGER.error("[selftest] BLENDEMOTES_SELFTEST_TIMEOUT in state " + state);
            state = 4;
            mc.stop();
            return;
        }
        switch (state) {
            case 0:
                if (timer > 100 && mc.level == null) {
                    ModernEmotes.LOGGER.info("[selftest] creating world");
                    createFlatWorld(mc, "blendemotes_selftest");
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
                    //#if MC >= 11700
                    mc.player.setYRot(0);
                    mc.player.setXRot(10);
                    //#else
                    mc.player.yRot = 0;
                    mc.player.xRot = 10;
                    //#endif
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

    /** A superflat creative world, with the API of each version. */
    private static void createFlatWorld(Minecraft mc, String name) {
        //#if MC >= 12102
        LevelSettings settings = new LevelSettings(name, GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                new GameRules(WorldDataConfiguration.DEFAULT.enabledFeatures()), WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel(name, settings, new WorldOptions(1L, false, false),
                registries -> registries.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value()
                        .createWorldDimensions(), mc.screen);
        //#elseif MC >= 12002
        LevelSettings settings = new LevelSettings(name, GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                new GameRules(), WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel(name, settings, new WorldOptions(1L, false, false),
                registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value()
                        .createWorldDimensions(), mc.screen);
        //#elseif MC >= 11903
        LevelSettings settings = new LevelSettings(name, GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                new GameRules(), WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel(name, settings, new WorldOptions(1L, false, false),
                registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value()
                        .createWorldDimensions());
        //#elseif MC >= 11900
        LevelSettings settings = new LevelSettings(name, GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                new GameRules(), DataPackConfig.DEFAULT);
        RegistryAccess.Frozen registries = RegistryAccess.BUILTIN.get();
        WorldGenSettings gen = registries.registryOrThrow(Registry.WORLD_PRESET_REGISTRY).getHolderOrThrow(WorldPresets.FLAT)
                .value().createWorldGenSettings(1L, false, false);
        mc.createWorldOpenFlows().createFreshLevel(name, settings, registries, gen);
        //#elseif MC >= 11800
        LevelSettings settings = new LevelSettings(name, GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                new GameRules(), DataPackConfig.DEFAULT);
        RegistryAccess.Frozen registries = RegistryAccess.BUILTIN.get();
        WorldGenSettings gen = WorldPresetAccessor.blendemotes$flat().create(registries, 1L, false, false);
        mc.createLevel(name, settings, registries, gen);
        //#else
        LevelSettings settings = new LevelSettings(name, GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                new GameRules(), DataPackConfig.DEFAULT);
        RegistryAccess.RegistryHolder registries = RegistryAccess.builtin();
        WorldGenSettings gen = WorldPresetAccessor.blendemotes$flat().create(registries, 1L, false, false);
        mc.createLevel(name, settings, registries, gen);
        //#endif
    }

    private static void shot(Minecraft mc, String name) {
        String file = "blendemotes_" + name + ".png";
        //#if MC >= 12106
        Screenshot.grab(mc.gameDirectory, file, mc.getMainRenderTarget(), 1, msg -> { });
        //#elseif MC >= 11700
        Screenshot.grab(mc.gameDirectory, file, mc.getMainRenderTarget(), msg -> { });
        //#else
        Screenshot.grab(mc.gameDirectory, file, mc.getWindow().getWidth(), mc.getWindow().getHeight(),
                mc.getMainRenderTarget(), msg -> { });
        //#endif
        PlayerPose pose = ModernEmotes.client().pose(mc.player.getUUID(), new VanillaPose(), false, 0, new PlayerPose());
        if (pose != null) {
            ModernEmotes.LOGGER.info("[selftest] " + name + " right_arm " + pose.transform(PlayerPart.RIGHT_ARM)
                    + " bend " + Math.toDegrees(pose.bend(PlayerPart.RIGHT_ARM)));
        } else {
            ModernEmotes.LOGGER.info("[selftest] " + name + " (no emote)");
        }
    }
}
