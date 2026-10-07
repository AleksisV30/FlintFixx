package com.flintfix.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
//? if >=1.21.5 {
/*import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.util.TriState;
*///?} else {
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.BufferUploader;
//?}
//? if >=1.21.2 {
/*import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShapeRenderer;
*///?} else {
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
//?}
//? if >=1.21.2 <1.21.5 {
/*import net.minecraft.client.renderer.CoreShaders;
*///?}
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

    /** Selected hotbar slot (0-8). */
    public static int selectedSlot(Inventory inventory) {
        //? if >=1.21.5 {
        /*return inventory.getSelectedSlot();
        *///?} else {
        return inventory.selected;
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
        //? if >=1.21.5 {
        /*context.blit(Pipelines.GUI_GLOW, texture, x, y, 0, 0, w, h, 1, 1, 1, 1, color);
        *///?} else if >=1.21.2 {
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
    // Custom sky: position/color triangles drawn right where the vanilla sky
    // would be, with normal or additive ("glow") blending.
    // ------------------------------------------------------------------

    //? if >=1.21.5 {
    /*private static boolean skyGlow;

    /^* Render pipelines exist from 1.21.5; created on first use, not at class load. ^/
    private static final class Pipelines {
        static final RenderType SKY = skyType("sky", BlendFunction.TRANSLUCENT);
        static final RenderType SKY_GLOW = skyType("sky_glow", BlendFunction.LIGHTNING);
        static final RenderPipeline GUI_GLOW_PIPELINE = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(id("flintfix", "pipeline/gui_glow"))
            .withBlend(BlendFunction.LIGHTNING)
            .build();
        static final Function<ResourceLocation, RenderType> GUI_GLOW = Util.memoize(texture -> RenderType.create(
            "flintfix_gui_glow", 786432, GUI_GLOW_PIPELINE,
            RenderType.CompositeState.builder()
                .setTextureState(new RenderStateShard.TextureStateShard(texture, TriState.TRUE, false))
                .createCompositeState(false)));

        private static RenderType skyType(String name, BlendFunction blend) {
            RenderPipeline pipeline = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                .withLocation(id("flintfix", "pipeline/" + name))
                .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES)
                .withBlend(blend)
                .withCull(false)
                .withDepthWrite(false)
                .build();
            return RenderType.create("flintfix_" + name, 1 << 20, false, false, pipeline,
                RenderType.CompositeState.builder().createCompositeState(false));
        }
    }
    *///?}

    /** Sets up drawing state for the sky: no depth writes, no culling, normal blending. */
    public static void beginSky() {
        //? if >=1.21.5 {
        /*skyGlow = false;
        *///?} else {
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        //? if >=1.21.2 {
        /*RenderSystem.setShader(CoreShaders.POSITION_COLOR);
        *///?} else {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        //?}
        //?}
    }

    /** Switches between normal blending and additive glow for the following sky draws. */
    public static void skyBlend(boolean glow) {
        //? if >=1.21.5 {
        /*skyGlow = glow;
        *///?} else {
        if (glow) RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        else RenderSystem.defaultBlendFunc();
        //?}
    }

    public static void endSky() {
        //? if <1.21.5 {
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        //?}
    }

    public static BufferBuilder beginPositionColor(VertexFormat.Mode mode) {
        //? if >=1.21 {
        return Tesselator.getInstance().begin(mode, DefaultVertexFormat.POSITION_COLOR);
        //?} else {
        /*BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(mode, DefaultVertexFormat.POSITION_COLOR);
        return buffer;
        *///?}
    }

    /** Draws and releases a sky buffer from {@link #beginPositionColor}; empty buffers are skipped. */
    public static void drawBuffer(BufferBuilder buffer) {
        //? if >=1.21.5 {
        /*MeshData built = buffer.build();
        if (built != null) (skyGlow ? Pipelines.SKY_GLOW : Pipelines.SKY).draw(built);
        *///?} else if >=1.21 {
        MeshData built = buffer.build();
        if (built != null) BufferUploader.drawWithShader(built);
        //?} else {
        /*BufferBuilder.RenderedBuffer built = buffer.endOrDiscardIfEmpty();
        if (built != null) BufferUploader.drawWithShader(built);
        *///?}
    }
}
