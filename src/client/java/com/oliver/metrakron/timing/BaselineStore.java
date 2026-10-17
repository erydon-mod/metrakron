package com.oliver.metrakron.timing;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.oliver.metrakron.MetrakronClient;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

final class BaselineStore {
    private static final int VOLATILE_PROFILE_SCHEMA_VERSION = 1;
    private static final int SINGLE_MARK_SCHEMA_VERSION = 2;
    private static final int ROLLING_HISTORY_SCHEMA_VERSION = 3;
    private static final int SCHEMA_VERSION = 4;
    private static final int MAX_COMPLETED_RUNS = 3;
    // FileLock blocks other processes, but overlapping locks in one JVM throw
    // instead of waiting. Serialize this JVM's stores before taking the OS lock.
    private static final Map<Path, ReentrantLock> JVM_LOCKS = new ConcurrentHashMap<>();
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private final Path file;
    private final List<PendingRun> pendingRuns = new ArrayList<>();
    private Data data;

    BaselineStore(Path file) {
        this.file = file.toAbsolutePath().normalize();
        this.data = readSafely(this.file);
    }

    synchronized OptionalLong startupAverage(ProfileFingerprint profile) {
        refresh(profile);
        return averageLatestDistinct(data.startup.values());
    }

    synchronized OptionalLong worldAverage(ProfileFingerprint profile, String worldKey) {
        refresh(profile);
        return averageForWorld(data.worlds, worldKey);
    }

    synchronized OptionalLong quickPlayAverage(ProfileFingerprint profile, String worldKey) {
        refresh(profile);
        return averageForWorld(data.quickPlayWorlds, worldKey);
    }

    private static OptionalLong averageForWorld(Map<String, History> histories, String worldKey) {
        int separator = worldKey.indexOf(':');
        String worldSuffix = separator < 0 ? worldKey : worldKey.substring(separator);
        List<History> matchingHistories = histories.entrySet()
                .stream()
                .filter(entry -> entry.getKey().endsWith(worldSuffix))
                .map(Map.Entry::getValue)
                .toList();
        return averageLatestDistinct(matchingHistories);
    }

    synchronized void recordStartup(ProfileFingerprint profile, long milliseconds) {
        record(new PendingRun(LoadKind.STARTUP, profile, profile.id(),
                Measurement.create(milliseconds, "Game startup")));
    }

    synchronized void recordWorld(
            ProfileFingerprint profile,
            String worldKey,
            String worldLabel,
            long milliseconds
    ) {
        record(new PendingRun(LoadKind.WORLD, profile, worldKey,
                Measurement.create(milliseconds, worldLabel)));
    }

    synchronized void recordQuickPlay(
            ProfileFingerprint profile,
            String worldKey,
            String worldLabel,
            long milliseconds
    ) {
        record(new PendingRun(LoadKind.QUICK_PLAY, profile, worldKey,
                Measurement.create(milliseconds, worldLabel)));
    }

    private void record(PendingRun run) {
        pendingRuns.add(run);
        apply(run); // Keep a completed run available locally if persistence fails.
        refresh(run.profile);
    }

    private void apply(PendingRun run) {
        registerProfile(run.profile);
        Map<String, History> histories = switch (run.kind) {
            case STARTUP -> data.startup;
            case WORLD -> data.worlds;
            case QUICK_PLAY -> data.quickPlayWorlds;
        };
        histories.computeIfAbsent(run.key, ignored -> new History()).record(run.measurement);
    }

    private boolean registerProfile(ProfileFingerprint profile) {
        if (data.profiles.containsKey(profile.id())) {
            return false;
        }
        data.profiles.put(profile.id(), new ProfileDetails(profile.summary()));
        return true;
    }

    /** Called only when a load begins or completes, never while rendering frames. */
    private void refresh(ProfileFingerprint profile) {
        try {
            Path parent = file.getParent();
            Files.createDirectories(parent);
            Path target = parent.toRealPath().resolve(file.getFileName());
            ReentrantLock localLock = JVM_LOCKS.computeIfAbsent(target, ignored -> new ReentrantLock());
            localLock.lock();
            try {
                // Lock a stable companion, since atomic replacement changes the data file itself.
                Path lockFile = target.resolveSibling(target.getFileName() + ".lock");
                try (FileChannel channel = FileChannel.open(lockFile,
                        StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                     FileLock ignored = channel.lock()) {
                    data = read(target);
                    boolean migrated = upgradeProfileWithoutWriting(profile);
                    pendingRuns.forEach(this::apply);
                    if (migrated || !pendingRuns.isEmpty()) {
                        write(target);
                        pendingRuns.clear();
                    }
                }
            } finally {
                localLock.unlock();
            }
        } catch (IOException exception) {
            MetrakronClient.LOGGER.warn("Could not update Metrakron timing history at {}", file, exception);
        }
    }

    private boolean upgradeProfileWithoutWriting(ProfileFingerprint profile) {
        if (data.schemaVersion == SCHEMA_VERSION) {
            return false;
        }

        if (data.schemaVersion == VOLATILE_PROFILE_SCHEMA_VERSION) {
            migrateVolatileProfile(profile);
        }

        // Version 2 already uses the stable profile identity. Its single mark
        // was converted into a one-run History while the file was read.
        // Version 3 histories remain separate from the new full Quick Play measurements.
        data.schemaVersion = SCHEMA_VERSION;
        return true;
    }

    /**
     * Version 1 included volatile Iris/Sodium file contents in profile IDs.
     * Move the oldest matching completed runs to the stable identity before
     * beginning the rolling history.
     */
    private void migrateVolatileProfile(ProfileFingerprint profile) {
        Set<String> compatibleProfileIds = new HashSet<>();
        for (Map.Entry<String, ProfileDetails> entry : data.profiles.entrySet()) {
            if (entry.getValue() != null
                    && Objects.equals(entry.getValue().summary, profile.summary())) {
                compatibleProfileIds.add(entry.getKey());
            }
        }

        Measurement earliestStartup = null;
        for (String profileId : compatibleProfileIds) {
            earliestStartup = earlier(earliestStartup, earliest(data.startup.get(profileId)));
        }
        if (earliestStartup != null) {
            data.startup.putIfAbsent(profile.id(), History.single(earliestStartup));
        }

        Map<String, Measurement> earliestWorldsBySuffix = new TreeMap<>();
        for (Map.Entry<String, History> entry : data.worlds.entrySet()) {
            int separator = entry.getKey().indexOf(':');
            if (separator < 1 || !compatibleProfileIds.contains(entry.getKey().substring(0, separator))) {
                continue;
            }
            Measurement candidate = earliest(entry.getValue());
            if (candidate == null) {
                continue;
            }
            String suffix = entry.getKey().substring(separator);
            earliestWorldsBySuffix.merge(suffix, candidate, BaselineStore::earlier);
        }
        for (Map.Entry<String, Measurement> entry : earliestWorldsBySuffix.entrySet()) {
            data.worlds.putIfAbsent(profile.id() + entry.getKey(), History.single(entry.getValue()));
        }

        registerProfile(profile);
    }

    private void write(Path target) throws IOException {
        Path parent = target.getParent();
        Path temporary = Files.createTempFile(parent, "." + target.getFileName() + '-', ".tmp");
        try {
            Files.writeString(temporary, GSON.toJson(data), StandardCharsets.UTF_8);
            try {
                Files.move(
                        temporary,
                        target,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING
                );
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static Data readSafely(Path file) {
        try {
            return read(file);
        } catch (IOException exception) {
            MetrakronClient.LOGGER.warn("Could not read Metrakron timing history from {}", file, exception);
            return new Data();
        }
    }

    private static Data read(Path file) throws IOException {
        if (!Files.isRegularFile(file)) {
            return new Data();
        }
        try {
            Data loaded = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), Data.class);
            if (loaded == null || !supportedSchema(loaded.schemaVersion)) {
                MetrakronClient.LOGGER.warn("Ignoring unsupported Metrakron timing schema in {}", file);
                return new Data();
            }
            loaded.sanitize();
            return loaded;
        } catch (JsonParseException exception) {
            MetrakronClient.LOGGER.warn("Could not read Metrakron timing history from {}", file, exception);
            return new Data();
        }
    }

    private static boolean supportedSchema(int schemaVersion) {
        return schemaVersion == VOLATILE_PROFILE_SCHEMA_VERSION
                || schemaVersion == SINGLE_MARK_SCHEMA_VERSION
                || schemaVersion == ROLLING_HISTORY_SCHEMA_VERSION
                || schemaVersion == SCHEMA_VERSION;
    }

    private static OptionalLong averageLatestDistinct(Iterable<History> histories) {
        Map<String, Measurement> distinctRuns = new HashMap<>();
        for (History history : histories) {
            if (history == null) {
                continue;
            }
            for (Measurement run : history.runs) {
                distinctRuns.putIfAbsent(measurementIdentity(run), run);
            }
        }
        if (distinctRuns.isEmpty()) {
            return OptionalLong.empty();
        }

        List<Measurement> latestRuns = distinctRuns.values()
                .stream()
                .sorted(Comparator
                        .comparing(BaselineStore::capturedAt)
                        .thenComparingLong(run -> run.milliseconds)
                        .thenComparing(run -> run.label))
                .toList();
        int firstIncludedRun = Math.max(0, latestRuns.size() - MAX_COMPLETED_RUNS);
        double total = 0.0D;
        for (int index = firstIncludedRun; index < latestRuns.size(); index++) {
            total += latestRuns.get(index).milliseconds;
        }
        int includedRunCount = latestRuns.size() - firstIncludedRun;
        return OptionalLong.of(Math.max(1L, Math.round(total / includedRunCount)));
    }

    private static String measurementIdentity(Measurement measurement) {
        return measurement.capturedAt
                + '\u001f'
                + measurement.milliseconds
                + '\u001f'
                + measurement.label;
    }

    private static Measurement earliest(History history) {
        if (history == null) {
            return null;
        }
        Measurement earliest = null;
        for (Measurement run : history.runs) {
            earliest = earlier(earliest, run);
        }
        return earliest;
    }

    private static Measurement earlier(Measurement first, Measurement second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return capturedAt(first).compareTo(capturedAt(second)) <= 0 ? first : second;
    }

    private static Instant capturedAt(Measurement measurement) {
        try {
            return Instant.parse(measurement.capturedAt);
        } catch (RuntimeException ignored) {
            return Instant.MAX;
        }
    }

    private static final class Data {
        private int schemaVersion = SCHEMA_VERSION;
        private Map<String, ProfileDetails> profiles = new TreeMap<>();
        private Map<String, History> startup = new TreeMap<>();
        private Map<String, History> worlds = new TreeMap<>();
        private Map<String, History> quickPlayWorlds = new TreeMap<>();

        private void sanitize() {
            profiles = profiles == null ? new TreeMap<>() : new TreeMap<>(profiles);
            startup = sanitizeHistories(startup);
            worlds = sanitizeHistories(worlds);
            quickPlayWorlds = sanitizeHistories(quickPlayWorlds);
        }

        private static Map<String, History> sanitizeHistories(Map<String, History> histories) {
            Map<String, History> sanitized = histories == null
                    ? new TreeMap<>()
                    : new TreeMap<>(histories);
            sanitized.values().removeIf(Objects::isNull);
            sanitized.values().forEach(History::sanitize);
            return sanitized;
        }
    }

    private static final class ProfileDetails {
        private final String summary;

        private ProfileDetails(String summary) {
            this.summary = summary;
        }
    }

    /**
     * The legacy fields allow Gson to read version 1/2 single-mark objects.
     * They are cleared during sanitization, so current writes use only `runs`.
     */
    private static final class History {
        private List<Measurement> runs = new ArrayList<>();
        private Long milliseconds;
        private String capturedAt;
        private String label;

        private static History single(Measurement measurement) {
            History history = new History();
            history.runs.add(measurement.copy());
            return history;
        }

        private void record(Measurement measurement) {
            // A successful replace followed by a cleanup error may be retried.
            // Reusing the original event identity prevents counting that run twice.
            if (runs.stream().anyMatch(run -> measurementIdentity(run).equals(measurementIdentity(measurement)))) {
                return;
            }
            runs.add(measurement.copy());
            runs.sort(Comparator.comparing(BaselineStore::capturedAt)
                    .thenComparingLong(run -> run.milliseconds).thenComparing(run -> run.label));
            trimToLatestRuns();
        }

        private void sanitize() {
            runs = runs == null ? new ArrayList<>() : new ArrayList<>(runs);
            runs.removeIf(Objects::isNull);
            runs.forEach(Measurement::sanitize);
            runs.removeIf(run -> run.milliseconds < 1L);

            if (runs.isEmpty() && milliseconds != null && milliseconds > 0L) {
                runs.add(new Measurement(milliseconds, capturedAt, label));
            }

            milliseconds = null;
            capturedAt = null;
            label = null;
            trimToLatestRuns();
        }

        private void trimToLatestRuns() {
            if (runs.size() > MAX_COMPLETED_RUNS) {
                runs = new ArrayList<>(runs.subList(runs.size() - MAX_COMPLETED_RUNS, runs.size()));
            }
        }
    }

    private enum LoadKind { STARTUP, WORLD, QUICK_PLAY }

    private record PendingRun(LoadKind kind, ProfileFingerprint profile, String key, Measurement measurement) { }

    private static final class Measurement {
        private long milliseconds;
        private String capturedAt;
        private String label;

        private Measurement(long milliseconds, String capturedAt, String label) {
            this.milliseconds = Math.max(1L, milliseconds);
            this.capturedAt = capturedAt;
            this.label = label;
        }

        private static Measurement create(long milliseconds, String label) {
            return new Measurement(milliseconds, Instant.now().toString(), label);
        }

        private Measurement copy() {
            return new Measurement(milliseconds, capturedAt, label);
        }

        private void sanitize() {
            milliseconds = Math.max(1L, milliseconds);
            if (capturedAt == null || capturedAt.isBlank()) {
                capturedAt = Instant.MAX.toString();
            }
            if (label == null) {
                label = "";
            }
        }
    }
}
