package com.ultraop.wallcanvas.core.painting;

import java.nio.file.Path;

/** Canonical resource-pack paths shared by Paper and Fabric adapters. */
public final class PaintingResourcePackLayout {
    public static final String NAMESPACE = "wallcanvas";
    public static final int PACK_FORMAT = 75;

    private PaintingResourcePackLayout() {
    }

    public static Path variantJson(Path packRoot, String variantId) {
        return packRoot.resolve("data").resolve(NAMESPACE).resolve("painting_variant")
                .resolve(fileName(variantId, ".json"));
    }

    public static Path texture(Path packRoot, String variantId) {
        return packRoot.resolve("assets").resolve(NAMESPACE).resolve("textures")
                .resolve("painting").resolve(fileName(variantId, ".png"));
    }

    public static Path atlas(Path packRoot) {
        return packRoot.resolve("assets").resolve(NAMESPACE).resolve("atlases")
                .resolve("paintings.json");
    }

    public static Path packMcmeta(Path packRoot) {
        return packRoot.resolve("pack.mcmeta");
    }

    private static String fileName(String id, String suffix) {
        int separator = id.indexOf(':');
        return (separator >= 0 ? id.substring(separator + 1) : id) + suffix;
    }
}
