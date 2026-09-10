package com.ultraop.wallcanvas.paper.painting;

import com.ultraop.wallcanvas.core.DisplayStore;
import com.ultraop.wallcanvas.core.model.DisplayDefinition;
import com.ultraop.wallcanvas.core.painting.PaintingMetadata;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Painting;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

/** Binds WallCanvas metadata to normal Minecraft Painting entities. */
public final class PaperPaintingListener implements Listener {
    private final JavaPlugin plugin;
    private final DisplayStore displayStore;
    private final NamespacedKey typeKey;
    private final NamespacedKey assetKey;
    private final NamespacedKey sizeKey;

    public PaperPaintingListener(JavaPlugin plugin, DisplayStore displayStore) {
        this.plugin = plugin;
        this.displayStore = displayStore;
        this.typeKey = new NamespacedKey(plugin, PaintingMetadata.TYPE);
        this.assetKey = new NamespacedKey(plugin, PaintingMetadata.ASSET);
        this.sizeKey = new NamespacedKey(plugin, PaintingMetadata.SIZE);
    }

    @EventHandler
    public void onHangingPlace(HangingPlaceEvent event) {
        if (!(event.getEntity() instanceof Painting painting)) return;

        ItemStack source = event.getItemStack();
        if (!isWallCanvasItem(source)) return;

        var container = source.getItemMeta().getPersistentDataContainer();
        String assetId = container.get(assetKey, PersistentDataType.STRING);
        String size = container.get(sizeKey, PersistentDataType.STRING);
        if (assetId == null || assetId.isBlank() || size == null || size.isBlank()) return;

        copyMetadata(painting, assetId, size);
        try {
            displayStore.add(DisplayDefinition.painting(
                    painting.getUniqueId(), assetId,
                    Math.max(1, painting.getWidth()), Math.max(1, painting.getHeight())));
        } catch (IOException exception) {
            event.setCancelled(true);
            plugin.getLogger().warning("Unable to persist WallCanvas Painting "
                    + painting.getUniqueId() + ": " + exception.getMessage());
        }
    }

    @EventHandler
    public void onHangingBreak(HangingBreakEvent event) {
        if (!(event.getEntity() instanceof Painting painting) || !isWallCanvasPainting(painting)) return;
        try {
            displayStore.remove(painting.getUniqueId());
        } catch (IOException exception) {
            plugin.getLogger().warning("Unable to remove WallCanvas Painting "
                    + painting.getUniqueId() + " from storage: " + exception.getMessage());
        }
    }

    private boolean isWallCanvasItem(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;
        var container = item.getItemMeta().getPersistentDataContainer();
        return PaintingMetadata.TYPE_VALUE.equals(container.get(typeKey, PersistentDataType.STRING))
                && container.has(assetKey, PersistentDataType.STRING)
                && container.has(sizeKey, PersistentDataType.STRING);
    }

    private boolean isWallCanvasPainting(Painting painting) {
        var container = painting.getPersistentDataContainer();
        return PaintingMetadata.TYPE_VALUE.equals(container.get(typeKey, PersistentDataType.STRING))
                && container.has(assetKey, PersistentDataType.STRING);
    }

    private void copyMetadata(Painting painting, String assetId, String size) {
        var container = painting.getPersistentDataContainer();
        container.set(typeKey, PersistentDataType.STRING, PaintingMetadata.TYPE_VALUE);
        container.set(assetKey, PersistentDataType.STRING, assetId);
        container.set(sizeKey, PersistentDataType.STRING, size);
    }
}
