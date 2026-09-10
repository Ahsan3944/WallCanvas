package com.ultraop.wallcanvas.core.library;

import com.ultraop.wallcanvas.core.WallCanvasCore;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class ImageImporter {
    private ImageImporter() {}

    public static Path importUrl(String url, Path imageDirectory, String fileName) throws IOException, InterruptedException {
        URI uri = URI.create(url);
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IOException("Only HTTPS image URLs are allowed");
        }
        String safeName = sanitize(fileName);
        String extension = ImageLibrary.extension(Path.of(uri.getPath()));
        if (!WallCanvasCore.isSupportedImageExtension(extension)) {
            throw new IOException("Unsupported image extension. Use PNG, JPG, JPEG or WebP.");
        }
        if (!safeName.toLowerCase(Locale.ROOT).endsWith("." + extension.toLowerCase(Locale.ROOT))) {
            safeName += "." + extension;
        }
        Files.createDirectories(imageDirectory);
        Path destination = imageDirectory.resolve(safeName).normalize();
        if (!destination.getParent().equals(imageDirectory.toAbsolutePath().normalize())) {
            throw new IOException("Invalid file name");
        }

        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("User-Agent", "WallCanvas/0.1")
                .GET().build();
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() / 100 != 2) throw new IOException("Image download failed: HTTP " + response.statusCode());
        if (response.body().length > 32 * 1024 * 1024) throw new IOException("Image is larger than 32 MiB");
        Files.write(destination, response.body());
        return destination;
    }

    private static String sanitize(String value) {
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
