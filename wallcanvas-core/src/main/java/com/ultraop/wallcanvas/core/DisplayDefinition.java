package com.ultraop.wallcanvas.core;

import java.util.Objects;
import java.util.UUID;

public record DisplayDefinition(
        UUID id,
        DisplayType type,
        String asset,
        double centerX,
        double centerY,
        double centerZ,
        int widthMaps,
        int heightMaps,
        float yaw,
        float pitch,
        float roll
) {
    public DisplayDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(asset, "asset");
        if (widthMaps < 1 || heightMaps < 1) throw new IllegalArgumentException("Display dimensions must be positive");
    }
}
