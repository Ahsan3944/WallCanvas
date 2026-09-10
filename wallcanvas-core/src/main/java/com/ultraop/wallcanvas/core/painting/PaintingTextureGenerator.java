package com.ultraop.wallcanvas.core.painting;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Converts a WallCanvas picture into the exact pixel dimensions required by a Painting variant. */
public final class PaintingTextureGenerator {
    private static final int PIXELS_PER_BLOCK = 16;

    private PaintingTextureGenerator() {
    }

    public static Path generate(Path source, Path packRoot, PaintingVariantDefinition definition) throws IOException {
        if (!Files.isRegularFile(source)) {
            throw new IOException("Picture does not exist: " + source);
        }

        BufferedImage input = ImageIO.read(source.toFile());
        if (input == null) {
            throw new IOException("Unsupported or unreadable image format: " + source.getFileName());
        }

        int targetWidth = definition.width() * PIXELS_PER_BLOCK;
        int targetHeight = definition.height() * PIXELS_PER_BLOCK;
        BufferedImage output = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);

        double sourceAspect = (double) input.getWidth() / input.getHeight();
        double targetAspect = (double) targetWidth / targetHeight;

        int cropWidth = input.getWidth();
        int cropHeight = input.getHeight();
        int cropX = 0;
        int cropY = 0;

        if (sourceAspect > targetAspect) {
            cropWidth = Math.max(1, (int) Math.round(input.getHeight() * targetAspect));
            cropX = (input.getWidth() - cropWidth) / 2;
        } else if (sourceAspect < targetAspect) {
            cropHeight = Math.max(1, (int) Math.round(input.getWidth() / targetAspect));
            cropY = (input.getHeight() - cropHeight) / 2;
        }

        Graphics2D graphics = output.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(input,
                    0, 0, targetWidth, targetHeight,
                    cropX, cropY, cropX + cropWidth, cropY + cropHeight,
                    null);
        } finally {
            graphics.dispose();
        }

        Path texture = PaintingResourcePackLayout.texture(packRoot, definition.variantId())
                .resolveSibling(PaintingResourcePackLayout.texture(packRoot, definition.variantId()).getFileName().toString().replace(".json", ".png"));
        Files.createDirectories(texture.getParent());
        if (!ImageIO.write(output, "PNG", texture.toFile())) {
            throw new IOException("PNG encoder is unavailable");
        }
        return texture;
    }
}
