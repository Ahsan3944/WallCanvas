package com.ultraop.wallcanvas.core.painting;

/** Creates canonical Painting variant definitions from shared Painting specs. */
public final class PaintingVariantFactory {
    private PaintingVariantFactory() {
    }

    public static PaintingVariantDefinition create(PaintingSpec spec) {
        String variantId = PaintingVariantIds.forAsset(spec.assetId(), spec.size());
        return new PaintingVariantDefinition(
                variantId,
                spec.size().widthBlocks(),
                spec.size().heightBlocks(),
                spec.assetId()
        );
    }
}
