package com.oliver.metrakron.timing;

/** Requires several consecutive playable HUD frames before a load can end. */
final class StableFrameGate {
    private final int requiredFrames;
    private int consecutiveFrames;

    StableFrameGate(int requiredFrames) {
        if (requiredFrames < 1) {
            throw new IllegalArgumentException("requiredFrames must be positive");
        }
        this.requiredFrames = requiredFrames;
    }

    boolean observe(boolean playableFrame) {
        if (!playableFrame) {
            reset();
            return false;
        }

        consecutiveFrames++;
        return consecutiveFrames >= requiredFrames;
    }

    void reset() {
        consecutiveFrames = 0;
    }
}
