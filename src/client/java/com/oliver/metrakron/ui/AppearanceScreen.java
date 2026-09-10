package com.oliver.metrakron.ui;

import com.oliver.metrakron.MetrakronClient;
import com.oliver.metrakron.overlay.AppearanceSettings;
import com.oliver.metrakron.overlay.BronzeFrameStyle;
import com.oliver.metrakron.timing.LoadSnapshot;
import com.oliver.metrakron.timing.LoadStage;
import com.oliver.metrakron.timing.LoadActivity;
//? if >=26.1 {
/*import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
*///?} else {
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
//? if >=1.20 {
import net.minecraft.client.gui.DrawContext;
//?} else {
/*import net.minecraft.client.util.math.MatrixStack;
*///?}
//? if <1.19 {
/*import net.minecraft.text.TranslatableText;
*///?}
//?}

/** A deliberately small, keyboard-accessible appearance editor. Escape cancels. */
public final class AppearanceScreen extends Screen {
    private final Screen parent;
    private final AppearanceSettings original;
    private AppearanceSettings draft;
    private boolean saved;
    private boolean saveFailed;
    private final java.util.List<Runnable> refreshLabels = new java.util.ArrayList<>();

    public AppearanceScreen(Screen parent) {
        super(t("metrakron.appearance.title"));
        this.parent = parent;
        original = AppearanceSettings.current();
        draft = original;
    }

    @Override protected void init() {
        refreshLabels.clear();
        int x = width / 2 - 108;
        int y = Math.max(8, (height - 226) / 2) + 103;
        button(x, y, 216, "stone", () -> {
            var values = AppearanceSettings.Stone.values();
            draft = new AppearanceSettings(values[(draft.stone().ordinal() + 1) % values.length], draft.font(), draft.metal());
        });
        button(x, y + 24, 216, "font", () -> {
            var values = AppearanceSettings.Typeface.values();
            draft = new AppearanceSettings(draft.stone(), values[(draft.font().ordinal() + 1) % values.length], draft.metal());
        });
        button(x, y + 48, 216, "metal", () -> {
            var values = AppearanceSettings.Metal.values();
            draft = new AppearanceSettings(draft.stone(), draft.font(), values[(draft.metal().ordinal() + 1) % values.length]);
        });
        button(x, y + 76, 104, "reset", () -> draft = AppearanceSettings.DEFAULT);
        button(x + 112, y + 76, 104, "save", () -> {
            try { draft.save(AppearanceConfig.path()); saved = true; leave(); }
            catch (java.io.IOException e) {
                saveFailed = true;
                MetrakronClient.LOGGER.warn("Unable to save Metrakron appearance", e);
            }
        });
        button(x, y + 100, 216, "cancel", this::leave);
    }

    private void button(int x, int y, int w, String key, Runnable action) {
        //? if >=26.1 {
        /*var widget = Button.builder(label(key), b -> changed(key, action)).bounds(x, y, w, 20).build();
        addRenderableWidget(widget);
        *///?} else if >=1.19.3 {
        var widget = ButtonWidget.builder(label(key), b -> changed(key, action)).dimensions(x, y, w, 20).build();
        addDrawableChild(widget);
        //?} else {
        /*var widget = new ButtonWidget(x, y, w, 20, label(key), b -> changed(key, action));
        addDrawableChild(widget);
        *///?}
        refreshLabels.add(() -> widget.setMessage(label(key)));
    }

    private void changed(String key, Runnable action) {
        action.run();
        if (!key.equals("cancel")) AppearanceSettings.preview(draft);
        refreshLabels.forEach(Runnable::run);
    }

    //? if >=26.1 {
    /*private Component label(String key) {
    *///?} else {
    private Text label(String key) {
    //?}
        String value = switch (key) {
            case "stone" -> draft.stone().name();
            case "font" -> draft.font().name();
            case "metal" -> draft.metal().name();
            default -> null;
        };
        if (key.equals("save") && saveFailed) return t("metrakron.appearance.save_failed");
        return value == null ? t("metrakron.appearance." + key)
                : t("metrakron.appearance." + key, t("metrakron.appearance." + value.toLowerCase(java.util.Locale.ROOT)));
    }

    //? if >=26.1 {
    /*@Override public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
    *///?} else if >=1.20 {
    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
    //?} else {
    /*@Override public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        LegacyDrawContext context = new LegacyDrawContext(matrices);
    *///?}
        int top = Math.max(8, (height - 226) / 2);
        context.fill(0, 0, width, height, 0xFF101011);
        BronzeFrameStyle.draw((l, t, r, b, color) -> context.fill(l, t, r, b, draft.metalColor(color)), width / 2 - 116, top - 5, 232, 233);
        context.fill(width / 2 - 111, top, width / 2 + 111, top + 223, 0xFF161617);
        MetrakronOverlayRenderer.drawAt(context,
                new LoadSnapshot(true, LoadStage.STARTUP, 1, 15_000, 60_000L, LoadActivity.STARTUP_RESOURCES),
                width / 2 - 108, top + 18, 216);
        BitmapCinzelFont.drawCentered(context, title.getString(), width / 2, top + 1, 216, .23F, 0, 0xFFF7F0E4, false);
        //? if >=26.1 {
        /*super.extractRenderState(context, mouseX, mouseY, delta);
        *///?} else if >=1.20 {
        super.render(context, mouseX, mouseY, delta);
        //?} else {
        /*super.render(matrices, mouseX, mouseY, delta);
        *///?}
    }

    //? if >=26.1 {
    /*@Override public void onClose() { leave(); }
    *///?} else {
    @Override public void close() { leave(); }
    //?}
    private void leave() {
        AppearanceSettings.preview(saved ? draft : original);
        //? if >=26.2 {
        /*if (minecraft != null) minecraft.gui.setScreen(parent);
        *///?} else if >=26.1 {
        /*if (minecraft != null) minecraft.setScreen(parent);
        *///?} else {
        if (client != null) client.setScreen(parent);
        //?}
    }

    //? if >=26.1 {
    /*private static Component t(String key, Object... args) { return Component.translatable(key, args); }
    *///?} else if >=1.19 {
    private static Text t(String key, Object... args) { return Text.translatable(key, args); }
    //?} else {
    /*private static Text t(String key, Object... args) { return new TranslatableText(key, args); }
    *///?}
}
