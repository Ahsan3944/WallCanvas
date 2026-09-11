package com.ultraop.wallcanvas.core.painting;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Persists the Painting size profiles that must be generated before server startup. */
public final class PaintingSizeStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path file;
    private final Set<String> sizeNames = new LinkedHashSet<>();

    public PaintingSizeStore(Path file) {
        this.file = file;
    }

    public List<PaintingSize> all() {
        List<PaintingSize> result = new ArrayList<>();
        for (String name : sizeNames) {
            try {
                result.add(PaintingSize.fromName(name));
            } catch (IllegalArgumentException ignored) {
                // Ignore obsolete/corrupt entries; they are removed on save.
            }
        }
        return List.copyOf(result);
    }

    public boolean contains(PaintingSize size) {
        return sizeNames.contains(size.name());
    }

    public boolean add(PaintingSize size) throws IOException {
        boolean changed = sizeNames.add(size.name());
        if (changed) save();
        return changed;
    }

    public void load() throws IOException {
        sizeNames.clear();
        if (!Files.exists(file)) {
            sizeNames.add(PaintingSize.DEFAULT.name());
            sizeNames.add(PaintingSize.LARGE.name());
            save();
            return;
        }

        String json = Files.readString(file, StandardCharsets.UTF_8);
        String[] values = GSON.fromJson(json, String[].class);
        if (values != null) {
            for (String value : values) {
                try {
                    sizeNames.add(PaintingSize.fromName(value).name());
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid entries.
                }
            }
        }
        sizeNames.add(PaintingSize.DEFAULT.name());
        sizeNames.add(PaintingSize.LARGE.name());
        save();
    }

    public void save() throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.writeString(file, GSON.toJson(sizeNames), StandardCharsets.UTF_8);
    }
}
