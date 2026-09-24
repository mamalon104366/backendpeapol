package dev.blendemotes.modern;

import com.mojang.blaze3d.platform.InputConstants;
import dev.blendemotes.core.client.ClientEmotes;
import dev.blendemotes.core.client.ClientPlatform;
import dev.blendemotes.core.client.LocalInput;
import dev.blendemotes.core.emote.BuiltinEmotes;
import dev.blendemotes.core.emote.EmoteLibrary;
import dev.blendemotes.modern.gui.EmoteMenuScreen;
import dev.blendemotes.modern.gui.EmoteWheelScreen;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.lwjgl.glfw.GLFW;
//#if MC >= 11800
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//#else
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//#endif

import java.io.File;
import java.util.List;
import java.util.UUID;

/**
 * Client side of BlendEmotes for Minecraft 1.16.5 and newer, shared by Fabric, Forge and
 * NeoForge. Loader code registers the key mappings, forwards client ticks and plugin channel
 * packets here.
 */
public final class ModernEmotes {
    public static final String MOD_ID = "blendemotes";
    //#if MC >= 11800
    public static final Logger LOGGER = LoggerFactory.getLogger("BlendEmotes");
    //#else
    public static final Logger LOGGER = LogManager.getLogger("BlendEmotes");
    //#endif

    public interface Sender {
        void send(byte[] payload);
    }

    //#if MC >= 12109
    private static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(Compat.id(MOD_ID, "main"));
    //#else
    private static final String KEY_CATEGORY = "key.categories.blendemotes";
    //#endif
    public static final KeyMapping KEY_WHEEL = new KeyMapping("key.blendemotes.wheel", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, KEY_CATEGORY);
    public static final KeyMapping KEY_MENU = new KeyMapping("key.blendemotes.menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, KEY_CATEGORY);
    public static final KeyMapping KEY_STOP = new KeyMapping("key.blendemotes.stop", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), KEY_CATEGORY);

    private static ClientEmotes client;
    private static Sender sender;
    private static boolean sendFailureLogged;
    private static long ticks;
    private static ClientLevel lastLevel;
    private static final LocalInput INPUT = new LocalInput();

    private ModernEmotes() {
    }

    public static ClientEmotes client() {
        return client;
    }

    public static boolean bendsEnabled() {
        return client == null || client.config().bends;
    }

    /** Loader code: how packets reach the server (called once at startup). */
    public static void setSender(Sender packetSender) {
        sender = packetSender;
    }

    private static void initClient() {
        client = new ClientEmotes(new ClientPlatform() {
            @Override
            public void sendPacket(byte[] payload) {
                if (sender != null && Minecraft.getInstance().getConnection() != null) {
                    try {
                        sender.send(payload);
                    } catch (RuntimeException e) {
                        // a server without the mod: some loaders refuse channels the server did not announce
                        if (!sendFailureLogged) {
                            sendFailureLogged = true;
                            LOGGER.info("Emote packets are not sent to this server: " + e);
                        }
                    }
                }
            }

            @Override
            public double clock() {
                return (ticks + Compat.partialTick(Minecraft.getInstance())) / 20.0;
            }

            @Override
            public UUID localPlayer() {
                LocalPlayer p = Minecraft.getInstance().player;
                return p == null ? null : p.getUUID();
            }

            @Override
            public int perspective() {
                return Minecraft.getInstance().options.getCameraType().ordinal();
            }

            @Override
            public void setPerspective(int perspective) {
                CameraType[] types = CameraType.values();
                Minecraft.getInstance().options.setCameraType(types[Math.max(0, Math.min(types.length - 1, perspective))]);
            }

            @Override
            public File gameDirectory() {
                return Minecraft.getInstance().gameDirectory;
            }

            @Override
            public List<EmoteLibrary.Resource> builtinEmotes() {
                return BuiltinEmotes.list();
            }

            @Override
            public void log(String message) {
                LOGGER.info(message);
            }
        });
        client.init();
    }

    /** End of every client tick (see MinecraftMixin). */
    public static void tick(Minecraft mc) {
        if (client == null) {
            initClient();
        }
        ticks++;
        ClientLevel level = mc.level;
        if (level != null && lastLevel == null) {
            client.onJoin();
        } else if (level == null && lastLevel != null) {
            client.onLeave();
        }
        lastLevel = level;

        LocalPlayer player = mc.player;
        INPUT.clear();
        if (player != null) {
            INPUT.moving = Compat.moving(player);
            INPUT.jumping = Compat.jumping(player);
            INPUT.sneaking = player.isShiftKeyDown();
            INPUT.attacking = mc.options.keyAttack.isDown();
            INPUT.usingItem = mc.options.keyUse.isDown();
            INPUT.hurt = player.hurtTime > 0;
            INPUT.blocked = player.isPassenger() || player.isSleeping() || player.isDeadOrDying() || player.isFallFlying();
        }
        client.tick(player != null && mc.screen == null ? INPUT : null);

        while (KEY_WHEEL.consumeClick()) {
            if (mc.screen == null && player != null) {
                mc.setScreen(new EmoteWheelScreen(KEY_WHEEL));
            }
        }
        while (KEY_MENU.consumeClick()) {
            if (mc.screen == null && player != null) {
                mc.setScreen(new EmoteMenuScreen(null));
            }
        }
        while (KEY_STOP.consumeClick()) {
            client.stopLocal();
        }
        SelfTest.tick(mc);
    }

    public static void onPacket(byte[] payload) {
        if (client != null) {
            client.onPacket(payload);
        }
    }
}
