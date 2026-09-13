package com.oliver.metrakron.overlay;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

/** The same authored Portorium chamfer and recessed metal inlay as the appearance controls. */
final class PolishedFrame {
    private static final BufferedImage BRONZE = load("portorium_control_frame.png");
    private static final BufferedImage SILVER = load("portorium_control_frame_silver.png");
    private PolishedFrame() { }

    static void draw(Graphics2D g, int w, int h, AppearanceSettings appearance) {
        BufferedImage image = appearance.metal() == AppearanceSettings.Metal.SILVER ? SILVER : BRONZE;
        if (image == null) {
            BronzeFrameStyle.draw((l, t, r, b, color) -> {
                g.setColor(new java.awt.Color(appearance.metalColor(color), true));
                g.fillRect(l, t, r - l, b - t);
            }, 0, 0, w, h);
            return;
        }
        for (int row = 0; row < 3; row++) {
            int y = row == 0 ? 0 : row == 1 ? 4 : h - 4;
            int dh = row == 1 ? h - 8 : 4;
            int v = row == 0 ? 0 : row == 1 ? 16 : 64;
            int sh = row == 1 ? 48 : 16;
            for (int col = 0; col < 3; col++) {
                if (row == 1 && col == 1) continue;
                int x = col == 0 ? 0 : col == 1 ? 4 : w - 4;
                int dw = col == 1 ? w - 8 : 4;
                int u = col == 0 ? 0 : col == 1 ? 16 : 112;
                int sw = col == 1 ? 96 : 16;
                g.drawImage(image, x, y, x + dw, y + dh, u, v, u + sw, v + sh, null);
            }
        }
    }

    private static BufferedImage load(String name) {
        try (var stream = PolishedFrame.class.getResourceAsStream("/assets/metrakron/textures/gui/" + name)) {
            return stream == null ? null : ImageIO.read(stream);
        } catch (IOException ignored) {
            return null;
        }
    }
}
