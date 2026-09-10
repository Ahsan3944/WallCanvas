package com.ultraop.wallcanvas.core.painting;

import com.ultraop.wallcanvas.core.library.ImageLibrary;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Builds all generated Painting assets from the server picture library. */
public final class PaintingPackBuilder {
    private PaintingPackBuilder() {
    }

    public static List<PaintingVariantDefinition> rebuild(Path picturesDirectory, Path packRoot,
                                                           PaintingSize size) throws IOException {
        Files.createDirectories(packRoot);
        List<PaintingVariantDefinition> generated = new ArrayList<>();
        for (Path picture : ImageLibrary.scan(picturesDirectory)) {
            try {
                PaintingSpec spec = new PaintingSpec(picture.getFileName().toString(), size);
                generated.add(PaintingResourcePackGenerator.generate(picture, packRoot, spec));
            } catch (IOException exception) {
                throw new IOException("Unable to generate Painting asset for " + picture.getFileName(), exception);
            }
        }
        return List.copyOf(generated);
    }

    public static List<PaintingVariantDefinition> rebuildAll(Path picturesDirectory, Path packRoot,
                                                               Path dataPackRoot) throws IOException {
        List<PaintingVariantDefinition> all = new ArrayList<>();
        all.addAll(rebuild(picturesDirectory, packRoot, PaintingSize.DEFAULT));
        all.addAll(rebuild(picturesDirectory, packRoot, PaintingSize.LARGE));
        PaintingDataPackGenerator.generate(dataPackRoot, all);
        return List.copyOf(all);
    }

    public static Path defaultPackRoot(Path serverRoot) {
        return serverRoot.resolve("wallcanvas").resolve("resourcepack");
    }

    public static Path defaultDataPackRoot(Path worldRoot) {
        return worldRoot.resolve("datapacks").resolve("wallcanvas");
    }
}
