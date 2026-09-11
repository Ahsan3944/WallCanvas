package com.ultraop.wallcanvas.paper;

import com.ultraop.wallcanvas.core.DisplayStore;
import com.ultraop.wallcanvas.core.WallCanvasCore;
import com.ultraop.wallcanvas.core.library.ImageImporter;
import com.ultraop.wallcanvas.core.library.ImageLibrary;
import com.ultraop.wallcanvas.core.map.MapSpec;
import com.ultraop.wallcanvas.core.painting.PaintingPackBuilder;
import com.ultraop.wallcanvas.core.painting.PaintingResourcePackArchive;
import com.ultraop.wallcanvas.core.painting.PaintingSize;
import com.ultraop.wallcanvas.core.painting.PaintingSpec;
import com.ultraop.wallcanvas.paper.map.PaperMapDisplayManager;
import com.ultraop.wallcanvas.paper.painting.PaperPaintingItems;
import com.ultraop.wallcanvas.paper.painting.PaperPaintingListener;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class WallCanvasPaper extends JavaPlugin implements CommandExecutor, org.bukkit.command.TabCompleter, Listener {
    private Path picturesDirectory;
    private PaperPaintingItems paintingItems;
    private DisplayStore displayStore;
    private PaperMapDisplayManager mapManager;
    private String resourcePackUrl = "";
    private byte[] resourcePackHash = new byte[0];

    @Override
    public void onLoad() {
        picturesDirectory = getDataFolder().toPath().resolve("pictures");
        try {
            Files.createDirectories(picturesDirectory);
            saveDefaultConfig();
            rebuildGeneratedPacks();
        } catch (IOException exception) {
            getLogger().severe("Unable to initialize WallCanvas Painting resources: " + exception.getMessage());
        }
    }

    @Override
    public void onEnable() {
        if (picturesDirectory == null) {
            getLogger().severe("WallCanvas resources were not initialized during onLoad; disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        displayStore = new DisplayStore(getDataFolder().toPath().resolve("displays.json"));
        try {
            displayStore.load();
        } catch (IOException exception) {
            getLogger().severe("Unable to load WallCanvas displays: " + exception.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        paintingItems = new PaperPaintingItems(this);
        mapManager = new PaperMapDisplayManager(this, picturesDirectory, displayStore);
        getServer().getPluginManager().registerEvents(new PaperPaintingListener(this, displayStore), this);
        getServer().getPluginManager().registerEvents(this, this);
        Bukkit.getScheduler().runTask(this, () -> mapManager.restoreRenderers());

        var command = getCommand("wallcanvas");
        if (command == null) {
            getLogger().severe("wallcanvas command is missing from plugin.yml");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        command.setExecutor(this);
        command.setTabCompleter(this);

        getLogger().info(WallCanvasCore.NAME + " initialized. Picture library: " + picturesDirectory);
        getLogger().info("Loaded " + displayStore.all().size() + " persistent display(s).");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (resourcePackUrl.isBlank()) return;
        event.getPlayer().setResourcePack(resourcePackUrl, resourcePackHash,
                "WallCanvas custom Painting artwork", true);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            try {
                var pictures = ImageLibrary.scan(picturesDirectory);
                sender.sendMessage("WallCanvas pictures (" + pictures.size() + "):");
                for (Path picture : pictures) sender.sendMessage("- " + picture.getFileName());
            } catch (IOException exception) {
                sender.sendMessage("Unable to scan picture library: " + exception.getMessage());
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("info") && args.length >= 2) {
            Path picture = picturesDirectory.resolve(args[1]).normalize();
            if (!isPictureFile(picture)) { sender.sendMessage("Picture not found."); return true; }
            sender.sendMessage("Picture: " + picture.getFileName());
            return true;
        }

        if (args[0].equalsIgnoreCase("create") && args.length >= 4 && args[1].equalsIgnoreCase("web")) {
            String name = args[2];
            String url = args[3];
            sender.sendMessage("Downloading WallCanvas picture '" + name + "'...");
            CompletableFuture.runAsync(() -> {
                try {
                    Path imported = ImageImporter.importUrl(url, picturesDirectory, name);
                    rebuildGeneratedPacks();
                    Bukkit.getScheduler().runTask(this, () -> sender.sendMessage(
                            "Imported '" + imported.getFileName()
                                    + "'. Restart the server to register the new Painting variant."));
                } catch (Exception exception) {
                    Bukkit.getScheduler().runTask(this, () -> sender.sendMessage(
                            "Web import failed: " + safeMessage(exception)));
                }
            });
            return true;
        }

        if (args[0].equalsIgnoreCase("give") && args.length >= 3) {
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) { sender.sendMessage("Player not found or offline."); return true; }
            String pictureName = args[2];
            if (!isPictureFile(picturesDirectory.resolve(pictureName).normalize())) {
                sender.sendMessage("Picture not found."); return true;
            }
            try {
                target.getInventory().addItem(paintingItems.create(new PaintingSpec(pictureName, PaintingSize.DEFAULT)));
                sender.sendMessage("Gave WallCanvas Painting '" + pictureName + "' to " + target.getName() + ".");
            } catch (IllegalStateException exception) {
                sender.sendMessage("Painting variant is not registered yet: " + exception.getMessage());
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("map")) {
            return handleMapCommand(sender, args);
        }

        sender.sendMessage("Usage: /wallcanvas list | /wallcanvas info <picture> | /wallcanvas create web <name> <url> | /wallcanvas give <player> <picture> | /wallcanvas map <create|give|remove> ...");
        return true;
    }

    private boolean handleMapCommand(CommandSender sender, String[] args) {
        if (args.length >= 2 && args[1].equalsIgnoreCase("give")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("A player must use /wallcanvas map give.");
                return true;
            }
            if (args.length < 3 || !isPictureFile(picturesDirectory.resolve(args[2]).normalize())) {
                sender.sendMessage("Usage: /wallcanvas map give <picture>");
                return true;
            }
            try {
                player.getInventory().addItem(mapManager.createMapItem(player.getWorld(), args[2]));
                sender.sendMessage("Gave WallCanvas Map for '" + args[2] + "'.");
            } catch (IOException exception) {
                sender.sendMessage("Unable to create map: " + safeMessage(exception));
            }
            return true;
        }

        if (args.length >= 2 && args[1].equalsIgnoreCase("create")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("A player must use /wallcanvas map create.");
                return true;
            }
            if (args.length < 3 || !isPictureFile(picturesDirectory.resolve(args[2]).normalize())) {
                sender.sendMessage("Usage: /wallcanvas map create <picture> [x y z] [width height]");
                return true;
            }
            try {
                String asset = args[2];
                double x = player.getX();
                double y = player.getY();
                double z = player.getZ();
                int width = 1;
                int height = 1;
                if (args.length >= 6) {
                    x = Double.parseDouble(args[3]);
                    y = Double.parseDouble(args[4]);
                    z = Double.parseDouble(args[5]);
                }
                if (args.length >= 8) {
                    width = Integer.parseInt(args[6]);
                    height = Integer.parseInt(args[7]);
                }
                MapSpec spec = new MapSpec(asset, width, height, x, y, z);
                UUID id = mapManager.create(() -> player.getWorld(), spec, player.getYaw());
                sender.sendMessage("Created WallCanvas Map display " + id + " at "
                        + x + ", " + y + ", " + z + " (" + width + "x" + height + ").");
            } catch (NumberFormatException exception) {
                sender.sendMessage("Coordinates and map size must be valid numbers.");
            } catch (IllegalArgumentException | IOException exception) {
                sender.sendMessage("Unable to create map: " + safeMessage(exception));
            }
            return true;
        }

        if (args.length >= 2 && args[1].equalsIgnoreCase("remove")) {
            if (args.length < 3) {
                sender.sendMessage("Usage: /wallcanvas map remove <display-uuid>");
                return true;
            }
            try {
                int removed = mapManager.remove(UUID.fromString(args[2]));
                sender.sendMessage("Removed " + removed + " map display entity(s).");
            } catch (IllegalArgumentException exception) {
                sender.sendMessage("Invalid display UUID.");
            } catch (IOException exception) {
                sender.sendMessage("Unable to remove map: " + safeMessage(exception));
            }
            return true;
        }

        sender.sendMessage("Usage: /wallcanvas map create <picture> [x y z] [width height] | /wallcanvas map give <picture> | /wallcanvas map remove <display-uuid>");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return filter(List.of("list", "info", "create", "give", "map"), args[0]);
        if (args.length == 2 && args[0].equalsIgnoreCase("info")) return pictureNames(args[1]);
        if (args.length == 2 && args[0].equalsIgnoreCase("create")) return filter(List.of("web"), args[1]);
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) return pictureNames(args[2]);
        if (args.length == 2 && args[0].equalsIgnoreCase("map")) return filter(List.of("create", "give", "remove"), args[1]);
        if (args.length == 3 && args[0].equalsIgnoreCase("map") && (args[1].equalsIgnoreCase("create") || args[1].equalsIgnoreCase("give"))) return pictureNames(args[2]);
        return List.of();
    }

    private List<String> pictureNames(String prefix) {
        try { return filter(ImageLibrary.scan(picturesDirectory).stream().map(path -> path.getFileName().toString()).toList(), prefix); }
        catch (IOException ignored) { return List.of(); }
    }

    private boolean isPictureFile(Path path) {
        try { return path.getParent().equals(picturesDirectory.toAbsolutePath().normalize()) && Files.isRegularFile(path) && WallCanvasCore.isSupportedImageExtension(ImageLibrary.extension(path)); }
        catch (Exception ignored) { return false; }
    }

    private static List<String> filter(List<String> values, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String value : values) if (value.toLowerCase(Locale.ROOT).startsWith(lower)) result.add(value);
        return result;
    }

    private void rebuildGeneratedPacks() throws IOException {
        Path packRoot = PaintingPackBuilder.defaultPackRoot(getDataFolder().toPath());
        Path worldRoot = resolveDefaultWorldRoot();
        PaintingPackBuilder.rebuildAll(picturesDirectory, packRoot,
                PaintingPackBuilder.defaultDataPackRoot(worldRoot));
        Path archive = PaintingResourcePackArchive.zip(packRoot);
        String sha1 = PaintingResourcePackArchive.sha1Hex(archive);
        resourcePackHash = java.util.HexFormat.of().parseHex(sha1);
        resourcePackUrl = getConfig().getString("resource-pack.url", "").trim();
        getLogger().info("Generated WallCanvas resource/data packs: " + archive + " (SHA-1 " + sha1 + ")");
        if (resourcePackUrl.isBlank()) {
            getLogger().warning("resource-pack.url is not configured; players will not receive the generated pack automatically.");
        }
    }

    private static String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    private Path resolveDefaultWorldRoot() throws IOException {
        Path container = getServer().getWorldContainer().toPath();
        Path propertiesFile = container.resolve("server.properties");
        String levelName = "world";
        if (Files.isRegularFile(propertiesFile)) {
            Properties properties = new Properties();
            try (var input = Files.newInputStream(propertiesFile)) {
                properties.load(input);
            }
            String configured = properties.getProperty("level-name", "world").trim();
            if (!configured.isBlank()) levelName = configured;
        }
        return container.resolve(Paths.get(levelName)).normalize();
    }
}
