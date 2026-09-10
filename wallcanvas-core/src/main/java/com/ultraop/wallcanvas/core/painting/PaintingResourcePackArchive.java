package com.ultraop.wallcanvas.core.painting;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Creates the distributable ZIP and SHA-1 fingerprint for a generated Painting pack. */
public final class PaintingResourcePackArchive {
    private PaintingResourcePackArchive() {
    }

    public static Path zip(Path packRoot) throws IOException {
        Path archive = packRoot.resolveSibling("resourcepack.zip");
        Files.deleteIfExists(archive);
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(archive))) {
            try (var files = Files.walk(packRoot)) {
                files.filter(Files::isRegularFile).sorted().forEach(path -> {
                    try {
                        output.putNextEntry(new ZipEntry(packRoot.relativize(path).toString().replace('\\', '/')));
                        try (InputStream input = Files.newInputStream(path)) {
                            input.transferTo(output);
                        }
                        output.closeEntry();
                    } catch (IOException exception) {
                        throw new PackArchiveException(exception);
                    }
                });
            }
        } catch (PackArchiveException exception) {
            throw exception.cause;
        }
        return archive;
    }

    public static String sha1Hex(Path archive) throws IOException {
        try (InputStream input = Files.newInputStream(archive)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) digest.update(buffer, 0, read);
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-1 is unavailable", exception);
        }
    }

    private static final class PackArchiveException extends RuntimeException {
        private final IOException cause;
        private PackArchiveException(IOException cause) { this.cause = cause; }
    }
}
