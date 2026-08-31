package com.oliver.metrakron.overlay;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BronzeFrameStyleTest {
    @Test
    void drawsAContainedFivePixelInlayWithDirectionalLightCatches() {
        List<FillCall> fills = new ArrayList<>();

        BronzeFrameStyle.draw(
                (left, top, right, bottom, argb) ->
                        fills.add(new FillCall(left, top, right, bottom, argb)),
                6,
                6,
                OverlayLayout.PANEL_WIDTH,
                96
        );

        assertEquals(15, fills.size());
        assertEquals(
                new FillCall(6, 6, 222, 102, BronzeFrameStyle.OUTER_GROOVE),
                fills.get(0)
        );
        assertEquals(
                new FillCall(40, 7, 98, 8, BronzeFrameStyle.TOP_GLINT),
                fills.get(10)
        );
        assertEquals(
                new FillCall(7, 15, 8, 36, BronzeFrameStyle.LEFT_GLINT),
                fills.get(11)
        );
        assertTrue(fills.stream().allMatch(fill ->
                fill.left >= 6
                        && fill.top >= 6
                        && fill.right <= 222
                        && fill.bottom <= 102
                        && fill.left < fill.right
                        && fill.top < fill.bottom
        ));
        assertEquals(5, BronzeFrameStyle.CONTENT_INSET);
    }

    @Test
    void skipsPanelsTooSmallForTheFivePixelRecess() {
        List<FillCall> fills = new ArrayList<>();

        BronzeFrameStyle.draw(
                (left, top, right, bottom, argb) ->
                        fills.add(new FillCall(left, top, right, bottom, argb)),
                0,
                0,
                9,
                9
        );

        assertTrue(fills.isEmpty());
    }

    private record FillCall(int left, int top, int right, int bottom, int argb) {
    }
}
