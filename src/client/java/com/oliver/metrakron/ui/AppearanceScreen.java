package com.oliver.metrakron.ui;

import com.oliver.metrakron.MetrakronClient;
import com.oliver.metrakron.overlay.AppearanceSettings;
import com.oliver.metrakron.overlay.OverlayPosition;
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
    private record Control(int x, int y, int width, java.util.function.BooleanSupplier focused,
                           java.util.function.Supplier<String> text) { }
    private final java.util.List<Control> controls = new java.util.ArrayList<>();
    private final java.util.List<Runnable> refreshLabels = new java.util.ArrayList<>();

    public AppearanceScreen(Screen parent) {
        super(t("metrakron.appearance.title"));
        this.parent = parent;
        original = AppearanceSettings.current();
        draft = original;
    }

    @Override protected void init() {
        refreshLabels.clear();
        controls.clear();
        var layout = AppearanceLayout.fit(width, height);
        int x = layout.controlX(), y = layout.firstControlY(), controlWidth = layout.controlWidth();
        button(x, y, controlWidth, "stone", () -> {
            var values = AppearanceSettings.Stone.values();
            draft = new AppearanceSettings(values[(draft.stone().ordinal() + 1) % values.length], draft.font(), draft.metal(), draft.position());
        });
        button(x, y + 24, controlWidth, "font", () -> {
            var values = AppearanceSettings.Typeface.values();
            draft = new AppearanceSettings(draft.stone(), values[(draft.font().ordinal() + 1) % values.length], draft.metal(), draft.position());
        });
        button(x, y + 48, controlWidth, "metal", () -> {
            var values = AppearanceSettings.Metal.values();
            draft = new AppearanceSettings(draft.stone(), draft.font(), values[(draft.metal().ordinal() + 1) % values.length], draft.position());
        });
        button(x, y + 72, controlWidth, "position", () -> {
            var values = OverlayPosition.values();
            draft = new AppearanceSettings(draft.stone(), draft.font(), draft.metal(),
                    values[(draft.position().ordinal() + 1) % values.length]);
        });
        int footerWidth = layout.footerWidth();
        button(x, y + 100, footerWidth, "reset", () -> draft = AppearanceSettings.DEFAULT);
        button(x + footerWidth + 6, y + 100, footerWidth, "save", () -> {
            try { draft.save(AppearanceConfig.path()); saved = true; leave(); }
            catch (java.io.IOException e) {
                saveFailed = true;
                MetrakronClient.LOGGER.warn("Unable to save Metrakron appearance", e);
            }
        });
        button(x + controlWidth - footerWidth, y + 100, footerWidth, "cancel", this::leave);
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
        controls.add(new Control(x, y, w, widget::isFocused, () -> widget.getMessage().getString()));
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
            case "position" -> draft.position().name();
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
        var layout = AppearanceLayout.fit(width, height);
        int header = layout.header(), top = layout.top();
        context.fill(0, 0, width, height, 0xFF101011);
        PortoriumSurfaces.panel(context, layout.panelX(), layout.panelY(), layout.panelWidth(), layout.panelHeight(),
                draft.metal() == AppearanceSettings.Metal.SILVER);
        PortoriumLogo.draw(context, header > 0 ? width / 2 : layout.controlX() + 108, top, header);
        if (layout.showPreview()) MetrakronOverlayRenderer.drawPreviewAt(context,
                new LoadSnapshot(true, LoadStage.STARTUP, 1, 15_000, 60_000L, LoadActivity.STARTUP_RESOURCES),
                layout.controlX(), top + header + 18, layout.controlWidth());
        else {
            // The usual preview initializes the atlas; compact menus still need the selected font.
            //? if >=26.1 {
            /*BitmapCinzelFont.initialize(minecraft);
            *///?} else {
            BitmapCinzelFont.initialize(client);
            //?}
        }
        BitmapCinzelFont.drawMenuLabel(context, title.getString(), layout.titleCenterX(),
                layout.titleCenterY(), layout.titleWidth(), 14, .23F, 0xFF1F1F1F);
        // Render controls only here. The registered native widgets still handle
        // clicks, keyboard focus and narration, but their default render pass
        // would also draw vanilla labels beneath our custom lettering.
        for (Control control : controls) {
            boolean hovered = mouseX >= control.x && mouseX < control.x + control.width
                    && mouseY >= control.y && mouseY < control.y + 20;
            PortoriumSurfaces.button(context, control.x, control.y, control.width,
                    hovered || control.focused.getAsBoolean(), draft.metal() == AppearanceSettings.Metal.SILVER);
            BitmapCinzelFont.drawMenuLabel(context, control.text.get(), control.x + control.width / 2,
                    control.y + 10, control.width - 14, 12, .20F, 0xFFF8F3E8);
        }
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
