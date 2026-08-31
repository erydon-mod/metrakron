package com.oliver.metrakron.overlay;

import java.awt.Desktop;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;

final class DesktopOverlayLink {
    static final String LABEL = "erydon.co.uk";
    static final URI TARGET = URI.create("https://erydon.co.uk");

    static final int PANEL_HEIGHT = 96;
    static final int DETAIL_BASELINE = 70;
    static final int LINK_BASELINE = 84;
    static final int LINK_HIT_TOP = 76;
    static final int LINK_HIT_BOTTOM = 90;
    static final int LINK_HORIZONTAL_PADDING = 4;

    private DesktopOverlayLink() {
    }

    static boolean isPointerOver(
            int componentX,
            int componentY,
            int componentWidth,
            int componentHeight,
            int logicalLinkLeft,
            int logicalLinkRight
    ) {
        if (componentWidth <= 0
                || componentHeight <= 0
                || logicalLinkLeft >= logicalLinkRight) {
            return false;
        }

        double logicalX = componentX * OverlayLayout.PANEL_WIDTH / (double) componentWidth;
        double logicalY = componentY * PANEL_HEIGHT / (double) componentHeight;
        return logicalX >= logicalLinkLeft - LINK_HORIZONTAL_PADDING
                && logicalX <= logicalLinkRight + LINK_HORIZONTAL_PADDING
                && logicalY >= LINK_HIT_TOP
                && logicalY <= LINK_HIT_BOTTOM;
    }

    static boolean isActivation(
            int mouseButton,
            int componentX,
            int componentY,
            int componentWidth,
            int componentHeight,
            int logicalLinkLeft,
            int logicalLinkRight
    ) {
        return mouseButton == MouseEvent.BUTTON1
                && isPointerOver(
                        componentX,
                        componentY,
                        componentWidth,
                        componentHeight,
                        logicalLinkLeft,
                        logicalLinkRight
                );
    }

    static void openInBrowser() {
        Thread browserThread = new Thread(() -> {
            try {
                if (!Desktop.isDesktopSupported()) {
                    return;
                }
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.BROWSE)) {
                    desktop.browse(TARGET);
                }
            } catch (IOException | RuntimeException ignored) {
                // A missing browser must never interrupt the independent loading clock.
            }
        }, "Metrakron ERYDON website");
        browserThread.setDaemon(true);
        browserThread.start();
    }
}
