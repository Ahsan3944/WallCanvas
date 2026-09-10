package com.ultraop.wallcanvas.core.model;

import java.util.UUID;

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
}
