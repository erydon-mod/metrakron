package com.oliver.metrakron.overlay;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import static org.junit.jupiter.api.Assertions.*;

class OverlayPositionTest {
    @Test void allEightAnchorsHaveExpectedCoordinates() {
        int[][] expected = {{6, 6}, {292, 6}, {578, 6}, {578, 228},
                {578, 450}, {292, 450}, {6, 450}, {6, 228}};
        assertEquals(8, OverlayPosition.values().length);
        for (var position : OverlayPosition.values()) {
            var bounds = position.place(800, 600, 216, 144, 6);
            assertEquals(expected[position.ordinal()][0], bounds.x());
            assertEquals(expected[position.ordinal()][1], bounds.y());
            assertEquals(216, bounds.width());
            assertEquals(144, bounds.height());
        }
    }

    @Test void anchorsStayInsideResizedWindowsAndRespectDifferentPanelHeights() {
        for (var position : OverlayPosition.values()) {
            for (int width : new int[]{1, 100, 228, 800, 3840}) {
                for (int height : new int[]{1, 90, 144, 600, 2160}) {
                    for (int panelHeight : new int[]{80, 128, 144, 576}) {
                        var b = position.place(width, height, 216, panelHeight, 6);
                        assertTrue(b.x() >= 0 && b.y() >= 0);
                        assertTrue(b.width() > 0 && b.height() > 0);
                        assertTrue(b.x() + b.width() <= width);
                        assertTrue(b.y() + b.height() <= height);
                    }
                }
            }
        }
    }

    @Test void scaledDesktopCoordinatesMatchLogicalAnchors() {
        for (var position : OverlayPosition.values()) {
            var logical = position.place(800, 600, 216, 144, 6);
            var scaled = position.place(1600, 1200, 432, 288, 12);
            assertEquals(logical.x() * 2, scaled.x());
            assertEquals(logical.y() * 2, scaled.y());
        }
    }

    @Test void positionLabelsExistInAllSupportedLanguages() throws Exception {
        for (String language : new String[]{"en_us", "de_de", "es_es"}) {
            try (var stream = getClass().getResourceAsStream("/assets/metrakron/lang/" + language + ".json")) {
                assertNotNull(stream);
                var labels = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
                assertTrue(labels.get("metrakron.appearance.position").getAsString().contains("%s"));
                for (var position : OverlayPosition.values()) {
                    assertFalse(labels.get("metrakron.appearance." + position.name().toLowerCase(Locale.ROOT)).getAsString().isBlank());
                }
            }
        }
    }
}
