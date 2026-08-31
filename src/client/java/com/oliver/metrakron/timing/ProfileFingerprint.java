package com.oliver.metrakron.timing;

import java.util.Objects;

public record ProfileFingerprint(String id, String summary) {
    public ProfileFingerprint {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(summary, "summary");
    }
}
