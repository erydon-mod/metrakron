package com.oliver.metrakron.timing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimeFormatterTest {
    @Test
    void formatsWholeElapsedSeconds() {
        assertEquals("00:00", TimeFormatter.format(0L));
        assertEquals("00:12", TimeFormatter.format(12_999L));
        assertEquals("01:01", TimeFormatter.format(61_000L));
        assertEquals("1:02:03", TimeFormatter.format(3_723_999L));
    }

    @Test
    void roundsPositiveCountdownsUpToTheNextWholeSecond() {
        assertEquals("00:00", TimeFormatter.formatCountdown(0L));
        assertEquals("00:01", TimeFormatter.formatCountdown(1L));
        assertEquals("00:01", TimeFormatter.formatCountdown(1_000L));
        assertEquals("00:02", TimeFormatter.formatCountdown(1_001L));
        assertEquals("01:01", TimeFormatter.formatCountdown(60_001L));
    }
}
