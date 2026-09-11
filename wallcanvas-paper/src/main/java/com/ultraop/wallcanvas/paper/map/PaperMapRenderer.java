package com.ultraop.wallcanvas.paper.map;

import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.map.MapPalette;
import org.bukkit.entity.Player;

import java.awt.image.BufferedImage;

/** Renders one 128x128 tile of a WallCanvas picture onto a normal Minecraft map. */
public final class PaperMapRenderer extends MapRenderer {
    private final BufferedImage image;
    private final int offsetX;
    private final int offsetY;
    private boolean rendered;

    public PaperMapRenderer(BufferedImage image, int offsetX, int offsetY) {
        super(false);
        this.image = image;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    @Override
    public void render(MapView map, MapCanvas canvas, Player player) {
        if (rendered) return;
        for (int y = 0; y < 128; y++) {
            for (int x = 0; x < 128; x++) {
                int argb = image.getRGB(offsetX + x, offsetY + y);
                int alpha = (argb >>> 24) & 0xff;
                if (alpha < 8) {
                    canvas.setPixel(x, y, MapPalette.TRANSPARENT);
                    continue;
                }
                canvas.setPixel(x, y, MapPalette.matchColor(
                        (argb >>> 16) & 0xff,
                        (argb >>> 8) & 0xff,
                        argb & 0xff));
            }
        }
        rendered = true;
    }
}
