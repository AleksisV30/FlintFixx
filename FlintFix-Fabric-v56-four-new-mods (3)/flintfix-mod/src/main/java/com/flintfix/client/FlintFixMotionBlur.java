package com.flintfix.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Motion blur as a post effect: each frame is blended with the previous one
 * (assets/flintfix/shaders/post/motion_blur.json). It runs after the world is
 * drawn, so the hand and HUD stay sharp. If the effect fails to load it turns
 * itself off instead of breaking rendering.
 */
public final class FlintFixMotionBlur {
    private static final Logger LOGGER = LoggerFactory.getLogger("FlintFix");
    private static final Identifier CHAIN = Identifier.of("flintfix", "shaders/post/motion_blur.json");
    private static PostEffectProcessor processor;
    private static int width = -1;
    private static int height = -1;
    private static boolean failed;

    private FlintFixMotionBlur() {}

    public static void render(float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        FlintFixConfig config = FlintFixClient.CONFIG;
        if (config == null || !config.motionBlurEnabled || failed || client.world == null) {
            close();
            return;
        }
        try {
            int w = client.getWindow().getFramebufferWidth();
            int h = client.getWindow().getFramebufferHeight();
            if (processor == null) {
                processor = new PostEffectProcessor(client.getTextureManager(), client.getResourceManager(),
                    client.getFramebuffer(), CHAIN);
                width = -1;
            }
            if (w != width || h != height) {
                processor.setupDimensions(w, h);
                width = w;
                height = h;
            }
            processor.setUniforms("BlurFactor", config.motionBlurStrength);
            RenderSystem.disableBlend();
            RenderSystem.disableDepthTest();
            RenderSystem.resetTextureMatrix();
            processor.render(tickDelta);
            client.getFramebuffer().beginWrite(true);
            RenderSystem.enableDepthTest();
        } catch (Exception | LinkageError error) {
            LOGGER.warn("FlintFix motion blur is unavailable and was turned off", error);
            failed = true;
            close();
            client.getFramebuffer().beginWrite(true);
        }
    }

    private static void close() {
        if (processor != null) {
            processor.close();
            processor = null;
        }
        width = -1;
        height = -1;
    }
}
