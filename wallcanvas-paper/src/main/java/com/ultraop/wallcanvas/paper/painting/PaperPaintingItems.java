package com.ultraop.wallcanvas.paper.painting;

import com.ultraop.wallcanvas.core.painting.PaintingMetadata;
import com.ultraop.wallcanvas.core.painting.PaintingSpec;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.NamespacedKey;

/** Paper adapter that turns the shared PaintingSpec into a native ItemStack. */
public final class PaperPaintingItems {
    private final NamespacedKey typeKey;
    private final NamespacedKey assetKey;
    private final NamespacedKey sizeKey;

    public PaperPaintingItems(JavaPlugin plugin) {
        typeKey = new NamespacedKey(plugin, PaintingMetadata.TYPE);
        assetKey = new NamespacedKey(plugin, PaintingMetadata.ASSET);
        sizeKey = new NamespacedKey(plugin, PaintingMetadata.SIZE);
    }

    public ItemStack create(PaintingSpec spec) {
        ItemStack item = new ItemStack(Material.PAINTING);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("WallCanvas: " + spec.assetId());
        meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, PaintingMetadata.TYPE_VALUE);
        meta.getPersistentDataContainer().set(assetKey, PersistentDataType.STRING, spec.assetId());
        meta.getPersistentDataContainer().set(sizeKey, PersistentDataType.STRING, spec.size().name());
        item.setItemMeta(meta);
        return item;
    }
}
