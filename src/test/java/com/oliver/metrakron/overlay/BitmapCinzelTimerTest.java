package com.oliver.metrakron.overlay;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BitmapCinzelTimerTest {
    @Test
    void drawsFourAndEightFromTheBakedCinzelAtlas() {
        BitmapCinzelTimer timer = BitmapCinzelTimer.load(
                new Color(0xFFC65A),
                new Color(0, 0, 0, 176)
        );
        assertNotNull(timer);

        BufferedImage image = new BufferedImage(420, 180, BufferedImage.TYPE_INT_ARGB);
        Graphics2D context = image.createGraphics();
        try {
            timer.drawCentered(context, "00:48", 210, 10, 400, 0.75F, 0.0F);
        } finally {
            context.dispose();
        }

        int visiblePixels = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) > 0) {
                    visiblePixels++;
                }
            }
        }
        assertTrue(visiblePixels > 5_000);
    }
}
