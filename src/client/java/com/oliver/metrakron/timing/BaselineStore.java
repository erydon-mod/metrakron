package com.oliver.metrakron.timing;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.oliver.metrakron.MetrakronClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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

final class BaselineStore {
    private static final int VOLATILE_PROFILE_SCHEMA_VERSION = 1;
    private static final int SINGLE_MARK_SCHEMA_VERSION = 2;
    private static final int ROLLING_HISTORY_SCHEMA_VERSION = 3;
    private static final int SCHEMA_VERSION = 4;
    private static final int MAX_COMPLETED_RUNS = 3;
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private final Path file;
    private Data data;

    BaselineStore(Path file) {
        this.file = file;
        this.data = read(file);
    }

    synchronized OptionalLong startupAverage(ProfileFingerprint profile) {
        upgradeProfile(profile);
        return averageLatestDistinct(data.startup.values());
    }

    synchronized OptionalLong worldAverage(ProfileFingerprint profile, String worldKey) {
        upgradeProfile(profile);
        return averageForWorld(data.worlds, worldKey);
    }

    synchronized OptionalLong quickPlayAverage(ProfileFingerprint profile, String worldKey) {
        upgradeProfile(profile);
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
        upgradeProfileWithoutWriting(profile);
        registerProfile(profile);
        data.startup
                .computeIfAbsent(profile.id(), ignored -> new History())
                .record(milliseconds, "Game startup");
        write();
    }

    synchronized void recordWorld(
            ProfileFingerprint profile,
            String worldKey,
            String worldLabel,
            long milliseconds
    ) {
        upgradeProfileWithoutWriting(profile);
        registerProfile(profile);
        data.worlds
                .computeIfAbsent(worldKey, ignored -> new History())
                .record(milliseconds, worldLabel);
        write();
    }

    synchronized void recordQuickPlay(
            ProfileFingerprint profile,
            String worldKey,
            String worldLabel,
            long milliseconds
    ) {
        upgradeProfileWithoutWriting(profile);
        registerProfile(profile);
        data.quickPlayWorlds
                .computeIfAbsent(worldKey, ignored -> new History())
                .record(milliseconds, worldLabel);
        write();
    }

    private boolean registerProfile(ProfileFingerprint profile) {
        if (data.profiles.containsKey(profile.id())) {
            return false;
        }
        data.profiles.put(profile.id(), new ProfileDetails(profile.summary()));
        return true;
    }

    private void upgradeProfile(ProfileFingerprint profile) {
        if (upgradeProfileWithoutWriting(profile)) {
            write();
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

    private void write() {
        Path parent = file.getParent();
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.createDirectories(parent);
            Files.writeString(temporary, GSON.toJson(data), StandardCharsets.UTF_8);
            try {
                Files.move(
                        temporary,
                        file,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING
                );
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            MetrakronClient.LOGGER.warn("Could not save Metrakron timing history to {}", file, exception);
        } finally {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException ignored) {
                // The next successful write replaces this transient file.
            }
        }
    }

    private static Data read(Path file) {
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
        } catch (IOException | JsonParseException exception) {
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

        private void record(long milliseconds, String label) {
            runs.add(Measurement.create(milliseconds, label));
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
