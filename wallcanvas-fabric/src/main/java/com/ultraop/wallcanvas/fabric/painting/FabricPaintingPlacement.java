package com.ultraop.wallcanvas.fabric.painting;

import com.ultraop.wallcanvas.core.DisplayStore;
import com.ultraop.wallcanvas.core.model.DisplayDefinition;
import com.ultraop.wallcanvas.core.painting.PaintingMetadata;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Fabric adapter for binding WallCanvas metadata to vanilla Painting placement.
 * Vanilla Painting placement remains responsible for creating the entity.
 */
public final class FabricPaintingPlacement {
    private final DisplayStore displayStore;
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
