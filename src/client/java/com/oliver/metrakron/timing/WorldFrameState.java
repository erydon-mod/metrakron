package com.oliver.metrakron.timing;

/** Readiness observations only; no Minecraft progress or timing values. */
record WorldFrameState(boolean worldPresent, boolean playerPresent, boolean screenPresent,
                       boolean pauseScreen, boolean overlayPresent) {
    boolean ready() {
        return worldPresent && playerPresent && !overlayPresent && (!screenPresent || pauseScreen);
    }

    boolean acceptsCallback(boolean screenCallback) {
        // A paused frame can have both HUD and screen callbacks. Only the screen
        // callback owns it, including when the HUD is hidden or not rendered.
        return screenCallback ? screenPresent && pauseScreen : !screenPresent;
    }
}
