package com.oliver.metrakron.ui;

import com.oliver.metrakron.timing.LoadSnapshot;
import com.oliver.metrakron.timing.LoadStage;
import com.oliver.metrakron.overlay.OverlayClock;
//? if >=26.1 {
/*import net.minecraft.network.chat.Component;
*///?} else {
import net.minecraft.text.Text;
//? if <1.19
/*import net.minecraft.text.TranslatableText;*/
//?}

import java.util.Locale;

public final class OverlayCopy {
    private OverlayCopy() {
    }

    public static String heading(LoadSnapshot snapshot) {
        if (OverlayClock.isRelearning(snapshot.elapsedMillis(), snapshot.averageMillis())) {
            return relearningHeading();
        }
        return baseHeading(snapshot);
    }

    public static String baseHeading(LoadSnapshot snapshot) {
        if (snapshot.averageMillis() == null) {
            return translated("text.metrakron.learning", "LEARNING");
        }
        if (snapshot.stage() == LoadStage.STARTUP) {
            return translated("text.metrakron.main_menu_in", "MAIN MENU IN");
        }
        return translated("text.metrakron.world_ready_in", "WORLD READY IN");
    }

    public static String relearningHeading() {
        return translated("text.metrakron.relearning", "RELEARNING LOAD TIME");
    }

    public static String detail(LoadSnapshot snapshot) {
        return translated(
                snapshot.activity().translationKey(),
                snapshot.activity().fallback()
        );
    }

    private static String translated(String key, String fallback) {
        //? if >=26.1 {
        /*return Component.translatableWithFallback(key, fallback)
                .getString()
                .toUpperCase(Locale.ROOT);
        *///?} else {
        //? if >=1.19.4 {
        return Text.translatableWithFallback(key, fallback)
                .getString()
                .toUpperCase(Locale.ROOT);
        //?} else if >=1.19 {
        /*String translated = Text.translatable(key).getString();
        return (translated.equals(key) ? fallback : translated).toUpperCase(Locale.ROOT);
        *///?} else {
        /*String translated = new TranslatableText(key).getString();
        return (translated.equals(key) ? fallback : translated).toUpperCase(Locale.ROOT);
        *///?}
        //?}
    }
}
