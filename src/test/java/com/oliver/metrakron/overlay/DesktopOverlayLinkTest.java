package com.oliver.metrakron.overlay;

import org.junit.jupiter.api.Test;

import java.awt.event.MouseEvent;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DesktopOverlayLinkTest {
    private static final int LINK_LEFT = 80;
    private static final int LINK_RIGHT = 136;

    @Test
    void acceptsOnlyALeftClickInsideTheLink() {
        assertTrue(DesktopOverlayLink.isActivation(
                MouseEvent.BUTTON1,
                108,
                83,
                OverlayLayout.PANEL_WIDTH,
                DesktopOverlayLink.PANEL_HEIGHT,
                LINK_LEFT,
                LINK_RIGHT
        ));
        assertFalse(DesktopOverlayLink.isActivation(
                MouseEvent.BUTTON3,
                108,
                83,
                OverlayLayout.PANEL_WIDTH,
                DesktopOverlayLink.PANEL_HEIGHT,
                LINK_LEFT,
                LINK_RIGHT
        ));
        assertFalse(DesktopOverlayLink.isActivation(
                MouseEvent.BUTTON1,
                108,
                70,
                OverlayLayout.PANEL_WIDTH,
                DesktopOverlayLink.PANEL_HEIGHT,
                LINK_LEFT,
                LINK_RIGHT
        ));
    }

    @Test
    void mapsScaledDesktopCoordinatesBackToTheLogicalPanel() {
        assertTrue(DesktopOverlayLink.isPointerOver(
                216,
                166,
                OverlayLayout.PANEL_WIDTH * 2,
                DesktopOverlayLink.PANEL_HEIGHT * 2,
                LINK_LEFT,
                LINK_RIGHT
        ));
        assertFalse(DesktopOverlayLink.isPointerOver(
                60,
                166,
                OverlayLayout.PANEL_WIDTH * 2,
                DesktopOverlayLink.PANEL_HEIGHT * 2,
                LINK_LEFT,
                LINK_RIGHT
        ));
    }

    @Test
    void reservesClearSpaceBetweenTheActivityLineLinkAndFrame() {
        assertTrue(DesktopOverlayLink.LINK_HIT_TOP > DesktopOverlayLink.DETAIL_BASELINE);
        assertTrue(DesktopOverlayLink.LINK_HIT_BOTTOM < DesktopOverlayLink.PANEL_HEIGHT - 5);
        assertTrue(DesktopOverlayLink.LINK_BASELINE + 2 < DesktopOverlayLink.PANEL_HEIGHT - 5);
        assertTrue(OverlayLayout.PANEL_HEIGHT >= 80);
    }
}
