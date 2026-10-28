package com.skyblocktoolkit;

public final class LevelRendererResizeHelper {
    private LevelRendererResizeHelper() {}

    /**
     * Decision check for window resize events.
     * Rejects zero/negative dimensions (minimized/unmapped during Alt-Tab)
     * and identical dimensions (redundant X11/Xwayland focus events).
     */
    public static boolean shouldSkipResize(int width, int height, int lastWidth, int lastHeight) {
        if (width <= 0 || height <= 0) return true;
        return width == lastWidth && height == lastHeight;
    }
}
