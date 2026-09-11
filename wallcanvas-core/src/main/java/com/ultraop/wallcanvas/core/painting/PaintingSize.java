package com.ultraop.wallcanvas.core.painting;

import java.util.Objects;

/** Physical Painting canvas size plus texture pixel density per Minecraft block. */
public final class PaintingSize {
    public static final int DEFAULT_PIXELS_PER_BLOCK = 16;
    public static final PaintingSize DEFAULT = new PaintingSize("DEFAULT", 2, 2, DEFAULT_PIXELS_PER_BLOCK);
    public static final PaintingSize LARGE = new PaintingSize("LARGE", 4, 2, DEFAULT_PIXELS_PER_BLOCK);

    private final String name;
    private final int widthBlocks;
    private final int heightBlocks;
    private final int pixelsPerBlock;

    private PaintingSize(String name, int widthBlocks, int heightBlocks, int pixelsPerBlock) {
        this.name = name;
        this.widthBlocks = widthBlocks;
        this.heightBlocks = heightBlocks;
        this.pixelsPerBlock = pixelsPerBlock;
    }

    public static PaintingSize of(int widthBlocks, int heightBlocks) {
        return of(widthBlocks, heightBlocks, DEFAULT_PIXELS_PER_BLOCK);
    }

    public static PaintingSize of(int widthBlocks, int heightBlocks, int pixelsPerBlock) {
        if (widthBlocks < 1 || widthBlocks > 16) throw new IllegalArgumentException("width must be between 1 and 16 blocks");
        if (heightBlocks < 1 || heightBlocks > 16) throw new IllegalArgumentException("height must be between 1 and 16 blocks");
        if (pixelsPerBlock < 4 || pixelsPerBlock > 256 || (pixelsPerBlock & (pixelsPerBlock - 1)) != 0) {
            throw new IllegalArgumentException("pixels-per-block must be a power of two between 4 and 256");
        }
        if (pixelsPerBlock == DEFAULT_PIXELS_PER_BLOCK && widthBlocks == 2 && heightBlocks == 2) return DEFAULT;
        if (pixelsPerBlock == DEFAULT_PIXELS_PER_BLOCK && widthBlocks == 4 && heightBlocks == 2) return LARGE;
        return new PaintingSize("CUSTOM_" + widthBlocks + "X" + heightBlocks + "_P" + pixelsPerBlock,
                widthBlocks, heightBlocks, pixelsPerBlock);
    }

    public String name() { return name; }
    public int mapAreaMultiplier() { return widthBlocks * heightBlocks; }
    public int widthBlocks() { return widthBlocks; }
    public int heightBlocks() { return heightBlocks; }
    public int pixelsPerBlock() { return pixelsPerBlock; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof PaintingSize size)) return false;
        return widthBlocks == size.widthBlocks && heightBlocks == size.heightBlocks
                && pixelsPerBlock == size.pixelsPerBlock;
    }

    @Override
    public int hashCode() { return Objects.hash(widthBlocks, heightBlocks, pixelsPerBlock); }
    @Override
    public String toString() { return widthBlocks + "x" + heightBlocks + " @ " + pixelsPerBlock + "px/block"; }
}
