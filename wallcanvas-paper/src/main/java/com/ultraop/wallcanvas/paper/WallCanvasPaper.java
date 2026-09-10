package com.ultraop.wallcanvas.paper;

import com.ultraop.wallcanvas.core.DisplayStore;
import com.ultraop.wallcanvas.core.WallCanvasCore;
import com.ultraop.wallcanvas.core.library.ImageLibrary;
import com.ultraop.wallcanvas.core.painting.PaintingPackBuilder;
import com.ultraop.wallcanvas.core.painting.PaintingSize;
import com.ultraop.wallcanvas.core.painting.PaintingSpec;
import com.ultraop.wallcanvas.paper.painting.PaperPaintingItems;
import com.ultraop.wallcanvas.paper.painting.PaperPaintingListener;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class WallCanvasPaper extends JavaPlugin implements CommandExecutor, TabCompleter {
    private Path picturesDirectory;
    private PaperPaintingItems paintingItems;
    private DisplayStore displayStore;

    @Override
    public void onEnable() {
        picturesDirectory = getDataFolder().toPath().resolve("pictures");
        try {
            Files.createDirectories(picturesDirectory);
        } catch (IOException exception) {
            getLogger().severe("Unable to create WallCanvas pictures directory: " + exception.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        displayStore = new DisplayStore(getDataFolder().toPath().resolve("displays.json"));
        try {
            displayStore.load();
            Path packRoot = PaintingPackBuilder.defaultPackRoot(getDataFolder().toPath());
            var generated = PaintingPackBuilder.rebuild(picturesDirectory, packRoot, PaintingSize.DEFAULT);
            PaintingPackBuilder.rebuild(picturesDirectory, packRoot, PaintingSize.LARGE);
            getLogger().info("Generated " + generated.size() + " WallCanvas Painting variant(s) at " + packRoot);
        } catch (IOException exception) {
            getLogger().severe("Unable to initialize WallCanvas Painting resources: " + exception.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        paintingItems = new PaperPaintingItems(this);
        getServer().getPluginManager().registerEvents(new PaperPaintingListener(this, displayStore), this);

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
            if (!isPictureFile(picture)) {
                sender.sendMessage("Picture not found.");
                return true;
            }
            sender.sendMessage("Picture: " + picture.getFileName());
            return true;
        }

        if (args[0].equalsIgnoreCase("give") && args.length >= 3) {
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage("Player not found or offline.");
                return true;
            }
            String pictureName = args[2];
            if (!isPictureFile(picturesDirectory.resolve(pictureName).normalize())) {
                sender.sendMessage("Picture not found.");
                return true;
            }
            PaintingSpec spec = new PaintingSpec(pictureName, PaintingSize.DEFAULT);
            target.getInventory().addItem(paintingItems.create(spec));
            sender.sendMessage("Gave WallCanvas Painting '" + pictureName + "' to " + target.getName() + ".");
            return true;
        }

        sender.sendMessage("Usage: /wallcanvas list | /wallcanvas info <picture> | /wallcanvas give <player> <picture>");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return filter(List.of("list", "info", "give"), args[0]);
        if (args.length == 2 && args[0].equalsIgnoreCase("info")) return pictureNames(args[1]);
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) return pictureNames(args[2]);
        return List.of();
    }

    private List<String> pictureNames(String prefix) {
        try {
            return filter(ImageLibrary.scan(picturesDirectory).stream()
                    .map(path -> path.getFileName().toString()).toList(), prefix);
        } catch (IOException ignored) {
            return List.of();
        }
    }

    private boolean isPictureFile(Path path) {
        try {
            return path.getParent().equals(picturesDirectory.toAbsolutePath().normalize())
                    && Files.isRegularFile(path)
                    && WallCanvasCore.isSupportedImageExtension(ImageLibrary.extension(path));
        } catch (Exception ignored) {
            return false;
        }
    }

    private static List<String> filter(List<String> values, String prefix) {
        String lower = prefix.toLowerCase(java.util.Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(java.util.Locale.ROOT).startsWith(lower)) result.add(value);
        }
        return result;
    }
}
