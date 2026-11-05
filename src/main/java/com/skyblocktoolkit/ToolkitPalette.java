package com.skyblocktoolkit;

/** Menu colors are independent from the HUD's semantic money/XP colors. */
public record ToolkitPalette(int panel, int sidebar, int surface, int hover, int text, int muted,
        int accent, int accentSurface, int accentTrack, int onAccent, int border, int track,
        int divider, int scrim, int shadow, int error, int moneyText, int xpText) {
    public static final ToolkitPalette DARK = new ToolkitPalette(
        0xFF1B1D21, 0xFF101216, 0xFF24272D, 0xFF30343B, 0xFFF4F5F7, 0xFFADB4BF,
        0xFFFF7089, 0xFF361923, 0xFFE5254A, 0xFFFFFFFF, 0xFF373B44, 0xFF454B56,
        0xFF323640, 0x38090B10, 0x40000000, 0xFFFF9EAD, 0xFF67C982, 0xFF9D9AEB);
    public static final ToolkitPalette LIGHT = new ToolkitPalette(
        0xFFFAFAFC, 0xFFEEEFF3, 0xFFE8E9F0, 0xFFDDDFE8, 0xFF232632, 0xFF596171,
        0xFFAE1939, 0xFFF8DDE4, 0xFFD51F43, 0xFFFFFFFF, 0xFFCCD0DB, 0xFFB5BBC8,
        0xFFD7DAE3, 0x240E1420, 0x26070B14, 0xFFAA2140, 0xFF237142, 0xFF6550B5);
    /** Artwork crimson is structural; text accents use contrast-adjusted shades. */
    public int brand(){return 0xFFE5254A;}
    public int motif(){return this==LIGHT?0xFFE1BCC6:0xFF43202C;}
    public int carbon(){return this==LIGHT?0xFFE5E6EB:0xFF181B20;}
    public int facetLight(){return this==LIGHT?0xFFD47990:0xFF9D344E;}
    public int facetDark(){return this==LIGHT?0xFFE9B7C4:0xFF542133;}
    public static ToolkitPalette of(boolean light) { return light ? LIGHT : DARK; }
}
