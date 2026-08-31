package com.oliver.metrakron.overlay;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OverlayClockTest {
    @Test
    void independentClockKeepsAdvancingWithoutAnotherPublishedState() {
        long anchor = 10_000_000_000L;

        assertEquals(250L, OverlayClock.elapsedMillis(250L, anchor, anchor));
        assertEquals(2_250L, OverlayClock.elapsedMillis(250L, anchor, anchor + 2_000_000_000L));
        assertEquals("00:02", OverlayClock.displayedTime(2_250L, null));
    }

    @Test
    void learnedCountdownRoundsUpThenBecomesRelearningOvertime() {
        assertEquals("00:08", OverlayClock.displayedTime(2_250L, 10_000L));
        assertEquals("00:00", OverlayClock.displayedTime(10_000L, 10_000L));
        assertEquals("+00:00", OverlayClock.displayedTime(10_001L, 10_000L));
        assertEquals("+00:02", OverlayClock.displayedTime(12_000L, 10_000L));
        assertEquals(false, OverlayClock.isRelearning(10_000L, 10_000L));
        assertEquals(true, OverlayClock.isRelearning(10_001L, 10_000L));
    }
}
