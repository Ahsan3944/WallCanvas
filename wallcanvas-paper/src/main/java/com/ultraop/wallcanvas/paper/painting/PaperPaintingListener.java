package com.ultraop.wallcanvas.paper.painting;

import com.ultraop.wallcanvas.core.DisplayStore;
import com.ultraop.wallcanvas.core.model.DisplayDefinition;
import com.ultraop.wallcanvas.core.painting.PaintingMetadata;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Painting;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.UUID;

/** Binds a WallCanvas Painting item to the normal Minecraft Painting entity. */
public final class PaperPaintingListener implements Listener {
    private final DisplayStore displayStore;
    private final NamespacedKey typeKey;
    private final NamespacedKey assetKey;
    private final NamespacedKey sizeKey;

    public PaperPaintingListener(JavaPlugin plugin, DisplayStore displayStore) {
        this.displayStore = displayStore;
        this.typeKey = new NamespacedKey(plugin, PaintingMetadata.TYPE);
        this.assetKey = new NamespacedKey(plugin, PaintingMetadata.ASSET);
        this.sizeKey = new NamespacedKey(plugin, PaintingMetadata.SIZE);
    }

    @EventHandler
    public void onHangingPlace(HangingPlaceEvent event) {
        Hanging hanging = event.getEntity();
        if (!(hanging instanceof Painting painting)) return;

        ItemStack source = event.getItemStack();
        if (source == null || source.getType().isAir() || !source.hasItemMeta()) return;

        var container = source.getItemMeta().getPersistentDataContainer();
        String type = container.get(typeKey, PersistentDataType.STRING);
        String assetId = container.get(assetKey, PersistentDataType.STRING);
        String size = container.get(sizeKey, PersistentDataType.STRING);
        if (!PaintingMetadata.TYPE_VALUE.equals(type) || assetId == null || assetId.isBlank() || size == null) return;

        painting.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, PaintingMetadata.TYPE_VALUE);
        painting.getPersistentDataContainer().set(assetKey, PersistentDataType.STRING, assetId);
        painting.getPersistentDataContainer().set(sizeKey, PersistentDataType.STRING, size);

        UUID id = painting.getUniqueId();
        int width = Math.max(1, painting.getWidth());
        int height = Math.max(1, painting.getHeight());
        try {
            displayStore.add(DisplayDefinition.painting(id, assetId, width, height));
        } catch (IOException exception) {
            event.setCancelled(true);
            pluginLog(painting, exception);
        }
    }

    private static void pluginLog(Painting painting, IOException exception) {
        painting.getServer().getLogger().warning("Unable to persist WallCanvas Painting "
                + painting.getUniqueId() + ": " + exception.getMessage());
    }
}
