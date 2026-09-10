package com.oliver.metrakron.overlay;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Presentation only: never included in timing histories or profile fingerprints. */
public record AppearanceSettings(Stone stone, Typeface font, Metal metal) {
    public enum Stone { NERIUM, AGANITE, GLACIUM, KELASTRION, SELENEPHOS }
    public enum Typeface { CINZEL, LATO, SPACEMONO }
    public enum Metal { BRONZE, SILVER }
    public static final AppearanceSettings DEFAULT = new AppearanceSettings(Stone.NERIUM, Typeface.CINZEL, Metal.BRONZE);
    private static volatile AppearanceSettings current = DEFAULT;
    public static AppearanceSettings current() { return current; }
    public static void preview(AppearanceSettings settings) { current = settings; }
    public String fontId() { return font.name().toLowerCase(java.util.Locale.ROOT); }
    public String stoneResource() {
        String suffix = stone == Stone.KELASTRION || stone == Stone.SELENEPHOS ? "_quiet_panel.png" : "_panel.png";
        return "/assets/metrakron/textures/gui/" + stone.name().toLowerCase(java.util.Locale.ROOT) + suffix;
    }
    // Original stone colours remain untouched. Opposing shadows protect text over veins.
    public boolean darkText() { return stone == Stone.SELENEPHOS || stone == Stone.AGANITE || stone == Stone.GLACIUM; }
    public int shadowColor() { return darkText() ? 0xEEFFFFFF : 0xEE000000; }
    public int textColor() { return darkText() ? 0xFF111111 : 0xFFFFFFFF; }
    public int accentColor() {
        if (darkText()) return 0xFF302618;
        return metal == Metal.SILVER ? 0xFFE2E8ED : 0xFFFFC65A;
    }
    public int metalColor(int bronze) {
        if (metal == Metal.BRONZE) return bronze;
        int level = Math.min(255, (int) (((bronze >> 16 & 255) * .299 + (bronze >> 8 & 255) * .587 + (bronze & 255) * .114) * 1.18));
        return (bronze & 0xFF000000) | level << 16 | level << 8 | Math.min(255, level + 6);
    }
    public static AppearanceSettings read(Path file) throws IOException {
        if (!Files.exists(file)) return DEFAULT;
        Properties p = new Properties();
        try (var input = Files.newInputStream(file)) { p.load(input); }
        return new AppearanceSettings(value(Stone.class, p.getProperty("stone"), DEFAULT.stone),
                value(Typeface.class, p.getProperty("font"), DEFAULT.font),
                value(Metal.class, p.getProperty("metal"), DEFAULT.metal));
    }
    private static <E extends Enum<E>> E value(Class<E> type, String name, E fallback) {
        try { return Enum.valueOf(type, name); } catch (IllegalArgumentException | NullPointerException ignored) { return fallback; }
    }
    public void save(Path file) throws IOException {
        Properties p = new Properties();
        p.setProperty("stone", stone.name()); p.setProperty("font", font.name()); p.setProperty("metal", metal.name());
        OverlayStateFile.writeProperties(file, p, "ERYDON Metrakron appearance");
    }
}
