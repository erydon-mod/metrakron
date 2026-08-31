package com.oliver.metrakron.overlay;

import com.oliver.metrakron.timing.TimeFormatter;

public final class OverlayClock {
    private static final long NANOS_PER_MILLISECOND = 1_000_000L;

    private OverlayClock() {
    }

    public static long elapsedMillis(long publishedElapsedMillis, long anchorNanos, long nowNanos) {
        long elapsedSinceAnchor = Math.max(0L, nowNanos - anchorNanos) / NANOS_PER_MILLISECOND;
        if (Long.MAX_VALUE - publishedElapsedMillis < elapsedSinceAnchor) {
            return Long.MAX_VALUE;
        }
        return Math.max(0L, publishedElapsedMillis) + elapsedSinceAnchor;
    }

    public static long displayedMillis(long elapsedMillis, Long averageMillis) {
        if (averageMillis == null) {
            return Math.max(0L, elapsedMillis);
        }
        return isRelearning(elapsedMillis, averageMillis)
                ? Math.max(0L, elapsedMillis - averageMillis)
                : Math.max(0L, averageMillis - elapsedMillis);
    }

    public static String displayedTime(long elapsedMillis, Long averageMillis) {
        long displayedMillis = displayedMillis(elapsedMillis, averageMillis);
        if (averageMillis == null) {
            return TimeFormatter.format(displayedMillis);
        }
        if (isRelearning(elapsedMillis, averageMillis)) {
            return '+' + TimeFormatter.format(displayedMillis);
        }
        return TimeFormatter.formatCountdown(displayedMillis);
    }

    public static boolean isRelearning(long elapsedMillis, Long averageMillis) {
        return averageMillis != null && elapsedMillis > averageMillis;
    }
}
