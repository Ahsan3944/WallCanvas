package com.ultraop.wallcanvas.fabric;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.ultraop.wallcanvas.core.DisplayStore;
import com.ultraop.wallcanvas.core.WallCanvasCore;
import com.ultraop.wallcanvas.core.library.ImageImporter;
import com.ultraop.wallcanvas.core.library.ImageLibrary;
import com.ultraop.wallcanvas.core.painting.PaintingPackBuilder;
import com.ultraop.wallcanvas.core.painting.PaintingSize;
import com.ultraop.wallcanvas.core.painting.PaintingSpec;
import com.ultraop.wallcanvas.fabric.painting.FabricPaintingItems;
import com.ultraop.wallcanvas.fabric.painting.FabricPaintingPlacement;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public final class WallCanvasFabric implements ModInitializer {
    private static Path picturesDirectory;
    private static FabricPaintingItems paintingItems;
    private static DisplayStore displayStore;

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(this::onServerStarting);
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            var wallCanvas = Commands.literal("wallcanvas");

            wallCanvas.then(Commands.literal("list")
                    .executes(context -> listPictures(context.getSource().getServer())));

            wallCanvas.then(Commands.literal("info")
                    .then(Commands.argument("name", StringArgumentType.word())
                            .suggests((context, builder) -> {
                                for (String name : pictureNames()) builder.suggest(name);
                                return builder.buildFuture();
                            })
                            .executes(context -> {
                                Path picture = picturesDirectory.resolve(
                                        StringArgumentType.getString(context, "name")).normalize();
                                if (!isPictureFile(picture)) {
                                    context.getSource().sendFailure(Component.literal("Picture not found."));
                                    return 0;
                                }
                                context.getSource().sendSuccess(
                                        () -> Component.literal("Picture: " + picture.getFileName()), false);
                                return 1;
                            })));

            wallCanvas.then(Commands.literal("create")
                    .then(Commands.literal("web")
                            .then(Commands.argument("name", StringArgumentType.word())
                                    .then(Commands.argument("url", StringArgumentType.string())
                                            .executes(context -> {
                                                MinecraftServer server = context.getSource().getServer();
                                                String name = StringArgumentType.getString(context, "name");
                                                String url = StringArgumentType.getString(context, "url");
                                                context.getSource().sendSuccess(
                                                        () -> Component.literal("Downloading WallCanvas picture '" + name + "'..."), false);
                                                CompletableFuture.runAsync(() -> {
                                                    try {
                                                        Path imported = ImageImporter.importUrl(url, picturesDirectory, name);
                                                        Path worldRoot = server.getWorldPath(LevelResource.ROOT);
                                                        PaintingPackBuilder.rebuildAll(
                                                                picturesDirectory,
                                                                PaintingPackBuilder.defaultPackRoot(worldRoot),
                                                                PaintingPackBuilder.defaultDataPackRoot(worldRoot));
                                                        server.execute(() -> context.getSource().sendSuccess(
                                                                () -> Component.literal("Imported '" + imported.getFileName()
                                                                        + "'. Restart the server to register the new Painting variant."), false));
                                                    } catch (Exception exception) {
                                                        server.execute(() -> context.getSource().sendFailure(
                                                                Component.literal("Web import failed: " + safeMessage(exception))));
                                                    }
                                                });
                                                return 1;
                                            }))));

            wallCanvas.then(Commands.literal("give")
                    .then(Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player())
                            .then(Commands.argument("picture", StringArgumentType.word())
                                    .suggests((context, builder) -> {
                                        for (String name : pictureNames()) builder.suggest(name);
                                        return builder.buildFuture();
                                    })
                                    .executes(context -> {
                                        ServerPlayer target = net.minecraft.commands.EntityArgument.getPlayer(
                                                context, "player");
                                        String pictureName = StringArgumentType.getString(context, "picture");
                                        if (!isPictureFile(picturesDirectory.resolve(pictureName).normalize())) {
                                            context.getSource().sendFailure(Component.literal("Picture not found."));
                                            return 0;
                                        }
                                        ItemStack item = paintingItems.create(
                                                new PaintingSpec(pictureName, PaintingSize.DEFAULT),
                                                context.getSource().getServer().registryAccess());
                                        if (!target.getInventory().add(item)) {
                                            target.drop(item, false);
                                            context.getSource().sendFailure(Component.literal(
                                                    "Target inventory is full; Painting was dropped nearby."));
                                        } else {
                                            context.getSource().sendSuccess(() -> Component.literal(
                                                    "Gave WallCanvas Painting '" + pictureName + "' to "
                                                            + target.getName().getString() + "."), true);
                                        }
                                        return 1;
                                    })));

            dispatcher.register(wallCanvas);
        });
    }

    private int listPictures(MinecraftServer server) {
        try {
            var pictures = ImageLibrary.scan(picturesDirectory);
            server.getPlayerList().broadcastSystemMessage(
                    Component.literal("WallCanvas pictures (" + pictures.size() + ")"), false);
            return pictures.size();
        } catch (Exception ignored) {
            return 0;
        }
    }

    private void onServerStarting(MinecraftServer server) {
        Path worldRoot = server.getWorldPath(LevelResource.ROOT);
        picturesDirectory = worldRoot.resolve("wallcanvas").resolve("pictures");
        paintingItems = new FabricPaintingItems();
        displayStore = new DisplayStore(worldRoot.resolve("wallcanvas").resolve("displays.json"));
        Path packRoot = PaintingPackBuilder.defaultPackRoot(worldRoot);
        Path dataPackRoot = PaintingPackBuilder.defaultDataPackRoot(worldRoot);
        try {
            Files.createDirectories(picturesDirectory);
            displayStore.load();
            var variants = PaintingPackBuilder.rebuildAll(picturesDirectory, packRoot, dataPackRoot);
            System.out.println("[WallCanvas] Generated " + variants.size()
                    + " Painting variant(s) before world loading at " + packRoot + " and " + dataPackRoot);
        } catch (IOException exception) {
            throw new RuntimeException("Unable to initialize WallCanvas storage/resources", exception);
        }
        new FabricPaintingPlacement(displayStore).register();

        System.out.println("[WallCanvas] " + WallCanvasCore.NAME + " "
                + WallCanvasCore.MINECRAFT_VERSION + " initialized. Picture library: " + picturesDirectory);
        System.out.println("[WallCanvas] Loaded " + displayStore.all().size() + " persistent display(s).");
    }

    private static Iterable<String> pictureNames() {
        try {
            return ImageLibrary.scan(picturesDirectory).stream()
                    .map(path -> path.getFileName().toString())
                    .toList();
        } catch (Exception ignored) {
            return java.util.List.of();
        }
    }

    private static boolean isPictureFile(Path path) {
        try {
            return path.getParent().equals(picturesDirectory.toAbsolutePath().normalize())
                    && Files.isRegularFile(path)
                    && WallCanvasCore.isSupportedImageExtension(ImageLibrary.extension(path));
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }
}
