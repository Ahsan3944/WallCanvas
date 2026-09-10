package com.ultraop.wallcanvas.fabric;

import com.ultraop.wallcanvas.core.WallCanvasCore;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;

public final class WallCanvasFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
    }

    private void onServerStarted(MinecraftServer server) {
        Path imageDirectory = server.getSavePath(net.minecraft.world.level.storage.LevelResource.ROOT)
                .resolve("wallcanvas")
                .resolve("images");
        try {
            java.nio.file.Files.createDirectories(imageDirectory);
        } catch (java.io.IOException exception) {
            throw new RuntimeException("Unable to create WallCanvas image directory", exception);
        }

        System.out.println("[WallCanvas] " + WallCanvasCore.NAME + " initialized. Image library: " + imageDirectory);
    }
}
