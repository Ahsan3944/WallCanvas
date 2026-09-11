package com.ultraop.wallcanvas.core.painting;

import com.ultraop.wallcanvas.core.library.ImageLibrary;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Builds the generated Painting metadata for every picture in the shared library. */
public final class PaintingResourcePackBuilder {
    private PaintingResourcePackBuilder() {
    }

    public static Map<String, PaintingVariantDefinition> build(Path picturesDirectory, Path packRoot) throws IOException {
        Map<String, PaintingVariantDefinition> definitions = new LinkedHashMap<>();
        for (Path picture : ImageLibrary.scan(picturesDirectory)) {
            String assetId = picture.getFileName().toString();
            for (PaintingSize size : new PaintingSize[]{PaintingSize.DEFAULT, PaintingSize.LARGE}) {
                PaintingVariantDefinition definition = PaintingVariantFactory.create(new PaintingSpec(assetId, size));
                PaintingResourcePack.writeVariant(packRoot, definition);
                definitions.put(definition.variantId(), definition);
            }
        }
        return Map.copyOf(definitions);
    }

    public static Path texturePath(Path packRoot, PaintingVariantDefinition definition) {
        String pathId = definition.variantId().substring(definition.variantId().indexOf(':') + 1);
        return packRoot.resolve("assets").resolve("wallcanvas").resolve("textures")
                .resolve("painting").resolve(pathId + ".png");
    }
}
