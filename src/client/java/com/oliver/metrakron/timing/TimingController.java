package com.oliver.metrakron.timing;

//? if >=26.1 {
/*import net.minecraft.client.Minecraft;
*///?} else {
import net.minecraft.client.MinecraftClient;
//?}

import java.nio.file.Path;
import java.util.OptionalLong;
import java.util.function.LongSupplier;

public final class TimingController {
    private static final long NANOS_PER_MILLISECOND = 1_000_000L;
    private static final int PLAYABLE_HUD_FRAMES_REQUIRED = 3;

    private final BaselineStore baselines;
    private final LongSupplier nanoClock;
    private final StableFrameGate playableFrameGate = new StableFrameGate(PLAYABLE_HUD_FRAMES_REQUIRED);
    private final boolean quickPlayRequested;

    private ActiveLoad active;
    private boolean startupFinished;
    private String quickPlayFolderName;
    private boolean quickPlayWorldOpening;
    private long nextSequence;

    public TimingController(Path baselineFile, LongSupplier nanoClock) {
        this(baselineFile, nanoClock, QuickPlayLaunch.none());
    }

    public TimingController(Path baselineFile, LongSupplier nanoClock, QuickPlayLaunch launch) {
        this.baselines = new BaselineStore(baselineFile);
        this.nanoClock = nanoClock;
        this.quickPlayRequested = launch.requested();
        this.quickPlayFolderName = launch.folderName();
    }

    //? if >=26.1 {
    /*public synchronized void ensureStartupStarted(Minecraft client) {
    *///?} else {
    public synchronized void ensureStartupStarted(MinecraftClient client) {
    //?}
        if (startupFinished || active != null) {
            return;
        }

        ensureStartupStarted(EnvironmentFingerprint.capture(client));
    }

    synchronized void ensureStartupStarted(ProfileFingerprint profile) {
        if (startupFinished || active != null) {
            return;
        }
        String quickPlayKey = quickPlayFolderName == null ? null : worldKey(profile, quickPlayFolderName);
        active = new ActiveLoad(
                quickPlayRequested ? LoadKind.QUICK_PLAY : LoadKind.STARTUP,
                ++nextSequence,
                nanoClock.getAsLong(),
                profile,
                quickPlayRequested ? quickPlayKey : profile.id(),
                quickPlayRequested ? quickPlayFolderName : "Game startup",
                quickPlayRequested
                        ? (quickPlayKey == null ? null : boxed(baselines.quickPlayAverage(profile, quickPlayKey)))
                        : boxed(baselines.startupAverage(profile)),
                quickPlayRequested
                        ? (quickPlayWorldOpening ? LoadActivity.WORLD_OPENING : LoadActivity.QUICK_PLAY_RESOURCES)
                        : LoadActivity.STARTUP_RESOURCES
        );
    }

    /** A loading screen without a splash is not a completed Quick Play launch. */
    public synchronized boolean finishStartupOnReadyScreen() {
        if (active == null || active.kind != LoadKind.STARTUP) {
            return false;
        }
        onTitleScreenRendered();
        return true;
    }

    public synchronized void onTitleScreenRendered() {
        if (active != null && active.kind == LoadKind.STARTUP) {
            long elapsedMillis = elapsedMillis(active);
            baselines.recordStartup(active.profile, elapsedMillis);
        }
        active = null;
        playableFrameGate.reset();
        startupFinished = true;
    }

    public synchronized boolean cancelWorldLoad() {
        if (active == null || active.kind == LoadKind.STARTUP) {
            return false;
        }
        active = null;
        playableFrameGate.reset();
        startupFinished = true;
        return true;
    }

    /** Observes the real save-open route, also resolving newer versions' bare Quick Play flag. */
    public synchronized boolean noteQuickPlayWorldOpening(String folderName) {
        if (!quickPlayRequested || startupFinished || folderName == null || folderName.isBlank()
                || (active != null && active.kind != LoadKind.QUICK_PLAY)) {
            return false;
        }
        quickPlayFolderName = folderName;
        quickPlayWorldOpening = true;
        if (active == null) {
            return false;
        }
        String key = worldKey(active.profile, folderName);
        active = new ActiveLoad(
                active.kind,
                active.sequence,
                active.startedAtNanos,
                active.profile,
                key,
                folderName,
                boxed(baselines.quickPlayAverage(active.profile, key)),
                LoadActivity.WORLD_OPENING
        );
        playableFrameGate.reset();
        return true;
    }

    //? if >=26.1 {
    /*public synchronized void startWorld(Minecraft client, String folderName, String displayName) {
    *///?} else {
    public synchronized void startWorld(MinecraftClient client, String folderName, String displayName) {
    //?}
        startWorld(EnvironmentFingerprint.capture(client), folderName, displayName);
    }

    synchronized void startWorld(ProfileFingerprint profile, String folderName, String displayName) {
        String worldKey = worldKey(profile, folderName);
        // A world-list click is a new manual attempt, even after an abandoned Quick Play.
        startupFinished = true;
        playableFrameGate.reset();
        active = new ActiveLoad(
                LoadKind.WORLD,
                ++nextSequence,
                nanoClock.getAsLong(),
                profile,
                worldKey,
                displayName,
                boxed(baselines.worldAverage(profile, worldKey)),
                LoadActivity.WORLD_OPENING
        );
    }

    public synchronized void noteBlockingScreen(LoadActivity activity) {
        if (active != null && active.kind != LoadKind.STARTUP) {
            playableFrameGate.reset();
            updateActivity(activity);
        }
    }

    public synchronized void noteWorldActivity(LoadActivity activity) {
        if (active != null && active.kind != LoadKind.STARTUP) {
            updateActivity(activity);
        }
    }

    //? if >=26.1 {
    /*public synchronized boolean observeHudFrame(Minecraft client) {
    *///?} else {
    public synchronized boolean observeHudFrame(MinecraftClient client) {
    //?}
        if (active == null || active.kind == LoadKind.STARTUP) {
            return false;
        }

        //? if >=26.2 {
        /*boolean playableFrame = client.level != null
                && client.player != null
                && client.gui.screen() == null
                && client.gui.overlay() == null;
        *///?} else if >=26.1 {
        /*boolean playableFrame = client.level != null
                && client.player != null
                && client.screen == null
                && client.getOverlay() == null;
        *///?} else {
        boolean playableFrame = client.world != null
                && client.player != null
                && client.currentScreen == null
                && client.getOverlay() == null;
        //?}
        return observeHudFrame(playableFrame);
    }

    synchronized boolean observeHudFrame(boolean playableFrame) {
        if (active == null || active.kind == LoadKind.STARTUP) {
            return false;
        }
        if (!playableFrameGate.observe(playableFrame)) {
            return false;
        }

        long elapsedMillis = elapsedMillis(active);
        if (active.kind == LoadKind.QUICK_PLAY) {
            // Never mix an unresolved latest-world request with any other save's history.
            if (active.key != null) {
                baselines.recordQuickPlay(active.profile, active.key, active.label, elapsedMillis);
            }
        } else {
            baselines.recordWorld(active.profile, active.key, active.label, elapsedMillis);
        }
        active = null;
        playableFrameGate.reset();
        startupFinished = true;
        return true;
    }

    public synchronized LoadSnapshot snapshot(LoadStage stage) {
        if (active == null || !stageMatchesActiveLoad(stage, active.kind)) {
            return LoadSnapshot.inactive(stage);
        }
        return new LoadSnapshot(
                true,
                active.kind == LoadKind.QUICK_PLAY ? LoadStage.QUICK_PLAY : stage,
                active.sequence,
                elapsedMillis(active),
                active.averageMillis,
                active.activity
        );
    }

    private void updateActivity(LoadActivity activity) {
        if (active.activity != activity) {
            active = new ActiveLoad(
                    active.kind,
                    active.sequence,
                    active.startedAtNanos,
                    active.profile,
                    active.key,
                    active.label,
                    active.averageMillis,
                    activity
            );
        }
    }

    private static boolean stageMatchesActiveLoad(LoadStage stage, LoadKind kind) {
        if (kind == LoadKind.QUICK_PLAY) {
            return true;
        }
        if (stage == LoadStage.QUICK_PLAY) {
            return false;
        }
        return (stage == LoadStage.STARTUP) == (kind == LoadKind.STARTUP);
    }

    private static String worldKey(ProfileFingerprint profile, String folderName) {
        return profile.id() + ':' + EnvironmentFingerprint.shortSha256(folderName);
    }

    private long elapsedMillis(ActiveLoad load) {
        long elapsedNanos = Math.max(0L, nanoClock.getAsLong() - load.startedAtNanos);
        return elapsedNanos / NANOS_PER_MILLISECOND;
    }

    private static Long boxed(OptionalLong value) {
        return value.isPresent() ? value.getAsLong() : null;
    }

    private enum LoadKind {
        STARTUP,
        QUICK_PLAY,
        WORLD
    }

    private record ActiveLoad(
            LoadKind kind,
            long sequence,
            long startedAtNanos,
            ProfileFingerprint profile,
            String key,
            String label,
            Long averageMillis,
            LoadActivity activity
    ) {
    }
}
