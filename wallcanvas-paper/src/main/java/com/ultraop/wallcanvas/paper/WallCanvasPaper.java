package com.ultraop.wallcanvas.paper;

import com.ultraop.wallcanvas.core.WallCanvasCore;
import com.ultraop.wallcanvas.core.library.ImageLibrary;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class WallCanvasPaper extends JavaPlugin implements CommandExecutor, TabCompleter {
    private Path picturesDirectory;

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

        var command = getCommand("wallcanvas");
        if (command == null) {
            getLogger().severe("wallcanvas command is missing from plugin.yml");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        command.setExecutor(this);
        command.setTabCompleter(this);

        getLogger().info(WallCanvasCore.NAME + " initialized. Picture library: " + picturesDirectory);
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

        sender.sendMessage("Usage: /wallcanvas list | /wallcanvas info <picture>");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return filter(List.of("list", "info"), args[0]);
        if (args.length == 2 && args[0].equalsIgnoreCase("info")) {
            try {
                List<String> names = ImageLibrary.scan(picturesDirectory).stream()
                        .map(path -> path.getFileName().toString()).toList();
                return filter(names, args[1]);
            } catch (IOException ignored) {
                return List.of();
            }
        }
        return List.of();
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
