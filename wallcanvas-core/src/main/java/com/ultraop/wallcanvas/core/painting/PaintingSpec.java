package com.ultraop.wallcanvas.core.painting;

import java.util.Objects;

/**
 * Platform-independent specification for a WallCanvas Painting item.
 * Paper and Fabric adapters translate this specification into their native
 * Minecraft ItemStack representation.
 */
public record PaintingSpec(String assetId, PaintingSize size) {
    public PaintingSpec {
        if (assetId == null || assetId.isBlank()) {
            throw new IllegalArgumentException("assetId cannot be blank");
        }
        Objects.requireNonNull(size, "size");
    }
}
