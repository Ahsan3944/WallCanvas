package com.ultraop.wallcanvas.core.library;

import com.ultraop.wallcanvas.core.WallCanvasCore;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;

public final class ImageImporter {
    private static final int MAX_BYTES = 32 * 1024 * 1024;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);

    private ImageImporter() {
    }

    public static Path importUrl(String url, Path pictureDirectory, String fileName)
            throws IOException, InterruptedException {
        URI uri = parseHttpsUri(url);
        rejectPrivateHost(uri.getHost());

        String safeName = sanitize(fileName);
        if (safeName.isBlank()) {
            throw new IOException("Picture name cannot be blank");
        }
        String extension = extensionFromPath(uri.getPath());
        if (!WallCanvasCore.isSupportedImageExtension(extension)) {
            extension = "png";
        }
        if (!safeName.toLowerCase(Locale.ROOT).endsWith("." + extension.toLowerCase(Locale.ROOT))) {
            safeName += "." + extension;
        }

        Files.createDirectories(pictureDirectory);
        Path destination = pictureDirectory.resolve(safeName).normalize();
        if (!destination.getParent().equals(pictureDirectory.toAbsolutePath().normalize())) {
            throw new IOException("Invalid picture name");
        }

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .header("User-Agent", "WallCanvas/0.1")
                .header("Accept", "image/png,image/jpeg,image/webp;q=0.9,*/*;q=0.1")
                .GET()
                .build();

        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("Image download failed: HTTP " + response.statusCode());
        }
        byte[] body = response.body();
        if (body.length == 0) throw new IOException("Downloaded file is empty");
        if (body.length > MAX_BYTES) throw new IOException("Image is larger than 32 MiB");

        BufferedImage image;
        try (ByteArrayInputStream input = new ByteArrayInputStream(body)) {
            image = ImageIO.read(input);
        }
        if (image == null) {
            throw new IOException("Downloaded content is not a supported PNG, JPEG or WebP image");
        }
        if (image.getWidth() < 1 || image.getHeight() < 1) {
            throw new IOException("Image has invalid dimensions");
        }

        String detectedExtension = detectExtension(body);
        if (detectedExtension == null) {
            throw new IOException("Downloaded content is not a recognized image format");
        }
        if (!safeName.toLowerCase(Locale.ROOT).endsWith("." + detectedExtension)) {
            safeName = replaceExtension(safeName, detectedExtension);
            destination = pictureDirectory.resolve(safeName).normalize();
        }
        Files.write(destination, body);
        return destination;
    }

    private static URI parseHttpsUri(String value) throws IOException {
        final URI uri;
        try {
            uri = URI.create(value.trim());
        } catch (IllegalArgumentException exception) {
            throw new IOException("Invalid image URL", exception);
        }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
            throw new IOException("Only HTTPS image URLs are allowed");
        }
        return uri;
    }

    private static void rejectPrivateHost(String host) throws IOException {
        try {
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (address.isAnyLocalAddress() || address.isLoopbackAddress()
                        || address.isLinkLocalAddress() || address.isSiteLocalAddress()
                        || address.isMulticastAddress()) {
                    throw new IOException("Private or local image hosts are not allowed");
                }
            }
        } catch (UnknownHostException exception) {
            throw new IOException("Unable to resolve image host", exception);
        }
    }

    private static String extensionFromPath(String path) {
        if (path == null) return "";
        int dot = path.lastIndexOf('.');
        return dot < 0 ? "" : path.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String detectExtension(byte[] body) {
        if (body.length >= 8 && (body[0] & 0xff) == 0x89 && body[1] == 0x50
                && body[2] == 0x4e && body[3] == 0x47) return "png";
        if (body.length >= 3 && (body[0] & 0xff) == 0xff && (body[1] & 0xff) == 0xd8
                && (body[2] & 0xff) == 0xff) return "jpg";
        if (body.length >= 12 && body[0] == 'R' && body[1] == 'I' && body[2] == 'F' && body[3] == 'F'
                && body[8] == 'W' && body[9] == 'E' && body[10] == 'B' && body[11] == 'P') return "webp";
        return null;
    }

    private static String replaceExtension(String name, String extension) {
        int dot = name.lastIndexOf('.');
        return (dot < 0 ? name : name.substring(0, dot)) + "." + extension;
    }

    private static String sanitize(String value) {
        return value == null ? "" : value.trim().replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
