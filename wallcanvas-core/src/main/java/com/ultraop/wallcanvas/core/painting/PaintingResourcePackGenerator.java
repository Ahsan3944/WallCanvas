package com.ultraop.wallcanvas.core.painting;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Generates the data/resource-pack files required for a WallCanvas Painting variant. */
public final class PaintingResourcePackGenerator {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private PaintingResourcePackGenerator() {
    }

    public static PaintingVariantDefinition generate(Path picture, Path packRoot, PaintingSpec spec) throws IOException {
        PaintingVariantDefinition definition = PaintingVariantFactory.create(spec);
        PaintingTextureGenerator.generate(picture, packRoot, definition);
        writeVariant(packRoot, definition);
        writePaintingAtlas(packRoot);
        writePlaceableTag(packRoot, definition);
        writePackMetadata(packRoot);
        return definition;
    }

    private static void writeVariant(Path packRoot, PaintingVariantDefinition definition) throws IOException {
        Path file = PaintingResourcePackLayout.variantJson(packRoot, definition.variantId());
        Files.createDirectories(file.getParent());
        String id = definition.variantId().substring(definition.variantId().indexOf(':') + 1);
        String json = GSON.toJson(new VariantJson(
                definition.assetId(),
                definition.width(),
                definition.height(),
                "WallCanvas",
                "painting.wallcanvas." + id));
        Files.writeString(file, json, StandardCharsets.UTF_8);
    }

    private static void writePaintingAtlas(Path packRoot) throws IOException {
        Path file = PaintingResourcePackLayout.atlas(packRoot);
        Files.createDirectories(file.getParent());
        String json = GSON.toJson(new AtlasJson(List.of(
                new AtlasSource("directory", "wallcanvas/painting", "wallcanvas:")
        )));
        Files.writeString(file, json, StandardCharsets.UTF_8);
    }

    private static void writePlaceableTag(Path packRoot, PaintingVariantDefinition definition) throws IOException {
        Path file = packRoot.resolve("data").resolve("minecraft").resolve("tags")
                .resolve("painting_variant").resolve("placeable.json");
        Files.createDirectories(file.getParent());
        String id = definition.variantId().startsWith("wallcanvas:")
                ? definition.variantId()
                : "wallcanvas:" + definition.variantId();
        String json = GSON.toJson(new PlaceableTag(false, List.of(id)));
        Files.writeString(file, json, StandardCharsets.UTF_8);
    }

    private static void writePackMetadata(Path packRoot) throws IOException {
        Path file = PaintingResourcePackLayout.packMcmeta(packRoot);
        Files.createDirectories(file.getParent());
        String json = GSON.toJson(new PackMetadata(
                new PackInfo(PaintingResourcePackLayout.PACK_FORMAT,
                        "WallCanvas generated Painting pack")));
        Files.writeString(file, json, StandardCharsets.UTF_8);
    }

    private record VariantJson(String asset_id, int width, int height, String author, String title) {
    }

    private record AtlasJson(List<AtlasSource> sources) {
    }

    private record AtlasSource(String type, String source, String prefix) {
    }

    private record PlaceableTag(boolean replace, List<String> values) {
    }

    private record PackMetadata(PackInfo pack) {
    }

    private record PackInfo(int pack_format, String description) {
    }
}
