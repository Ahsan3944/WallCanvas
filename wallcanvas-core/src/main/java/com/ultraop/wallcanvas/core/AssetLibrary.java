package com.ultraop.wallcanvas.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public final class AssetLibrary {
    private static final List<String> EXTENSIONS = List.of(".png", ".jpg", ".jpeg", ".webp");
    private final Path directory;

    public AssetLibrary(Path directory) {
        this.directory = directory;
    }

    public Path directory() { return directory; }

    public void ensureDirectory() throws IOException {
        Files.createDirectories(directory);
    }

    public List<Path> listImages() throws IOException {
        ensureDirectory();
        try (var stream = Files.list(directory)) {
            return stream.filter(Files::isRegularFile)
                    .filter(p -> EXTENSIONS.stream().anyMatch(e -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(e)))
                    .sorted()
                    .toList();
        }
    }
}
