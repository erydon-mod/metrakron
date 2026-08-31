package com.oliver.metrakron.overlay;

import java.util.Objects;

public record OverlayState(
        int protocolVersion,
        String sessionId,
        long parentPid,
        long revision,
        boolean active,
        long sequence,
        long elapsedMillis,
        Long averageMillis,
        String heading,
        String relearningHeading,
        String detail,
        int x,
        int y,
        int width,
        int height
) {
    public static final int CURRENT_PROTOCOL = 2;

    public OverlayState {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(heading, "heading");
        Objects.requireNonNull(relearningHeading, "relearningHeading");
        Objects.requireNonNull(detail, "detail");
    }

    public static OverlayState inactive(
            String sessionId,
            long parentPid,
            long revision,
            int x,
            int y,
            int width,
            int height
    ) {
        return new OverlayState(
                CURRENT_PROTOCOL,
                sessionId,
                parentPid,
                revision,
                false,
                0L,
                0L,
                null,
                "",
                "",
                "",
                x,
                y,
                width,
                height
        );
    }
}
