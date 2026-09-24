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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.UUID;

/**
 * Client side of BlendEmotes for Minecraft 1.20.1, shared by Fabric and Forge. Loader code
 * registers the key mappings, forwards client ticks and plugin channel packets here.
 */
public final class ModernEmotes {
    public static final String MOD_ID = "blendemotes";
    public static final Logger LOGGER = LoggerFactory.getLogger("BlendEmotes");

    public interface Sender {
        void send(byte[] payload);
    }

    public static final KeyMapping KEY_WHEEL = new KeyMapping("key.blendemotes.wheel", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.blendemotes");
    public static final KeyMapping KEY_MENU = new KeyMapping("key.blendemotes.menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, "key.categories.blendemotes");
    public static final KeyMapping KEY_STOP = new KeyMapping("key.blendemotes.stop", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "key.categories.blendemotes");

    private static ClientEmotes client;
    private static Sender sender;
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

    public static void initClient(Sender packetSender) {
        sender = packetSender;
        client = new ClientEmotes(new ClientPlatform() {
            @Override
            public void sendPacket(byte[] payload) {
                if (sender != null && Minecraft.getInstance().getConnection() != null) {
                    sender.send(payload);
                }
            }

            @Override
            public double clock() {
                return (ticks + Minecraft.getInstance().getFrameTime()) / 20.0;
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

    /** End of every client tick. */
    public static void tick(Minecraft mc) {
        if (client == null) {
            return;
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
            INPUT.moving = player.input != null && (Math.abs(player.input.forwardImpulse) > 1e-3 || Math.abs(player.input.leftImpulse) > 1e-3);
            INPUT.jumping = player.input != null && player.input.jumping;
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
