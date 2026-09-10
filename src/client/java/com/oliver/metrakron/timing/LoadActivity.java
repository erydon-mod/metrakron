package com.oliver.metrakron.timing;

public enum LoadActivity {
    STARTUP_RESOURCES(
            "text.metrakron.activity.startup_resources",
            "RESOURCE RELOAD · MODELS · TEXTURES · FONTS"
    ),
    QUICK_PLAY_RESOURCES(
            "text.metrakron.activity.quick_play_resources",
            "QUICK PLAY · MODELS · TEXTURES · FONTS"
    ),
    WORLD_OPENING(
            "text.metrakron.activity.world_opening",
            "SAVE OPEN · LEVEL DATA · REGISTRIES · DATA PACKS"
    ),
    WORLD_DATA(
            "text.metrakron.activity.world_data",
            "SERVER DATA · REGISTRIES · TAGS · RECIPES"
    ),
    WORLD_GENERATION(
            "text.metrakron.activity.world_generation",
            "SPAWN PREP · CHUNKS · TERRAIN · LIGHTING"
    ),
    WORLD_JOINING(
            "text.metrakron.activity.world_joining",
            "CLIENT SYNC · CHUNKS · PLAYER STATE"
    ),
    WORLD_FIRST_FRAME(
            "text.metrakron.activity.world_first_frame",
            "FIRST FRAME · CHUNK MESHES · TEXTURES · HUD"
    );

    private final String translationKey;
    private final String fallback;

    LoadActivity(String translationKey, String fallback) {
        this.translationKey = translationKey;
        this.fallback = fallback;
    }

    public String translationKey() {
        return translationKey;
    }

    public String fallback() {
        return fallback;
    }
}
