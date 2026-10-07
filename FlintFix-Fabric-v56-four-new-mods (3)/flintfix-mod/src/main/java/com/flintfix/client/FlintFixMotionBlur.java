package com.flintfix.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//? if <1.21.5 {
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;
//?}

/**
 * Motion blur by frame accumulation: a copy of the previous (already blurred)
 * frame is blended over each newly drawn world frame, then the result is kept
 * for the next frame. It runs right after the world is drawn, so the hand and
 * HUD stay sharp. Plain framebuffer operations, no shader files involved.
 *
 * Minecraft 1.21.5 replaced direct framebuffer access with its GPU device API;
 * motion blur is not available there yet and stays off.
 */
public final class FlintFixMotionBlur {
    private static final Logger LOGGER = LoggerFactory.getLogger("FlintFix");

    private FlintFixMotionBlur() {}

    /** False on Minecraft versions where motion blur cannot run yet. */
    public static boolean supported() {
        //? if >=1.21.5 {
        /*return false;
        *///?} else {
        return true;
        //?}
    }

    //? if >=1.21.5 {
    /*private static boolean noticeLogged;

    public static void render() {
        FlintFixConfig config = FlintFixClient.CONFIG;
        if (config != null && config.motionBlurEnabled && !noticeLogged) {
            LOGGER.info("FlintFix motion blur is not available on this Minecraft version yet");
            noticeLogged = true;
        }
    }
    *///?} else {
    private static RenderTarget history;
    private static boolean primed;
    private static boolean failed;

    public static void render() {
        Minecraft client = Minecraft.getInstance();
        FlintFixConfig config = FlintFixClient.CONFIG;
        if (config == null || !config.motionBlurEnabled || failed || client.level == null) {
            release();
            return;
        }
        RenderTarget main = client.getMainRenderTarget();
        int width = main.width;
        int height = main.height;
        try {
            if (history == null || history.width != width || history.height != height) {
                release();
                //? if >=1.21.2 {
                /*history = new TextureTarget(width, height, false);
                *///?} else {
                history = new TextureTarget(width, height, false, Minecraft.ON_OSX);
                //?}
                primed = false;
            }
            if (primed) {
                // Draw the previous frame over this one at the chosen strength.
                main.bindWrite(false);
                RenderSystem.enableBlend();
                GL14.glBlendColor(0.0f, 0.0f, 0.0f, config.motionBlurStrength);
                GlStateManager._blendFuncSeparate(GL14.GL_CONSTANT_ALPHA, GL14.GL_ONE_MINUS_CONSTANT_ALPHA,
                    GL11.GL_ZERO, GL11.GL_ONE);
                // A textured quad that keeps the blend state set above (1.21.2+ draw() is a raw blit).
                //? if >=1.21.2 {
                /*history.blitAndBlendToScreen(width, height);
                *///?} else {
                history.blitToScreen(width, height, false);
                //?}
                RenderSystem.defaultBlendFunc();
                RenderSystem.disableBlend();
            }
            // Keep the blended result for the next frame.
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, history.frameBufferId);
            GlStateManager._glBlitFrameBuffer(0, 0, width, height, 0, 0, width, height,
                GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
            primed = true;
            main.bindWrite(true);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
        } catch (Exception | LinkageError error) {
            LOGGER.warn("FlintFix motion blur is unavailable and was turned off", error);
            failed = true;
            release();
            main.bindWrite(true);
        }
    }

    private static void release() {
        if (history != null) {
            history.destroyBuffers();
            history = null;
        }
        primed = false;
    }
    //?}
}
