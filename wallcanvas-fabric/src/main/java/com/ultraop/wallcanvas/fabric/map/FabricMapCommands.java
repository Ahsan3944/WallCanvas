package com.ultraop.wallcanvas.fabric.map;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.ultraop.wallcanvas.core.DisplayStore;
import com.ultraop.wallcanvas.core.WallCanvasCore;
import com.ultraop.wallcanvas.core.library.ImageLibrary;
import com.ultraop.wallcanvas.core.map.MapSpec;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public final class FabricMapCommands {
    private static Path picturesDirectory;
    private static DisplayStore displayStore;

    private FabricMapCommands() {
    }

    public static void configure(Path pictures, DisplayStore store) {
        picturesDirectory = pictures;
        displayStore = store;
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            var createPicture = Commands.argument("picture", StringArgumentType.word())
                    .executes(context -> create(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "picture")));

            var height = Commands.argument("height", IntegerArgumentType.integer(1, 16))
                    .executes(context -> create(context.getSource().getPlayerOrException(),
                            StringArgumentType.getString(context, "picture"),
                            DoubleArgumentType.getDouble(context, "x"),
                            DoubleArgumentType.getDouble(context, "y"),
                            DoubleArgumentType.getDouble(context, "z"),
                            IntegerArgumentType.getInteger(context, "width"),
                            IntegerArgumentType.getInteger(context, "height")));
            var width = Commands.argument("width", IntegerArgumentType.integer(1, 16)).then(height);
            var z = Commands.argument("z", DoubleArgumentType.doubleArg())
                    .executes(context -> create(context.getSource().getPlayerOrException(),
                            StringArgumentType.getString(context, "picture"),
                            DoubleArgumentType.getDouble(context, "x"),
                            DoubleArgumentType.getDouble(context, "y"),
                            DoubleArgumentType.getDouble(context, "z")))
                    .then(width);
            var y = Commands.argument("y", DoubleArgumentType.doubleArg()).then(z);
            var x = Commands.argument("x", DoubleArgumentType.doubleArg()).then(y);
            createPicture.then(x);
            var create = Commands.literal("create").then(createPicture);

            var givePicture = Commands.argument("picture", StringArgumentType.word())
                    .executes(context -> give(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "picture")));
            var give = Commands.literal("give").then(givePicture);

            var display = Commands.argument("display", StringArgumentType.word())
                    .executes(context -> remove(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "display")));
            var remove = Commands.literal("remove").then(display);

            var map = Commands.literal("map").then(create).then(give).then(remove);
            dispatcher.register(Commands.literal("wallcanvas").then(map));
        });
    }

    private static int create(ServerPlayer player, String asset) {
        return create(player, asset, player.getX(), player.getY(), player.getZ(), 1, 1);
    }

    private static int create(ServerPlayer player, String asset, double x, double y, double z) {
        return create(player, asset, x, y, z, 1, 1);
    }

    private static int create(ServerPlayer player, String asset, double x, double y, double z, int width, int height) {
        try {
            if (!isPicture(asset)) {
                player.sendSystemMessage(Component.literal("Picture not found."));
                return 0;
            }
            ServerLevel world = (ServerLevel) player.level();
            UUID id = new FabricMapDisplayManager(picturesDirectory, displayStore)
                    .create(world, new MapSpec(asset, width, height, x, y, z), player.getYRot());
            player.sendSystemMessage(Component.literal("Created WallCanvas Map display " + id + "."));
            return 1;
        } catch (Exception exception) {
            player.sendSystemMessage(Component.literal("Unable to create map: " + safeMessage(exception)));
            return 0;
        }
    }

    private static int give(ServerPlayer player, String asset) {
        try {
            if (!isPicture(asset)) {
                player.sendSystemMessage(Component.literal("Picture not found."));
                return 0;
            }
            ServerLevel world = (ServerLevel) player.level();
            ItemStack map = new FabricMapDisplayManager(picturesDirectory, displayStore).createMapItem(world, asset);
            if (!player.getInventory().add(map)) player.drop(map, false);
            player.sendSystemMessage(Component.literal("Gave WallCanvas Map for '" + asset + "'."));
            return 1;
        } catch (Exception exception) {
            player.sendSystemMessage(Component.literal("Unable to create map: " + safeMessage(exception)));
            return 0;
        }
    }

    private static int remove(ServerPlayer player, String rawId) {
        try {
            UUID id = UUID.fromString(rawId);
            ServerLevel world = (ServerLevel) player.level();
            int removed = new FabricMapDisplayManager(picturesDirectory, displayStore).remove(world, id);
            player.sendSystemMessage(Component.literal("Removed " + removed + " map display entity(s)."));
            return 1;
        } catch (IllegalArgumentException exception) {
            player.sendSystemMessage(Component.literal("Invalid display UUID."));
            return 0;
        } catch (Exception exception) {
            player.sendSystemMessage(Component.literal("Unable to remove map: " + safeMessage(exception)));
            return 0;
        }
    }

    private static boolean isPicture(String name) {
        try {
            Path path = picturesDirectory.resolve(name).normalize();
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
