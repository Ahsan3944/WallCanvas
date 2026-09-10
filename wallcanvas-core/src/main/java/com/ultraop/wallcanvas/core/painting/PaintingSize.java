package com.ultraop.wallcanvas.core.painting;

/** Supported WallCanvas Painting canvas presets. */
public enum PaintingSize {
    /** Four normal Minecraft map areas. */
    DEFAULT(4),

    /** Eight normal Minecraft map areas. */
    LARGE(8);

    private final int mapAreaMultiplier;

    PaintingSize(int mapAreaMultiplier) {
        this.mapAreaMultiplier = mapAreaMultiplier;
    }

    public int mapAreaMultiplier() {
        return mapAreaMultiplier;
    }
}
