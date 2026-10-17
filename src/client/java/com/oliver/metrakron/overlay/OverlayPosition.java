package com.oliver.metrakron.overlay;

/** Anchors within Minecraft's client area, not the entire desktop. */
public enum OverlayPosition {
    TOP_LEFT(0, 0), TOP_CENTRE(1, 0), TOP_RIGHT(2, 0),
    RIGHT_CENTRE(2, 1), BOTTOM_RIGHT(2, 2), BOTTOM_CENTRE(1, 2),
    BOTTOM_LEFT(0, 2), LEFT_CENTRE(0, 1);

    private final int horizontal;
    private final int vertical;

    OverlayPosition(int horizontal, int vertical) {
        this.horizontal = horizontal;
        this.vertical = vertical;
    }

    public Bounds place(int areaWidth, int areaHeight, int panelWidth, int panelHeight, int margin) {
        areaWidth = Math.max(1, areaWidth);
        areaHeight = Math.max(1, areaHeight);
        int width = Math.max(1, Math.min(panelWidth, areaWidth));
        int height = Math.max(1, Math.min(panelHeight, areaHeight));
        return new Bounds(offset(areaWidth - width, margin, horizontal),
                offset(areaHeight - height, margin, vertical), width, height);
    }

    private static int offset(int space, int margin, int alignment) {
        int inset = Math.min(Math.max(0, margin), space / 2);
        return alignment == 0 ? inset : alignment == 1 ? space / 2 : space - inset;
    }

    public record Bounds(int x, int y, int width, int height) { }
}
