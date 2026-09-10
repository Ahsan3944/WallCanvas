package com.ultraop.wallcanvas.fabric.painting;

import com.ultraop.wallcanvas.core.painting.PaintingMetadata;
import com.ultraop.wallcanvas.core.painting.PaintingSpec;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

/** Fabric adapter that turns the shared PaintingSpec into a native ItemStack. */
public final class FabricPaintingItems {
    public ItemStack create(PaintingSpec spec) {
        ItemStack item = new ItemStack(Items.PAINTING);
        CompoundTag tag = new CompoundTag();
        tag.putString(PaintingMetadata.TYPE, PaintingMetadata.TYPE_VALUE);
        tag.putString(PaintingMetadata.ASSET, spec.assetId());
        tag.putString(PaintingMetadata.SIZE, spec.size().name());
        item.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return item;
    }
}
