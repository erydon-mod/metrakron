package com.oliver.metrakron.timing;

import java.util.Locale;

public final class TimeFormatter {
    private TimeFormatter() {
    }

    public static String format(long milliseconds) {
        long totalSeconds = Math.max(0L, milliseconds) / 1_000L;
        return formatSeconds(totalSeconds);
    }

    public static String formatCountdown(long milliseconds) {
        long clamped = Math.max(0L, milliseconds);
        long totalSeconds = clamped == 0L ? 0L : ((clamped - 1L) / 1_000L) + 1L;
        return formatSeconds(totalSeconds);
    }

    private static String formatSeconds(long totalSeconds) {
        long seconds = totalSeconds % 60L;
        long totalMinutes = totalSeconds / 60L;

        if (totalMinutes < 60L) {
            return String.format(Locale.ROOT, "%02d:%02d", totalMinutes, seconds);
        }

        long minutes = totalMinutes % 60L;
        long hours = totalMinutes / 60L;
        return String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds);
    }
}
