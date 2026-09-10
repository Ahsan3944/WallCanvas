package com.ultraop.wallcanvas.core.painting;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Generates the server-side data pack that registers WallCanvas Painting variants. */
public final class PaintingDataPackGenerator {
    public static final int DATA_PACK_FORMAT = 94;

    private PaintingDataPackGenerator() {
    }

    public static void generate(Path dataPackRoot, List<PaintingVariantDefinition> definitions) throws IOException {
        Files.createDirectories(dataPackRoot);
        writeVariants(dataPackRoot, definitions);
        writePlaceableTag(dataPackRoot, definitions);
        writePackMetadata(dataPackRoot);
    }

    private static void writeVariants(Path root, List<PaintingVariantDefinition> definitions) throws IOException {
        for (PaintingVariantDefinition definition : definitions) {
            Path file = root.resolve("data").resolve("wallcanvas").resolve("painting_variant")
                    .resolve(fileName(definition.variantId()) + ".json");
            Files.createDirectories(file.getParent());
            String json = "{\n"
                    + "  \"asset_id\": \"wallcanvas:painting/" + fileName(definition.variantId()) + "\",\n"
                    + "  \"width\": " + definition.width() + ",\n"
                    + "  \"height\": " + definition.height() + ",\n"
                    + "  \"author\": {\"text\": \"WallCanvas\"},\n"
                    + "  \"title\": {\"translate\": \"painting.wallcanvas." + fileName(definition.variantId()) + "\"}\n"
                    + "}";
            Files.writeString(file, json, StandardCharsets.UTF_8);
        }
    }

    private static void writePlaceableTag(Path root, List<PaintingVariantDefinition> definitions) throws IOException {
        Path file = root.resolve("data").resolve("minecraft").resolve("tags")
                .resolve("painting_variant").resolve("placeable.json");
        Files.createDirectories(file.getParent());
        List<String> values = new ArrayList<>();
        for (PaintingVariantDefinition definition : definitions) {
            values.add("wallcanvas:" + fileName(definition.variantId()));
        }
        StringBuilder json = new StringBuilder("{\n  \"replace\": false,\n  \"values\": [\n");
        for (int i = 0; i < values.size(); i++) {
            json.append("    \"").append(values.get(i)).append("\"");
            if (i + 1 < values.size()) json.append(',');
            json.append('\n');
        }
        json.append("  ]\n}\n");
        Files.writeString(file, json, StandardCharsets.UTF_8);
    }

    private static void writePackMetadata(Path root) throws IOException {
        Path file = root.resolve("pack.mcmeta");
        Files.createDirectories(file.getParent());
        Files.writeString(file,
                "{\n  \"pack\": {\n    \"pack_format\": " + DATA_PACK_FORMAT
                        + ",\n    \"description\": \"WallCanvas Painting variants\"\n  }\n}\n",
                StandardCharsets.UTF_8);
    }

    private static String fileName(String variantId) {
        int separator = variantId.indexOf(':');
        return separator >= 0 ? variantId.substring(separator + 1) : variantId;
    }
}
