package com.oliver.metrakron.timing;

import com.oliver.metrakron.overlay.OverlayClock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class TimingControllerTest {
    private static final ProfileFingerprint PROFILE = new ProfileFingerprint("profile", "Test modpack");
    private static final String SAVE = "Generated World";
    private static final String KEY = PROFILE.id() + ':' + EnvironmentFingerprint.shortSha256(SAVE);

    @TempDir
    Path directory;
    private final AtomicLong clock = new AtomicLong();

    @Test
    void quickPlayIsOneMeasurementFromSplashThroughReadingJoiningAndPlayableFrames() {
        BaselineStore existing = store();
        existing.recordStartup(PROFILE, 10_000L);
        existing.recordWorld(PROFILE, KEY, SAVE, 20_000L);
        TimingController timer = quickPlay(SAVE);
        atSeconds(5);
        timer.ensureStartupStarted(PROFILE);
        LoadSnapshot splash = timer.snapshot(LoadStage.STARTUP);
        assertTrue(splash.active());
        assertEquals(LoadStage.QUICK_PLAY, splash.stage());
        assertEquals(LoadActivity.QUICK_PLAY_RESOURCES, splash.activity());
        assertNull(splash.averageMillis(), "Normal startup/world history must not estimate a cold world launch");

        atSeconds(15);
        assertTrue(timer.noteQuickPlayWorldOpening(SAVE));
        for (LoadStage stage : new LoadStage[]{LoadStage.WORLD_READING, LoadStage.WORLD_GENERATION,
                LoadStage.WORLD_JOINING}) {
            clock.addAndGet(10_000_000_000L);
            timer.noteBlockingScreen(LoadActivity.WORLD_DATA);
            timer.ensureStartupStarted(PROFILE);
            assertFalse(timer.finishStartupOnReadyScreen(), "A loading screen is not a main menu");
            LoadSnapshot snapshot = timer.snapshot(stage);
            assertTrue(snapshot.active());
            assertEquals(LoadStage.QUICK_PLAY, snapshot.stage());
            assertEquals(splash.sequence(), snapshot.sequence(), "Desktop clock must not restart");
            assertEquals((clock.get() - 5_000_000_000L) / 1_000_000L, snapshot.elapsedMillis());
        }

        atSeconds(65);
        finishPlayable(timer);
        assertEquals(60_000L, store().quickPlayAverage(PROFILE, KEY).orElseThrow());
        assertEquals(10_000L, store().startupAverage(PROFILE).orElseThrow());
        assertEquals(20_000L, store().worldAverage(PROFILE, KEY).orElseThrow());
        assertFalse(timer.snapshot(LoadStage.QUICK_PLAY).active());
        timer.ensureStartupStarted(PROFILE);
        assertFalse(timer.snapshot(LoadStage.STARTUP).active(), "Resource reload must not start a new launch");
    }

    @Test
    void subsequentQuickPlayCountsDownTheFullAverageAndRelearnsWhenItRunsOver() {
        store().recordQuickPlay(PROFILE, KEY, SAVE, 60_000L);
        TimingController timer = quickPlay(SAVE);
        timer.ensureStartupStarted(PROFILE);
        atSeconds(20);
        LoadSnapshot splash = timer.snapshot(LoadStage.STARTUP);
        assertEquals(60_000L, splash.averageMillis());
        assertEquals("00:40", OverlayClock.displayedTime(splash.elapsedMillis(), splash.averageMillis()));
        timer.noteQuickPlayWorldOpening(SAVE);
        atSeconds(45);
        LoadSnapshot world = timer.snapshot(LoadStage.WORLD_JOINING);
        assertEquals("00:15", OverlayClock.displayedTime(world.elapsedMillis(), world.averageMillis()));
        assertEquals(splash.sequence(), world.sequence());
        atSeconds(65);
        world = timer.snapshot(LoadStage.WORLD_JOINING);
        assertTrue(OverlayClock.isRelearning(world.elapsedMillis(), world.averageMillis()));
        assertEquals("+00:05", OverlayClock.displayedTime(world.elapsedMillis(), world.averageMillis()));
        finishPlayable(timer);
        assertEquals(62_500L, store().quickPlayAverage(PROFILE, KEY).orElseThrow());
    }

    @Test
    void bareQuickPlayResolvesActualSaveWithoutLosingElapsedTimeOrSequence() {
        store().recordQuickPlay(PROFILE, KEY, SAVE, 50_000L);
        TimingController timer = quickPlay(null);
        timer.ensureStartupStarted(PROFILE);
        LoadSnapshot before = timer.snapshot(LoadStage.STARTUP);
        assertNull(before.averageMillis());
        atSeconds(18);
        assertTrue(timer.noteQuickPlayWorldOpening(SAVE));
        LoadSnapshot after = timer.snapshot(LoadStage.WORLD_READING);
        assertEquals(before.sequence(), after.sequence());
        assertEquals(18_000L, after.elapsedMillis());
        assertEquals(50_000L, after.averageMillis());
        atSeconds(60);
        finishPlayable(timer);
        assertEquals(55_000L, store().quickPlayAverage(PROFILE, KEY).orElseThrow());
    }

    @Test
    void saveOpenBeforeFirstSplashIsRetainedButDoesNotStartAPrelaunchTimer() {
        TimingController timer = quickPlay(null);
        assertFalse(timer.noteQuickPlayWorldOpening(SAVE));
        atSeconds(10);
        timer.ensureStartupStarted(PROFILE);
        assertEquals(0L, timer.snapshot(LoadStage.STARTUP).elapsedMillis());
        assertEquals(LoadActivity.WORLD_OPENING, timer.snapshot(LoadStage.STARTUP).activity());
        atSeconds(20);
        finishPlayable(timer);
        assertEquals(10_000L, store().quickPlayAverage(PROFILE, KEY).orElseThrow());
    }

    @Test
    void unresolvedQuickPlayDoesNotContaminateAnySaveHistory() {
        TimingController timer = quickPlay(null);
        timer.ensureStartupStarted(PROFILE);
        atSeconds(20);
        finishPlayable(timer);
        assertTrue(store().quickPlayAverage(PROFILE, KEY).isEmpty());
        assertTrue(store().startupAverage(PROFILE).isEmpty());
        assertTrue(store().worldAverage(PROFILE, KEY).isEmpty());
    }

    @Test
    void titleScreenOrCancelledWorldNeverRecordsAnIncompleteQuickPlay() {
        for (boolean titleScreen : new boolean[]{true, false}) {
            TimingController timer = quickPlay(SAVE);
            timer.ensureStartupStarted(PROFILE);
            clock.addAndGet(40_000_000_000L);
            if (titleScreen) {
                timer.onTitleScreenRendered();
            } else {
                assertTrue(timer.cancelWorldLoad());
                assertFalse(timer.cancelWorldLoad());
            }
            assertFalse(timer.observeHudFrame(true));
            timer.ensureStartupStarted(PROFILE);
            assertFalse(timer.snapshot(LoadStage.STARTUP).active());
            assertTrue(store().quickPlayAverage(PROFILE, KEY).isEmpty());
            assertTrue(store().startupAverage(PROFILE).isEmpty());
        }
    }

    @Test
    void blockingScreensAndNonPlayableFramesResetTheCompletionGate() {
        TimingController timer = quickPlay(SAVE);
        timer.ensureStartupStarted(PROFILE);
        atSeconds(30);
        assertFalse(timer.observeHudFrame(true));
        assertFalse(timer.observeHudFrame(true));
        timer.noteBlockingScreen(LoadActivity.WORLD_JOINING);
        assertFalse(timer.observeHudFrame(true));
        assertFalse(timer.observeHudFrame(true));
        assertFalse(timer.observeHudFrame(false));
        assertTrue(store().quickPlayAverage(PROFILE, KEY).isEmpty());
        atSeconds(35);
        finishPlayable(timer);
        assertEquals(35_000L, store().quickPlayAverage(PROFILE, KEY).orElseThrow());
    }

    @Test
    void manualWorldLoadAfterQuickPlayCancellationUsesOnlyManualHistory() {
        store().recordWorld(PROFILE, KEY, SAVE, 12_000L);
        TimingController timer = quickPlay(SAVE);
        timer.ensureStartupStarted(PROFILE);
        atSeconds(30);
        timer.onTitleScreenRendered();
        atSeconds(40);
        timer.startWorld(PROFILE, SAVE, SAVE);
        assertFalse(timer.noteQuickPlayWorldOpening(SAVE));
        assertFalse(timer.finishStartupOnReadyScreen());
        LoadSnapshot world = timer.snapshot(LoadStage.WORLD_READING);
        assertEquals(LoadStage.WORLD_READING, world.stage());
        assertEquals(12_000L, world.averageMillis());
        assertEquals(0L, world.elapsedMillis());
        atSeconds(50);
        finishPlayable(timer);
        assertEquals(11_000L, store().worldAverage(PROFILE, KEY).orElseThrow());
        assertTrue(store().quickPlayAverage(PROFILE, KEY).isEmpty());
    }

    @Test
    void normalStartupStillEndsAtTheFirstReadyScreenAndKeepsSeparateWorldTiming() {
        TimingController timer = new TimingController(file(), clock::get);
        timer.ensureStartupStarted(PROFILE);
        atSeconds(10);
        assertEquals(LoadStage.STARTUP, timer.snapshot(LoadStage.STARTUP).stage());
        assertFalse(timer.snapshot(LoadStage.QUICK_PLAY).active());
        assertFalse(timer.cancelWorldLoad());
        assertFalse(timer.noteQuickPlayWorldOpening(SAVE));
        assertTrue(timer.finishStartupOnReadyScreen());
        assertFalse(timer.finishStartupOnReadyScreen());
        assertEquals(10_000L, store().startupAverage(PROFILE).orElseThrow());
        atSeconds(20);
        timer.startWorld(PROFILE, SAVE, SAVE);
        atSeconds(35);
        finishPlayable(timer);
        assertEquals(15_000L, store().worldAverage(PROFILE, KEY).orElseThrow());
        assertTrue(store().quickPlayAverage(PROFILE, KEY).isEmpty());
    }

    @Test
    void backgroundPauseFinishesManualAndQuickPlayWithoutRecordingLaterIdleTime() {
        for (boolean quick : new boolean[]{false, true}) {
            atSeconds(0);
            TimingController timer = quick ? quickPlay(SAVE) : new TimingController(file(), clock::get);
            if (quick) timer.ensureStartupStarted(PROFILE);
            else timer.startWorld(PROFILE, SAVE, SAVE);
            var paused = new WorldFrameState(true, true, true, true, false);
            atSeconds(30);
            // HUD + pause-screen callbacks from the same frame must count once.
            for (int frame = 0; frame < 3; frame++) {
                assertFalse(timer.observeWorldFrame(paused, false));
                assertEquals(frame == 2, timer.observeWorldFrame(paused, true));
            }
            assertFalse(timer.snapshot(LoadStage.WORLD_FIRST_FRAME).active());
            atSeconds(600);
            assertFalse(timer.observeWorldFrame(paused, true));
            assertFalse(timer.observeWorldFrame(new WorldFrameState(true, true, false, false, false), false));
            long recorded = quick ? store().quickPlayAverage(PROFILE, KEY).orElseThrow()
                    : store().worldAverage(PROFILE, KEY).orElseThrow();
            assertEquals(30_000L, recorded, "Background idle time must not enter the average");
        }
    }

    @Test
    void pauseCanFinishWithoutAnyHudCallbacksButNotWithoutWorldPlayerOrWithOverlay() {
        TimingController timer = quickPlay(SAVE);
        timer.ensureStartupStarted(PROFILE);
        var paused = new WorldFrameState(true, true, true, true, false);
        for (var blocked : new WorldFrameState[]{
                new WorldFrameState(false, true, true, true, false),
                new WorldFrameState(true, false, true, true, false),
                new WorldFrameState(true, true, true, true, true)}) {
            assertFalse(timer.observeWorldFrame(paused, true));
            assertFalse(timer.observeWorldFrame(paused, true));
            assertFalse(timer.observeWorldFrame(blocked, true));
        }
        atSeconds(25);
        assertFalse(timer.observeWorldFrame(paused, true));
        assertFalse(timer.observeWorldFrame(paused, true));
        assertTrue(timer.observeWorldFrame(paused, true));
        assertEquals(25_000L, store().quickPlayAverage(PROFILE, KEY).orElseThrow());
    }

    @Test
    void loadingScreensStillResetReadinessAndCancellationStillDiscardsPausedAttempt() {
        TimingController timer = quickPlay(SAVE);
        timer.ensureStartupStarted(PROFILE);
        var paused = new WorldFrameState(true, true, true, true, false);
        var loading = new WorldFrameState(true, true, true, false, false);
        assertFalse(loading.ready());
        assertFalse(timer.observeWorldFrame(paused, true));
        assertFalse(timer.observeWorldFrame(paused, true));
        // Same route as renderScreenTimer for reading/generation/joining screens.
        timer.noteBlockingScreen(LoadActivity.WORLD_JOINING);
        for (int i = 0; i < 10; i++) {
            assertFalse(timer.observeWorldFrame(loading, false));
            assertFalse(timer.observeWorldFrame(loading, true));
        }
        assertFalse(timer.observeWorldFrame(paused, true));
        assertFalse(timer.observeWorldFrame(paused, true));
        assertTrue(timer.cancelWorldLoad());
        assertFalse(timer.observeWorldFrame(paused, true));
        assertTrue(store().quickPlayAverage(PROFILE, KEY).isEmpty());
    }

    private TimingController quickPlay(String folder) {
        return new TimingController(file(), clock::get, new QuickPlayLaunch(true, folder));
    }

    private BaselineStore store() {
        return new BaselineStore(file());
    }

    private Path file() {
        return directory.resolve("baselines.json");
    }

    private void atSeconds(long seconds) {
        clock.set(seconds * 1_000_000_000L);
    }

    private static void finishPlayable(TimingController timer) {
        assertFalse(timer.observeHudFrame(true));
        assertFalse(timer.observeHudFrame(true));
        assertTrue(timer.observeHudFrame(true));
        assertFalse(timer.observeHudFrame(true), "A completed run is recorded only once");
    }
}
