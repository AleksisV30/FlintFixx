package com.flintfix.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.World;
import org.joml.Matrix4f;

//? if >=1.21.2 {
/*import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexRendering;
*///?} else {
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.WorldRenderer;
//?}
//? if >=1.21 {
import net.minecraft.client.render.BuiltBuffer;
//?}

/**
 * The few Minecraft calls whose shape changed between 1.20.1 and 1.21.4, behind
 * one stable API. Everything else in FlintFix calls these instead of the
 * version-specific methods, so a new Minecraft version only touches this file.
 */
public final class FlintFixCompat {
    private FlintFixCompat() {}

    public static Identifier id(String namespace, String path) {
        //? if >=1.21 {
        return Identifier.of(namespace, path);
        //?} else {
        /*return new Identifier(namespace, path);
        *///?}
    }

    // ------------------------------------------------------------------
    // Game state
    // ------------------------------------------------------------------

    public static String effectName(StatusEffectInstance effect) {
        //? if >=1.20.5 {
        return effect.getEffectType().value().getName().getString();
        //?} else {
        /*return effect.getEffectType().getName().getString();
        *///?}
    }

    public static RegistryKey<World> dimension(GlobalPos pos) {
        //? if >=1.20.5 {
        return pos.dimension();
        //?} else {
        /*return pos.getDimension();
        *///?}
    }

    public static BlockPos blockPos(GlobalPos pos) {
        //? if >=1.20.5 {
        return pos.pos();
        //?} else {
        /*return pos.getPos();
        *///?}
    }

    public static boolean debugHudVisible(MinecraftClient client) {
        //? if >=1.20.2 {
        return client.getDebugHud().shouldShowDebugHud();
        //?} else {
        /*return client.options.debugEnabled;
        *///?}
    }

    /** Elytra flight. */
    public static boolean isGliding(LivingEntity entity) {
        //? if >=1.21.2 {
        /*return entity.isGliding();
        *///?} else {
        return entity.isFallFlying();
        //?}
    }

    // ------------------------------------------------------------------
    // GUI textures
    // ------------------------------------------------------------------

    /** Blits a region of a texture scaled to w x h, tinted by an ARGB color. */
    public static void drawTexture(DrawContext context, Identifier texture, int x, int y, int w, int h,
                                   float u, float v, int regionW, int regionH, int textureW, int textureH, int color) {
        //? if >=1.21.2 {
        /*context.drawTexture(RenderLayer::getGuiTextured, texture, x, y, u, v, w, h, regionW, regionH,
            textureW, textureH, color);
        *///?} else {
        float a = ((color >>> 24) & 0xFF) / 255.0f;
        float r = ((color >>> 16) & 0xFF) / 255.0f;
        float g = ((color >>> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(r, g, b, a);
        try {
            context.drawTexture(texture, x, y, w, h, u, v, regionW, regionH, textureW, textureH);
        } finally {
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }
        //?}
    }

    /** Draws a whole texture with additive blending (light glows), tinted by an ARGB color. */
    public static void drawAdditiveTexture(DrawContext context, Identifier texture, int x, int y, int w, int h,
                                           int color) {
        float a = ((color >>> 24) & 0xFF) / 255.0f;
        float r = ((color >>> 16) & 0xFF) / 255.0f;
        float g = ((color >>> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        //? if >=1.21.2 {
        /*// GUI draws are batched from 1.21.2, and each batch sets its own blending, so flush them
        // and draw this quad immediately with additive blending.
        context.draw();
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        Matrix4f m = context.getMatrices().peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS,
            VertexFormats.POSITION_TEXTURE_COLOR);
        buffer.vertex(m, x, y, 0.0f).texture(0.0f, 0.0f).color(r, g, b, a);
        buffer.vertex(m, x, y + h, 0.0f).texture(0.0f, 1.0f).color(r, g, b, a);
        buffer.vertex(m, x + w, y + h, 0.0f).texture(1.0f, 1.0f).color(r, g, b, a);
        buffer.vertex(m, x + w, y, 0.0f).texture(1.0f, 0.0f).color(r, g, b, a);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        *///?} else {
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.setShaderColor(r, g, b, a);
        try {
            context.drawTexture(texture, x, y, w, h, 0, 0, 1, 1, 1, 1);
        } finally {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.defaultBlendFunc();
        }
        //?}
    }

    public static void drawSprite(DrawContext context, int x, int y, int w, int h, Sprite sprite) {
        //? if >=1.21.2 {
        /*context.drawSpriteStretched(RenderLayer::getGuiTextured, sprite, x, y, w, h);
        *///?} else {
        context.drawSprite(x, y, 0, w, h, sprite);
        //?}
    }

    // ------------------------------------------------------------------
    // World geometry
    // ------------------------------------------------------------------

    /** Partial tick for world rendering callbacks. */
    public static float tickDelta(WorldRenderContext context) {
        //? if >=1.21 {
        return context.tickCounter().getTickDelta(true);
        //?} else {
        /*return context.tickDelta();
        *///?}
    }

    /** One position + color vertex (debug quads, sky). */
    public static void colorVertex(VertexConsumer consumer, Matrix4f matrix, float x, float y, float z,
                                   int r, int g, int b, int a) {
        //? if >=1.21 {
        consumer.vertex(matrix, x, y, z).color(r, g, b, a);
        //?} else {
        /*consumer.vertex(matrix, x, y, z).color(r, g, b, a).next();
        *///?}
    }

    public static void colorVertex(VertexConsumer consumer, Matrix4f matrix, float x, float y, float z, int argb) {
        colorVertex(consumer, matrix, x, y, z, (argb >>> 16) & 0xFF, (argb >>> 8) & 0xFF, argb & 0xFF, argb >>> 24);
    }

    /** One vertex for RenderLayer.getLines(): position, color and the line direction as normal. */
    public static void lineVertex(VertexConsumer consumer, MatrixStack.Entry entry, float x, float y, float z,
                                  int r, int g, int b, int a, float nx, float ny, float nz) {
        //? if >=1.21 {
        consumer.vertex(entry.getPositionMatrix(), x, y, z).color(r, g, b, a).normal(entry, nx, ny, nz);
        //?} else if >=1.20.5 {
        /*consumer.vertex(entry.getPositionMatrix(), x, y, z).color(r, g, b, a).normal(entry, nx, ny, nz).next();
        *///?} else {
        /*consumer.vertex(entry.getPositionMatrix(), x, y, z).color(r, g, b, a)
            .normal(entry.getNormalMatrix(), nx, ny, nz).next();
        *///?}
    }

    /** Box wireframe for RenderLayer.getLines(). */
    public static void drawBoxOutline(MatrixStack matrices, VertexConsumer lines, Box box,
                                      float r, float g, float b, float a) {
        //? if >=1.21.2 {
        /*VertexRendering.drawBox(matrices, lines, box, r, g, b, a);
        *///?} else {
        WorldRenderer.drawBox(matrices, lines, box, r, g, b, a);
        //?}
    }

    // ------------------------------------------------------------------
    // Immediate-mode position/color buffers (custom sky)
    // ------------------------------------------------------------------

    public static BufferBuilder beginPositionColor(VertexFormat.DrawMode mode) {
        //? if >=1.21 {
        return Tessellator.getInstance().begin(mode, VertexFormats.POSITION_COLOR);
        //?} else {
        /*BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(mode, VertexFormats.POSITION_COLOR);
        return buffer;
        *///?}
    }

    /** Draws and releases a buffer from {@link #beginPositionColor}; empty buffers are skipped. */
    public static void drawBuffer(BufferBuilder buffer) {
        //? if >=1.21 {
        BuiltBuffer built = buffer.endNullable();
        //?} else {
        /*BufferBuilder.BuiltBuffer built = buffer.endNullable();
        *///?}
        if (built != null) BufferRenderer.drawWithGlobalProgram(built);
    }

    public static void usePositionColorShader() {
        //? if >=1.21.2 {
        /*RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        *///?} else {
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        //?}
    }
}
