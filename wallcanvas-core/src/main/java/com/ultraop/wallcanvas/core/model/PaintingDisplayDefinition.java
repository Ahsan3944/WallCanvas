package com.ultraop.wallcanvas.core.model;

import java.util.UUID;

/**
 * Definition for a WallCanvas picture rendered as a Minecraft Painting.
 *
 * <p>The player places the Painting manually, so this definition intentionally
 * does not contain a world coordinate. Placement is determined by the
 * Painting's normal Minecraft wall-placement behaviour.</p>
 */
public record PaintingDisplayDefinition(
        UUID id,
        String pictureAssetId,
        String paintingVariant,
        int widthPixels,
        int heightPixels
) {
    public PaintingDisplayDefinition {
        if (id == null) throw new IllegalArgumentException("id cannot be null");
        if (pictureAssetId == null || pictureAssetId.isBlank()) {
            throw new IllegalArgumentException("pictureAssetId cannot be blank");
        }
        if (paintingVariant == null || paintingVariant.isBlank()) {
            throw new IllegalArgumentException("paintingVariant cannot be blank");
        }
        if (widthPixels < 1 || heightPixels < 1) {
            throw new IllegalArgumentException("picture dimensions must be positive");
        }
    }
}
