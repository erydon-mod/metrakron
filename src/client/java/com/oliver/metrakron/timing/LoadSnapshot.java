package com.oliver.metrakron.timing;

public record LoadSnapshot(
        boolean active,
        LoadStage stage,
        long sequence,
        long elapsedMillis,
        Long averageMillis,
        LoadActivity activity
) {
    public static LoadSnapshot inactive(LoadStage stage) {
        return new LoadSnapshot(false, stage, 0L, 0L, null, defaultActivity(stage));
    }

    private static LoadActivity defaultActivity(LoadStage stage) {
        return switch (stage) {
            case STARTUP -> LoadActivity.STARTUP_RESOURCES;
            case QUICK_PLAY -> LoadActivity.QUICK_PLAY_RESOURCES;
            case WORLD_READING -> LoadActivity.WORLD_DATA;
            case WORLD_GENERATION -> LoadActivity.WORLD_GENERATION;
            case WORLD_JOINING -> LoadActivity.WORLD_JOINING;
            case WORLD_FIRST_FRAME -> LoadActivity.WORLD_FIRST_FRAME;
        };
    }
}
