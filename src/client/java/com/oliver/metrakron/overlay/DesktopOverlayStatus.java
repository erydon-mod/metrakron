package com.oliver.metrakron.overlay;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

record DesktopOverlayStatus(
        String processState,
        String sessionId,
        long helperPid,
        long parentPid,
        long revision,
        long paintedRevision,
        boolean active,
        boolean visible,
        long sequence,
        long framesRendered,
        long updatedAtEpochMillis
) {
    static DesktopOverlayStatus read(Path file) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            properties.load(input);
        }
        return new DesktopOverlayStatus(
                required(properties, "processState"),
                required(properties, "sessionId"),
                parseLong(properties, "helperPid"),
                parseLong(properties, "parentPid"),
                parseLong(properties, "revision"),
                parseLong(properties, "paintedRevision"),
                Boolean.parseBoolean(required(properties, "active")),
                Boolean.parseBoolean(required(properties, "visible")),
                parseLong(properties, "sequence"),
                parseLong(properties, "framesRendered"),
                parseLong(properties, "updatedAtEpochMillis")
        );
    }

    boolean acknowledges(
            String expectedSessionId,
            long expectedParentPid,
            long expectedHelperPid,
            long expectedRevision,
            long expectedSequence,
            long nowEpochMillis,
            long maximumAgeMillis
    ) {
        return matchesIdentity(expectedSessionId, expectedParentPid, expectedHelperPid)
                && "running".equals(processState)
                && active
                && visible
                && sequence == expectedSequence
                && revision >= expectedRevision
                && paintedRevision >= expectedRevision
                && framesRendered > 0L
                && isFresh(nowEpochMillis, maximumAgeMillis);
    }

    boolean acknowledgesHidden(
            String expectedSessionId,
            long expectedParentPid,
            long expectedHelperPid,
            long expectedRevision,
            long nowEpochMillis,
            long maximumAgeMillis
    ) {
        return matchesIdentity(expectedSessionId, expectedParentPid, expectedHelperPid)
                && "running".equals(processState)
                && !active
                && !visible
                && revision >= expectedRevision
                && isFresh(nowEpochMillis, maximumAgeMillis);
    }

    boolean matchesIdentity(String expectedSessionId, long expectedParentPid, long expectedHelperPid) {
        return sessionId.equals(expectedSessionId)
                && parentPid == expectedParentPid
                && helperPid == expectedHelperPid;
    }

    boolean isFresh(long nowEpochMillis, long maximumAgeMillis) {
        long ageMillis = Math.max(0L, nowEpochMillis - updatedAtEpochMillis);
        return ageMillis <= maximumAgeMillis;
    }

    private static String required(Properties properties, String key) throws IOException {
        String value = properties.getProperty(key);
        if (value == null) {
            throw new IOException("Missing desktop overlay status field " + key);
        }
        return value;
    }

    private static long parseLong(Properties properties, String key) throws IOException {
        try {
            return Long.parseLong(required(properties, key));
        } catch (NumberFormatException exception) {
            throw new IOException("Invalid desktop overlay status field " + key, exception);
        }
    }
}
