package com.flintfix.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
//? if >=1.21.2 {
/*import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShapeRenderer;
*///?} else {
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
//?}
//? if >=1.21 {
import com.mojang.blaze3d.vertex.MeshData;
//?}

/**
 * The few Minecraft calls whose shape changed between versions, behind one
 * stable API (Mojang names). Everything else in FlintFix calls these instead of
 * the version-specific methods, so a new Minecraft version mostly touches this file.
 */
public final class FlintFixCompat {
    private FlintFixCompat() {}

    public static ResourceLocation id(String namespace, String path) {
        //? if >=1.21 {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
        //?} else {
        /*return new ResourceLocation(namespace, path);
        *///?}
    }

    // ------------------------------------------------------------------
    // Game state
    // ------------------------------------------------------------------

    public static String effectName(MobEffectInstance effect) {
        //? if >=1.20.5 {
        return effect.getEffect().value().getDisplayName().getString();
        //?} else {
        /*return effect.getEffect().getDisplayName().getString();
        *///?}
    }

    public static boolean debugHudVisible(Minecraft client) {
        //? if >=1.20.2 {
        return client.getDebugOverlay().showDebugScreen();
        //?} else {
        /*return client.options.renderDebug;
        *///?}
    }

    /** True in dimensions with the normal overworld sky (the custom sky only replaces that one). */
    public static boolean hasOverworldSky(ClientLevel level) {
        //? if >=1.21.2 {
        /*return level.effects().skyType() == DimensionSpecialEffects.SkyType.OVERWORLD;
        *///?} else {
        return level.effects().skyType() == DimensionSpecialEffects.SkyType.NORMAL;
        //?}
    }

    /** Lowest buildable Y of the world. */
    public static int minBuildY(Level level) {
        //? if >=1.21.2 {
        /*return level.getMinY();
        *///?} else {
        return level.getMinBuildHeight();
        //?}
    }

    /** Elytra flight. */
    public static boolean isGliding(LivingEntity entity) {
        return entity.isFallFlying();
    }

    // ------------------------------------------------------------------
    // GUI textures
    // ------------------------------------------------------------------

    /** Blits a region of a texture scaled to w x h, tinted by an ARGB color. */
    public static void drawTexture(GuiGraphics context, ResourceLocation texture, int x, int y, int w, int h,
                                   float u, float v, int regionW, int regionH, int textureW, int textureH, int color) {
        //? if >=1.21.2 {
        /*context.blit(RenderType::guiTextured, texture, x, y, u, v, w, h, regionW, regionH, textureW, textureH, color);
        *///?} else {
        float a = ((color >>> 24) & 0xFF) / 255.0f;
        float r = ((color >>> 16) & 0xFF) / 255.0f;
        float g = ((color >>> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(r, g, b, a);
        try {
            context.blit(texture, x, y, w, h, u, v, regionW, regionH, textureW, textureH);
        } finally {
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }
        //?}
    }

    /** Draws a whole texture with additive blending (light glows), tinted by an ARGB color. */
    public static void drawAdditiveTexture(GuiGraphics context, ResourceLocation texture, int x, int y, int w, int h,
                                           int color) {
        float a = ((color >>> 24) & 0xFF) / 255.0f;
        float r = ((color >>> 16) & 0xFF) / 255.0f;
        float g = ((color >>> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        //? if >=1.21.2 {
        /*// GUI draws are batched from 1.21.2, and each batch sets its own blending, so flush them
        // and draw this quad immediately with additive blending.
        context.flush();
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShader(CoreShaders.POSITION_TEX_COLOR);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        Matrix4f m = context.pose().last().pose();
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
            DefaultVertexFormat.POSITION_TEX_COLOR);
        buffer.addVertex(m, x, y, 0.0f).setUv(0.0f, 0.0f).setColor(r, g, b, a);
        buffer.addVertex(m, x, y + h, 0.0f).setUv(0.0f, 1.0f).setColor(r, g, b, a);
        buffer.addVertex(m, x + w, y + h, 0.0f).setUv(1.0f, 1.0f).setColor(r, g, b, a);
        buffer.addVertex(m, x + w, y, 0.0f).setUv(1.0f, 0.0f).setColor(r, g, b, a);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        *///?} else {
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.setShaderColor(r, g, b, a);
        try {
            context.blit(texture, x, y, w, h, 0, 0, 1, 1, 1, 1);
        } finally {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.defaultBlendFunc();
        }
        //?}
    }

    public static void drawSprite(GuiGraphics context, int x, int y, int w, int h, TextureAtlasSprite sprite) {
        //? if >=1.21.2 {
        /*context.blitSprite(RenderType::guiTextured, sprite, x, y, w, h);
        *///?} else {
        context.blit(x, y, 0, w, h, sprite);
        //?}
    }

    // ------------------------------------------------------------------
    // World geometry
    // ------------------------------------------------------------------

    /** Partial tick for world rendering callbacks. */
    public static float tickDelta(WorldRenderContext context) {
        //? if >=1.21 {
        return context.tickCounter().getGameTimeDeltaPartialTick(true);
        //?} else {
        /*return context.tickDelta();
        *///?}
    }

    /** One position + color vertex (debug quads, sky). */
    public static void colorVertex(VertexConsumer consumer, Matrix4f matrix, float x, float y, float z,
                                   int r, int g, int b, int a) {
        //? if >=1.21 {
        consumer.addVertex(matrix, x, y, z).setColor(r, g, b, a);
        //?} else {
        /*consumer.vertex(matrix, x, y, z).color(r, g, b, a).endVertex();
        *///?}
    }

    public static void colorVertex(VertexConsumer consumer, Matrix4f matrix, float x, float y, float z, int argb) {
        colorVertex(consumer, matrix, x, y, z, (argb >>> 16) & 0xFF, (argb >>> 8) & 0xFF, argb & 0xFF, argb >>> 24);
    }

    /** One vertex for RenderType.lines(): position, color and the line direction as normal. */
    public static void lineVertex(VertexConsumer consumer, PoseStack.Pose entry, float x, float y, float z,
                                  int r, int g, int b, int a, float nx, float ny, float nz) {
        //? if >=1.21 {
        consumer.addVertex(entry.pose(), x, y, z).setColor(r, g, b, a).setNormal(entry, nx, ny, nz);
        //?} else if >=1.20.5 {
        /*consumer.vertex(entry.pose(), x, y, z).color(r, g, b, a).normal(entry, nx, ny, nz).endVertex();
        *///?} else {
        /*consumer.vertex(entry.pose(), x, y, z).color(r, g, b, a).normal(entry.normal(), nx, ny, nz).endVertex();
        *///?}
    }

    /** Box wireframe for RenderType.lines(). */
    public static void drawBoxOutline(PoseStack matrices, VertexConsumer lines, AABB box,
                                      float r, float g, float b, float a) {
        //? if >=1.21.2 {
        /*ShapeRenderer.renderLineBox(matrices, lines, box, r, g, b, a);
        *///?} else {
        LevelRenderer.renderLineBox(matrices, lines, box, r, g, b, a);
        //?}
    }

    // ------------------------------------------------------------------
    // Immediate-mode position/color buffers (custom sky)
    // ------------------------------------------------------------------

    public static BufferBuilder beginPositionColor(VertexFormat.Mode mode) {
        //? if >=1.21 {
        return Tesselator.getInstance().begin(mode, DefaultVertexFormat.POSITION_COLOR);
        //?} else {
        /*BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(mode, DefaultVertexFormat.POSITION_COLOR);
        return buffer;
        *///?}
    }

    /** Draws and releases a buffer from {@link #beginPositionColor}; empty buffers are skipped. */
    public static void drawBuffer(BufferBuilder buffer) {
        //? if >=1.21 {
        MeshData built = buffer.build();
        //?} else {
        /*BufferBuilder.RenderedBuffer built = buffer.endOrDiscardIfEmpty();
        *///?}
        if (built != null) BufferUploader.drawWithShader(built);
    }

    public static void usePositionColorShader() {
        //? if >=1.21.2 {
        /*RenderSystem.setShader(CoreShaders.POSITION_COLOR);
        *///?} else {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        //?}
    }
}
