package com.oliver.metrakron.ui;

import com.oliver.metrakron.MetrakronClient;
import com.oliver.metrakron.overlay.AppearanceSettings;
import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.nio.file.Path;

public final class AppearanceConfig {
    private AppearanceConfig() { }
    public static Path path() { return FabricLoader.getInstance().getConfigDir().resolve("metrakron/appearance.properties"); }
    public static void load() {
        try { AppearanceSettings.preview(AppearanceSettings.read(path())); }
        catch (IOException | IllegalArgumentException e) { MetrakronClient.LOGGER.warn("Unable to read Metrakron appearance; using defaults", e); }
    }
}
