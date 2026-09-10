package com.ultraop.wallcanvas.fabric.painting;

import com.ultraop.wallcanvas.core.painting.PaintingMetadata;
import com.ultraop.wallcanvas.core.painting.PaintingSpec;
import com.ultraop.wallcanvas.core.painting.PaintingVariantFactory;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;

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

    public ItemStack create(PaintingSpec spec, RegistryAccess registryAccess) {
        ItemStack item = create(spec);
        String variantId = PaintingVariantFactory.create(spec).variantId();
        ResourceLocation id = ResourceLocation.parse(variantId);
        ResourceKey<PaintingVariant> key = ResourceKey.create(Registries.PAINTING_VARIANT, id);
        Holder<PaintingVariant> variant = registryAccess.lookupOrThrow(Registries.PAINTING_VARIANT).getOrThrow(key);
        item.set(DataComponents.PAINTING_VARIANT, variant);
        return item;
    }
}
