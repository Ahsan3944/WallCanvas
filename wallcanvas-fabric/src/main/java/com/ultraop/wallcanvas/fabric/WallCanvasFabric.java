package com.ultraop.wallcanvas.fabric;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.ultraop.wallcanvas.core.DisplayStore;
import com.ultraop.wallcanvas.core.WallCanvasCore;
import com.ultraop.wallcanvas.core.library.ImageImporter;
import com.ultraop.wallcanvas.core.library.ImageLibrary;
import com.ultraop.wallcanvas.core.painting.PaintingPackBuilder;
import com.ultraop.wallcanvas.core.painting.PaintingResourcePackArchive;
import com.ultraop.wallcanvas.core.painting.PaintingSize;
import com.ultraop.wallcanvas.core.painting.PaintingSizeStore;
import com.ultraop.wallcanvas.core.painting.PaintingSpec;
import com.ultraop.wallcanvas.fabric.map.FabricMapCommands;
import com.ultraop.wallcanvas.fabric.painting.FabricPaintingItems;
import com.ultraop.wallcanvas.fabric.painting.FabricPaintingPlacement;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
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
    private static PaintingSizeStore paintingSizeStore;
    private static FabricPaintingItems paintingItems;
    private static DisplayStore displayStore;
    private static FabricResourcePackDelivery resourcePackDelivery;

    @Override
    public void onInitialize() {
        resourcePackDelivery = new FabricResourcePackDelivery(FabricLoader.getInstance().getConfigDir());
        resourcePackDelivery.register();
        ServerLifecycleEvents.SERVER_STARTING.register(this::onServerStarting);
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            var wallCanvas = Commands.literal("wallcanvas");
            var list = Commands.literal("list").executes(context -> listPictures(context.getSource().getServer()));
            wallCanvas.then(list);

            var infoName = Commands.argument("name", StringArgumentType.word())
                    .suggests((context, builder) -> { for (String name : pictureNames()) builder.suggest(name); return builder.buildFuture(); })
                    .executes(context -> {
                        Path picture = picturesDirectory.resolve(StringArgumentType.getString(context, "name")).normalize();
                        if (!isPictureFile(picture)) { context.getSource().sendFailure(Component.literal("Picture not found.")); return 0; }
                        context.getSource().sendSuccess(() -> Component.literal("Picture: " + picture.getFileName()), false); return 1;
                    });
            wallCanvas.then(Commands.literal("info").then(infoName));

            var webUrl = Commands.argument("url", StringArgumentType.string()).executes(context -> createWebPicture(
                    context.getSource(), StringArgumentType.getString(context, "name"), StringArgumentType.getString(context, "url")));
            wallCanvas.then(Commands.literal("create").then(Commands.literal("web")
                    .then(Commands.argument("name", StringArgumentType.word()).then(webUrl))));

            SuggestionProvider<CommandSourceStack> pictures = (context, builder) -> {
                for (String name : pictureNames()) builder.suggest(name);
                return builder.buildFuture();
            };
            var givePicture = Commands.argument("picture", StringArgumentType.word()).suggests(pictures)
                    .executes(context -> givePainting(context.getSource(), EntityArgument.getPlayer(context, "player"),
                            StringArgumentType.getString(context, "picture"), PaintingSize.DEFAULT));
            var width = Commands.argument("width", IntegerArgumentType.integer(1, 16));
            var height = Commands.argument("height", IntegerArgumentType.integer(1, 16));
            var pixels = Commands.argument("pixels-per-block", IntegerArgumentType.integer(4, 256));
            var sized = pixels.executes(context -> givePainting(context.getSource(), EntityArgument.getPlayer(context, "player"),
                    StringArgumentType.getString(context, "picture"), PaintingSize.of(
                            IntegerArgumentType.getInteger(context, "width"), IntegerArgumentType.getInteger(context, "height"),
                            IntegerArgumentType.getInteger(context, "pixels-per-block"))));
            height.then(sized);
            width.then(height);
            givePicture.then(width);
            var givePlayer = Commands.argument("player", EntityArgument.player()).then(givePicture);
            wallCanvas.then(Commands.literal("give").then(givePlayer));

            wallCanvas.then(FabricMapCommands.mapCommand());
            dispatcher.register(wallCanvas);
        });
    }

    private int createWebPicture(CommandSourceStack source, String name, String url) {
        MinecraftServer server = source.getServer();
        source.sendSuccess(() -> Component.literal("Downloading WallCanvas picture '" + name + "'..."), false);
        CompletableFuture.runAsync(() -> {
            try {
                Path imported = ImageImporter.importUrl(url, picturesDirectory, name);
                Path worldRoot = server.getWorldPath(LevelResource.ROOT);
                Path packRoot = PaintingPackBuilder.defaultPackRoot(worldRoot);
                PaintingPackBuilder.rebuildAll(picturesDirectory, packRoot, PaintingPackBuilder.defaultDataPackRoot(worldRoot), paintingSizeStore.all());
                Path archive = PaintingResourcePackArchive.zip(packRoot);
                resourcePackDelivery.initialize(archive);
                server.execute(() -> source.sendSuccess(() -> Component.literal("Imported '" + imported.getFileName() + "'. Restart the server to register new Painting variants."), false));
            } catch (Exception exception) {
                server.execute(() -> source.sendFailure(Component.literal("Web import failed: " + safeMessage(exception))));
            }
        });
        return 1;
    }

    private int givePainting(CommandSourceStack source, ServerPlayer target, String pictureName, PaintingSize size) {
        if (!isPictureFile(picturesDirectory.resolve(pictureName).normalize())) { source.sendFailure(Component.literal("Picture not found.")); return 0; }
        if (!paintingSizeStore.contains(size)) {
            try {
                paintingSizeStore.add(size);
                Path worldRoot = source.getServer().getWorldPath(LevelResource.ROOT);
                Path packRoot = PaintingPackBuilder.defaultPackRoot(worldRoot);
                PaintingPackBuilder.rebuildAll(picturesDirectory, packRoot, PaintingPackBuilder.defaultDataPackRoot(worldRoot), paintingSizeStore.all());
                Path archive = PaintingResourcePackArchive.zip(packRoot);
                resourcePackDelivery.initialize(archive);
                source.sendFailure(Component.literal("Registered Painting size " + size + ". Restart the server before using it."));
            } catch (IOException exception) { source.sendFailure(Component.literal("Unable to register Painting size: " + safeMessage(exception))); }
            return 0;
        }
        try {
            ItemStack item = paintingItems.create(new PaintingSpec(pictureName, size), source.getServer().registryAccess());
            if (!target.getInventory().add(item)) target.drop(item, false);
            source.sendSuccess(() -> Component.literal("Gave WallCanvas Painting '" + pictureName + "' (" + size.widthBlocks() + "x" + size.heightBlocks() + " blocks, " + size.pixelsPerBlock() + " px/block) to " + target.getName().getString() + "."), true);
            return 1;
        } catch (IllegalStateException exception) { source.sendFailure(Component.literal("Painting variant is not registered yet: " + exception.getMessage())); return 0; }
    }

    private int listPictures(MinecraftServer server) {
        try { var pictures = ImageLibrary.scan(picturesDirectory); server.getPlayerList().broadcastSystemMessage(Component.literal("WallCanvas pictures (" + pictures.size() + ")"), false); return pictures.size(); }
        catch (Exception ignored) { return 0; }
    }

    private void onServerStarting(MinecraftServer server) {
        Path worldRoot = server.getWorldPath(LevelResource.ROOT);
        Path storageRoot = worldRoot.resolve("wallcanvas");
        picturesDirectory = storageRoot.resolve("pictures");
        paintingSizeStore = new PaintingSizeStore(storageRoot.resolve("painting-sizes.json"));
        paintingItems = new FabricPaintingItems();
        displayStore = new DisplayStore(storageRoot.resolve("displays.json"));
        Path packRoot = PaintingPackBuilder.defaultPackRoot(worldRoot);
        Path dataPackRoot = PaintingPackBuilder.defaultDataPackRoot(worldRoot);
        try {
            Files.createDirectories(picturesDirectory);
            paintingSizeStore.load();
            displayStore.load();
            FabricMapCommands.configure(picturesDirectory, displayStore);
            var variants = PaintingPackBuilder.rebuildAll(picturesDirectory, packRoot, dataPackRoot, paintingSizeStore.all());
            Path archive = PaintingResourcePackArchive.zip(packRoot);
            resourcePackDelivery.initialize(archive);
            System.out.println("[WallCanvas] Generated " + variants.size() + " Painting variant(s) using sizes " + paintingSizeStore.all());
            if (resourcePackDelivery.isConfigured()) System.out.println("[WallCanvas] Fabric resource pack delivery enabled: " + resourcePackDelivery.url());
            else System.out.println("[WallCanvas] Fabric resource pack delivery disabled. Configure config/wallcanvas.properties -> resource-pack.url");
        } catch (IOException exception) { throw new RuntimeException("Unable to initialize WallCanvas storage/resources", exception); }
        new FabricPaintingPlacement(displayStore).register();
        System.out.println("[WallCanvas] " + WallCanvasCore.NAME + " " + WallCanvasCore.MINECRAFT_VERSION + " initialized. Picture library: " + picturesDirectory);
        System.out.println("[WallCanvas] Loaded " + displayStore.all().size() + " persistent display(s).");
    }

    private static Iterable<String> pictureNames() {
        try { return ImageLibrary.scan(picturesDirectory).stream().map(path -> path.getFileName().toString()).toList(); }
        catch (Exception ignored) { return java.util.List.of(); }
    }

    private static boolean isPictureFile(Path path) {
        try { return path.getParent().equals(picturesDirectory.toAbsolutePath().normalize()) && Files.isRegularFile(path) && WallCanvasCore.isSupportedImageExtension(ImageLibrary.extension(path)); }
        catch (Exception ignored) { return false; }
    }

    private static String safeMessage(Exception exception) {
        String message = exception.getMessage(); return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }
}
