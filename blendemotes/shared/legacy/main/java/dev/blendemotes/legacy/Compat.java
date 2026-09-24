package dev.blendemotes.legacy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
//#if MC >= 11202
import net.minecraft.client.renderer.BufferBuilder;
//#else
import net.minecraft.client.renderer.WorldRenderer;
//#endif

/** The small API differences between Minecraft 1.8.9 and 1.12.2 (MCP names). */
public final class Compat {
    /**
     * Where the renderer's model space starts: vanilla translates by this after the model flip
     * (the model is 24 px tall, 1.5 blocks, plus a small offset that differs per version).
     */
    //#if MC >= 11202
    public static final float MODEL_ORIGIN_Y = 1.501F;
    //#else
    public static final float MODEL_ORIGIN_Y = 1.5078125F;
    //#endif

    private Compat() {
    }

    public static EntityPlayerSP player() {
        //#if MC >= 11202
        return Minecraft.getMinecraft().player;
        //#else
        return Minecraft.getMinecraft().thePlayer;
        //#endif
    }

    public static World world() {
        //#if MC >= 11202
        return Minecraft.getMinecraft().world;
        //#else
        return Minecraft.getMinecraft().theWorld;
        //#endif
    }

    /** True while connected to a server (singleplayer included). */
    public static boolean connected() {
        //#if MC >= 11202
        return Minecraft.getMinecraft().getConnection() != null;
        //#else
        return Minecraft.getMinecraft().getNetHandler() != null;
        //#endif
    }

    public static FontRenderer font() {
        //#if MC >= 11202
        return Minecraft.getMinecraft().fontRenderer;
        //#else
        return Minecraft.getMinecraft().fontRendererObj;
        //#endif
    }

    /** Server of a player (1.12 removed the static server getter). */
    public static MinecraftServer server(EntityPlayerMP player) {
        //#if MC >= 11202
        return player.getServer();
        //#else
        return MinecraftServer.getServer();
        //#endif
    }

    // ------------------------------------------------------------------ immediate mode drawing

    //#if MC >= 11202
    private static BufferBuilder buffer() {
        return Tessellator.getInstance().getBuffer();
    }
    //#else
    private static WorldRenderer buffer() {
        return Tessellator.getInstance().getWorldRenderer();
    }
    //#endif

    public static void begin(int mode, VertexFormat format) {
        buffer().begin(mode, format);
    }

    public static void vertex(double x, double y, double z, double u, double v, float nx, float ny, float nz) {
        buffer().pos(x, y, z).tex(u, v).normal(nx, ny, nz).endVertex();
    }

    public static void vertex(double x, double y, double z, float r, float g, float b, float a) {
        buffer().pos(x, y, z).color(r, g, b, a).endVertex();
    }

    public static void draw() {
        Tessellator.getInstance().draw();
    }
}
