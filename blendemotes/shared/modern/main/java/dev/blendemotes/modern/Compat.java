package dev.blendemotes.modern;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec2;
//#if MC >= 12109
import net.minecraft.world.entity.player.PlayerModelType;
//#elseif MC >= 12002
import net.minecraft.client.resources.PlayerSkin;
//#endif
//#if MC < 11900
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
//#endif

/** Small API differences between the Minecraft versions built from shared/modern. */
public final class Compat {
    private Compat() {
    }

    public static ResourceLocation id(String namespace, String path) {
        //#if MC >= 12100
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
        //#else
        return new ResourceLocation(namespace, path);
        //#endif
    }

    public static Component literal(String text) {
        //#if MC >= 11900
        return Component.literal(text);
        //#else
        return new TextComponent(text);
        //#endif
    }

    public static Component translatable(String key, Object... args) {
        //#if MC >= 11900
        return Component.translatable(key, args);
        //#else
        return new TranslatableComponent(key, args);
        //#endif
    }

    /** Fraction of the current tick that has passed (for smooth animation). */
    public static float partialTick(Minecraft mc) {
        //#if MC >= 12102
        return mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        //#elseif MC >= 12100
        return mc.getTimer().getGameTimeDeltaPartialTick(true);
        //#else
        return mc.getFrameTime();
        //#endif
    }

    public static boolean moving(LocalPlayer player) {
        if (player.input == null) {
            return false;
        }
        Vec2 move = player.input.getMoveVector();
        return Math.abs(move.x) > 1e-3 || Math.abs(move.y) > 1e-3;
    }

    public static boolean jumping(LocalPlayer player) {
        if (player.input == null) {
            return false;
        }
        //#if MC >= 12102
        return player.input.keyPresses.jump();
        //#else
        return player.input.jumping;
        //#endif
    }

    /** True for players with the slim (3 px) arms skin model. */
    public static boolean slim(AbstractClientPlayer player) {
        //#if MC >= 12109
        return player.getSkin().model() == PlayerModelType.SLIM;
        //#elseif MC >= 12002
        return player.getSkin().model() == PlayerSkin.Model.SLIM;
        //#else
        return "slim".equals(player.getModelName());
        //#endif
    }
}
