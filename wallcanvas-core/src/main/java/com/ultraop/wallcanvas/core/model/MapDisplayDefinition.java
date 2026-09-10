package com.ultraop.wallcanvas.core.model;

import java.util.UUID;

/**
 * A map display is anchored by its logical center rather than requiring the
 * player to physically reach the location where the map should appear.
 */
public record MapDisplayDefinition(
        UUID id,
        String mapAssetId,
        double centerX,
        double centerY,
        double centerZ,
        int tilesWide,
        int tilesHigh,
        float yaw,
        float pitch,
        float roll
) {
    public MapDisplayDefinition {
        if (id == null) throw new IllegalArgumentException("id cannot be null");
        if (mapAssetId == null || mapAssetId.isBlank()) throw new IllegalArgumentException("mapAssetId cannot be blank");
        if (tilesWide < 1 || tilesHigh < 1) throw new IllegalArgumentException("tile dimensions must be positive");
    }
}
