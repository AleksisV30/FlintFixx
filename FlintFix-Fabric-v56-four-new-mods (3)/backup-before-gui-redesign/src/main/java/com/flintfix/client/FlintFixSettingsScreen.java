package com.flintfix.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Compact live-rendered FlintFix dashboard. */
public final class FlintFixSettingsScreen extends Screen {
    private final Screen parent;

    private int x, y, w, h;
    private int sidebarW;
    private int contentX, contentW;
    private int searchW;
    private int listTop, listBottom;
    private int cardW;
    private int rowH;
    private int scroll, maxScroll;
    private static final int GRID_GAP = 6;
    private boolean searchFocused;
    private String searchText = "";
    private String profileSearch = "";
    private boolean profileSearchFocused;
    private long transitionStartedAt = System.currentTimeMillis();
    private boolean closing;
    private static final long TRANSITION_MS = 190L;
    private static final long MODULE_COLOR_MS = 260L;
    private static final long RIPPLE_MS = 460L;
    private static final int MODULE_COUNT = 13;
    private final boolean[] moduleStateSeen = new boolean[MODULE_COUNT];
    private final boolean[] lastRenderedEnabled = new boolean[MODULE_COUNT];
    private final boolean[] colorTransitionFromEnabled = new boolean[MODULE_COUNT];
    private final long[] colorTransitionStartedAt = new long[MODULE_COUNT];
    private final long[] rippleStartedAt = new long[MODULE_COUNT];
    private final int[] rippleX = new int[MODULE_COUNT];
    private final int[] rippleY = new int[MODULE_COUNT];
    private final boolean[] rippleEnabled = new boolean[MODULE_COUNT];

    private static final int MAX_W = 270;
    private static final int MAX_H = 176;
    private static final ItemStack FLINT_PFP = new ItemStack(Items.FLINT);

    private static final String[][] MODULES = {
        {"fps", "FPS Display", "Live frame rate", "hud"},
        {"cps", "CPS Counter", "Clicks / sec", "input"},
        {"coordinates", "Coordinates", "XYZ + biome", "hud"},
        {"keystrokes", "Keystrokes", "Key input", "input"},
        {"ping", "Ping Display", "Server latency", "server"},
        {"armor", "Armor Stats", "Points + wear", "hud"},
        {"chunks", "Chunks", "Chunk border grid", "world"},
        {"freecam", "Freecam", "Detached camera", "client"},
        {"trajectory", "Trajectory", "Projectile landing path", "world"},
        {"hitboxes", "Show Hitboxes", "Player, mob, item bounds", "hitboxes"},
        {"zoom", "Zoom", "Hold key and scroll to zoom", "zoom"},
        {"lookaround", "Look Around", "Look without turning player", "lookaround"},
        {"shulkers", "See in Shulkers", "Preview box contents on hover", "shulkers"}
    };
    private static final String[] CARD_LABELS = {"FPS", "CPS", "XYZ", "WASD", "Ping", "Armor", "Chunks", "Freecam", "Path", "Hitbox", "Zoom", "Look", "Shulker"};

    public FlintFixSettingsScreen(Screen parent) {
        super(Text.literal("FlintFix Client"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        layout();
    }

    private void layout() {
        int availableW = Math.max(1, width - 20);
        int availableH = Math.max(1, height - 8);
        w = Math.min(MAX_W, availableW);
        h = Math.min(MAX_H, availableH);
        x = (width - w) / 2;
        y = (height - h) / 2;

        sidebarW = Math.min(96, Math.max(64, w / 4));
        contentX = x + sidebarW + 9;
        contentW = x + w - 9 - contentX;
        searchW = Math.min(92, Math.max(62, contentW - 104));
        // The search remains in the top bar; card space starts directly beneath it.
        listTop = y + 35;
        listBottom = y + h - 40;
        rowH = 50;
        cardW = Math.max(1, (contentW - GRID_GAP * 2) / 3);

        int count = filteredModules().size();
        int rows = (count + 2) / 3;
        int contentHeight = rows <= 0 ? 0 : rows * (rowH + GRID_GAP) - GRID_GAP;
        maxScroll = Math.max(0, contentHeight - Math.max(1, listBottom - listTop));
        scroll = clamp(scroll, 0, maxScroll);
    }

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        layout();
        float visibility = visibility();
        applyBlur(delta);
        c.fill(0, 0, width, height, alpha(0x60000000, visibility));

        float scale = 0.965f + 0.035f * visibility;
        float centerX = x + w / 2.0f;
        float centerY = y + h / 2.0f;
        c.getMatrices().push();
        c.getMatrices().translate(centerX, centerY + (1.0f - visibility) * 5.0f, 0);
        c.getMatrices().scale(scale, scale, 1.0f);
        c.getMatrices().translate(-centerX, -centerY, 0);

        FlintFixUi.fadeOutline(c, x, y, w, h, 8, alpha(FlintFixUi.ACCENT, visibility));
        FlintFixUi.rounded(c, x, y, w, h, 8, alpha(0xFF10151D, visibility));
        FlintFixUi.rounded(c, x + 1, y + 1, sidebarW - 1, h - 2, 7, alpha(0xFF171E28, visibility));
        c.fill(x + sidebarW, y + 12, x + sidebarW + 1, y + h - 12, alpha(0xFF383838, visibility));

        List<Integer> filtered = filteredModules();
        renderSidebar(c, mouseX, mouseY);
        renderHeader(c, mouseX, mouseY);
        renderCardContainer(c);
        c.enableScissor(contentX - 1, listTop, contentX + contentW + 1, listBottom);
        for (int visibleIndex = 0; visibleIndex < filtered.size(); visibleIndex++) {
            int moduleIndex = filtered.get(visibleIndex);
            int col = visibleIndex % 3;
            int row = visibleIndex / 3;
            int cardX = contentX + col * (cardW + GRID_GAP);
            int rowY = listTop + row * (rowH + GRID_GAP) - scroll;
            if (rowY + rowH >= listTop && rowY <= listBottom) {
                renderModule(c, moduleIndex, cardX, rowY, mouseX, mouseY);
            }
        }
        if (filtered.isEmpty()) {
            FlintFixFont.drawCentered(c, "No matching modules", contentX + contentW / 2, listTop + 18, 8, 0xFF8E8793, false);
        }
        c.disableScissor();
        renderScrollbar(c);

        int footerControlY = y + h - 22;
        int layoutButtonW = Math.max(56, FlintFixFont.width("HUD LAYOUT", 5, true) + 14);
        FlintFixUi.compactButton(c, contentX, footerControlY, layoutButtonW, 14, "HUD LAYOUT",
            FlintFixUi.inside(mouseX, mouseY, contentX, footerControlY, layoutButtonW, 14), true);
        String keyLabel = FlintFixClient.getSettingsKeyLabel();
        int keyW = FlintFixFont.width(keyLabel, 5, false) + 10;
        int keyX = x + w - keyW - 7;
        FlintFixUi.rounded(c, keyX, footerControlY, keyW, 14, 5, alpha(0xFF303030, visibility));
        int keyTextY = footerControlY + Math.round((14 - 5) / 2.0f);
        FlintFixFont.drawCentered(c, keyLabel, keyX + keyW / 2, keyTextY, 5, 0xFFB1BDCC, false);
        c.getMatrices().pop();
    }

    private float visibility() {
        float t = Math.max(0.0f, Math.min(1.0f,
            (System.currentTimeMillis() - transitionStartedAt) / (float) TRANSITION_MS));
        float eased = t * t * (3.0f - 2.0f * t);
        return closing ? 1.0f - eased : eased;
    }

    private static int alpha(int color, float visibility) {
        int original = (color >>> 24) & 0xFF;
        int adjusted = Math.round(original * Math.max(0.0f, Math.min(1.0f, visibility)));
        return (color & 0x00FFFFFF) | (adjusted << 24);
    }

    private static int blendColor(int from, int to, float amount) {
        float t = Math.max(0.0f, Math.min(1.0f, amount));
        int a = Math.round(((from >>> 24) & 0xFF) + (((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * t);
        int r = Math.round(((from >>> 16) & 0xFF) + (((to >>> 16) & 0xFF) - ((from >>> 16) & 0xFF)) * t);
        int g = Math.round(((from >>> 8) & 0xFF) + (((to >>> 8) & 0xFF) - ((from >>> 8) & 0xFF)) * t);
        int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    @Override
    public void tick() {
        super.tick();
        if (closing && System.currentTimeMillis() - transitionStartedAt >= TRANSITION_MS) {
            closing = false;
            if (client != null) client.setScreen(parent);
        }
    }

    private void renderSidebar(DrawContext c, int mouseX, int mouseY) {
        int avatarX = x + 7;
        int avatarY = y + 8;
        FlintFixUi.rounded(c, avatarX - 1, avatarY - 1, 22, 22, 6, 0xFF424242);
        c.drawItem(FLINT_PFP, avatarX + 2, avatarY + 2);
        FlintFixFont.draw(c, "FlintFix", x + 31, y + 9, 5, 0xFFF7F4F9, true);
        FlintFixFont.draw(c, "CLIENT", x + 31, y + 16, 4, 0xFF9AA8B9, false);

        c.fill(x + 8, y + 34, x + sidebarW - 8, y + 35, 0xFF383838);
        FlintFixFont.draw(c, "PROFILE", x + 9, y + 38, 5, 0xFF8D98A8, true);
        int profileButtonY = y + 45;
        boolean profileButtonHover = FlintFixUi.inside(mouseX, mouseY, x + 7, profileButtonY, sidebarW - 14, 15);
        float profileButtonMotion = FlintFixUi.hoverProgress("profile-button", profileButtonHover);
        FlintFixUi.outlinedBox(c, x + 7, profileButtonY, sidebarW - 14, 15, 3,
            FlintFixUi.interactiveBorder(profileButtonMotion > 0.3f),
            FlintFixUi.blendColors(0xFF20242A, 0xFF303640, profileButtonMotion));
        FlintFixIcons.draw(c, "profiles", x + 11, profileButtonY + 4, 7, 0xFFD6E0ED);
        FlintFixUi.drawTrimmed(c, FlintFixProfileStore.selectedName(), x + 22, profileButtonY + 5,
            sidebarW - 32, 4, 0xFFE5EAF1, true);

        FlintFixFont.draw(c, "PROFILES", x + 9, y + 63, 4, 0xFF8D98A8, true);
        List<String> profiles = visibleProfiles();
        for (int i = 0; i < Math.min(3, profiles.size()); i++) {
            String name = profiles.get(i);
            int rowY = y + 69 + i * 14;
            boolean selected = name.equalsIgnoreCase(FlintFixProfileStore.selectedName());
            boolean hover = FlintFixUi.inside(mouseX, mouseY, x + 7, rowY, sidebarW - 14, 13);
            float rowMotion = FlintFixUi.hoverProgress("sidebar-profile:" + name, hover || selected);
            if (rowMotion > 0.01f) {
                int activeFill = FlintFixUi.activeTheme() == FlintFixTheme.LIGHT ? 0xFFD9DDE1 : 0xFF303239;
                FlintFixUi.outlinedBox(c, x + 7, rowY, sidebarW - 14, 13, 2,
                    FlintFixUi.interactiveBorder(rowMotion > 0.3f),
                    FlintFixUi.blendColors(0xFF1E2024, activeFill, rowMotion));
                FlintFixUi.rounded(c, x + 8, rowY + 3, 2, 7, 1, FlintFixUi.ACCENT_BRIGHT);
            }
            FlintFixUi.drawTrimmed(c, name, x + 13, rowY + 4, sidebarW - 25, 4,
                selected ? 0xFFF1F2F4 : 0xFFB3BECD, selected);
        }
        int profileSearchY = y + 113;
        FlintFixUi.outlinedBox(c, x + 7, profileSearchY, sidebarW - 14, 13, 2,
            FlintFixUi.interactiveBorder(profileSearchFocused),
            profileSearchFocused ? 0xFF303239 : 0xFF20242A);
        FlintFixIcons.draw(c, "search", x + 10, profileSearchY + 4, 5, 0xFF9AA4B1);
        FlintFixUi.drawTrimmed(c, profileSearch.isEmpty() ? "Find profile" : profileSearch,
            x + 18, profileSearchY + 4, sidebarW - 28, 4,
            profileSearch.isEmpty() ? 0xFF8A909B : 0xFFE5EAF1, false);

        int manageY = y + 130;
        FlintFixUi.compactButton(c, x + 7, manageY, sidebarW - 14, 14, "MANAGE",
            FlintFixUi.inside(mouseX, mouseY, x + 7, manageY, sidebarW - 14, 14), false);
        int themeY = y + 149;
        FlintFixUi.compactButton(c, x + 7, themeY, sidebarW - 14, 15, "THEMES",
            FlintFixUi.inside(mouseX, mouseY, x + 7, themeY, sidebarW - 14, 15), true);
    }

    private void renderHeader(DrawContext c, int mouseX, int mouseY) {
        int searchX = x + w - searchW - 23;
        int searchY = y + 6;
        FlintFixUi.outlinedBox(c, searchX, searchY, searchW, 15, 2,
            FlintFixUi.interactiveBorder(searchFocused),
            searchFocused ? 0xFF242D39 : 0xFF1B222C);
        FlintFixIcons.draw(c, "search", searchX + 5, searchY + 4, 7, 0xFF93A0B1);
        String shown = FlintFixFont.trim(searchText.isEmpty() ? "Search..." : searchText,
            searchW - 24, 5, false);
        FlintFixFont.drawCentered(c, shown, searchX + searchW / 2, searchY + 5,
            5, searchText.isEmpty() ? 0xFF7F8A9A : 0xFFE2E8F0, false);
        if (searchFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int textX = searchX + searchW / 2 - FlintFixFont.width(shown, 5, false) / 2;
            int caretX = textX + FlintFixFont.width(searchText, 5, false);
            c.fill(caretX, searchY + 5, caretX + 1, searchY + 10, 0xFFE6E0EA);
        }

        int closeX = x + w - 18;
        FlintFixUi.rounded(c, closeX, y + 6, 11, 11, 4,
            FlintFixUi.inside(mouseX, mouseY, closeX, y + 6, 11, 11) ? 0xFF404D5E : 0xFF252B34);
        int centerX = closeX + 5;
        int centerY = y + 11;
        for (int i = -2; i <= 2; i++) {
            c.fill(centerX + i, centerY + i, centerX + i + 1, centerY + i + 1, 0xFFD2DAE5);
            c.fill(centerX + i, centerY - i, centerX + i + 1, centerY - i + 1, 0xFFD2DAE5);
        }
    }

    private void renderModule(DrawContext c, int index, int cardX, int rowY, int mouseX, int mouseY) {
        boolean implemented = isImplemented(index);
        boolean enabled = isModuleEnabled(index);
        long now = System.currentTimeMillis();
        if (!moduleStateSeen[index]) {
            moduleStateSeen[index] = true;
            lastRenderedEnabled[index] = enabled;
            colorTransitionFromEnabled[index] = enabled;
            colorTransitionStartedAt[index] = now;
        } else if (lastRenderedEnabled[index] != enabled) {
            colorTransitionFromEnabled[index] = lastRenderedEnabled[index];
            lastRenderedEnabled[index] = enabled;
            colorTransitionStartedAt[index] = now;
        }
        float progress = Math.min(1.0f, (now - colorTransitionStartedAt[index]) / (float) MODULE_COLOR_MS);
        float eased = progress * progress * (3.0f - 2.0f * progress);
        boolean hover = mouseY >= listTop && mouseY < listBottom && FlintFixUi.inside(mouseX, mouseY, cardX, rowY, cardW, rowH);
        int inactiveCard = hover ? 0xFF303239 : 0xFF1E2024;
        int activeCard = hover ? 0xFF303239 : 0xFF22252B;
        int fromCard = colorTransitionFromEnabled[index] ? activeCard : inactiveCard;
        int targetCard = enabled ? activeCard : inactiveCard;
        int cardColor = implemented ? blendColor(fromCard, targetCard, eased) : (hover ? 0xFF26303B : 0xFF1B2027);

        int borderColor = FlintFixUi.activeTheme() == FlintFixTheme.LIGHT
            ? (hover ? 0xFF171C22 : (enabled ? 0xFF515B66 : 0xFF717B85))
            : (hover ? 0xFFC2C2C2 : (enabled ? 0xFF5B5B5B : 0xFF454545));
        FlintFixUi.outlinedBox(c, cardX, rowY, cardW, rowH, 2, borderColor, cardColor);
        renderRipple(c, index, cardX, rowY, now);
        int iconSize = Math.min(12, Math.max(10, cardW - 8));
        FlintFixIcons.draw(c, MODULES[index][0], cardX + (cardW - iconSize) / 2,
            rowY + 4, iconSize, FlintFixUi.ACCENT_BRIGHT);
        FlintFixFont.drawCentered(c, CARD_LABELS[index], cardX + cardW / 2, rowY + 18, 5,
            implemented ? 0xFFB9BBC0 : 0xFF8D9198, false);

        // A dedicated full-width options strip matches the reference card layout.
        int optionsY = rowY + 28;
        int optionsX = cardX + 3;
        int optionsW = Math.max(1, cardW - 6);
        int optionsH = 7;
        boolean optionsHover = hover && FlintFixUi.inside(mouseX, mouseY, optionsX, optionsY, optionsW, optionsH);
        int optionsBase = optionsHover ? 0xFF4B4B4B : 0xFF363636;
        FlintFixUi.outlinedBox(c, optionsX, optionsY, optionsW, optionsH, 2,
            optionsHover ? FlintFixUi.interactiveBorder(true) : 0xFF16191E, optionsBase);
        int gearAreaW = Math.min(10, Math.max(8, optionsW / 4));
        int gearX = optionsX + optionsW - gearAreaW;
        c.fill(gearX, optionsY + 1, gearX + 1, optionsY + optionsH - 1, 0xFF555555);
        FlintFixFont.drawCentered(c, "OPTIONS", optionsX + (optionsW - gearAreaW) / 2,
            optionsY + 1, 3, optionsHover ? 0xFFFFFFFF : 0xFFE1E4E8, true);
        FlintFixIcons.draw(c, "settings", gearX + (gearAreaW - 6) / 2, optionsY, 6,
            optionsHover ? 0xFFFFFFFF : 0xFFE1E1E1);

        // Separate red/green state button provides a clear enable/disable target.
        int stateY = rowY + 39;
        int stateX = optionsX;
        int stateW = optionsW;
        int stateH = 8;
        boolean stateHover = hover && FlintFixUi.inside(mouseX, mouseY, stateX, stateY, stateW, stateH);
        int fromStatus = colorTransitionFromEnabled[index] ? 0xFF24975E : 0xFFA51D3E;
        int targetStatus = enabled ? 0xFF24975E : 0xFFA51D3E;
        int stateColor = implemented ? blendColor(fromStatus, targetStatus, eased) : 0xFF3A3D42;
        if (stateHover) stateColor = blendColor(stateColor, enabled ? 0xFF35AA70 : 0xFFB92B4D, 0.75f);
        FlintFixUi.outlinedBox(c, stateX, stateY, stateW, stateH, 2, 0xFF17191D, stateColor);
        FlintFixFont.drawCentered(c, implemented ? (enabled ? "ENABLED" : "DISABLED") : "OFFLINE",
            stateX + stateW / 2, stateY + 1, 3, 0xFFFFFFFF, true);
    }

    /** Layered slate-blue tray that gives the module grid a clear, finished surface. */
    private void renderCardContainer(DrawContext c) {
        int boxX = contentX - 4;
        int boxY = listTop - 5;
        int boxW = contentW + 8;
        int boxH = listBottom - listTop + 8;
        if (boxW <= 8 || boxH <= 8) return;

        FlintFixUi.fadeOutline(c, boxX, boxY, boxW, boxH, 3, 0x55808080);
        FlintFixUi.rounded(c, boxX, boxY, boxW, boxH, 3, 0xFF414141);
        FlintFixUi.rounded(c, boxX + 1, boxY + 1, boxW - 2, boxH - 2, 3, 0xFF171717);
        FlintFixUi.rounded(c, boxX + 2, boxY + 2, boxW - 4, boxH - 4, 2, 0xFF131313);
    }

    /** Expanding color ring anchored at the exact point of the most recent toggle click. */
    private void renderRipple(DrawContext c, int index, int cardX, int rowY, long now) {
        long started = rippleStartedAt[index];
        if (started == 0L) return;
        float t = Math.min(1.0f, (now - started) / (float) RIPPLE_MS);
        if (t >= 1.0f) return;
        float eased = t * t * (3.0f - 2.0f * t);
        float radius = (float) Math.hypot(cardW, rowH) * eased;
        float thickness = Math.max(2.0f, Math.min(5.0f, rowH * 0.12f));
        int rgb = rippleEnabled[index] ? 0x0079D69A : 0x00E38A7C;
        int alpha = Math.round((1.0f - t) * 88.0f);
        int color = (alpha << 24) | rgb;
        int centerX = cardX + rippleX[index];
        int centerY = rowY + rippleY[index];
        c.enableScissor(cardX + 2, rowY + 2, cardX + cardW - 2, rowY + rowH - 2);
        for (int dy = -rowH; dy <= rowH; dy++) {
            float outerSq = radius * radius - dy * dy;
            if (outerSq <= 0.0f) continue;
            int outer = (int) Math.sqrt(outerSq);
            float innerRadius = Math.max(0.0f, radius - thickness);
            float innerSq = innerRadius * innerRadius - dy * dy;
            int inner = innerSq > 0.0f ? (int) Math.sqrt(innerSq) : -1;
            int scanY = centerY + dy;
            if (inner < 0) {
                c.fill(centerX - outer, scanY, centerX + outer + 1, scanY + 1, color);
            } else {
                c.fill(centerX - outer, scanY, centerX - inner, scanY + 1, color);
                c.fill(centerX + inner + 1, scanY, centerX + outer + 1, scanY + 1, color);
            }
        }
        c.disableScissor();
    }

    private void renderScrollbar(DrawContext c) {
        if (maxScroll <= 0) return;
        int trackH = listBottom - listTop;
        int thumbH = Math.max(18, trackH * trackH / (trackH + maxScroll));
        int thumbY = listTop + (trackH - thumbH) * scroll / maxScroll;
        int sx = contentX + contentW - 2;
        FlintFixUi.rounded(c, sx, listTop, 2, trackH, 1, 0x2E312E38);
        FlintFixUi.rounded(c, sx, thumbY, 2, thumbH, 1, FlintFixUi.ACCENT);
    }

    private List<Integer> filteredModules() {
        List<Integer> result = new ArrayList<>();
        String q = searchText.trim().toLowerCase(Locale.ROOT);
        for (int i = 0; i < MODULES.length; i++) {
            boolean matchesText = q.isEmpty() || MODULES[i][1].toLowerCase(Locale.ROOT).contains(q)
                || MODULES[i][2].toLowerCase(Locale.ROOT).contains(q);
            if (matchesText) result.add(i);
        }
        return result;
    }

    private List<String> visibleProfiles() {
        List<String> result = new ArrayList<>();
        String query = profileSearch.trim().toLowerCase(Locale.ROOT);
        for (String name : FlintFixProfileStore.names()) {
            if (query.isEmpty() || name.toLowerCase(Locale.ROOT).contains(query)) result.add(name);
        }
        return result;
    }

    private boolean isImplemented(int index) {
        return index >= 0 && index <= 12;
    }

    private boolean isModuleEnabled(int index) {
        return switch (index) {
            case 0 -> FlintFixClient.CONFIG.fpsEnabled;
            case 1 -> FlintFixClient.CONFIG.cpsEnabled;
            case 2 -> FlintFixClient.CONFIG.coordinatesEnabled;
            case 3 -> FlintFixClient.CONFIG.keystrokesEnabled;
            case 4 -> FlintFixClient.CONFIG.pingEnabled;
            case 5 -> FlintFixClient.CONFIG.armorEnabled;
            case 6 -> FlintFixClient.CONFIG.chunksEnabled;
            case 7 -> FlintFixFreecam.isActive();
            case 8 -> FlintFixClient.CONFIG.trajectoryEnabled;
            case 9 -> FlintFixClient.CONFIG.hitboxesEnabled;
            case 10 -> FlintFixClient.CONFIG.zoomEnabled;
            case 11 -> FlintFixClient.CONFIG.lookAroundEnabled;
            case 12 -> FlintFixClient.CONFIG.shulkerPreviewEnabled;
            default -> false;
        };
    }

    private void toggleModule(int index) {
        if (index == 7) {
            if (FlintFixFreecam.isActive()) FlintFixFreecam.disable(MinecraftClient.getInstance());
            else FlintFixFreecam.requestEnable(MinecraftClient.getInstance(), this);
            return;
        }
        switch (index) {
            case 0 -> FlintFixClient.CONFIG.fpsEnabled = !FlintFixClient.CONFIG.fpsEnabled;
            case 1 -> FlintFixClient.CONFIG.cpsEnabled = !FlintFixClient.CONFIG.cpsEnabled;
            case 2 -> FlintFixClient.CONFIG.coordinatesEnabled = !FlintFixClient.CONFIG.coordinatesEnabled;
            case 3 -> FlintFixClient.CONFIG.keystrokesEnabled = !FlintFixClient.CONFIG.keystrokesEnabled;
            case 4 -> FlintFixClient.CONFIG.pingEnabled = !FlintFixClient.CONFIG.pingEnabled;
            case 5 -> FlintFixClient.CONFIG.armorEnabled = !FlintFixClient.CONFIG.armorEnabled;
            case 6 -> FlintFixClient.CONFIG.chunksEnabled = !FlintFixClient.CONFIG.chunksEnabled;
            case 8 -> FlintFixClient.CONFIG.trajectoryEnabled = !FlintFixClient.CONFIG.trajectoryEnabled;
            case 9 -> FlintFixClient.CONFIG.hitboxesEnabled = !FlintFixClient.CONFIG.hitboxesEnabled;
            case 10 -> FlintFixClient.CONFIG.zoomEnabled = !FlintFixClient.CONFIG.zoomEnabled;
            case 11 -> FlintFixClient.CONFIG.lookAroundEnabled = !FlintFixClient.CONFIG.lookAroundEnabled;
            case 12 -> FlintFixClient.CONFIG.shulkerPreviewEnabled = !FlintFixClient.CONFIG.shulkerPreviewEnabled;
            default -> { return; }
        }
        FlintFixClient.CONFIG.save();
    }

    private int searchX() { return x + w - searchW - 23; }
    private int searchY() { return y + 6; }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        layout();
        if (mouseX >= contentX - 4 && mouseX <= contentX + contentW + 4 && mouseY >= listTop && mouseY <= listBottom) {
            scroll = clamp(scroll - (int) Math.round(verticalAmount * (rowH + GRID_GAP)), 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        layout();
        if (closing || System.currentTimeMillis() - transitionStartedAt < TRANSITION_MS) return true;
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        if (FlintFixUi.inside(mouseX, mouseY, searchX(), searchY(), searchW, 15)) {
            searchFocused = true;
            profileSearchFocused = false;
            return true;
        }
        searchFocused = false;

        int closeX = x + w - 18;
        if (FlintFixUi.inside(mouseX, mouseY, closeX, y + 6, 11, 11)) {
            close();
            return true;
        }

        if (FlintFixUi.inside(mouseX, mouseY, x + 7, y + 45, sidebarW - 14, 15)
            || FlintFixUi.inside(mouseX, mouseY, x + 7, y + 130, sidebarW - 14, 14)) {
            if (client != null) client.setScreen(new FlintFixProfileScreen(this));
            return true;
        }
        if (FlintFixUi.inside(mouseX, mouseY, x + 7, y + 149, sidebarW - 14, 15)) {
            if (client != null) client.setScreen(new FlintFixThemeScreen(this));
            return true;
        }
        if (FlintFixUi.inside(mouseX, mouseY, x + 7, y + 113, sidebarW - 14, 13)) {
            profileSearchFocused = true;
            return true;
        }
        profileSearchFocused = false;
        List<String> profiles = visibleProfiles();
        for (int i = 0; i < Math.min(3, profiles.size()); i++) {
            int rowY = y + 69 + i * 14;
            if (FlintFixUi.inside(mouseX, mouseY, x + 7, rowY, sidebarW - 14, 13)) {
                FlintFixProfileStore.select(profiles.get(i));
                scroll = 0;
                return true;
            }
        }

        List<Integer> filtered = filteredModules();
        for (int visibleIndex = 0; visibleIndex < filtered.size(); visibleIndex++) {
            int moduleIndex = filtered.get(visibleIndex);
            int col = visibleIndex % 3;
            int row = visibleIndex / 3;
            int cardX = contentX + col * (cardW + GRID_GAP);
            int rowY = listTop + row * (rowH + GRID_GAP) - scroll;
            if (mouseY >= listTop && mouseY < listBottom && FlintFixUi.inside(mouseX, mouseY, cardX, rowY, cardW, rowH)) {
                int optionsY = rowY + 28;
                int optionsX = cardX + 3;
                int optionsW = Math.max(1, cardW - 6);
                if (isImplemented(moduleIndex) && FlintFixUi.inside(mouseX, mouseY,
                    optionsX, optionsY, optionsW, 7)) {
                    if (client != null) {
                        switch (moduleIndex) {
                            case 0 -> client.setScreen(new FlintFixFpsSettingsScreen(this));
                            case 1 -> client.setScreen(new FlintFixModuleSettingsScreen(this, FlintFixModuleSettingsScreen.Module.CPS));
                            case 2 -> client.setScreen(new FlintFixModuleSettingsScreen(this, FlintFixModuleSettingsScreen.Module.COORDINATES));
                            case 3 -> client.setScreen(new FlintFixModuleSettingsScreen(this, FlintFixModuleSettingsScreen.Module.KEYSTROKES));
                            case 4 -> client.setScreen(new FlintFixModuleSettingsScreen(this, FlintFixModuleSettingsScreen.Module.PING));
                            case 5 -> client.setScreen(new FlintFixModuleSettingsScreen(this, FlintFixModuleSettingsScreen.Module.ARMOR));
                            case 6 -> client.setScreen(new FlintFixModuleSettingsScreen(this, FlintFixModuleSettingsScreen.Module.CHUNKS));
                            case 7 -> client.setScreen(new FlintFixFreecamSettingsScreen(this));
                            case 8 -> client.setScreen(new FlintFixModuleSettingsScreen(this, FlintFixModuleSettingsScreen.Module.TRAJECTORY));
                            case 9 -> client.setScreen(new FlintFixModuleSettingsScreen(this, FlintFixModuleSettingsScreen.Module.HITBOXES));
                            case 10 -> client.setScreen(new FlintFixModuleSettingsScreen(this, FlintFixModuleSettingsScreen.Module.ZOOM));
                            case 11 -> client.setScreen(new FlintFixModuleSettingsScreen(this, FlintFixModuleSettingsScreen.Module.LOOK_AROUND));
                            case 12 -> client.setScreen(new FlintFixModuleSettingsScreen(this, FlintFixModuleSettingsScreen.Module.SHULKERS));
                            default -> { }
                        }
                    }
                } else {
                    toggleModule(moduleIndex);
                    rippleX[moduleIndex] = clamp((int) mouseX - cardX, 0, cardW);
                    rippleY[moduleIndex] = clamp((int) mouseY - rowY, 0, rowH);
                    rippleStartedAt[moduleIndex] = System.currentTimeMillis();
                    rippleEnabled[moduleIndex] = isModuleEnabled(moduleIndex);
                }
                return true;
            }
        }

        int footerY = y + h - 22;
        int layoutButtonW = Math.max(56, FlintFixFont.width("HUD LAYOUT", 5, true) + 14);
        if (FlintFixUi.inside(mouseX, mouseY, contentX, footerY, layoutButtonW, 14)) {
            if (client != null) client.setScreen(new FlintFixHudEditorScreen(this));
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (profileSearchFocused) {
            if (!Character.isISOControl(chr) && profileSearch.length() < 18) {
                profileSearch += chr;
                return true;
            }
            return true;
        }
        if (!searchFocused) return super.charTyped(chr, modifiers);
        if (!Character.isISOControl(chr) && searchText.length() < 28) {
            searchText += chr;
            scroll = 0;
            return true;
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (closing) return true;
        if (profileSearchFocused) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                profileSearchFocused = false;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !profileSearch.isEmpty()) {
                profileSearch = profileSearch.substring(0, profileSearch.length() - 1);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_DELETE) {
                profileSearch = "";
                return true;
            }
        }
        if (searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                searchFocused = false;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!searchText.isEmpty()) searchText = searchText.substring(0, searchText.length() - 1);
                scroll = 0;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_DELETE) {
                searchText = "";
                scroll = 0;
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        if (closing) return;
        closing = true;
        transitionStartedAt = System.currentTimeMillis();
    }
}
