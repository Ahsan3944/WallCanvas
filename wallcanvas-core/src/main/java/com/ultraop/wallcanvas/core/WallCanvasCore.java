package com.ultraop.wallcanvas.core;

import java.util.Set;

public final class WallCanvasCore {
    public static final String ID = "wallcanvas";
    public static final String NAME = "WallCanvas";
    public static final String MINECRAFT_VERSION = "1.21.11";

    private static final Set<String> SUPPORTED_IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg", "webp");

    private WallCanvasCore() {
    }

    public static boolean isSupportedImageExtension(String extension) {
        return extension != null && SUPPORTED_IMAGE_EXTENSIONS.contains(extension.toLowerCase());
    }
}
