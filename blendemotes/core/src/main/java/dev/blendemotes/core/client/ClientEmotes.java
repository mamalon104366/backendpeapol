package dev.blendemotes.core.client;

import dev.blendemotes.core.config.EmoteConfig;
import dev.blendemotes.core.emote.Emote;
import dev.blendemotes.core.emote.EmoteLibrary;
import dev.blendemotes.core.emote.EmotePlayer;
import dev.blendemotes.core.net.EmoteClient;
import dev.blendemotes.core.pose.PlayerPose;
import dev.blendemotes.core.pose.VanillaPose;
import dev.blendemotes.core.rig.RigDefinition;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Everything the client does with emotes, independent of the Minecraft version. The platform
 * code calls: {@link #init()}, {@link #tick(LocalInput)} every client tick, {@link #pose} while
 * rendering a player, {@link #onJoin()}/{@link #onLeave()} and {@link #onPacket(byte[])}.
 */
public final class ClientEmotes {
    private final ClientPlatform platform;
    public final EmoteLibrary library = new EmoteLibrary();
    public final EmoteClient network;
    private EmoteConfig config = new EmoteConfig();
    private final Map<UUID, EmotePlayer> players = new HashMap<UUID, EmotePlayer>();
    /** Perspective to restore after the local emote, -1 when we did not change it. */
    private int savedPerspective = -1;
    private boolean localWasPlaying;

    public ClientEmotes(final ClientPlatform platform) {
        this.platform = platform;
        this.network = new EmoteClient(new EmoteClient.Bridge() {
            @Override
            public void send(byte[] packet) {
                platform.sendPacket(packet);
            }

            @Override
            public double now() {
                return platform.clock();
            }

            @Override
            public void startEmote(UUID player, Emote emote, double elapsedSeconds) {
                if (config.showOtherPlayers && !player.equals(platform.localPlayer())) {
                    playerState(player).play(emote, platform.clock(), elapsedSeconds);
                }
            }

            @Override
            public void stopEmote(UUID player) {
                EmotePlayer p = players.get(player);
                if (p != null && !player.equals(platform.localPlayer())) {
                    p.stop(platform.clock());
                }
            }

            @Override
            public void log(String message) {
                platform.log(message);
            }
        }, library);
    }

    public void init() {
        config = EmoteConfig.load(configFile());
        reload();
    }

    public File configFile() {
        return new File(new File(platform.gameDirectory(), "config"), "blendemotes.json");
    }

    public File emotesFolder() {
        File f = new File(config.emotesFolder);
        return f.isAbsolute() ? f : new File(platform.gameDirectory(), config.emotesFolder);
    }

    public EmoteConfig config() {
        return config;
    }

    public void saveConfig() {
        try {
            config.save(configFile());
        } catch (IOException ex) {
            platform.log("Could not save BlendEmotes config: " + ex.getMessage());
        }
    }

    /** Re-reads the emote folder. */
    public void reload() {
        library.reload(platform.builtinEmotes(), emotesFolder());
        for (Map.Entry<String, String> e : library.errors().entrySet()) {
            platform.log("Could not load emote " + e.getKey() + ": " + e.getValue());
        }
        platform.log("Loaded " + library.all().size() + " emotes");
    }

    private EmotePlayer playerState(UUID player) {
        EmotePlayer p = players.get(player);
        if (p == null) {
            p = new EmotePlayer();
            players.put(player, p);
        }
        p.setFades(config.fadeIn, config.fadeOut);
        return p;
    }

    // ------------------------------------------------------------------ local player

    /** Emote assigned to a wheel slot (by id, or by name for hand edited configs). */
    public Emote wheelEmote(int slot) {
        if (slot < 0 || slot >= config.wheel.size()) {
            return null;
        }
        String ref = config.wheel.get(slot);
        if (ref == null || ref.isEmpty()) {
            return null;
        }
        try {
            Emote e = library.get(UUID.fromString(ref));
            if (e != null) {
                return e;
            }
        } catch (IllegalArgumentException ignored) {
            // not a uuid
        }
        return library.findByName(ref);
    }

    public void setWheelEmote(int slot, Emote emote) {
        if (slot >= 0 && slot < config.wheel.size()) {
            config.wheel.set(slot, emote == null ? "" : emote.id.toString());
            saveConfig();
        }
    }

    public boolean isLocalPlaying() {
        UUID me = platform.localPlayer();
        EmotePlayer p = me == null ? null : players.get(me);
        return p != null && p.current() != null && !p.isStopping();
    }

    public void playLocal(Emote emote) {
        UUID me = platform.localPlayer();
        if (me == null || emote == null) {
            return;
        }
        playerState(me).play(emote, platform.clock(), 0);
        if (config.shareEmotes) {
            network.sendPlay(emote, 0);
        }
        if (config.autoThirdPerson && platform.perspective() == 0) {
            savedPerspective = 0;
            platform.setPerspective(1);
        }
        localWasPlaying = true;
    }

    public void stopLocal() {
        UUID me = platform.localPlayer();
        EmotePlayer p = me == null ? null : players.get(me);
        if (p != null && p.current() != null && !p.isStopping()) {
            p.stop(platform.clock());
            if (config.shareEmotes) {
                network.sendStop();
            }
        }
    }

    /** Called every client tick. */
    public void tick(LocalInput input) {
        double now = platform.clock();
        UUID me = platform.localPlayer();
        if (me != null && isLocalPlaying() && input != null) {
            boolean stop = input.blocked
                    || (config.stopOnMove && input.moving)
                    || (config.stopOnJump && input.jumping)
                    || (config.stopOnSneak && input.sneaking)
                    || (config.stopOnAttack && (input.attacking || input.usingItem))
                    || (config.stopOnHurt && input.hurt);
            if (stop) {
                stopLocal();
            }
        }
        Iterator<Map.Entry<UUID, EmotePlayer>> it = players.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, EmotePlayer> e = it.next();
            if (!e.getValue().update(now)) {
                it.remove();
            }
        }
        if (localWasPlaying && (me == null || !players.containsKey(me))) {
            localWasPlaying = false;
            if (savedPerspective >= 0) {
                if (platform.perspective() == 1) {
                    platform.setPerspective(savedPerspective);
                }
                savedPerspective = -1;
            }
        }
    }

    /**
     * Pose of a player for this frame, or null when it is not emoting.
     *
     * @param lifeTime seconds the entity has existed (Molang)
     */
    public PlayerPose pose(UUID player, VanillaPose vanilla, boolean slim, double lifeTime, PlayerPose out) {
        EmotePlayer p = players.get(player);
        if (p == null) {
            return null;
        }
        return p.evaluate(platform.clock(), vanilla, RigDefinition.minecraft(slim), lifeTime, out);
    }

    public boolean isEmoting(UUID player) {
        EmotePlayer p = players.get(player);
        return p != null && p.current() != null;
    }

    // ------------------------------------------------------------------ connection

    public void onJoin() {
        players.clear();
        savedPerspective = -1;
        localWasPlaying = false;
        network.onJoin();
    }

    public void onLeave() {
        players.clear();
        network.reset();
        if (savedPerspective >= 0) {
            savedPerspective = -1;
        }
    }

    public void onPacket(byte[] payload) {
        network.onPacket(payload);
    }

    public List<Emote> emotes() {
        return library.all();
    }
}
