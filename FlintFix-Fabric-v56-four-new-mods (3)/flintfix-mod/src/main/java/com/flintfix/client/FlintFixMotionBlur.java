package com.flintfix.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Motion blur by frame accumulation: a copy of the previous (already blurred)
 * frame is blended over each newly drawn world frame, then the result is kept
 * for the next frame. It runs right after the world is drawn, so the hand and
 * HUD stay sharp. Plain framebuffer operations, no shader files involved.
 */
public final class FlintFixMotionBlur {
    private static final Logger LOGGER = LoggerFactory.getLogger("FlintFix");
    private static Framebuffer history;
    private static boolean primed;
    private static boolean failed;

    private FlintFixMotionBlur() {}

    public static void render() {
        MinecraftClient client = MinecraftClient.getInstance();
        FlintFixConfig config = FlintFixClient.CONFIG;
        if (config == null || !config.motionBlurEnabled || failed || client.world == null) {
            release();
            return;
        }
        Framebuffer main = client.getFramebuffer();
        int width = main.textureWidth;
        int height = main.textureHeight;
        try {
            if (history == null || history.textureWidth != width || history.textureHeight != height) {
                release();
                history = new SimpleFramebuffer(width, height, false, MinecraftClient.IS_SYSTEM_MAC);
                primed = false;
            }
            if (primed) {
                // Draw the previous frame over this one at the chosen strength.
                main.beginWrite(false);
                RenderSystem.enableBlend();
                GL14.glBlendColor(0.0f, 0.0f, 0.0f, config.motionBlurStrength);
                GlStateManager._blendFuncSeparate(GL14.GL_CONSTANT_ALPHA, GL14.GL_ONE_MINUS_CONSTANT_ALPHA,
                    GL11.GL_ZERO, GL11.GL_ONE);
                history.draw(width, height, false);
                RenderSystem.defaultBlendFunc();
                RenderSystem.disableBlend();
            }
            // Keep the blended result for the next frame.
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.fbo);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, history.fbo);
            GlStateManager._glBlitFrameBuffer(0, 0, width, height, 0, 0, width, height,
                GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
            primed = true;
            main.beginWrite(true);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
        } catch (Exception | LinkageError error) {
            LOGGER.warn("FlintFix motion blur is unavailable and was turned off", error);
            failed = true;
            release();
            main.beginWrite(true);
        }
    }

    private static void release() {
        if (history != null) {
            history.delete();
            history = null;
        }
        primed = false;
    }
}
