package dev.blendemotes.legacy;

import dev.blendemotes.core.client.ClientEmotes;
import dev.blendemotes.core.client.ClientPlatform;
import dev.blendemotes.core.client.LocalInput;
import dev.blendemotes.core.emote.BuiltinEmotes;
import dev.blendemotes.core.emote.EmoteLibrary;
import dev.blendemotes.legacy.gui.EmoteMenuScreen;
import dev.blendemotes.legacy.gui.EmoteWheelScreen;
import dev.blendemotes.legacy.render.EmoteRenderPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.world.World;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Client side of BlendEmotes for Minecraft 1.8.9 and 1.12.2, shared by Forge and Legacy Fabric. The
 * loader specific classes only forward ticks and network packets here.
 */
public final class LegacyEmotes {
    public static final String MOD_ID = "blendemotes";
    public static final Logger LOGGER = LogManager.getLogger("BlendEmotes");

    /** Sends a payload to the server on the BlendEmotes channel (set by the loader code). */
    public interface Sender {
        void send(byte[] payload);
    }

    private static ClientEmotes client;
    private static Sender sender;
    private static long ticks;
    private static float partialTicks;
    private static boolean renderersInstalled;
    private static World lastWorld;
    private static final LocalInput INPUT = new LocalInput();

    public static KeyBinding keyWheel;
    public static KeyBinding keyMenu;
    public static KeyBinding keyStop;

    private LegacyEmotes() {
    }

    public static ClientEmotes client() {
        return client;
    }

    public static void setPartialTicks(float pt) {
        partialTicks = pt;
    }

    /** Called once when the client starts. */
    public static void initClient(Sender packetSender) {
        sender = packetSender;
        client = new ClientEmotes(new ClientPlatform() {
            @Override
            public void sendPacket(byte[] payload) {
                if (sender != null && Compat.connected()) {
                    sender.send(payload);
                }
            }

            @Override
            public double clock() {
                return (ticks + partialTicks) / 20.0;
            }

            @Override
            public UUID localPlayer() {
                EntityPlayerSP p = Compat.player();
                return p == null ? null : p.getUniqueID();
            }

            @Override
            public int perspective() {
                return Minecraft.getMinecraft().gameSettings.thirdPersonView;
            }

            @Override
            public void setPerspective(int perspective) {
                Minecraft.getMinecraft().gameSettings.thirdPersonView = perspective;
            }

            @Override
            public File gameDirectory() {
                return Compat.gameDir();
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
        registerKeys();
    }

    private static void registerKeys() {
        String category = "key.categories.blendemotes";
        keyWheel = new KeyBinding("key.blendemotes.wheel", Keyboard.KEY_B, category);
        keyMenu = new KeyBinding("key.blendemotes.menu", Keyboard.KEY_N, category);
        keyStop = new KeyBinding("key.blendemotes.stop", Keyboard.KEY_NONE, category);
        Minecraft mc = Minecraft.getMinecraft();
        mc.gameSettings.keyBindings = ArrayUtils.addAll(mc.gameSettings.keyBindings, keyWheel, keyMenu, keyStop);
        // pick up the saved key codes of the new bindings
        mc.gameSettings.loadOptions();
        KeyBinding.resetKeyBindingArrayAndHash();
    }

    /** Replaces the player renderers; needs the RenderManager, so it runs on the first tick. */
    @SuppressWarnings("unchecked")
    private static void installRenderers(Minecraft mc) {
        RenderManager rm = mc.getRenderManager();
        if (rm == null) {
            return;
        }
        renderersInstalled = true;
        // there is no getter for the skin map: find the private fields by type (their names
        // differ between Forge and Fabric at runtime)
        Map<String, RenderPlayer> skins = null;
        Field defaultField = null;
        for (Field f : RenderManager.class.getDeclaredFields()) {
            try {
                if (Map.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    Object value = f.get(rm);
                    if (value instanceof Map && ((Map<?, ?>) value).get("default") instanceof RenderPlayer) {
                        skins = (Map<String, RenderPlayer>) value;
                    }
                } else if (f.getType() == RenderPlayer.class) {
                    f.setAccessible(true);
                    defaultField = f;
                }
            } catch (Exception ex) {
                LOGGER.warn("Could not inspect RenderManager field " + f.getName(), ex);
            }
        }
        if (skins == null) {
            LOGGER.error("BlendEmotes could not find the player renderers; emotes will not be visible");
            return;
        }
        RenderPlayer wide = skins.get("default");
        RenderPlayer slim = skins.get("slim");
        EmoteRenderPlayer newWide = wide instanceof EmoteRenderPlayer ? (EmoteRenderPlayer) wide : new EmoteRenderPlayer(rm, false, wide);
        EmoteRenderPlayer newSlim = slim instanceof EmoteRenderPlayer ? (EmoteRenderPlayer) slim : new EmoteRenderPlayer(rm, true, slim);
        skins.put("default", newWide);
        skins.put("slim", newSlim);
        if (defaultField != null) {
            try {
                defaultField.set(rm, newWide);
            } catch (Exception ex) {
                LOGGER.warn("Could not replace the default player renderer", ex);
            }
        }
        LOGGER.info("BlendEmotes player renderers installed");
    }

    /** End of every client tick. */
    public static void tick() {
        Minecraft mc = Minecraft.getMinecraft();
        if (client == null) {
            return;
        }
        ticks++;
        if (!renderersInstalled) {
            installRenderers(mc);
        }
        World world = Compat.world();
        if (world != null && lastWorld == null) {
            client.onJoin();
        } else if (world == null && lastWorld != null) {
            client.onLeave();
        }
        lastWorld = world;

        EntityPlayerSP player = Compat.player();
        INPUT.clear();
        if (player != null) {
            INPUT.moving = player.movementInput != null
                    && (Math.abs(player.movementInput.moveForward) > 1e-3 || Math.abs(player.movementInput.moveStrafe) > 1e-3);
            INPUT.jumping = player.movementInput != null && player.movementInput.jump;
            INPUT.sneaking = player.isSneaking();
            INPUT.attacking = mc.gameSettings.keyBindAttack.isKeyDown();
            INPUT.usingItem = mc.gameSettings.keyBindUseItem.isKeyDown();
            INPUT.hurt = player.hurtTime > 0;
            INPUT.blocked = player.isRiding() || player.isPlayerSleeping() || player.isDead;
        }
        client.tick(player != null && mc.currentScreen == null ? INPUT : null);

        if (mc.currentScreen == null && player != null) {
            if (keyWheel.isPressed()) {
                mc.displayGuiScreen(new EmoteWheelScreen(keyWheel.getKeyCode()));
            }
            if (keyMenu.isPressed()) {
                mc.displayGuiScreen(new EmoteMenuScreen(null));
            }
            if (keyStop.isPressed()) {
                client.stopLocal();
            }
        }
        SelfTest.tick(mc);
    }

    /** A packet arrived on the BlendEmotes channel (main thread). */
    public static void onPacket(byte[] payload) {
        if (client != null) {
            client.onPacket(payload);
        }
    }
}
