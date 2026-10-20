package com.skyblocktoolkit;

/** Menu colors are independent from the HUD's semantic money/XP colors. */
public record ToolkitPalette(int panel, int sidebar, int surface, int hover, int text, int muted,
        int accent, int accentSurface, int accentTrack, int onAccent, int border, int track,
        int divider, int scrim, int shadow, int error, int moneyText, int xpText) {
    public static final ToolkitPalette DARK = new ToolkitPalette(
        0xFF1B1D22, 0xFF15171B, 0xFF25282E, 0xFF30343B, 0xFFE4E7EC, 0xFFA3AAB5,
        0xFFE6647A, 0xFF391E28, 0xFFC33D56, 0xFFFFF7F9, 0xFF373B44, 0xFF454B56,
        0xFF323640, 0x38090B10, 0x40000000, 0xFFFF9EAD, 0xFF67C982, 0xFF9D9AEB);
    public static final ToolkitPalette LIGHT = new ToolkitPalette(
        0xFFF9F9FC, 0xFFF0F0F5, 0xFFE8E9F0, 0xFFDDDFE8, 0xFF232632, 0xFF596171,
        0xFFB41F42, 0xFFF8DDE4, 0xFFBF2949, 0xFFFFF7F9, 0xFFCCD0DB, 0xFFB5BBC8,
        0xFFD7DAE3, 0x240E1420, 0x26070B14, 0xFFAA2140, 0xFF237142, 0xFF6550B5);
    public static ToolkitPalette of(boolean light) { return light ? LIGHT : DARK; }
}
