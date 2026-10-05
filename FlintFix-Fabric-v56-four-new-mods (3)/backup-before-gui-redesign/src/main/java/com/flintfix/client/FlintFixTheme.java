package com.flintfix.client;

/** Color palettes used by FlintFix's in-game client screens. */
public enum FlintFixTheme {
    GRAPHITE("Graphite", "Balanced neutral gray", 0xFF10151D, 0xFF171E28, 0xFF1E2024, 0xFF303239,
        0xFF414141, 0xFF454545, 0xFFB0B0B0, 0xFFD4D4D4, 0xFFF1F2F4, 0xFFA4A4A4),
    LIGHT("Light", "Soft gray with dark edges", 0xFFE2E5E8, 0xFFECEEF0, 0xFFF7F8F9, 0xFFD9DDE1,
        0xFF49515B, 0xFF7D858F, 0xFF394653, 0xFF202832, 0xFF151A20, 0xFF4D5661),
    MIDNIGHT("Midnight", "Deep blue slate", 0xFF0D1119, 0xFF121A25, 0xFF182333, 0xFF223248,
        0xFF2A3A50, 0xFF34465E, 0xFF6F9FE8, 0xFF9FC0FA, 0xFFEAF1FC, 0xFF9AAAC0),
    FOREST("Forest", "Muted evergreen", 0xFF101713, 0xFF17211B, 0xFF1D2A22, 0xFF293A2F,
        0xFF304638, 0xFF405A48, 0xFF69A77F, 0xFF9BD3AA, 0xFFEAF4EC, 0xFFA0B2A4),
    VIOLET("Violet", "Soft purple slate", 0xFF14121A, 0xFF1D1925, 0xFF282130, 0xFF362C43,
        0xFF40334F, 0xFF514161, 0xFF9D82C8, 0xFFC2A9E8, 0xFFF2EDF9, 0xFFB2A7C2);

    private final String label;
    private final String description;
    private final int background;
    private final int panel;
    private final int card;
    private final int raised;
    private final int border;
    private final int divider;
    private final int accent;
    private final int accentBright;
    private final int text;
    private final int muted;

    FlintFixTheme(String label, String description, int background, int panel, int card, int raised,
                  int border, int divider, int accent, int accentBright, int text, int muted) {
        this.label = label;
        this.description = description;
        this.background = background;
        this.panel = panel;
        this.card = card;
        this.raised = raised;
        this.border = border;
        this.divider = divider;
        this.accent = accent;
        this.accentBright = accentBright;
        this.text = text;
        this.muted = muted;
    }

    public String id() { return name(); }
    public String label() { return label; }
    public String description() { return description; }
    public int background() { return background; }
    public int panel() { return panel; }
    public int card() { return card; }
    public int raised() { return raised; }
    public int border() { return border; }
    public int divider() { return divider; }
    public int accent() { return accent; }
    public int accentBright() { return accentBright; }
    public int text() { return text; }
    public int muted() { return muted; }

    public static FlintFixTheme fromId(String id) {
        if (id == null) return GRAPHITE;
        try {
            return valueOf(id.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return GRAPHITE;
        }
    }
}
