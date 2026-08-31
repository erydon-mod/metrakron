package com.oliver.metrakron.overlay;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DesktopOverlayClasspathTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void preservesEveryOrdinaryDevelopmentClasspathEntry() throws Exception {
        Path classes = temporaryDirectory.resolve("classes");
        Path resources = temporaryDirectory.resolve("resources");

        DesktopOverlayClasspath.Resolution resolution = DesktopOverlayClasspath.direct(
                List.of(classes, resources)
        );

        assertEquals(
                classes.toAbsolutePath() + File.pathSeparator + resources.toAbsolutePath(),
                resolution.classpath()
        );
        assertFalse(resolution.cached());
    }

    @Test
    void leavesAnOrdinaryStandaloneJarInPlace() throws Exception {
        Path standalone = temporaryDirectory.resolve("metrakron.jar");
        Files.write(standalone, new byte[]{1, 2, 3});

        DesktopOverlayClasspath.Resolution resolution = DesktopOverlayClasspath.direct(
                List.of(standalone)
        );

        assertEquals(standalone.toAbsolutePath().toString(), resolution.classpath());
        assertFalse(resolution.cached());
        assertFalse(Files.exists(temporaryDirectory.resolve("config/desktop-helper-cache")));
    }

    @Test
    void cachesANestedAdapterByVersionAndVerifiedContent() throws Exception {
        Path selectedAdapter = temporaryDirectory.resolve("selected-adapter.jar");
        byte[] contents = new byte[]{11, 22, 33, 44};
        Files.write(selectedAdapter, contents);
        Path config = temporaryDirectory.resolve("config");

        DesktopOverlayClasspath.Resolution first = DesktopOverlayClasspath.selectedCodeSource(
                selectedAdapter,
                config,
                "0.2.0+mc/1.20.1"
        );
        DesktopOverlayClasspath.Resolution second = DesktopOverlayClasspath.selectedCodeSource(
                selectedAdapter,
                config,
                "0.2.0+mc/1.20.1"
        );

        assertTrue(first.cached());
        assertEquals(first, second);
        assertTrue(first.cachedJar().startsWith(config.resolve("desktop-helper-cache")));
        assertTrue(first.cachedJar().getFileName().toString().startsWith(
                "metrakron-desktop-helper-0.2.0_mc_1.20.1-"
        ));
        assertArrayEquals(contents, Files.readAllBytes(first.cachedJar()));
        assertEquals(1L, cachedFiles(config));
        assertEquals(0L, temporaryFiles(config));
    }

    @Test
    void differentAdapterContentReceivesADifferentStablePath() throws Exception {
        Path selectedAdapter = temporaryDirectory.resolve("selected-adapter.jar");
        Path config = temporaryDirectory.resolve("config");
        Files.write(selectedAdapter, new byte[]{1, 2, 3});
        Path first = DesktopOverlayClasspath.selectedCodeSource(
                selectedAdapter,
                config,
                "0.2.0"
        ).cachedJar();

        Files.write(selectedAdapter, new byte[]{4, 5, 6});
        Path second = DesktopOverlayClasspath.selectedCodeSource(
                selectedAdapter,
                config,
                "0.2.0"
        ).cachedJar();

        assertNotEquals(first, second);
        assertEquals(2L, cachedFiles(config));
    }

    @Test
    void repairsACorruptedCachedCopyBeforeLaunchingIt() throws Exception {
        Path selectedAdapter = temporaryDirectory.resolve("selected-adapter.jar");
        byte[] contents = new byte[]{7, 8, 9};
        Files.write(selectedAdapter, contents);
        Path config = temporaryDirectory.resolve("config");
        Path cached = DesktopOverlayClasspath.selectedCodeSource(
                selectedAdapter,
                config,
                "0.2.0"
        ).cachedJar();
        Files.write(cached, new byte[]{0});

        DesktopOverlayClasspath.Resolution repaired = DesktopOverlayClasspath.selectedCodeSource(
                selectedAdapter,
                config,
                "0.2.0"
        );

        assertEquals(cached, repaired.cachedJar());
        assertArrayEquals(contents, Files.readAllBytes(cached));
        assertEquals(0L, temporaryFiles(config));
    }

    @Test
    void directlyUsesASelectedCodeSourceDirectory() throws Exception {
        Path selectedClasses = Files.createDirectories(temporaryDirectory.resolve("selected-classes"));
        Path config = temporaryDirectory.resolve("config");

        DesktopOverlayClasspath.Resolution resolution = DesktopOverlayClasspath.selectedCodeSource(
                selectedClasses,
                config,
                "development"
        );

        assertFalse(resolution.cached());
        assertEquals(selectedClasses.toAbsolutePath().toString(), resolution.classpath());
        assertFalse(Files.exists(config));
    }

    @Test
    void rejectsMissingSelectedAdapterFiles() {
        Path missing = temporaryDirectory.resolve("missing.jar");

        assertThrows(
                IOException.class,
                () -> DesktopOverlayClasspath.selectedCodeSource(
                        missing,
                        temporaryDirectory.resolve("config"),
                        "0.2.0"
                )
        );
    }

    @Test
    void rejectsAnEmptyDirectClasspath() {
        assertThrows(IOException.class, () -> DesktopOverlayClasspath.direct(List.of()));
    }

    private static long cachedFiles(Path config) throws IOException {
        try (var paths = Files.list(config.resolve("desktop-helper-cache"))) {
            return paths.filter(path -> path.getFileName().toString().endsWith(".jar")).count();
        }
    }

    private static long temporaryFiles(Path config) throws IOException {
        try (var paths = Files.list(config.resolve("desktop-helper-cache"))) {
            return paths.filter(path -> path.getFileName().toString().endsWith(".tmp")).count();
        }
    }
}
