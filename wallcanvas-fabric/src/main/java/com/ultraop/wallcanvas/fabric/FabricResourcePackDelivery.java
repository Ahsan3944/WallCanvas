package com.ultraop.wallcanvas.fabric;

import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.ultraop.wallcanvas.core.painting.PaintingResourcePackArchive;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

public final class FabricResourcePackDelivery {
    private static final String CONFIG_FILE = "wallcanvas.properties";
    private static final String URL_KEY = "resource-pack.url";

    private final Path configFile;
    private String resourcePackUrl = "";
    private String resourcePackHash = "";

    public FabricResourcePackDelivery(Path configDirectory) {
        this.configFile = configDirectory.resolve(CONFIG_FILE);
    }

    public void initialize(Path archive) throws IOException {
        Files.createDirectories(configFile.getParent());
        Properties properties = new Properties();
        if (Files.isRegularFile(configFile)) {
            try (var input = Files.newInputStream(configFile)) {
                properties.load(input);
            }
        }

        resourcePackUrl = properties.getProperty(URL_KEY, "").trim();
        if (!resourcePackUrl.isBlank()) {
            if (!resourcePackUrl.startsWith("https://")) {
                throw new IOException("resource-pack.url must use HTTPS");
            }
            resourcePackHash = PaintingResourcePackArchive.sha1Hex(archive);
        }

        properties.setProperty(URL_KEY, resourcePackUrl);
        try (var output = Files.newOutputStream(configFile)) {
            properties.store(output, "WallCanvas Fabric configuration");
        }
    }

    public void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (resourcePackUrl.isBlank()) return;
            ServerPlayer player = handler.player;
            player.connection.send(new ClientboundResourcePackPushPacket(
                    UUID.randomUUID(),
                    resourcePackUrl,
                    resourcePackHash,
                    true,
                    Optional.of(Component.literal("WallCanvas custom Painting artwork"))));
        });
    }

    public boolean isConfigured() {
        return !resourcePackUrl.isBlank();
    }

    public String url() {
        return resourcePackUrl;
    }
}
