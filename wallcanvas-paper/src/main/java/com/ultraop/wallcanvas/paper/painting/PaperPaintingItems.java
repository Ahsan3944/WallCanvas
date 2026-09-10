package com.ultraop.wallcanvas.paper.painting;

import com.ultraop.wallcanvas.core.painting.PaintingMetadata;
import com.ultraop.wallcanvas.core.painting.PaintingSpec;
import com.ultraop.wallcanvas.core.painting.PaintingVariantFactory;
import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.Art;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

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

        String variantId = PaintingVariantFactory.create(spec).variantId();
        NamespacedKey key = NamespacedKey.fromString(variantId);
        if (key == null) {
            throw new IllegalArgumentException("Invalid WallCanvas Painting variant id: " + variantId);
        }
        Art art = Bukkit.getRegistry(Art.class).get(key);
        if (art == null) {
            throw new IllegalStateException("WallCanvas Painting variant is not registered: " + variantId
                    + ". Ensure the WallCanvas datapack is loaded before giving paintings.");
        }
        item.setData(DataComponentTypes.PAINTING_VARIANT, art);
        return item;
    }
}
