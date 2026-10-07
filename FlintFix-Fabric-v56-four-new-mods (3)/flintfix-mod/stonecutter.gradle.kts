plugins {
    id("dev.kikugie.stonecutter")
    id("net.fabricmc.fabric-loom-remap") version "1.18.3" apply false
    id("net.fabricmc.fabric-loom") version "1.18.3" apply false
}

stonecutter active "1.21.1" /* [SC] DO NOT EDIT */

// Copies every version's jar into build/libs/all so the launcher can pick one per Minecraft version.
tasks.register("buildAll") {
    group = "build"
    dependsOn(stonecutter.tasks.named("collectJar"))
}

stonecutter parameters {
    // 1.21.11 renamed ResourceLocation to Identifier and turned the graphics
    // mode into a preset (same FAST/FANCY values; setting one applies it).
    replacements.string(eval(current.version, ">=1.21.11")) {
        replace("ResourceLocation", "Identifier")
    }
    replacements.string(eval(current.version, ">=1.21.11")) {
        replace("GraphicsStatus", "GraphicsPreset")
    }
    replacements.string(eval(current.version, ">=1.21.11")) {
        replace("graphicsMode()", "graphicsPreset()")
    }
    // 26.1 renamed the GUI drawing class and its methods, Fabric's level-render
    // and key-mapping APIs, and moved the full-bright light constant.
    replacements.regex(eval(current.version, ">=26.1")) {
        replace("\\bGuiGraphics\\b", "GuiGraphicsExtractor")
        reverse("\\bGuiGraphicsExtractor\\b", "GuiGraphics")
    }
    replacements.regex(eval(current.version, ">=26.1")) {
        replace("\\.drawString\\(", ".text(")
        reverse("(?!)", "_")
    }
    replacements.regex(eval(current.version, ">=26.1")) {
        replace("\\.drawCenteredString\\(", ".centeredText(")
        reverse("(?!)", "_")
    }
    replacements.regex(eval(current.version, ">=26.1")) {
        replace("\\.renderItemDecorations\\(", ".itemDecorations(")
        reverse("(?!)", "_")
    }
    replacements.regex(eval(current.version, ">=26.1")) {
        replace("\\.renderItem\\(", ".item(")
        reverse("(?!)", "_")
    }
    replacements.regex(eval(current.version, ">=26.1")) {
        replace("rendering\\.v1\\.world\\.", "rendering.v1.level.")
        reverse("(?!)", "_")
    }
    replacements.regex(eval(current.version, ">=26.1")) {
        replace("WorldRenderContext", "LevelRenderContext")
        reverse("(?!)", "_")
    }
    replacements.regex(eval(current.version, ">=26.1")) {
        replace("WorldRenderEvents", "LevelRenderEvents")
        reverse("(?!)", "_")
    }
    replacements.regex(eval(current.version, ">=26.1")) {
        replace("keybinding\\.v1\\.KeyBindingHelper", "keymapping.v1.KeyMappingHelper")
        reverse("(?!)", "_")
    }
    replacements.regex(eval(current.version, ">=26.1")) {
        replace("KeyBindingHelper\\.registerKeyBinding", "KeyMappingHelper.registerKeyMapping")
        reverse("(?!)", "_")
    }
    replacements.regex(eval(current.version, ">=26.1")) {
        replace("import\\ net\\.minecraft\\.client\\.renderer\\.LightTexture;", "import net.minecraft.util.LightCoordsUtil;")
        reverse("(?!)", "_")
    }
    replacements.regex(eval(current.version, ">=26.1")) {
        replace("LightTexture\\.FULL_BRIGHT", "LightCoordsUtil.FULL_BRIGHT")
        reverse("(?!)", "_")
    }
    replacements.regex(eval(current.version, ">=26.1")) {
        replace("context\\.consumers\\(\\)", "context.bufferSource()")
        reverse("(?!)", "_")
    }
    replacements.regex(eval(current.version, ">=26.1")) {
        replace("Screens\\.getButtons\\(", "Screens.getWidgets(")
        reverse("(?!)", "_")
    }
    replacements.regex(eval(current.version, ">=26.1")) {
        replace("ScreenEvents\\.afterRender\\(", "ScreenEvents.afterExtract(")
        reverse("(?!)", "_")
    }
    replacements.regex(eval(current.version, ">=26.1")) {
        replace("renderer\\.block\\.model\\.ItemTransform", "resources.model.cuboid.ItemTransform")
        reverse("(?!)", "_")
    }
    replacements.regex(eval(current.version, ">=26.1")) {
        replace("renderer\\.state\\.SkyRenderState", "renderer.state.level.SkyRenderState")
        reverse("(?!)", "_")
    }
    replacements.regex(eval(current.version, ">=26.1")) {
        replace("nameField\\.render\\(", "nameField.extractRenderState(")
        reverse("(?!)", "_")
    }
}
