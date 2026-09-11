package com.ultraop.wallcanvas.core.map;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Shared image preparation for map displays. Platform adapters turn these tiles into map data. */
public final class MapImageRenderer {
    public static final int MAP_SIZE = 128;

    private MapImageRenderer() {
    }

    public static BufferedImage prepare(Path image, MapSpec spec) throws IOException {
        BufferedImage source = ImageIO.read(image.toFile());
        if (source == null) throw new IOException("Unsupported image: " + image.getFileName());
        int targetWidth = spec.tilesWide() * MAP_SIZE;
        int targetHeight = spec.tilesHigh() * MAP_SIZE;
        BufferedImage output = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = output.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            double scale = Math.max((double) targetWidth / source.getWidth(),
                    (double) targetHeight / source.getHeight());
            int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
            int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
            int x = (targetWidth - width) / 2;
            int y = (targetHeight - height) / 2;
            graphics.drawImage(source, x, y, width, height, null);
        } finally {
            graphics.dispose();
        }
        return output;
    }

    public static List<MapTile> tiles(MapSpec spec) {
        List<MapTile> result = new ArrayList<>(spec.tilesWide() * spec.tilesHigh());
        for (int y = 0; y < spec.tilesHigh(); y++) {
            for (int x = 0; x < spec.tilesWide(); x++) {
                result.add(new MapTile(x * MAP_SIZE, y * MAP_SIZE, MAP_SIZE, MAP_SIZE));
            }
        }
        return List.copyOf(result);
    }

    public static void writeTile(BufferedImage prepared, MapTile tile, Path destination) throws IOException {
        Files.createDirectories(destination.toAbsolutePath().getParent());
        BufferedImage output = prepared.getSubimage(tile.x(), tile.y(), tile.width(), tile.height());
        ImageIO.write(output, "png", destination.toFile());
    }
}
