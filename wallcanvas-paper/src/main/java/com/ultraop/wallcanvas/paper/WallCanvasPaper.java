package com.ultraop.wallcanvas.paper;

import com.ultraop.wallcanvas.core.WallCanvasCore;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class WallCanvasPaper extends JavaPlugin {
    @Override
    public void onEnable() {
        Path imageDirectory = getDataFolder().toPath().resolve("images");
        try {
            Files.createDirectories(imageDirectory);
        } catch (IOException exception) {
            getLogger().severe("Unable to create WallCanvas image directory: " + exception.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getLogger().info(WallCanvasCore.NAME + " initialized. Image library: " + imageDirectory);
    }
}
