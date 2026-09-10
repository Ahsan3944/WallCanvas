package com.ultraop.wallcanvas.core.library;

import com.ultraop.wallcanvas.core.WallCanvasCore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

public final class ImageLibrary {
    private ImageLibrary() {
    }

    public static List<Path> scan(Path root) throws IOException {
        Files.createDirectories(root);
        try (var stream = Files.walk(root, 1)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> WallCanvasCore.isSupportedImageExtension(extension(path)))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER))
                    .toList();
        }
    }

    public static String extension(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1);
    }
}
