package com.ultraop.wallcanvas.paper.map;

import com.ultraop.wallcanvas.core.DisplayStore;
import com.ultraop.wallcanvas.core.library.ImageLibrary;
import com.ultraop.wallcanvas.core.map.MapImageRenderer;
import com.ultraop.wallcanvas.core.map.MapSpec;
import com.ultraop.wallcanvas.core.model.DisplayDefinition;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.MapId;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Paper adapter for coordinate-based WallCanvas map displays using normal filled-map state. */
public final class PaperMapDisplayManager {
    private final JavaPlugin plugin;
    private final Path picturesDirectory;
    private final DisplayStore displayStore;
    private final NamespacedKey displayKey;
    private final NamespacedKey assetKey;
    private final NamespacedKey tileXKey;
    private final NamespacedKey tileYKey;
    private final NamespacedKey widthKey;
    private final NamespacedKey heightKey;
    private final NamespacedKey mapIdKey;

    public PaperMapDisplayManager(JavaPlugin plugin, Path picturesDirectory, DisplayStore displayStore) {
        this.plugin = plugin;
        this.picturesDirectory = picturesDirectory;
        this.displayStore = displayStore;
        this.displayKey = new NamespacedKey(plugin, "map_display");
        this.assetKey = new NamespacedKey(plugin, "map_asset");
        this.tileXKey = new NamespacedKey(plugin, "map_tile_x");
        this.tileYKey = new NamespacedKey(plugin, "map_tile_y");
        this.widthKey = new NamespacedKey(plugin, "map_width");
        this.heightKey = new NamespacedKey(plugin, "map_height");
        this.mapIdKey = new NamespacedKey(plugin, "map_id");
    }

    public UUID create(PlayerLike player, MapSpec spec, float yaw) throws IOException {
        Path picture = picturesDirectory.resolve(spec.assetId()).normalize();
        if (!isPictureFile(picture)) throw new IOException("Picture not found: " + spec.assetId());

        BufferedImage prepared = MapImageRenderer.prepare(picture, spec);
        UUID displayId = UUID.randomUUID();
        DisplayDefinition definition = DisplayDefinition.map(displayId, spec.assetId(),
                spec.centerX(), spec.centerY(), spec.centerZ(), spec.tilesWide(), spec.tilesHigh());

        List<ItemDisplay> spawned = new ArrayList<>();
        try {
            for (int tileY = 0; tileY < spec.tilesHigh(); tileY++) {
                for (int tileX = 0; tileX < spec.tilesWide(); tileX++) {
                    MapView map = Bukkit.createMap(player.world());
                    prepareMap(map, prepared, tileX, tileY);
                    ItemStack item = new ItemStack(Material.FILLED_MAP);
                    item.setData(DataComponentTypes.MAP_ID, MapId.mapId(map.getId()));

                    double yawRad = Math.toRadians(yaw);
                    double rightX = Math.cos(yawRad);
                    double rightZ = -Math.sin(yawRad);
                    double xOffset = (tileX - (spec.tilesWide() - 1) / 2.0);
                    double yOffset = ((spec.tilesHigh() - 1) / 2.0 - tileY);
                    Location location = new Location(player.world(),
                            spec.centerX() + rightX * xOffset,
                            spec.centerY() + yOffset,
                            spec.centerZ() + rightZ * xOffset,
                            yaw, 0);

                    ItemDisplay display = (ItemDisplay) player.world().spawnEntity(location, EntityType.ITEM_DISPLAY);
                    display.setItemStack(item);
                    display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
                    display.setBillboard(org.bukkit.entity.Display.Billboard.FIXED);
                    display.setGravity(false);
                    display.setPersistent(true);
                    display.setInvulnerable(true);
                    display.setViewRange(64.0f);
                    writeMetadata(display, displayId, spec.assetId(), tileX, tileY,
                            spec.tilesWide(), spec.tilesHigh(), map.getId());
                    spawned.add(display);
                }
            }
            displayStore.add(definition);
            return displayId;
        } catch (Exception exception) {
            for (ItemDisplay display : spawned) display.remove();
            if (exception instanceof IOException io) throw io;
            throw new IOException("Unable to create map display", exception);
        }
    }

    public ItemStack createMapItem(World world, String assetId) throws IOException {
        Path picture = picturesDirectory.resolve(assetId).normalize();
        if (!isPictureFile(picture)) throw new IOException("Picture not found: " + assetId);
        MapSpec spec = MapSpec.single(assetId, 0, 0, 0);
        BufferedImage prepared = MapImageRenderer.prepare(picture, spec);
        MapView map = Bukkit.createMap(world);
        prepareMap(map, prepared, 0, 0);
        ItemStack item = new ItemStack(Material.FILLED_MAP);
        item.setData(DataComponentTypes.MAP_ID, MapId.mapId(map.getId()));
        return item;
    }

    public int remove(UUID displayId) throws IOException {
        int removed = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!(entity instanceof ItemDisplay display)) continue;
                String value = display.getPersistentDataContainer().get(displayKey, PersistentDataType.STRING);
                if (displayId.toString().equals(value)) {
                    display.remove();
                    removed++;
                }
            }
        }
        displayStore.remove(displayId);
        return removed;
    }

    public void restoreRenderers() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!(entity instanceof ItemDisplay display)) continue;
                PersistentDataContainer pdc = display.getPersistentDataContainer();
                String asset = pdc.get(assetKey, PersistentDataType.STRING);
                Integer tileX = pdc.get(tileXKey, PersistentDataType.INTEGER);
                Integer tileY = pdc.get(tileYKey, PersistentDataType.INTEGER);
                Integer width = pdc.get(widthKey, PersistentDataType.INTEGER);
                Integer height = pdc.get(heightKey, PersistentDataType.INTEGER);
                Integer mapId = pdc.get(mapIdKey, PersistentDataType.INTEGER);
                if (asset == null || tileX == null || tileY == null || width == null || height == null || mapId == null) continue;
                try {
                    Path picture = picturesDirectory.resolve(asset).normalize();
                    if (!isPictureFile(picture)) continue;
                    MapView map = Bukkit.getMap(mapId);
                    if (map == null) continue;
                    BufferedImage prepared = MapImageRenderer.prepare(picture,
                            new MapSpec(asset, width, height, display.getX(), display.getY(), display.getZ()));
                    prepareMap(map, prepared, tileX, tileY);
                } catch (Exception exception) {
                    plugin.getLogger().warning("Unable to restore WallCanvas map renderer: " + exception.getMessage());
                }
            }
        }
    }

    private void prepareMap(MapView map, BufferedImage image, int tileX, int tileY) {
        for (MapRenderer renderer : new ArrayList<>(map.getRenderers())) map.removeRenderer(renderer);
        map.setTrackingPosition(false);
        map.setUnlimitedTracking(false);
        map.addRenderer(new PaperMapRenderer(image, tileX * 128, tileY * 128));
    }

    private void writeMetadata(ItemDisplay display, UUID id, String asset, int tileX, int tileY,
                               int width, int height, int mapId) {
        PersistentDataContainer pdc = display.getPersistentDataContainer();
        pdc.set(displayKey, PersistentDataType.STRING, id.toString());
        pdc.set(assetKey, PersistentDataType.STRING, asset);
        pdc.set(tileXKey, PersistentDataType.INTEGER, tileX);
        pdc.set(tileYKey, PersistentDataType.INTEGER, tileY);
        pdc.set(widthKey, PersistentDataType.INTEGER, width);
        pdc.set(heightKey, PersistentDataType.INTEGER, height);
        pdc.set(mapIdKey, PersistentDataType.INTEGER, mapId);
    }

    private boolean isPictureFile(Path path) {
        try {
            return path.getParent().equals(picturesDirectory.toAbsolutePath().normalize())
                    && Files.isRegularFile(path)
                    && com.ultraop.wallcanvas.core.WallCanvasCore.isSupportedImageExtension(ImageLibrary.extension(path));
        } catch (Exception ignored) {
            return false;
        }
    }

    public interface PlayerLike {
        World world();
    }
}
