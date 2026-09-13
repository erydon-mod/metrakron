package com.oliver.metrakron.overlay;

import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BrandingLayoutTest {
    @Test void stoneCropsStayInsideEveryMasterWithoutStretching() throws Exception {
        for (var stone : AppearanceSettings.Stone.values()) {
            var appearance = new AppearanceSettings(stone, AppearanceSettings.Typeface.CINZEL, AppearanceSettings.Metal.BRONZE);
            try (var stream = getClass().getResourceAsStream(appearance.stoneResource())) {
                var image = ImageIO.read(stream);
                for (int height : new int[]{80, 128, 144, 289}) {
                    var crop = StoneCrop.fit(image.getWidth(), image.getHeight(), 216, height, stone);
                    assertTrue(crop.x() >= 0 && crop.y() >= 0);
                    assertTrue(crop.x() + crop.width() <= image.getWidth());
                    assertTrue(crop.y() + crop.height() <= image.getHeight());
                    assertEquals(216.0 / height, crop.width() / (double) crop.height(), .015);
                }
            }
        }
    }

    @Test void animationRepeatsExactlyAndEveryFrameFitsTheBundledAtlas() throws Exception {
        long now = System.nanoTime();
        long cycle = LogoAnimation.FRAMES * LogoAnimation.FRAME_NANOS;
        try (var stream = getClass().getResourceAsStream(LogoAnimation.RESOURCE)) {
            assertNotNull(stream);
            var atlas = ImageIO.read(stream);
            for (int i = 0; i < LogoAnimation.FRAMES; i++) {
                long time = now + i * LogoAnimation.FRAME_NANOS;
                int frame = LogoAnimation.frame(time);
                assertEquals(frame, LogoAnimation.frame(time + cycle));
                assertTrue((frame % LogoAnimation.COLUMNS + 1) * LogoAnimation.FRAME_WIDTH <= atlas.getWidth());
                assertTrue((frame / LogoAnimation.COLUMNS + 1) * LogoAnimation.FRAME_HEIGHT <= atlas.getHeight());
            }
        }
    }
}
