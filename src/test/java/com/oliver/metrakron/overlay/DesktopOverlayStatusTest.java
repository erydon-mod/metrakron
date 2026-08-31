package com.oliver.metrakron.overlay;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DesktopOverlayStatusTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void acknowledgesOnlyAPaintedCurrentAndFreshPresentation() throws Exception {
        long now = 50_000L;
        DesktopOverlayStatus status = status(now - 250L, true, true, 8L, 8L, 3L, 42L);

        assertTrue(status.acknowledges("session", 100L, 200L, 8L, 3L, now, 2_500L));
        assertFalse(status.acknowledges("other", 100L, 200L, 8L, 3L, now, 2_500L));
        assertFalse(status.acknowledges("session", 100L, 200L, 9L, 3L, now, 2_500L));
        assertFalse(status.acknowledges("session", 100L, 200L, 8L, 4L, now, 2_500L));
    }

    @Test
    void rejectsInvisibleUnpaintedAndStaleHelpers() {
        long now = 50_000L;

        assertFalse(status(now, true, false, 8L, 8L, 3L, 42L)
                .acknowledges("session", 100L, 200L, 8L, 3L, now, 2_500L));
        assertFalse(status(now, true, true, 8L, 7L, 3L, 42L)
                .acknowledges("session", 100L, 200L, 8L, 3L, now, 2_500L));
        assertFalse(status(now - 2_501L, true, true, 8L, 8L, 3L, 42L)
                .acknowledges("session", 100L, 200L, 8L, 3L, now, 2_500L));
        assertFalse(status(now, true, true, 8L, 8L, 3L, 0L)
                .acknowledges("session", 100L, 200L, 8L, 3L, now, 2_500L));
    }

    @Test
    void acknowledgesOnlyACurrentFreshHiddenWindow() {
        long now = 50_000L;
        DesktopOverlayStatus hidden = status(now - 250L, false, false, 9L, 8L, 0L, 42L);

        assertTrue(hidden.acknowledgesHidden("session", 100L, 200L, 9L, now, 2_500L));
        assertFalse(status(now, false, true, 9L, 8L, 0L, 42L)
                .acknowledgesHidden("session", 100L, 200L, 9L, now, 2_500L));
        assertFalse(hidden.acknowledgesHidden("session", 100L, 200L, 10L, now, 2_500L));
        assertFalse(status(now - 2_501L, false, false, 9L, 8L, 0L, 42L)
                .acknowledgesHidden("session", 100L, 200L, 9L, now, 2_500L));
    }

    @Test
    void readsTheAtomicHelperStatusFormat() throws Exception {
        Path file = temporaryDirectory.resolve("window-overlay-status.properties");
        Properties properties = new Properties();
        properties.setProperty("processState", "running");
        properties.setProperty("sessionId", "session");
        properties.setProperty("helperPid", "200");
        properties.setProperty("parentPid", "100");
        properties.setProperty("revision", "8");
        properties.setProperty("paintedRevision", "8");
        properties.setProperty("active", "true");
        properties.setProperty("visible", "true");
        properties.setProperty("sequence", "3");
        properties.setProperty("framesRendered", "42");
        properties.setProperty("updatedAtEpochMillis", "49750");
        OverlayStateFile.writeProperties(file, properties, "test helper status");

        DesktopOverlayStatus status = DesktopOverlayStatus.read(file);

        assertTrue(status.acknowledges("session", 100L, 200L, 8L, 3L, 50_000L, 2_500L));
    }

    private static DesktopOverlayStatus status(
            long updatedAt,
            boolean active,
            boolean visible,
            long revision,
            long paintedRevision,
            long sequence,
            long framesRendered
    ) {
        return new DesktopOverlayStatus(
                "running",
                "session",
                200L,
                100L,
                revision,
                paintedRevision,
                active,
                visible,
                sequence,
                framesRendered,
                updatedAt
        );
    }
}
