package com.ultraop.wallcanvas.fabric.painting;

import com.ultraop.wallcanvas.core.DisplayStore;
import com.ultraop.wallcanvas.core.model.DisplayDefinition;
import com.ultraop.wallcanvas.core.painting.PaintingMetadata;
import com.ultraop.wallcanvas.core.painting.PaintingSize;
import com.ultraop.wallcanvas.core.painting.PaintingSpec;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Fabric adapter for WallCanvas Painting placement and player pickup parity. */
public final class FabricPaintingPlacement {
    private final DisplayStore displayStore;
    private final FabricPaintingItems paintingItems = new FabricPaintingItems();
    private final Map<UUID, PendingPlacement> pending = new HashMap<>();

    public FabricPaintingPlacement(DisplayStore displayStore) {
        this.displayStore = displayStore;
    }

    public void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
            ItemStack stack = serverPlayer.getItemInHand(hand);
            if (!isWallCanvasItem(stack)) return InteractionResult.PASS;
            if (!(world instanceof ServerLevel level)) return InteractionResult.PASS;

            Set<UUID> existing = new HashSet<>();
            for (Painting painting : level.getEntitiesOfClass(Painting.class,
                    player.getBoundingBox().inflate(4.0))) {
                existing.add(painting.getUUID());
            }

            var tag = stack.get(DataComponents.CUSTOM_DATA).copyTag();
            String asset = tag.getString(PaintingMetadata.ASSET).orElse("");
            String size = tag.getString(PaintingMetadata.SIZE).orElse("");
            if (asset.isBlank() || size.isBlank()) return InteractionResult.PASS;

            pending.put(serverPlayer.getUUID(), new PendingPlacement(
                    serverPlayer.getUUID(), level, hit.getBlockPos(), existing, asset, size));
            return InteractionResult.PASS;
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
            if (!(world instanceof ServerLevel level)) return InteractionResult.PASS;
            if (!(entity instanceof Painting painting)) return InteractionResult.PASS;

            var display = displayStore.find(painting.getUUID()).orElse(null);
            if (display == null || !display.isPainting()) return InteractionResult.PASS;

            if (!serverPlayer.isCreative()) {
                PaintingSize size = sizeFor(display);
                ItemStack item = paintingItems.create(
                        new PaintingSpec(display.assetId(), size), level.registryAccess());
                ItemEntity drop = new ItemEntity(level,
                        painting.getX(), painting.getY(), painting.getZ(), item);
                level.addFreshEntity(drop);
            }

            try {
                displayStore.remove(painting.getUUID());
            } catch (IOException exception) {
                return InteractionResult.FAIL;
            }

            painting.discard();
            return InteractionResult.SUCCESS;
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (pending.isEmpty()) return;
            for (PendingPlacement request : new ArrayList<>(pending.values())) {
                if (request.level().getServer().getPlayerList().getPlayer(request.playerId()) == null) {
                    pending.remove(request.playerId());
                    continue;
                }

                Painting found = null;
                for (Painting painting : request.level().getEntitiesOfClass(Painting.class,
                        new AABB(request.blockPos()).inflate(5.0))) {
                    if (!request.existing().contains(painting.getUUID())) {
                        found = painting;
                        break;
                    }
                }
                if (found == null) continue;

                try {
                    displayStore.add(DisplayDefinition.painting(
                            found.getUUID(), request.assetId(),
                            Math.max(1, found.getWidth()), Math.max(1, found.getHeight())));
                } catch (IOException exception) {
                    found.discard();
                }
                pending.remove(request.playerId());
            }
        });
    }

    private static PaintingSize sizeFor(DisplayDefinition display) {
        return display.width() >= PaintingSize.LARGE.widthBlocks()
                ? PaintingSize.LARGE
                : PaintingSize.DEFAULT;
    }

    private static boolean isWallCanvasItem(ItemStack stack) {
        if (stack.isEmpty() || !stack.has(DataComponents.CUSTOM_DATA)) return false;
        var tag = stack.get(DataComponents.CUSTOM_DATA).copyTag();
        return PaintingMetadata.TYPE_VALUE.equals(tag.getString(PaintingMetadata.TYPE).orElse(""))
                && !tag.getString(PaintingMetadata.ASSET).orElse("").isBlank()
                && !tag.getString(PaintingMetadata.SIZE).orElse("").isBlank();
    }

    private record PendingPlacement(UUID playerId, ServerLevel level, BlockPos blockPos,
                                    Set<UUID> existing, String assetId, String size) {
    }
}
