package com.ultraop.wallcanvas.fabric.map;

import com.ultraop.wallcanvas.core.DisplayStore;
import com.ultraop.wallcanvas.core.WallCanvasCore;
import com.ultraop.wallcanvas.core.library.ImageLibrary;
import com.ultraop.wallcanvas.core.map.MapImageRenderer;
import com.ultraop.wallcanvas.core.map.MapSpec;
import com.ultraop.wallcanvas.core.model.DisplayDefinition;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Fabric adapter for normal filled maps rendered through persistent ItemDisplay entities. */
public final class FabricMapDisplayManager {
    private static final String TAG_PREFIX = "wallcanvas_map|";
    private final Path picturesDirectory;
    private final DisplayStore displayStore;

    public FabricMapDisplayManager(Path picturesDirectory, DisplayStore displayStore) {
        this.picturesDirectory = picturesDirectory;
        this.displayStore = displayStore;
    }

    public UUID create(ServerLevel world, MapSpec spec, float yaw) throws IOException {
        Path picture = picturesDirectory.resolve(spec.assetId()).normalize();
        if (!isPictureFile(picture)) throw new IOException("Picture not found: " + spec.assetId());
        BufferedImage prepared = MapImageRenderer.prepare(picture, spec);
        UUID displayId = UUID.randomUUID();
        List<Display.ItemDisplay> spawned = new ArrayList<>();
        try {
            for (int tileY = 0; tileY < spec.tilesHigh(); tileY++) {
                for (int tileX = 0; tileX < spec.tilesWide(); tileX++) {
                    ItemStack map = MapItem.create(world, (int) Math.floor(spec.centerX()),
                            (int) Math.floor(spec.centerZ()), (byte) 0, false, false);
                    MapId mapId = map.get(DataComponents.MAP_ID);
                    if (mapId == null) throw new IOException("Minecraft did not assign a map id");
                    MapItemSavedData state = MapItem.getSavedData(mapId, world);
                    if (state == null) throw new IOException("Unable to access map state " + mapId.id());
                    paint(state, prepared, tileX * 128, tileY * 128);

                    double yawRad = Math.toRadians(yaw);
                    double rightX = Math.cos(yawRad);
                    double rightZ = -Math.sin(yawRad);
                    double xOffset = tileX - (spec.tilesWide() - 1) / 2.0;
                    double yOffset = (spec.tilesHigh() - 1) / 2.0 - tileY;
                    Display.ItemDisplay display = new Display.ItemDisplay(EntityType.ITEM_DISPLAY, world);
                    display.setPos(spec.centerX() + rightX * xOffset,
                            spec.centerY() + yOffset,
                            spec.centerZ() + rightZ * xOffset);
                    display.setYRot(yaw);
                    display.setXRot(0);
                    display.setItemStack(map);
                    display.setItemDisplayContext(ItemDisplayContext.FIXED);
                    display.setNoGravity(true);
                    display.addTag(tag(displayId, spec.assetId(), tileX, tileY, spec.tilesWide(), spec.tilesHigh()));
                    world.addFreshEntity(display);
                    spawned.add(display);
                }
            }
            displayStore.add(DisplayDefinition.map(displayId, spec.assetId(), spec.centerX(), spec.centerY(), spec.centerZ(), spec.tilesWide(), spec.tilesHigh()));
            return displayId;
        } catch (Exception exception) {
            for (Display.ItemDisplay display : spawned) display.discard();
            if (exception instanceof IOException io) throw io;
            throw new IOException("Unable to create map display", exception);
        }
    }

    public ItemStack createMapItem(ServerLevel world, String assetId) throws IOException {
        Path picture = picturesDirectory.resolve(assetId).normalize();
        if (!isPictureFile(picture)) throw new IOException("Picture not found: " + assetId);
        MapSpec spec = MapSpec.single(assetId, 0, 0, 0);
        BufferedImage prepared = MapImageRenderer.prepare(picture, spec);
        ItemStack map = MapItem.create(world, 0, 0, (byte) 0, false, false);
        MapId mapId = map.get(DataComponents.MAP_ID);
        if (mapId == null) throw new IOException("Minecraft did not assign a map id");
        MapItemSavedData state = MapItem.getSavedData(mapId, world);
        if (state == null) throw new IOException("Unable to access map state " + mapId.id());
        paint(state, prepared, 0, 0);
        return map;
    }

    public int remove(ServerLevel world, UUID displayId) throws IOException {
        int removed = 0;
        for (Entity entity : new ArrayList<>(world.getEntities().getAll())) {
            if (!(entity instanceof Display.ItemDisplay display)) continue;
            if (hasDisplayId(display, displayId)) {
                display.discard();
                removed++;
            }
        }
        displayStore.remove(displayId);
        return removed;
    }

    private static void paint(MapItemSavedData state, BufferedImage image, int offsetX, int offsetY) {
        for (int y = 0; y < 128; y++) {
            for (int x = 0; x < 128; x++) {
                int argb = image.getRGB(offsetX + x, offsetY + y);
                int alpha = (argb >>> 24) & 0xff;
                byte color = alpha < 8 ? 0 : FabricMapColorMatcher.match(argb);
                state.setColor(x, y, color);
            }
        }
        state.setDirty();
    }

    private static String tag(UUID id, String asset, int tileX, int tileY, int width, int height) {
        return TAG_PREFIX + id + "|" + asset + "|" + tileX + "|" + tileY + "|" + width + "|" + height;
    }

    private static boolean hasDisplayId(Display.ItemDisplay display, UUID id) {
        for (String tag : display.getTags()) {
            if (tag.startsWith(TAG_PREFIX + id + "|")) return true;
        }
        return false;
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

    private static final class FabricMapColorMatcher {
        private static final int[] COLORS = new int[248];
        private static final java.util.Map<Integer, Byte> CACHE = new java.util.HashMap<>();
        static {
            for (int i = 0; i < COLORS.length; i++) COLORS[i] = net.minecraft.world.level.material.MapColor.getRenderColor(i);
        }

        static synchronized byte match(int argb) {
            int r = (argb >>> 16) & 0xff;
            int g = (argb >>> 8) & 0xff;
            int b = argb & 0xff;
            int key = (r >> 3) << 10 | (g >> 3) << 5 | (b >> 3);
            Byte cached = CACHE.get(key);
            if (cached != null) return cached;
            int best = 0;
            long bestDistance = Long.MAX_VALUE;
            for (int i = 4; i < COLORS.length; i++) {
                int color = COLORS[i];
                int cr = (color >>> 16) & 0xff;
                int cg = (color >>> 8) & 0xff;
                int cb = color & 0xff;
                long dr = r - cr;
                long dg = g - cg;
                long db = b - cb;
                long distance = dr * dr + dg * dg + db * db;
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = i;
                }
            }
            byte result = (byte) best;
            CACHE.put(key, result);
            return result;
        }
    }
}
