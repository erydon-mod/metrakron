package com.oliver.metrakron.overlay;

/** Remaining fraction of Metrakron's own learned estimate, never game progress. */
public final class CountdownBar {
    private CountdownBar() { }
    public static double remaining(long elapsedMillis, Long averageMillis) {
        if (averageMillis == null || averageMillis <= 0) return 0;
        return Math.max(0, Math.min(1, 1.0 - (double) Math.max(0, elapsedMillis) / averageMillis));
    }
    public static void draw(BronzeFrameStyle.Fill fill, int x, int y, int width, long elapsed, Long average, AppearanceSettings style) {
        fill.accept(x, y, x + width, y + 3, style.darkText() ? 0x55302618 : 0x55FFFFFF);
        int remaining = (int) Math.round(width * remaining(elapsed, average));
        if (remaining > 0) fill.accept(x, y, x + remaining, y + 3, style.accentColor());
    }
}
