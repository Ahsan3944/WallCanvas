package com.ultraop.wallcanvas.fabric;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.ultraop.wallcanvas.core.WallCanvasCore;
import com.ultraop.wallcanvas.core.library.ImageLibrary;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;

public final class WallCanvasFabric implements ModInitializer {
    private static Path imageDirectory;

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal("wallcanvas")
                        .then(Commands.literal("list").executes(context -> listImages(context.getSource().getServer())))
                        .then(Commands.literal("info")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(context -> {
                                            Path image = imageDirectory.resolve(StringArgumentType.getString(context, "name")).normalize();
                                            if (!image.getParent().equals(imageDirectory) || !java.nio.file.Files.isRegularFile(image)) {
                                                context.getSource().sendFailure(Component.literal("Image not found."));
                                                return 0;
                                            }
                                            context.getSource().sendSuccess(() -> Component.literal("Image: " + image.getFileName()), false);
                                            return 1;
                                        }))));
    }

    private int listImages(MinecraftServer server) {
        try {
            var images = ImageLibrary.scan(imageDirectory);
            server.getPlayerList().broadcastSystemMessage(Component.literal("WallCanvas images (" + images.size() + ")"), false);
            return images.size();
        } catch (Exception e) {
            return 0;
        }
    }

    private void onServerStarted(MinecraftServer server) {
        imageDirectory = server.getSavePath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("wallcanvas").resolve("images");
        try {
            java.nio.file.Files.createDirectories(imageDirectory);
        } catch (java.io.IOException exception) {
            throw new RuntimeException("Unable to create WallCanvas image directory", exception);
        }
        System.out.println("[WallCanvas] " + WallCanvasCore.NAME + " " + WallCanvasCore.MINECRAFT_VERSION + " initialized. Image library: " + imageDirectory);
    }
}
