package com.ultraop.wallcanvas.core.painting;

import java.nio.file.Path;

/** Canonical resource-pack paths shared by Paper and Fabric adapters. */
public final class PaintingResourcePaths {
    private PaintingResourcePaths() {
    }

    public static Path variant(Path packRoot, PaintingVariantDefinition variant) {
        return packRoot.resolve("assets")
                .resolve("wallcanvas")
                .resolve("painting_variant")
                .resolve(variant.variantId().substring(variant.variantId().indexOf(':') + 1) + ".json");
    }

    public static Path texture(Path packRoot, PaintingVariantDefinition variant) {
        return packRoot.resolve("assets")
                .resolve("wallcanvas")
                .resolve("textures")
                .resolve("painting")
                .resolve(variant.variantId().substring(variant.variantId().indexOf(':') + 1) + ".png");
    }
}
