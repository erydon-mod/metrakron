package com.oliver.metrakron.timing;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class BaselineStoreConcurrencyTest {
    private static final ProfileFingerprint PROFILE = new ProfileFingerprint("shared-profile", "History process test");

    @TempDir Path directory;

    @Test
    void staleStoresMergeStartupWorldAndQuickPlayRunsAndKeepOnlyTheLatestThree() {
        Path file = directory.resolve("baselines.json");
        BaselineStore first = new BaselineStore(file);
        BaselineStore second = new BaselineStore(file);
        first.recordStartup(PROFILE, 10_000L);
        second.recordStartup(PROFILE, 20_000L);
        assertEquals(15_000L, first.startupAverage(PROFILE).orElseThrow(), "Existing stores must refresh too");
        first.recordStartup(PROFILE, 30_000L);
        second.recordStartup(PROFILE, 40_000L);
        assertEquals(30_000L, first.startupAverage(PROFILE).orElseThrow());

        String key = PROFILE.id() + ":same-save";
        first.recordWorld(PROFILE, key, "Same save", 10_000L);
        second.recordWorld(PROFILE, key, "Same save", 20_000L);
        first.recordWorld(PROFILE, key, "Same save", 30_000L);
        second.recordWorld(PROFILE, key, "Same save", 40_000L);
        first.recordQuickPlay(PROFILE, key, "Same save", 50_000L);
        second.recordQuickPlay(PROFILE, key, "Same save", 60_000L);
        first.recordQuickPlay(PROFILE, key, "Same save", 70_000L);
        second.recordQuickPlay(PROFILE, key, "Same save", 80_000L);
        BaselineStore reloaded = new BaselineStore(file);
        assertEquals(30_000L, reloaded.worldAverage(PROFILE, key).orElseThrow());
        assertEquals(70_000L, reloaded.quickPlayAverage(PROFILE, key).orElseThrow());
        assertEquals(30_000L, reloaded.startupAverage(PROFILE).orElseThrow(), "Other routes must survive world writes");
    }

    @Test
    void staleStoresPreserveIndependentSavesAndRoutes() {
        Path file = directory.resolve("baselines.json");
        BaselineStore first = new BaselineStore(file);
        BaselineStore second = new BaselineStore(file);
        String alpha = PROFILE.id() + ":alpha", beta = PROFILE.id() + ":beta";
        first.recordWorld(PROFILE, alpha, "Alpha", 11_000L);
        second.recordWorld(PROFILE, beta, "Beta", 22_000L);
        first.recordQuickPlay(PROFILE, alpha, "Alpha", 33_000L);
        second.recordQuickPlay(PROFILE, beta, "Beta", 44_000L);
        assertEquals(11_000L, second.worldAverage(PROFILE, alpha).orElseThrow());
        assertEquals(22_000L, first.worldAverage(PROFILE, beta).orElseThrow());
        assertEquals(33_000L, second.quickPlayAverage(PROFILE, alpha).orElseThrow());
        assertEquals(44_000L, first.quickPlayAverage(PROFILE, beta).orElseThrow());
    }

    @Test
    void overlappingStoresInOneJvmWaitInsteadOfOverlappingFileLocks() throws Exception {
        Path file = directory.resolve("baselines.json");
        BaselineStore first = new BaselineStore(file);
        // Equivalent paths must share the same JVM lock as well as the same OS lock.
        BaselineStore second = new BaselineStore(directory.resolve(".").resolve("baselines.json"));
        var barrier = new CyclicBarrier(2);
        var workers = Executors.newFixedThreadPool(2);
        try {
            var left = workers.submit(() -> { barrier.await(10, TimeUnit.SECONDS); recordBatch(first, "a", 10_000L); return null; });
            var right = workers.submit(() -> { barrier.await(10, TimeUnit.SECONDS); recordBatch(second, "b", 20_000L); return null; });
            left.get(20, TimeUnit.SECONDS);
            right.get(20, TimeUnit.SECONDS);
        } finally {
            workers.shutdownNow();
        }
        verifyBothBatches(file);
    }

    @Test
    void twoRealProcessesWaitOnTheSidecarAndPreserveEachOthersWrites() throws Exception {
        Path file = directory.resolve("baselines.json");
        Path lock = directory.resolve("baselines.json.lock");
        Process first = null, second = null;
        try {
            // Hold the actual companion lock while both children begin their write.
            // This exercises process exclusion, rather than relying on a scheduling race.
            try (FileChannel channel = FileChannel.open(lock, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                 FileLock ignored = channel.lock()) {
                first = child(file, "a", 10_000L);
                second = child(file, "b", 20_000L);
                awaitMarkers("a.ready", "b.ready");
                Files.writeString(directory.resolve("go"), "go");
                awaitMarkers("a.attempting", "b.attempting");
                assertFalse(first.waitFor(150, TimeUnit.MILLISECONDS), "Worker A must wait for the held sidecar lock");
                assertFalse(second.waitFor(150, TimeUnit.MILLISECONDS), "Worker B must wait for the held sidecar lock");
                assertFalse(Files.exists(directory.resolve("a.done")));
                assertFalse(Files.exists(directory.resolve("b.done")));
            }
            for (Process process : List.of(first, second)) {
                assertTrue(process.waitFor(30, TimeUnit.SECONDS), "History worker timed out");
                assertEquals(0, process.exitValue(), () -> readWorkerLogs());
            }
            verifyBothBatches(file);
            assertTrue(Files.isRegularFile(lock), "Keep the lock identity stable between transactions");
            try (var files = Files.list(directory)) {
                assertFalse(files.anyMatch(path -> path.getFileName().toString().endsWith(".tmp")), "Atomic-write temps must be cleaned");
            }
        } finally {
            if (first != null && first.isAlive()) first.destroyForcibly();
            if (second != null && second.isAlive()) second.destroyForcibly();
        }
    }

    @Test
    void failedPersistenceRetainsTheOriginalRunForOneRetryAlongsideNewDiskHistory() throws Exception {
        Path file = directory.resolve("baselines.json");
        Path lock = directory.resolve("baselines.json.lock");
        Files.createDirectory(lock); // Deterministic IO failure on Windows, macOS and Linux.
        BaselineStore waiting = new BaselineStore(file);
        waiting.recordStartup(PROFILE, 10_000L);
        assertEquals(10_000L, waiting.startupAverage(PROFILE).orElseThrow());
        Files.delete(lock);
        BaselineStore other = new BaselineStore(file);
        other.recordStartup(PROFILE, 20_000L);
        assertEquals(15_000L, waiting.startupAverage(PROFILE).orElseThrow());
        assertEquals(15_000L, waiting.startupAverage(PROFILE).orElseThrow(), "Retry must not duplicate the 10s run");
        assertEquals(15_000L, new BaselineStore(file).startupAverage(PROFILE).orElseThrow());
    }

    private static void recordBatch(BaselineStore store, String worker, long duration) {
        store.recordStartup(PROFILE, duration);
        for (int index = 0; index < 4; index++) {
            String key = PROFILE.id() + ":save-" + worker + '-' + index;
            store.recordWorld(PROFILE, key, key, duration + index);
            store.recordQuickPlay(PROFILE, key, key, duration + index + 100L);
        }
    }

    private static void verifyBothBatches(Path file) {
        BaselineStore store = new BaselineStore(file);
        assertEquals(15_000L, store.startupAverage(PROFILE).orElseThrow());
        for (String worker : List.of("a", "b")) {
            long base = worker.equals("a") ? 10_000L : 20_000L;
            for (int index = 0; index < 4; index++) {
                String key = PROFILE.id() + ":save-" + worker + '-' + index;
                assertEquals(base + index, store.worldAverage(PROFILE, key).orElseThrow(), key);
                assertEquals(base + index + 100L, store.quickPlayAverage(PROFILE, key).orElseThrow(), key);
            }
        }
    }

    private Process child(Path file, String worker, long duration) throws Exception {
        Path bin = Path.of(System.getProperty("java.home"), "bin");
        Path java = bin.resolve(Files.isRegularFile(bin.resolve("java.exe")) ? "java.exe" : "java");
        // Gradle's java.class.path can contain only its worker bootstrap JAR.
        // These code sources are the actual test/client outputs and required libraries.
        Set<String> classpath = new LinkedHashSet<>();
        for (Class<?> type : List.of(BaselineStoreProcess.class, BaselineStore.class, Gson.class, LoggerFactory.class)) {
            classpath.add(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toString());
        }
        return new ProcessBuilder(java.toString(), "-cp", String.join(File.pathSeparator, classpath),
                BaselineStoreProcess.class.getName(), file.toString(), directory.toString(), worker, Long.toString(duration))
                .redirectErrorStream(true).redirectOutput(directory.resolve(worker + ".log").toFile()).start();
    }

    private void awaitMarkers(String... markers) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        for (String marker : markers) {
            while (!Files.exists(directory.resolve(marker))) {
                assertTrue(System.nanoTime() < deadline, () -> "Missing worker marker " + marker + readWorkerLogs());
                Thread.sleep(10L);
            }
        }
    }

    private String readWorkerLogs() {
        StringBuilder text = new StringBuilder();
        for (String worker : List.of("a", "b")) {
            try { text.append('\n').append(worker).append(": ").append(Files.readString(directory.resolve(worker + ".log"))); }
            catch (java.io.IOException ignored) { }
        }
        return text.toString();
    }
}
