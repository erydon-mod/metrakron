package com.oliver.metrakron.overlay;

/**
 * Shared logical-pixel recipe for the recessed, top-left-lit ERYDON bronze inlay.
 */
public final class BronzeFrameStyle {
    public static final int CONTENT_INSET = 5;

    public static final int OUTER_GROOVE = 0xFF100F0B;
    public static final int METAL_BASE = 0xFFC2A06A;
    public static final int TOP_EDGE = 0xFFEAD9B1;
    public static final int LEFT_EDGE = 0xFFCAB381;
    public static final int BOTTOM_EDGE = 0xFF957140;
    public static final int RIGHT_EDGE = 0xFF6E522F;
    public static final int INNER_TOP = 0xFF41331F;
    public static final int INNER_LEFT = 0xFF574329;
    public static final int INNER_BOTTOM = 0xFFEFDCA9;
    public static final int INNER_RIGHT = 0xFFD0B580;
    public static final int TOP_GLINT = 0xFFFFF1CD;
    public static final int LEFT_GLINT = 0xFFF0DFB8;
    public static final int INNER_GROOVE = 0xFF0A0906;
    public static final int INNER_REFLECTION = 0xFFA18B61;

    private BronzeFrameStyle() {
    }

    public static void draw(Fill fill, int x, int y, int width, int height) {
        if (width < CONTENT_INSET * 2 || height < CONTENT_INSET * 2) {
            return;
        }

        // The outer dark lip seats the bronze into the surrounding screen.
        fill.accept(x, y, x + width, y + height, OUTER_GROOVE);
        fill.accept(x + 1, y + 1, x + width - 1, y + height - 1, METAL_BASE);

        // Directional edge light: illumination arrives from above and slightly left.
        fill.accept(x + 1, y + 1, x + width - 1, y + 2, TOP_EDGE);
        fill.accept(x + 1, y + 1, x + 2, y + height - 1, LEFT_EDGE);
        fill.accept(x + 1, y + height - 2, x + width - 1, y + height - 1, BOTTOM_EDGE);
        fill.accept(x + width - 2, y + 1, x + width - 1, y + height - 1, RIGHT_EDGE);

        // The second falloff gives the narrow strip a polished chamfer.
        fill.accept(x + 3, y + 3, x + width - 3, y + 4, INNER_TOP);
        fill.accept(x + 3, y + 3, x + 4, y + height - 3, INNER_LEFT);
        fill.accept(x + 3, y + height - 4, x + width - 3, y + height - 3, INNER_BOTTOM);
        fill.accept(x + width - 4, y + 3, x + width - 3, y + height - 3, INNER_RIGHT);

        // Short, restrained catches keep the metal reflective without returning to flat gold.
        fill.accept(
                x + percentage(width, 16),
                y + 1,
                x + percentage(width, 43),
                y + 2,
                TOP_GLINT
        );
        fill.accept(
                x + 1,
                y + percentage(height, 10),
                x + 2,
                y + percentage(height, 32),
                LEFT_GLINT
        );

        // Reversed inner shading makes the stone centre read as a true recess.
        fill.accept(x + 4, y + 4, x + width - 4, y + height - 4, INNER_GROOVE);
        fill.accept(
                x + 4,
                y + height - 5,
                x + width - 4,
                y + height - 4,
                INNER_REFLECTION
        );
        fill.accept(
                x + width - 5,
                y + 4,
                x + width - 4,
                y + height - 4,
                INNER_REFLECTION
        );
    }

    private static int percentage(int value, int percentage) {
        return value * percentage / 100;
    }

    @FunctionalInterface
    public interface Fill {
        void accept(int left, int top, int right, int bottom, int argb);
    }
}
