package com.oliver.metrakron.overlay;

/** Remaining fraction of Metrakron's own learned estimate, never game progress. */
public final class CountdownBar {
    private CountdownBar() { }
    public static double remaining(long elapsedMillis, Long averageMillis) {
        if (averageMillis == null || averageMillis <= 0) return 0;
        return Math.max(0, Math.min(1, 1.0 - (double) Math.max(0, elapsedMillis) / averageMillis));
    }
    public static void draw(BronzeFrameStyle.Fill fill, int x, int y, int width, long elapsed, Long average, AppearanceSettings style) {
        if (width < 5) return;
        // ERYDON-style recessed track, fine metal inlay and narrow bevelled marker.
        fill.accept(x, y, x + width, y + 5, 0xDD100E0B);
        fill.accept(x, y, x + width, y + 1, style.metalColor(0xFF60401F));
        fill.accept(x, y + 4, x + width, y + 5, style.metalColor(0xFFCFAC73));
        fill.accept(x, y, x + 1, y + 5, style.metalColor(0xFF60401F));
        fill.accept(x + width - 1, y, x + width, y + 5, style.metalColor(0xFFCFAC73));
        int remaining = (int) Math.round((width - 4) * remaining(elapsed, average));
        if (remaining > 0) {
            int end = x + 2 + remaining;
            fill.accept(x + 2, y + 2, end, y + 3, style.metalColor(0xFFD5A354));
            fill.accept(end - 1, y + 1, end, y + 4, style.metalColor(0xFFE6C895));
        }
    }
}
