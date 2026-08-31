package com.oliver.metrakron.overlay;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

final class DesktopOverlayClasspath {
    private static final String CACHE_DIRECTORY = "desktop-helper-cache";
    private static final int COPY_BUFFER_SIZE = 32 * 1024;

    private DesktopOverlayClasspath() {
    }

    static Resolution direct(List<Path> paths) throws IOException {
        Objects.requireNonNull(paths, "paths");
        List<Path> entries = paths.stream()
                .filter(Objects::nonNull)
                .map(Path::toAbsolutePath)
                .toList();
        if (entries.isEmpty()) {
            throw new IOException("Metrakron mod origin has no classpath entries");
        }
        return new Resolution(join(entries), null);
    }

    static Resolution selectedCodeSource(
            Path codeSource,
            Path configDirectory,
            String version
    ) throws IOException {
        Objects.requireNonNull(codeSource, "codeSource");
        Objects.requireNonNull(configDirectory, "configDirectory");

        Path source = codeSource.toAbsolutePath().normalize();
        if (Files.isDirectory(source)) {
            return direct(List.of(source));
        }
        if (!Files.isRegularFile(source)) {
            throw new IOException("Metrakron selected adapter is not a readable file: " + source);
        }

        Path cacheDirectory = configDirectory.resolve(CACHE_DIRECTORY);
        Files.createDirectories(cacheDirectory);

        String contentHash = sha256(source);
        String fileName = "metrakron-desktop-helper-"
                + safeVersion(version)
                + "-"
                + contentHash
                + ".jar";
        Path cachedJar = cacheDirectory.resolve(fileName);
        if (isExpectedContent(cachedJar, contentHash)) {
            return new Resolution(cachedJar.toString(), cachedJar);
        }

        Path temporary = Files.createTempFile(cacheDirectory, ".metrakron-helper-", ".tmp");
        try {
            Files.copy(source, temporary, StandardCopyOption.REPLACE_EXISTING);
            if (!contentHash.equals(sha256(temporary))) {
                throw new IOException("Metrakron desktop helper cache copy failed verification");
            }
            moveIntoPlace(temporary, cachedJar);
            if (!isExpectedContent(cachedJar, contentHash)) {
                throw new IOException("Metrakron desktop helper cache failed final verification");
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
        return new Resolution(cachedJar.toString(), cachedJar);
    }

    private static void moveIntoPlace(Path source, Path target) throws IOException {
        try {
            Files.move(
                    source,
                    target,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (AtomicMoveNotSupportedException ignored) {
            // The verified temporary file remains in the same directory, so replacement stays local.
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static boolean isExpectedContent(Path path, String expectedHash) throws IOException {
        return Files.isRegularFile(path) && expectedHash.equals(sha256(path));
    }

    private static String join(List<Path> entries) {
        return entries.stream()
                .map(Path::toString)
                .collect(Collectors.joining(File.pathSeparator));
    }

    private static String safeVersion(String version) {
        String candidate = version == null ? "unknown" : version.trim();
        if (candidate.isEmpty()) {
            candidate = "unknown";
        }
        candidate = candidate.replaceAll("[^A-Za-z0-9._-]", "_");
        return candidate.substring(0, Math.min(candidate.length(), 48));
    }

    private static String sha256(Path file) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
        try (InputStream input = Files.newInputStream(file)) {
            byte[] buffer = new byte[COPY_BUFFER_SIZE];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                if (read > 0) {
                    digest.update(buffer, 0, read);
                }
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    record Resolution(String classpath, Path cachedJar) {
        Resolution {
            Objects.requireNonNull(classpath, "classpath");
        }

        boolean cached() {
            return cachedJar != null;
        }
    }
}
