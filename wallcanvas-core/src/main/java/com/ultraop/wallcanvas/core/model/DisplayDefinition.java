package com.ultraop.wallcanvas.core.model;

import java.util.UUID;

/**
 * Canonical shared WallCanvas display model used by both Paper and Fabric.
 *
 * <p>Painting displays use {@code PAINTING} and are manually placed by the
 * player, so their coordinates are normally zero. Map displays use
 * {@code MAP} and use the center coordinates as their anchor.</p>
 */
public record DisplayDefinition(
        UUID id,
        DisplayType type,
        String assetId,
        double centerX,
        double centerY,
        double centerZ,
        int width,
        int height,
        float yaw,
        float pitch,
        float roll
) {
    public DisplayDefinition {
        if (id == null) throw new IllegalArgumentException("id cannot be null");
        if (type == null) throw new IllegalArgumentException("type cannot be null");
        if (assetId == null || assetId.isBlank()) throw new IllegalArgumentException("assetId cannot be blank");
        if (width < 1 || height < 1) throw new IllegalArgumentException("width and height must be positive");
    }

    public static DisplayDefinition painting(UUID id, String pictureAssetId, int width, int height) {
        return new DisplayDefinition(id, DisplayType.PAINTING, pictureAssetId, 0, 0, 0, width, height, 0, 0, 0);
    }

    public static DisplayDefinition map(UUID id, String pictureAssetId, double centerX, double centerY,
                                        double centerZ, int tilesWide, int tilesHigh) {
        return new DisplayDefinition(id, DisplayType.MAP, pictureAssetId, centerX, centerY, centerZ,
                tilesWide, tilesHigh, 0, 0, 0);
    }

    public boolean isPainting() {
        return type == DisplayType.PAINTING;
    }

    public boolean isMap() {
        return type == DisplayType.MAP;
    }
}
