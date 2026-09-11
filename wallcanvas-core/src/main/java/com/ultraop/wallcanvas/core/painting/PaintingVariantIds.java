package com.ultraop.wallcanvas.core.painting;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Deterministic IDs shared by both platform adapters for generated Painting variants. */
public final class PaintingVariantIds {
    public static final String NAMESPACE = "wallcanvas";

    private PaintingVariantIds() {
    }

    public static String forAsset(String assetId, PaintingSize size) {
        String normalized = assetId.replace('\\', '/').trim().toLowerCase(java.util.Locale.ROOT);
        String hash = sha256(normalized + "|" + size.widthBlocks() + "x" + size.heightBlocks()).substring(0, 16);
        return NAMESPACE + ":picture_" + hash;
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte b : digest) result.append(String.format("%02x", b));
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
