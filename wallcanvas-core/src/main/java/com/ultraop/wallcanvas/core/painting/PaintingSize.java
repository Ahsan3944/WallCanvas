package com.ultraop.wallcanvas.core.painting;

import java.util.Objects;

/**
 * Physical Painting canvas size in Minecraft blocks.
 * Presets remain available, while custom W x H sizes are supported.
 */
public final class PaintingSize {
    public static final PaintingSize DEFAULT = new PaintingSize("DEFAULT", 2, 2);
    public static final PaintingSize LARGE = new PaintingSize("LARGE", 4, 2);

    private final String name;
    private final int widthBlocks;
    private final int heightBlocks;

    private PaintingSize(String name, int widthBlocks, int heightBlocks) {
        this.name = name;
        this.widthBlocks = widthBlocks;
        this.heightBlocks = heightBlocks;
    }

    public static PaintingSize of(int widthBlocks, int heightBlocks) {
        if (widthBlocks < 1 || widthBlocks > 16) {
            throw new IllegalArgumentException("width must be between 1 and 16 blocks");
        }
        if (heightBlocks < 1 || heightBlocks > 16) {
            throw new IllegalArgumentException("height must be between 1 and 16 blocks");
        }
        if (widthBlocks == 2 && heightBlocks == 2) return DEFAULT;
        if (widthBlocks == 4 && heightBlocks == 2) return LARGE;
        return new PaintingSize("CUSTOM_" + widthBlocks + "X" + heightBlocks, widthBlocks, heightBlocks);
    }

    public String name() {
        return name;
    }

    public int mapAreaMultiplier() {
        return widthBlocks * heightBlocks;
    }

    public int widthBlocks() {
        return widthBlocks;
    }

    public int heightBlocks() {
        return heightBlocks;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof PaintingSize size)) return false;
        return widthBlocks == size.widthBlocks && heightBlocks == size.heightBlocks;
    }

    @Override
    public int hashCode() {
        return Objects.hash(widthBlocks, heightBlocks);
    }

    @Override
    public String toString() {
        return widthBlocks + "x" + heightBlocks;
    }
}
