package com.ultraop.wallcanvas.core.painting;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** Writes the data-driven Painting resources shared by Paper and Fabric. */
public final class PaintingResourcePack {
    private static final int PACK_FORMAT = 75;

    private PaintingResourcePack() {
    }

    public static void writeVariant(Path packRoot, PaintingVariantDefinition definition) throws IOException {
        String namespace = "wallcanvas";
        String id = definition.variantId().substring(definition.variantId().indexOf(':') + 1);
        Path variant = packRoot.resolve("data").resolve(namespace).resolve("painting_variant").resolve(id + ".json");
        Path atlas = packRoot.resolve("assets").resolve(namespace).resolve("atlases").resolve("paintings.json");
        Path textures = packRoot.resolve("assets").resolve(namespace).resolve("textures").resolve("painting");

        Files.createDirectories(variant.getParent());
        Files.createDirectories(atlas.getParent());
        Files.createDirectories(textures);

        String texture = namespace + ":painting/" + id;
        String json = "{\n" +
                "  \"asset_id\": \"" + escape(definition.assetId()) + "\",\n" +
                "  \"description\": {\"translate\": \"painting.wallcanvas." + id + "\"},\n" +
                "  \"width\": " + definition.width() + ",\n" +
                "  \"height\": " + definition.height() + ",\n" +
                "  \"author\": {\"translate\": \"painting.wallcanvas.author\"}\n" +
                "}\n";
        Files.writeString(variant, json, StandardCharsets.UTF_8);

        String atlasJson = "{\n  \"sources\": [\n    {\n      \"type\": \"directory\",\n      \"source\": \"wallcanvas/painting\",\n      \"prefix\": \"wallcanvas:\"\n    }\n  ]\n}\n";
        Files.writeString(atlas, atlasJson, StandardCharsets.UTF_8);

        Path packMeta = packRoot.resolve("pack.mcmeta");
        if (!Files.exists(packMeta)) {
            Files.writeString(packMeta,
                    "{\n  \"pack\": {\n    \"pack_format\": " + PACK_FORMAT + ",\n    \"description\": \"WallCanvas generated Painting resources\"\n  }\n}\n",
                    StandardCharsets.UTF_8);
        }
    }

    public static void writeAll(Path packRoot, Map<String, PaintingVariantDefinition> definitions) throws IOException {
        for (PaintingVariantDefinition definition : definitions.values()) {
            writeVariant(packRoot, definition);
        }
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
