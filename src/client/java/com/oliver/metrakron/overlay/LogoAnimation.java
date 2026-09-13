package com.oliver.metrakron.overlay;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/** Presentation clock and atlas geometry, shared by the independent and game displays. */
public final class LogoAnimation {
    public static final String RESOURCE = "/assets/metrakron/textures/gui/portorium_logo.png";
    public static final int FRAME_WIDTH = 256;
    public static final int FRAME_HEIGHT = 76;
    public static final int COLUMNS = 8;
    public static final int FRAMES = 174;
    public static final long FRAME_NANOS = 50_000_000L;
    private static final long START = System.nanoTime();

    private LogoAnimation() { }

    public static int frame(long nowNanos) {
        return (int) Math.floorMod(Math.floorDiv(nowNanos - START, FRAME_NANOS), FRAMES);
    }

    public static void draw(Graphics2D graphics, BufferedImage atlas, int x, int y,
                            int width, int height, long nowNanos) {
        if (atlas == null) return;
        int frame = frame(nowNanos);
        int u = frame % COLUMNS * FRAME_WIDTH;
        int v = frame / COLUMNS * FRAME_HEIGHT;
        graphics.drawImage(atlas, x, y, x + width, y + height,
                u, v, u + FRAME_WIDTH, v + FRAME_HEIGHT, null);
    }
}
