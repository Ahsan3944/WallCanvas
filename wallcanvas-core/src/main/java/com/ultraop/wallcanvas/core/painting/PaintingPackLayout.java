package com.ultraop.wallcanvas.core.painting;

import java.nio.file.Path;

/** Shared filesystem layout for generated WallCanvas Painting packs. */
public final class PaintingPackLayout {
    private PaintingPackLayout() {
    }

    public static Path variantDefinition(Path dataRoot, String variantId) {
        return dataRoot.resolve("wallcanvas/painting_variant/" + idPath(variantId) + ".json");
    }

    public static Path texture(Path assetsRoot, String assetId) {
        return assetsRoot.resolve(idPath(assetId) + ".png");
    }

    public static Path paintingsAtlas(Path assetsRoot) {
        return assetsRoot.resolve("wallcanvas/atlases/paintings.json");
    }

    public static String idPath(String namespacedId) {
        int colon = namespacedId.indexOf(':');
        if (colon < 1 || colon == namespacedId.length() - 1) {
            throw new IllegalArgumentException("Expected namespaced id: " + namespacedId);
        }
        return namespacedId.substring(0, colon) + "/" + namespacedId.substring(colon + 1);
    }
}
