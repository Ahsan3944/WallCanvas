package com.ultraop.wallcanvas.core.map;

/** Shared definition of a WallCanvas map display. Coordinates represent the display center. */
public record MapSpec(
        String assetId,
        int tilesWide,
        int tilesHigh,
        double centerX,
        double centerY,
        double centerZ
) {
    public MapSpec {
        if (assetId == null || assetId.isBlank()) {
            throw new IllegalArgumentException("assetId cannot be blank");
        }
        if (tilesWide < 1 || tilesWide > 64) {
            throw new IllegalArgumentException("tilesWide must be between 1 and 64");
        }
        if (tilesHigh < 1 || tilesHigh > 64) {
            throw new IllegalArgumentException("tilesHigh must be between 1 and 64");
        }
        if (!Double.isFinite(centerX) || !Double.isFinite(centerY) || !Double.isFinite(centerZ)) {
            throw new IllegalArgumentException("center coordinates must be finite");
        }
    }

    public static MapSpec single(String assetId, double centerX, double centerY, double centerZ) {
        return new MapSpec(assetId, 1, 1, centerX, centerY, centerZ);
    }
}
