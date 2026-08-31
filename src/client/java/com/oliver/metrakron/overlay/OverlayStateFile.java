package com.oliver.metrakron.overlay;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

public final class OverlayStateFile {
    private OverlayStateFile() {
    }

    public static OverlayState read(Path file) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            properties.load(input);
        }

        int protocolVersion = parseInt(properties, "protocolVersion");
        if (protocolVersion != OverlayState.CURRENT_PROTOCOL) {
            throw new IOException("Unsupported overlay protocol " + protocolVersion);
        }

        long averageValue = parseLong(properties, "averageMillis");
        return new OverlayState(
                protocolVersion,
                required(properties, "sessionId"),
                parseLong(properties, "parentPid"),
                parseLong(properties, "revision"),
                Boolean.parseBoolean(required(properties, "active")),
                parseLong(properties, "sequence"),
                parseLong(properties, "elapsedMillis"),
                averageValue < 0L ? null : averageValue,
                properties.getProperty("heading", ""),
                properties.getProperty("relearningHeading", ""),
                properties.getProperty("detail", ""),
                parseInt(properties, "x"),
                parseInt(properties, "y"),
                parseInt(properties, "width"),
                parseInt(properties, "height")
        );
    }

    public static void write(Path file, OverlayState state) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("protocolVersion", Integer.toString(state.protocolVersion()));
        properties.setProperty("sessionId", state.sessionId());
        properties.setProperty("parentPid", Long.toString(state.parentPid()));
        properties.setProperty("revision", Long.toString(state.revision()));
        properties.setProperty("active", Boolean.toString(state.active()));
        properties.setProperty("sequence", Long.toString(state.sequence()));
        properties.setProperty("elapsedMillis", Long.toString(state.elapsedMillis()));
        properties.setProperty(
                "averageMillis",
                state.averageMillis() == null ? "-1" : Long.toString(state.averageMillis())
        );
        properties.setProperty("heading", state.heading());
        properties.setProperty("relearningHeading", state.relearningHeading());
        properties.setProperty("detail", state.detail());
        properties.setProperty("x", Integer.toString(state.x()));
        properties.setProperty("y", Integer.toString(state.y()));
        properties.setProperty("width", Integer.toString(state.width()));
        properties.setProperty("height", Integer.toString(state.height()));
        writeProperties(file, properties, "ERYDON Metrakron window overlay state");
    }

    static void writeProperties(Path file, Properties properties, String comment) throws IOException {
        Path absoluteFile = file.toAbsolutePath();
        Path parent = absoluteFile.getParent();
        if (parent == null) {
            throw new IOException("Overlay state file has no parent directory: " + file);
        }
        Files.createDirectories(parent);

        Path temporaryFile = Files.createTempFile(
                parent,
                "." + absoluteFile.getFileName() + '-',
                ".tmp"
        );
        try {
            try (OutputStream output = Files.newOutputStream(temporaryFile)) {
                properties.store(output, comment);
            }
            try {
                Files.move(
                        temporaryFile,
                        absoluteFile,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING
                );
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(
                        temporaryFile,
                        absoluteFile,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    private static String required(Properties properties, String key) throws IOException {
        String value = properties.getProperty(key);
        if (value == null) {
            throw new IOException("Missing overlay state field " + key);
        }
        return value;
    }

    private static int parseInt(Properties properties, String key) throws IOException {
        try {
            return Integer.parseInt(required(properties, key));
        } catch (NumberFormatException exception) {
            throw new IOException("Invalid integer overlay state field " + key, exception);
        }
    }

    private static long parseLong(Properties properties, String key) throws IOException {
        try {
            return Long.parseLong(required(properties, key));
        } catch (NumberFormatException exception) {
            throw new IOException("Invalid long overlay state field " + key, exception);
        }
    }
}
