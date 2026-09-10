package com.ultraop.wallcanvas.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class DisplayStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path file;
    private final List<DisplayDefinition> displays = new ArrayList<>();

    public DisplayStore(Path file) { this.file = file; }

    public List<DisplayDefinition> all() { return List.copyOf(displays); }

    public void add(DisplayDefinition definition) throws IOException {
        displays.add(definition);
        save();
    }

    public boolean remove(UUID id) throws IOException {
        boolean removed = displays.removeIf(d -> d.id().equals(id));
        if (removed) save();
        return removed;
    }

    public void load() throws IOException {
        displays.clear();
        if (!Files.exists(file)) return;
        String json = Files.readString(file, StandardCharsets.UTF_8);
        DisplayDefinition[] values = GSON.fromJson(json, DisplayDefinition[].class);
        if (values != null) displays.addAll(List.of(values));
    }

    public void save() throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, GSON.toJson(displays), StandardCharsets.UTF_8);
    }
}
