package com.oliver.metrakron.timing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaselineStoreTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void averagesOnlyTheLatestThreeCompletedRuns() {
        Path file = temporaryDirectory.resolve("metrakron").resolve("baselines.json");
        ProfileFingerprint profile = new ProfileFingerprint("profile-a", "Test profile");
        BaselineStore store = new BaselineStore(file);

        store.recordStartup(profile, 12_000L);
        store.recordStartup(profile, 9_000L);
        store.recordStartup(profile, 15_000L);
        assertEquals(12_000L, store.startupAverage(profile).orElseThrow());

        store.recordStartup(profile, 18_000L);
        assertEquals(14_000L, store.startupAverage(profile).orElseThrow());

        store.recordWorld(profile, "profile-a:world-a", "World A", 30_000L);
        store.recordWorld(profile, "profile-a:world-a", "World A", 24_000L);
        store.recordWorld(profile, "profile-a:world-a", "World A", 36_000L);
        store.recordWorld(profile, "profile-a:world-a", "World A", 42_000L);

        BaselineStore reloaded = new BaselineStore(file);
        assertEquals(14_000L, reloaded.startupAverage(profile).orElseThrow());
        assertEquals(
                34_000L,
                reloaded.worldAverage(profile, "profile-a:world-a").orElseThrow()
        );
        assertTrue(file.toFile().isFile());
    }

    @Test
    void migratesTheEarliestVolatileProfileMarks() throws IOException {
        Path file = temporaryDirectory.resolve("legacy").resolve("baselines.json");
        Files.createDirectories(file.getParent());
        Files.writeString(file, """
                {
                  "schemaVersion": 1,
                  "profiles": {
                    "legacy-first": {"summary": "Same game"},
                    "legacy-second": {"summary": "Same game"}
                  },
                  "startup": {
                    "legacy-first": {
                      "milliseconds": 3466,
                      "capturedAt": "2026-08-30T08:00:55Z",
                      "label": "Game startup"
                    },
                    "legacy-second": {
                      "milliseconds": 3405,
                      "capturedAt": "2026-08-30T08:01:44Z",
                      "label": "Game startup"
                    }
                  },
                  "worlds": {
                    "legacy-first:world-hash": {
                      "milliseconds": 7145,
                      "capturedAt": "2026-08-30T08:01:07Z",
                      "label": "New World"
                    },
                    "legacy-second:world-hash": {
                      "milliseconds": 6890,
                      "capturedAt": "2026-08-30T08:02:07Z",
                      "label": "New World"
                    }
                  }
                }
                """, StandardCharsets.UTF_8);

        ProfileFingerprint stableProfile = new ProfileFingerprint("stable-profile", "Same game");
        BaselineStore store = new BaselineStore(file);

        assertEquals(3436L, store.startupAverage(stableProfile).orElseThrow());
        assertEquals(
                7018L,
                store.worldAverage(stableProfile, "stable-profile:world-hash").orElseThrow()
        );

        BaselineStore reloaded = new BaselineStore(file);
        assertEquals(3436L, reloaded.startupAverage(stableProfile).orElseThrow());
        assertEquals(
                7018L,
                reloaded.worldAverage(stableProfile, "stable-profile:world-hash").orElseThrow()
        );
        assertTrue(Files.readString(file).contains("\"schemaVersion\": 4"));
    }

    @Test
    void carriesVersionTwoSingleMarksIntoTheNewHistory() throws IOException {
        Path file = temporaryDirectory.resolve("version-two").resolve("baselines.json");
        Files.createDirectories(file.getParent());
        Files.writeString(file, """
                {
                  "schemaVersion": 2,
                  "profiles": {
                    "stable-profile": {"summary": "Same game"}
                  },
                  "startup": {
                    "stable-profile": {
                      "milliseconds": 3466,
                      "capturedAt": "2026-08-30T08:00:55Z",
                      "label": "Game startup"
                    }
                  },
                  "worlds": {
                    "stable-profile:world-hash": {
                      "milliseconds": 7145,
                      "capturedAt": "2026-08-30T08:01:07Z",
                      "label": "New World"
                    }
                  }
                }
                """, StandardCharsets.UTF_8);

        ProfileFingerprint stableProfile = new ProfileFingerprint("stable-profile", "Same game");
        BaselineStore store = new BaselineStore(file);

        assertEquals(3466L, store.startupAverage(stableProfile).orElseThrow());
        assertEquals(
                7145L,
                store.worldAverage(stableProfile, "stable-profile:world-hash").orElseThrow()
        );

        String migrated = Files.readString(file);
        assertTrue(migrated.contains("\"schemaVersion\": 4"));
        assertTrue(migrated.contains("\"runs\""));

        store.recordStartup(stableProfile, 4534L);
        assertEquals(4000L, store.startupAverage(stableProfile).orElseThrow());
    }

    @Test
    void profileHashIsStableAndCompact() {
        assertEquals(
                EnvironmentFingerprint.shortSha256("same profile"),
                EnvironmentFingerprint.shortSha256("same profile")
        );
        assertEquals(20, EnvironmentFingerprint.shortSha256("same profile").length());
    }

    @Test
    void carriesTheLatestStartupRunsAcrossModpackProfileChanges() {
        Path file = temporaryDirectory.resolve("profile-change").resolve("baselines.json");
        ProfileFingerprint firstProfile = new ProfileFingerprint("profile-a", "Original modpack");
        ProfileFingerprint changedProfile = new ProfileFingerprint("profile-b", "Changed modpack");
        BaselineStore store = new BaselineStore(file);

        store.recordStartup(firstProfile, 10_000L);
        store.recordStartup(firstProfile, 20_000L);
        assertEquals(15_000L, store.startupAverage(changedProfile).orElseThrow());

        store.recordStartup(changedProfile, 30_000L);
        store.recordStartup(changedProfile, 40_000L);

        assertEquals(30_000L, store.startupAverage(firstProfile).orElseThrow());
        assertEquals(30_000L, store.startupAverage(changedProfile).orElseThrow());
    }

    @Test
    void carriesWorldRunsAcrossProfilesOnlyForTheSameSave() {
        Path file = temporaryDirectory.resolve("world-profile-change").resolve("baselines.json");
        ProfileFingerprint firstProfile = new ProfileFingerprint("profile-a", "Original modpack");
        ProfileFingerprint changedProfile = new ProfileFingerprint("profile-b", "Changed modpack");
        BaselineStore store = new BaselineStore(file);

        store.recordWorld(firstProfile, "profile-a:world-one", "World One", 30_000L);
        store.recordWorld(firstProfile, "profile-a:world-one", "World One", 60_000L);

        assertEquals(
                45_000L,
                store.worldAverage(changedProfile, "profile-b:world-one").orElseThrow()
        );
        assertFalse(store.worldAverage(changedProfile, "profile-b:world-two").isPresent());
    }

    @Test
    void quickPlayKeepsItsOwnLatestThreeRunsAcrossProfileChanges() {
        Path file = temporaryDirectory.resolve("quick-play.json");
        ProfileFingerprint first = new ProfileFingerprint("first", "Original modpack");
        ProfileFingerprint changed = new ProfileFingerprint("changed", "Updated modpack");
        BaselineStore store = new BaselineStore(file);

        store.recordStartup(first, 5_000L);
        store.recordWorld(first, "first:save", "Save", 8_000L);
        store.recordQuickPlay(first, "first:save", "Save", 10_000L);
        store.recordQuickPlay(first, "first:save", "Save", 20_000L);
        store.recordQuickPlay(changed, "changed:save", "Save", 30_000L);
        store.recordQuickPlay(changed, "changed:save", "Save", 40_000L);
        store.recordQuickPlay(changed, "changed:other", "Other", 90_000L);

        BaselineStore reloaded = new BaselineStore(file);
        assertEquals(30_000L, reloaded.quickPlayAverage(changed, "changed:save").orElseThrow());
        assertEquals(90_000L, reloaded.quickPlayAverage(first, "first:other").orElseThrow());
        assertFalse(reloaded.quickPlayAverage(first, "first:unplayed").isPresent());
        assertEquals(5_000L, reloaded.startupAverage(changed).orElseThrow());
        assertEquals(8_000L, reloaded.worldAverage(changed, "changed:save").orElseThrow());
    }

    @Test
    void versionThreeHistoryIsPreservedWithoutGuessingQuickPlayDurations() throws IOException {
        Path file = temporaryDirectory.resolve("version-three.json");
        Files.writeString(file, """
                {
                  "schemaVersion": 3,
                  "profiles": {"profile": {"summary": "Existing game"}},
                  "startup": {"profile": {"runs": [
                    {"milliseconds": 12000, "capturedAt": "2026-09-01T10:00:00Z", "label": "Game startup"}
                  ]}},
                  "worlds": {"profile:save": {"runs": [
                    {"milliseconds": 18000, "capturedAt": "2026-09-01T10:02:00Z", "label": "Save"}
                  ]}}
                }
                """, StandardCharsets.UTF_8);
        ProfileFingerprint profile = new ProfileFingerprint("profile", "Existing game");
        BaselineStore store = new BaselineStore(file);
        assertFalse(store.quickPlayAverage(profile, "profile:save").isPresent());
        assertEquals(12_000L, store.startupAverage(profile).orElseThrow());
        assertEquals(18_000L, store.worldAverage(profile, "profile:save").orElseThrow());
        store.recordQuickPlay(profile, "profile:save", "Save", 35_000L);

        BaselineStore reloaded = new BaselineStore(file);
        assertEquals(35_000L, reloaded.quickPlayAverage(profile, "profile:save").orElseThrow());
        assertEquals(12_000L, reloaded.startupAverage(profile).orElseThrow());
        assertEquals(18_000L, reloaded.worldAverage(profile, "profile:save").orElseThrow());
        assertTrue(Files.readString(file).contains("\"schemaVersion\": 4"));
    }
}
