package com.oliver.metrakron.overlay;

/** Aspect-preserving stone sampling for the differently sized menu and loading panels. */
public record StoneCrop(int x, int y, int width, int height) {
    public static StoneCrop fit(int imageWidth, int imageHeight, int panelWidth, int panelHeight,
                                AppearanceSettings.Stone stone) {
        int limit = stone == AppearanceSettings.Stone.NERIUM ? 640
                : stone == AppearanceSettings.Stone.PORTORIUM ? 676 : imageWidth;
        double scale = Math.min(Math.min(imageWidth, limit) / (double) Math.max(1, panelWidth),
                imageHeight / (double) Math.max(1, panelHeight));
        int w = Math.max(1, Math.min(imageWidth, (int) Math.round(panelWidth * scale)));
        int h = Math.max(1, Math.min(imageHeight, (int) Math.round(panelHeight * scale)));
        return new StoneCrop((imageWidth - w) / 2, (imageHeight - h) / 2, w, h);
    }
}
