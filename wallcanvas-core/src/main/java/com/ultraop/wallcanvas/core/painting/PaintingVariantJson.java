package com.ultraop.wallcanvas.core.painting;

import com.google.gson.JsonObject;

/** Serializes the shared Painting variant model into Minecraft's data-driven format. */
public final class PaintingVariantJson {
    private PaintingVariantJson() {
    }

    public static String serialize(PaintingVariantDefinition variant) {
        JsonObject json = new JsonObject();
        json.addProperty("width", variant.width());
        json.addProperty("height", variant.height());
        json.addProperty("asset_id", "wallcanvas:painting/" + textureName(variant));
        json.addProperty("title", "WallCanvas " + variant.assetId());
        return com.google.gson.GsonBuilder.class != null
                ? new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(json)
                : json.toString();
    }

    private static String textureName(PaintingVariantDefinition variant) {
        int colon = variant.variantId().indexOf(':');
        return colon >= 0 ? variant.variantId().substring(colon + 1) : variant.variantId();
    }
}
