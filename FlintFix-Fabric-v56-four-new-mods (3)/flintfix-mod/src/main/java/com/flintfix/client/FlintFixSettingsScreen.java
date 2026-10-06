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

/** Live-rendered FlintFix module dashboard. */
public final class FlintFixSettingsScreen extends Screen {
    private final Screen parent;

    private int x, y, w, h;
    private int sidebarW;
    private int contentX, contentW;
    private int searchX, searchY, searchW;
    private int closeX, closeY;
    private int listTop, listBottom;
    private int columns, cardW;
    private int scroll, maxScroll;
    private int profileSearchY, profileListTop, profileRows;
    private int manageY, layoutY, themesY;
    private boolean searchFocused;
    private String searchText = "";
    private String profileSearch = "";
    private boolean profileSearchFocused;
    private long transitionStartedAt = System.currentTimeMillis();
    private boolean closing;

    private static final long TRANSITION_MS = 190L;
    private static final long RIPPLE_MS = 460L;
    private static final int MAX_W = 392;
    private static final int MAX_H = 248;
    private static final int GRID_GAP = 6;
    private static final int ROW_H = 52;
    private static final int PROFILE_ROW_H = 15;
    private static final int PROFILE_ROW_GAP = 2;
    private static final int GEAR_SIZE = 11;
    private static final ItemStack FLINT_PFP = new ItemStack(Items.FLINT);

    private static final String[][] MODULES = {
        {"fps", "FPS Display", "Live frame rate"},
        {"cps", "CPS Counter", "Clicks / second"},
        {"coordinates", "Coordinates", "XYZ + biome"},
        {"keystrokes", "Keystrokes", "WASD + mouse"},
        {"ping", "Ping", "Server latency"},
        {"armor", "Armor Stats", "Durability"},
        {"chunks", "Chunk Borders", "Chunk grid"},
        {"freecam", "Freecam", "Detached camera"},
        {"trajectory", "Trajectory", "Landing path"},
        {"hitboxes", "Hitboxes", "Entity bounds"},
        {"zoom", "Zoom", "Hold to zoom"},
        {"lookaround", "Look Around", "Free camera look"},
        {"shulkers", "Shulker View", "Box contents"},
        {"sky", "Custom Sky", "Sky presets"},
        {"inspect", "Item Inspect", "Spin held item"},
        {"showhand", "Show Hand", "Arm behind items"},
        {"fullbright", "Fullbright", "See in the dark"},
        {"potions", "Potion Effects", "Effect timers"},
        {"speed", "Speed Meter", "Blocks per second"},
        {"compass", "Compass Bar", "Heading strip"},
        {"outline", "Block Outline", "Custom outline"},
        {"crosshair", "Crosshair", "Custom crosshair"},
        {"lowoverlays", "Low Overlays", "Fire, shield, totem"},
        {"damage", "Damage Numbers", "Hit popups"},
        {"weather", "No Weather", "Hide rain and snow"},
        {"motionblur", "Motion Blur", "Smooth turning"},
        {"teamglow", "Team Glow", "Outline friends"},
        {"waypoints", "Waypoints", "Save places"},
        {"serverprofiles", "Server Profiles", "Profile per server"}
    };
    private static final int MODULE_COUNT = MODULES.length;
    private final long[] rippleStartedAt = new long[MODULE_COUNT];
    private final int[] rippleX = new int[MODULE_COUNT];
    private final int[] rippleY = new int[MODULE_COUNT];
    private final boolean[] rippleEnabled = new boolean[MODULE_COUNT];

    public FlintFixSettingsScreen(Screen parent) {
        super(Text.literal("FlintFix Client"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        layout();
    }

    private void layout() {
        w = Math.min(MAX_W, Math.max(1, width - 16));
        h = Math.min(MAX_H, Math.max(1, height - 16));
        x = (width - w) / 2;
        y = (height - h) / 2;

        sidebarW = Math.max(86, Math.min(108, Math.round(w * 0.27f)));
        contentX = x + sidebarW + 10;
        contentW = Math.max(1, x + w - 10 - contentX);

        closeX = x + w - 10 - 15;
        closeY = y + 9;
        searchW = Math.max(56, Math.min(120, contentW - 104));
        searchX = closeX - 5 - searchW;
        searchY = closeY;

        listTop = y + 38;
        listBottom = y + h - 22;
        columns = contentW >= 236 ? 3 : 2;
        cardW = Math.max(1, (contentW - GRID_GAP * (columns - 1)) / columns);

        // Sidebar: brand, profile search + list, then navigation pinned to the bottom.
        themesY = y + h - 8 - 16;
        layoutY = themesY - 20;
        manageY = layoutY - 26;
        profileSearchY = y + 52;
        profileListTop = profileSearchY + 18;
        profileRows = Math.max(0, (manageY - 4 - profileListTop + PROFILE_ROW_GAP) / (PROFILE_ROW_H + PROFILE_ROW_GAP));

        int count = filteredModules().size();
        int rows = (count + columns - 1) / columns;
        int contentHeight = rows <= 0 ? 0 : rows * (ROW_H + GRID_GAP) - GRID_GAP;
        maxScroll = Math.max(0, contentHeight - Math.max(1, listBottom - listTop));
        scroll = clamp(scroll, 0, maxScroll);
    }

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        layout();
        float visibility = visibility();
        applyBlur(delta);
        FlintFixUi.backdrop(c, width, height, visibility);

        float scale = 0.965f + 0.035f * visibility;
        float centerX = x + w / 2.0f;
        float centerY = y + h / 2.0f;
        c.getMatrices().push();
        c.getMatrices().translate(centerX, centerY + (1.0f - visibility) * 5.0f, 0);
        c.getMatrices().scale(scale, scale, 1.0f);
        c.getMatrices().translate(-centerX, -centerY, 0);

        FlintFixUi.panelFrame(c, x, y, w, h);
        FlintFixUi.roundedRaw(c, x + 1, y + 1, sidebarW - 1, h - 2, 2, FlintFixUi.panel());
        c.fill(x + sidebarW, y + 1, x + sidebarW + 1, y + h - 1, FlintFixUi.opacity(FlintFixUi.border(), 0.8f));

        renderSidebar(c, mouseX, mouseY);
        renderHeader(c, mouseX, mouseY);

        List<Integer> filtered = filteredModules();
        c.enableScissor(contentX - 1, listTop, contentX + contentW + 1, listBottom);
        for (int visibleIndex = 0; visibleIndex < filtered.size(); visibleIndex++) {
            int cardX = cardX(visibleIndex);
            int rowY = cardY(visibleIndex);
            if (rowY + ROW_H >= listTop && rowY <= listBottom) {
                renderModule(c, filtered.get(visibleIndex), cardX, rowY, mouseX, mouseY);
            }
        }
        if (filtered.isEmpty()) {
            FlintFixFont.drawCenteredExact(c, "No modules match \"" + FlintFixFont.trim(searchText, 80, 6, false) + "\"",
                contentX + contentW / 2, listTop + 24, 6, FlintFixUi.muted(), false);
        }
        c.disableScissor();
        FlintFixUi.scrollbar(c, contentX + contentW + 3, listTop, listBottom - listTop, scroll, maxScroll);
        renderFooter(c);

        // Fade the whole window in and out with the open/close transition.
        c.fill(x - 1, y - 1, x + w + 1, y + h + 1, FlintFixUi.opacity(FlintFixUi.bg(), 1.0f - visibility));
        c.getMatrices().pop();
    }

    private float visibility() {
        float t = Math.max(0.0f, Math.min(1.0f,
            (System.currentTimeMillis() - transitionStartedAt) / (float) TRANSITION_MS));
        float eased = t * t * (3.0f - 2.0f * t);
        return closing ? 1.0f - eased : eased;
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
        int innerX = x + 8;
        int innerW = sidebarW - 16;

        FlintFixUi.surface(c, innerX, y + 9, 22, 22, FlintFixUi.raised(),
            FlintFixUi.blendColors(FlintFixUi.border(), FlintFixUi.accent(), 0.35f));
        c.drawItem(FLINT_PFP, innerX + 3, y + 12);
        FlintFixFont.drawExact(c, "FlintFix", innerX + 28, y + 10, 8, FlintFixUi.text(), true);
        FlintFixFont.drawExact(c, "CLIENT", innerX + 28, y + 21, 6, FlintFixUi.muted(), false);
        FlintFixUi.hairline(c, innerX, y + 38, innerW);

        FlintFixUi.sectionLabel(c, "PROFILES", innerX, y + 43);
        FlintFixUi.searchField(c, innerX, profileSearchY, innerW, 14, profileSearch, "Find profile",
            profileSearchFocused);

        List<String> profiles = visibleProfiles();
        for (int i = 0; i < Math.min(profileRows, profiles.size()); i++) {
            String name = profiles.get(i);
            int rowY = profileRowY(i);
            boolean selected = name.equalsIgnoreCase(FlintFixProfileStore.selectedName());
            boolean hover = FlintFixUi.inside(mouseX, mouseY, innerX, rowY, innerW, PROFILE_ROW_H);
            FlintFixUi.selectableRow(c, "sidebar-profile:" + name, innerX, rowY, innerW, PROFILE_ROW_H, selected, hover);
            FlintFixFont.drawExact(c, FlintFixFont.trim(name, innerW - 14, 6, selected), innerX + 7,
                FlintFixFont.centeredY(rowY, PROFILE_ROW_H, 6), 6,
                selected ? FlintFixUi.text() : FlintFixUi.muted(), selected);
        }
        if (profiles.isEmpty() && profileRows > 0) {
            FlintFixFont.drawExact(c, "No match", innerX + 7, profileListTop + 4, 6, FlintFixUi.subtle(), false);
        }

        FlintFixUi.actionButton(c, innerX, manageY, innerW, 15, "MANAGE",
            FlintFixUi.inside(mouseX, mouseY, innerX, manageY, innerW, 15), FlintFixUi.ButtonStyle.SECONDARY);
        FlintFixUi.hairline(c, innerX, layoutY - 6, innerW);
        FlintFixUi.navButton(c, innerX, layoutY, innerW, 16, "layout", "HUD Layout",
            FlintFixUi.inside(mouseX, mouseY, innerX, layoutY, innerW, 16));
        FlintFixUi.navButton(c, innerX, themesY, innerW, 16, "themes", "Themes",
            FlintFixUi.inside(mouseX, mouseY, innerX, themesY, innerW, 16));
    }

    private void renderHeader(DrawContext c, int mouseX, int mouseY) {
        int active = 0;
        for (int i = 0; i < MODULE_COUNT; i++) if (isModuleEnabled(i)) active++;
        int titleW = Math.max(0, searchX - 6 - contentX);
        FlintFixFont.drawExact(c, FlintFixFont.trim("Modules", titleW, 9, true), contentX, y + 10, 9,
            FlintFixUi.text(), true);
        FlintFixFont.drawExact(c, FlintFixFont.trim(active + " of " + MODULE_COUNT + " active", titleW, 6, false),
            contentX, y + 22, 6, FlintFixUi.muted(), false);

        FlintFixUi.searchField(c, searchX, searchY, searchW, 15, searchText, "Search modules", searchFocused);
        FlintFixUi.iconButton(c, "dashboard-close", closeX, closeY, 15, "close",
            FlintFixUi.inside(mouseX, mouseY, closeX, closeY, 15, 15));
    }

    private void renderFooter(DrawContext c) {
        int footerY = y + h - 15;
        String keyLabel = FlintFixClient.getSettingsKeyLabel().toUpperCase(Locale.ROOT);
        int keyW = FlintFixFont.width(keyLabel, 6, true) + 8;
        String closeHint = "to close";
        int hintW = FlintFixFont.width(closeHint, 6, false);
        int hintX = x + w - 10 - hintW;
        int keyX = hintX - 4 - keyW;
        FlintFixUi.surface(c, keyX, footerY - 2, keyW, 11, FlintFixUi.card(), FlintFixUi.border());
        FlintFixFont.drawCenteredExact(c, keyLabel, keyX + keyW / 2, FlintFixFont.centeredY(footerY - 2, 11, 6), 6,
            FlintFixUi.text(), true);
        FlintFixFont.drawExact(c, closeHint, hintX, FlintFixFont.centeredY(footerY - 2, 11, 6), 6,
            FlintFixUi.subtle(), false);
        FlintFixUi.drawTrimmedExact(c, "Click to toggle  ·  Right-click for options", contentX,
            FlintFixFont.centeredY(footerY - 2, 11, 6), keyX - 8 - contentX, 6, FlintFixUi.subtle(), false);
    }

    private void renderModule(DrawContext c, int index, int cardX, int rowY, int mouseX, int mouseY) {
        boolean enabled = isModuleEnabled(index);
        boolean hover = mouseY >= listTop && mouseY < listBottom
            && FlintFixUi.inside(mouseX, mouseY, cardX, rowY, cardW, ROW_H);
        float hoverT = FlintFixUi.hoverProgress("card:" + index, hover);
        float onT = FlintFixUi.hoverProgress("card-on:" + index, enabled);

        int fill = FlintFixUi.blendColors(FlintFixUi.card(), FlintFixUi.raised(), hoverT * 0.55f);
        fill = FlintFixUi.blendColors(fill, FlintFixUi.accent(), onT * 0.07f);
        int edge = FlintFixUi.blendColors(FlintFixUi.border(), FlintFixUi.accent(), onT * 0.55f);
        edge = FlintFixUi.blendColors(edge, FlintFixUi.accentBright(), hoverT * 0.6f);
        FlintFixUi.surface(c, cardX, rowY, cardW, ROW_H, fill, edge);
        renderRipple(c, index, cardX, rowY, System.currentTimeMillis());

        int tileX = cardX + 6;
        int tileY = rowY + 6;
        FlintFixUi.roundedRaw(c, tileX, tileY, 18, 18, 3,
            FlintFixUi.blendColors(FlintFixUi.raised(), FlintFixUi.accent(), onT));
        FlintFixIcons.drawExact(c, MODULES[index][0], tileX + 3, tileY + 3, 12,
            FlintFixUi.blendColors(FlintFixUi.muted(), FlintFixUi.onAccent(), onT));
        FlintFixUi.switchToggle(c, "module:" + index, cardX + cardW - 26, rowY + 9, enabled);

        FlintFixUi.drawTrimmedExact(c, MODULES[index][1], cardX + 6, rowY + 29, cardW - 12, 7,
            FlintFixUi.text(), true);
        FlintFixUi.drawTrimmedExact(c, MODULES[index][2], cardX + 6, rowY + 40, cardW - 12 - GEAR_SIZE - 2, 6,
            FlintFixUi.muted(), false);

        int gearX = gearX(cardX);
        int gearY = gearY(rowY);
        boolean gearHover = hover && FlintFixUi.inside(mouseX, mouseY, gearX, gearY, GEAR_SIZE, GEAR_SIZE);
        float gearT = FlintFixUi.hoverProgress("card-gear:" + index, gearHover);
        if (gearT > 0.01f) {
            FlintFixUi.roundedRaw(c, gearX, gearY, GEAR_SIZE, GEAR_SIZE, 3,
                FlintFixUi.opacity(FlintFixUi.raised(), gearT));
        }
        FlintFixIcons.drawExact(c, "settings", gearX + 2, gearY + 2, GEAR_SIZE - 4,
            FlintFixUi.blendColors(FlintFixUi.subtle(), FlintFixUi.text(), Math.max(gearT, hoverT * 0.5f)));
    }

    /** Expanding ring anchored at the point of the most recent toggle click. */
    private void renderRipple(DrawContext c, int index, int cardX, int rowY, long now) {
        long started = rippleStartedAt[index];
        if (started == 0L) return;
        float t = Math.min(1.0f, (now - started) / (float) RIPPLE_MS);
        if (t >= 1.0f) return;
        float eased = t * t * (3.0f - 2.0f * t);
        float radius = (float) Math.hypot(cardW, ROW_H) * eased;
        float thickness = Math.max(2.0f, Math.min(5.0f, ROW_H * 0.12f));
        int rgb = (rippleEnabled[index] ? FlintFixUi.accentBright() : FlintFixUi.muted()) & 0x00FFFFFF;
        int alpha = Math.round((1.0f - t) * 70.0f);
        int color = (alpha << 24) | rgb;
        int centerX = cardX + rippleX[index];
        int centerY = rowY + rippleY[index];
        c.enableScissor(Math.max(cardX + 1, contentX - 1), Math.max(rowY + 1, listTop),
            cardX + cardW - 1, Math.min(rowY + ROW_H - 1, listBottom));
        for (int dy = -ROW_H; dy <= ROW_H; dy++) {
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

    private int cardX(int visibleIndex) {
        return contentX + (visibleIndex % columns) * (cardW + GRID_GAP);
    }

    private int cardY(int visibleIndex) {
        return listTop + (visibleIndex / columns) * (ROW_H + GRID_GAP) - scroll;
    }

    private int gearX(int cardX) { return cardX + cardW - GEAR_SIZE - 4; }
    private int gearY(int rowY) { return rowY + ROW_H - GEAR_SIZE - 4; }

    private int profileRowY(int index) {
        return profileListTop + index * (PROFILE_ROW_H + PROFILE_ROW_GAP);
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
            case 13 -> FlintFixClient.CONFIG.skyEnabled;
            case 14 -> FlintFixClient.CONFIG.itemInspectEnabled;
            case 15 -> FlintFixClient.CONFIG.showHandEnabled;
            case 16 -> FlintFixClient.CONFIG.fullbrightEnabled;
            case 17 -> FlintFixClient.CONFIG.potionsEnabled;
            case 18 -> FlintFixClient.CONFIG.speedEnabled;
            case 19 -> FlintFixClient.CONFIG.compassEnabled;
            case 20 -> FlintFixClient.CONFIG.blockOutlineEnabled;
            case 21 -> FlintFixClient.CONFIG.crosshairEnabled;
            case 22 -> FlintFixClient.CONFIG.lowOverlaysEnabled;
            case 23 -> FlintFixClient.CONFIG.damageNumbersEnabled;
            case 24 -> FlintFixClient.CONFIG.hideWeatherEnabled;
            case 25 -> FlintFixClient.CONFIG.motionBlurEnabled;
            case 26 -> FlintFixClient.CONFIG.teammateGlowEnabled;
            case 27 -> FlintFixClient.CONFIG.waypointsEnabled;
            case 28 -> FlintFixProfileStore.serverSwitchingEnabled();
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
            case 13 -> FlintFixClient.CONFIG.skyEnabled = !FlintFixClient.CONFIG.skyEnabled;
            case 14 -> FlintFixClient.CONFIG.itemInspectEnabled = !FlintFixClient.CONFIG.itemInspectEnabled;
            case 15 -> FlintFixClient.CONFIG.showHandEnabled = !FlintFixClient.CONFIG.showHandEnabled;
            case 16 -> FlintFixClient.CONFIG.fullbrightEnabled = !FlintFixClient.CONFIG.fullbrightEnabled;
            case 17 -> FlintFixClient.CONFIG.potionsEnabled = !FlintFixClient.CONFIG.potionsEnabled;
            case 18 -> FlintFixClient.CONFIG.speedEnabled = !FlintFixClient.CONFIG.speedEnabled;
            case 19 -> FlintFixClient.CONFIG.compassEnabled = !FlintFixClient.CONFIG.compassEnabled;
            case 20 -> FlintFixClient.CONFIG.blockOutlineEnabled = !FlintFixClient.CONFIG.blockOutlineEnabled;
            case 21 -> FlintFixClient.CONFIG.crosshairEnabled = !FlintFixClient.CONFIG.crosshairEnabled;
            case 22 -> FlintFixClient.CONFIG.lowOverlaysEnabled = !FlintFixClient.CONFIG.lowOverlaysEnabled;
            case 23 -> FlintFixClient.CONFIG.damageNumbersEnabled = !FlintFixClient.CONFIG.damageNumbersEnabled;
            case 24 -> FlintFixClient.CONFIG.hideWeatherEnabled = !FlintFixClient.CONFIG.hideWeatherEnabled;
            case 25 -> FlintFixClient.CONFIG.motionBlurEnabled = !FlintFixClient.CONFIG.motionBlurEnabled;
            case 26 -> FlintFixClient.CONFIG.teammateGlowEnabled = !FlintFixClient.CONFIG.teammateGlowEnabled;
            case 27 -> FlintFixClient.CONFIG.waypointsEnabled = !FlintFixClient.CONFIG.waypointsEnabled;
            case 28 -> FlintFixProfileStore.setServerSwitching(!FlintFixProfileStore.serverSwitchingEnabled());
            default -> { return; }
        }
        FlintFixClient.CONFIG.save();
    }

    private void openModuleOptions(int moduleIndex) {
        if (client == null) return;
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
            case 13 -> client.setScreen(new FlintFixSkyScreen(this));
            case 14 -> client.setScreen(new FlintFixModuleSettingsScreen(this, FlintFixModuleSettingsScreen.Module.INSPECT));
            case 15 -> client.setScreen(FlintFixModuleOptions.screenFor("showhand", this));
            case 16 -> client.setScreen(new FlintFixModuleSettingsScreen(this, FlintFixModuleSettingsScreen.Module.FULLBRIGHT));
            default -> {
                Screen options = FlintFixModuleOptions.screenFor(MODULES[moduleIndex][0], this);
                if (options != null) client.setScreen(options);
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        layout();
        if (mouseX >= contentX - 4 && mouseX <= contentX + contentW + 6 && mouseY >= listTop && mouseY <= listBottom) {
            scroll = clamp(scroll - (int) Math.round(verticalAmount * (ROW_H + GRID_GAP) / 2.0), 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        layout();
        if (closing || System.currentTimeMillis() - transitionStartedAt < TRANSITION_MS) return true;
        if (button != 0 && button != 1) return super.mouseClicked(mouseX, mouseY, button);

        if (button == 0) {
            if (FlintFixUi.inside(mouseX, mouseY, searchX, searchY, searchW, 15)) {
                searchFocused = true;
                profileSearchFocused = false;
                return true;
            }
            searchFocused = false;
            if (FlintFixUi.inside(mouseX, mouseY, closeX, closeY, 15, 15)) {
                close();
                return true;
            }
            if (handleSidebarClick(mouseX, mouseY)) return true;
        }

        List<Integer> filtered = filteredModules();
        if (mouseY >= listTop && mouseY < listBottom) {
            for (int visibleIndex = 0; visibleIndex < filtered.size(); visibleIndex++) {
                int moduleIndex = filtered.get(visibleIndex);
                int cardX = cardX(visibleIndex);
                int rowY = cardY(visibleIndex);
                if (!FlintFixUi.inside(mouseX, mouseY, cardX, rowY, cardW, ROW_H)) continue;
                boolean onGear = FlintFixUi.inside(mouseX, mouseY, gearX(cardX), gearY(rowY), GEAR_SIZE, GEAR_SIZE);
                if (button == 1 || onGear) {
                    openModuleOptions(moduleIndex);
                } else {
                    toggleModule(moduleIndex);
                    rippleX[moduleIndex] = clamp((int) mouseX - cardX, 0, cardW);
                    rippleY[moduleIndex] = clamp((int) mouseY - rowY, 0, ROW_H);
                    rippleStartedAt[moduleIndex] = System.currentTimeMillis();
                    rippleEnabled[moduleIndex] = isModuleEnabled(moduleIndex);
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleSidebarClick(double mouseX, double mouseY) {
        int innerX = x + 8;
        int innerW = sidebarW - 16;
        if (FlintFixUi.inside(mouseX, mouseY, innerX, profileSearchY, innerW, 14)) {
            profileSearchFocused = true;
            return true;
        }
        profileSearchFocused = false;
        List<String> profiles = visibleProfiles();
        for (int i = 0; i < Math.min(profileRows, profiles.size()); i++) {
            if (FlintFixUi.inside(mouseX, mouseY, innerX, profileRowY(i), innerW, PROFILE_ROW_H)) {
                FlintFixProfileStore.select(profiles.get(i));
                scroll = 0;
                return true;
            }
        }
        if (FlintFixUi.inside(mouseX, mouseY, innerX, manageY, innerW, 15)) {
            if (client != null) client.setScreen(new FlintFixProfileScreen(this));
            return true;
        }
        if (FlintFixUi.inside(mouseX, mouseY, innerX, layoutY, innerW, 16)) {
            if (client != null) client.setScreen(new FlintFixHudEditorScreen(this));
            return true;
        }
        if (FlintFixUi.inside(mouseX, mouseY, innerX, themesY, innerW, 16)) {
            if (client != null) client.setScreen(new FlintFixThemeScreen(this));
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (profileSearchFocused) {
            if (!Character.isISOControl(chr) && profileSearch.length() < 18) profileSearch += chr;
            return true;
        }
        if (!searchFocused) return super.charTyped(chr, modifiers);
        if (!Character.isISOControl(chr) && searchText.length() < 28) {
            searchText += chr;
            scroll = 0;
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
