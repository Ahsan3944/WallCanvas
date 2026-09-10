package com.ultraop.wallcanvas.core.painting;

import java.util.Objects;

/** Shared description of a Minecraft data-driven Painting variant. */
public record PaintingVariantDefinition(
        String variantId,
        int width,
        int height,
        String assetId
) {
    public PaintingVariantDefinition {
        if (variantId == null || variantId.isBlank()) throw new IllegalArgumentException("variantId cannot be blank");
        if (width < 1 || width > 16) throw new IllegalArgumentException("width must be between 1 and 16");
        if (height < 1 || height > 16) throw new IllegalArgumentException("height must be between 1 and 16");
        if (assetId == null || assetId.isBlank()) throw new IllegalArgumentException("assetId cannot be blank");
    }
}
