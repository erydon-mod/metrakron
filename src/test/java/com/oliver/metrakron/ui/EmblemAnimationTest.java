package com.oliver.metrakron.ui;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EmblemAnimationTest {
    @Test void approvedSquareFramesStayInsideTheSheetAndLoopEveryTwoSeconds() throws Exception {
        long now = System.nanoTime();
        long cycle = EmblemAnimation.FRAMES * EmblemAnimation.FRAME_NANOS;
        assertEquals(2_000_000_000L, cycle);
        assertEquals(64, EmblemAnimation.FRAME_SIZE);
        try (var stream = getClass().getResourceAsStream(EmblemAnimation.RESOURCE)) {
            assertNotNull(stream);
            var sheet = ImageIO.read(stream);
            assertEquals(64, sheet.getWidth());
            assertEquals(2560, sheet.getHeight());
            int first = EmblemAnimation.frame(now);
            for (int i = 0; i < EmblemAnimation.FRAMES; i++) {
                long time = now + i * EmblemAnimation.FRAME_NANOS;
                int frame = EmblemAnimation.frame(time);
                assertEquals((first + i) % EmblemAnimation.FRAMES, frame);
                assertEquals(frame, EmblemAnimation.frame(time + cycle));
                int top = frame * EmblemAnimation.FRAME_SIZE;
                assertTrue(top >= 0 && top + EmblemAnimation.FRAME_SIZE <= sheet.getHeight());
                assertEquals(0, sheet.getRGB(0, top) >>> 24, "Keep transparent square margins");
                assertTrue((sheet.getRGB(32, top + 32) >>> 24) > 200, "Every frame must contain the plaque");
            }
        }
    }

    @Test void guiTextureUsesSmoothFilteringWithoutASecondAnimationController() throws Exception {
        try (var stream = getClass().getResourceAsStream(EmblemAnimation.RESOURCE + ".mcmeta")) {
            assertNotNull(stream);
            var metadata = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            assertFalse(metadata.has("animation"));
            var texture = metadata.getAsJsonObject("texture");
            assertTrue(texture.get("blur").getAsBoolean());
            assertTrue(texture.get("clamp").getAsBoolean());
        }
    }
}
