package com.ultraop.wallcanvas.fabric;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.ultraop.wallcanvas.core.WallCanvasCore;
import com.ultraop.wallcanvas.core.library.ImageLibrary;
import com.ultraop.wallcanvas.core.painting.PaintingSize;
import com.ultraop.wallcanvas.core.painting.PaintingSpec;
import com.ultraop.wallcanvas.fabric.painting.FabricPaintingItems;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.nio.file.Files;
import java.nio.file.Path;

public final class WallCanvasFabric implements ModInitializer {
    private static Path picturesDirectory;
    private static FabricPaintingItems paintingItems;

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal("wallcanvas")
                        .then(Commands.literal("list")
                                .executes(context -> listPictures(context.getSource().getServer())))
                        .then(Commands.literal("info")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            for (String name : pictureNames()) builder.suggest(name);
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> {
                                            Path picture = picturesDirectory.resolve(StringArgumentType.getString(context, "name")).normalize();
                                            if (!isPictureFile(picture)) {
                                                context.getSource().sendFailure(Component.literal("Picture not found."));
                                                return 0;
                                            }
                                            context.getSource().sendSuccess(() -> Component.literal("Picture: " + picture.getFileName()), false);
                                            return 1;
                                        })))
                        .then(Commands.literal("give")
                                .then(Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player())
                                        .then(Commands.argument("picture", StringArgumentType.word())
                                                .suggests((context, builder) -> {
                                                    for (String name : pictureNames()) builder.suggest(name);
                                                    return builder.buildFuture();
                                                })
                                                .executes(context -> {
                                                    ServerPlayer target = net.minecraft.commands.arguments.EntityArgument.getPlayer(context, "player");
                                                    String pictureName = StringArgumentType.getString(context, "picture");
                                                    if (!isPictureFile(picturesDirectory.resolve(pictureName).normalize())) {
                                                        context.getSource().sendFailure(Component.literal("Picture not found."));
                                                        return 0;
                                                    }
                                                    ItemStack item = paintingItems.create(new PaintingSpec(pictureName, PaintingSize.DEFAULT));
                                                    if (!target.getInventory().add(item)) {
                                                        target.drop(item, false);
                                                        context.getSource().sendFailure(Component.literal("Target inventory is full; Painting was dropped nearby."));
                                                    } else {
                                                        context.getSource().sendSuccess(() -> Component.literal(
                                                                "Gave WallCanvas Painting '" + pictureName + "' to " + target.getName().getString() + "."), true);
                                                    }
                                                    return 1;
                                                }))));
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

    private void onServerStarted(MinecraftServer server) {
        picturesDirectory = server.getSavePath(net.minecraft.world.level.storage.LevelResource.ROOT)
                .resolve("wallcanvas")
                .resolve("pictures");
        paintingItems = new FabricPaintingItems();
        try {
            Files.createDirectories(picturesDirectory);
        } catch (java.io.IOException exception) {
            throw new RuntimeException("Unable to create WallCanvas pictures directory", exception);
        }
        System.out.println("[WallCanvas] " + WallCanvasCore.NAME + " "
                + WallCanvasCore.MINECRAFT_VERSION + " initialized. Picture library: " + picturesDirectory);
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
}
