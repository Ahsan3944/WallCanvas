package com.ultraop.wallcanvas.core.map;

/** A single 128x128 Minecraft-map-sized tile extracted from a source image. */
public record MapTile(int x, int y, int width, int height) {
    public MapTile {
        if (x < 0 || y < 0 || width < 1 || height < 1 || width > 128 || height > 128) {
            throw new IllegalArgumentException("Invalid map tile bounds");
        }
    }
}
