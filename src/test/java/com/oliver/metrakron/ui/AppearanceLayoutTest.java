package com.oliver.metrakron.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AppearanceLayoutTest {
    @Test void artworkAndAllSevenNativeControlsStayOnScreenAtSmallAndLargeSizes() {
        for (int width : new int[]{180, 240, 258, 320, 854}) for (int height : new int[]{160, 200, 240, 296, 360, 480}) {
            var layout = AppearanceLayout.fit(width, height);
            assertTrue(layout.panelX() >= 0 && layout.panelX() + layout.panelWidth() <= width);
            assertTrue(layout.panelY() >= 0 && layout.panelY() + layout.panelHeight() <= height);
            for (int offset : new int[]{0, 24, 48, 72, 100}) {
                assertTrue(layout.firstControlY() + offset >= layout.panelY());
                assertTrue(layout.firstControlY() + offset + 20 <= layout.panelY() + layout.panelHeight());
            }
            assertTrue(layout.footerWidth() >= 1);
            assertTrue(layout.footerWidth() * 3 + 12 <= layout.controlWidth());
        }
    }

    @Test void normalSizeRetainsTheApprovedFourOptionsAndThreeFooterPositions() {
        var layout = AppearanceLayout.fit(320, 240);
        assertEquals(216, layout.controlWidth());
        assertEquals(52, layout.controlX());
        assertEquals(111, layout.firstControlY());
        assertEquals(68, layout.footerWidth());
        assertTrue(layout.showPreview());
        assertFalse(AppearanceLayout.fit(240, 200).showPreview());
    }

    @Test void titleSharesThePreviewCentreAndClearsTheCompactEmblem() {
        for (int width : new int[]{180, 181, 240, 258, 320, 854}) {
            for (int height : new int[]{160, 240, 296, 480}) {
                var layout = AppearanceLayout.fit(width, height);
                assertEquals(layout.controlX() + layout.controlWidth() / 2, layout.titleCenterX());
                assertEquals(layout.top() + layout.header() + 8, layout.titleCenterY());
                if (layout.header() == 0) {
                    int emblemRight = layout.controlX() + 16;
                    assertTrue(layout.titleCenterX() - layout.titleWidth() / 2 >= emblemRight + 4);
                }
            }
        }
    }
}
