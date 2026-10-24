package com.oliver.metrakron.ui;

/** Purpose-sized menu emblem; independent of loading measurements and the wide logo. */
final class EmblemAnimation {
    static final String RESOURCE = "/assets/metrakron/textures/gui/portorium_emblem.png";
    static final int FRAME_SIZE = 64;
    static final int FRAMES = 40;
    static final long FRAME_NANOS = 50_000_000L;
    private static final long START = System.nanoTime();

    private EmblemAnimation() { }

    static int frame(long nowNanos) {
        return (int) Math.floorMod(Math.floorDiv(nowNanos - START, FRAME_NANOS), FRAMES);
    }
}
