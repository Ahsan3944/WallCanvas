package com.ultraop.wallcanvas.core.painting;

/** Supported WallCanvas Painting canvas presets. */
public enum PaintingSize {
    /** Four normal Minecraft map areas: a 2x2 block Painting canvas. */
    DEFAULT(4, 2, 2),

    /** Eight normal Minecraft map areas: a 4x2 block Painting canvas. */
    LARGE(8, 4, 2);

    private final int mapAreaMultiplier;
    private final int widthBlocks;
    private final int heightBlocks;

    PaintingSize(int mapAreaMultiplier, int widthBlocks, int heightBlocks) {
        this.mapAreaMultiplier = mapAreaMultiplier;
        this.widthBlocks = widthBlocks;
        this.heightBlocks = heightBlocks;
    }

    public int mapAreaMultiplier() {
        return mapAreaMultiplier;
    }

    public int widthBlocks() {
        return widthBlocks;
    }

    public int heightBlocks() {
        return heightBlocks;
    }
}
