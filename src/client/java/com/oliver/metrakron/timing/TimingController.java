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

    private ActiveLoad active;
    private boolean startupFinished;
    private long nextSequence;

    public TimingController(Path baselineFile, LongSupplier nanoClock) {
        this.baselines = new BaselineStore(baselineFile);
        this.nanoClock = nanoClock;
    }

    //? if >=26.1 {
    /*public synchronized void ensureStartupStarted(Minecraft client) {
    *///?} else {
    public synchronized void ensureStartupStarted(MinecraftClient client) {
    //?}
        if (startupFinished || active != null) {
            return;
        }

        ProfileFingerprint profile = EnvironmentFingerprint.capture(client);
        active = new ActiveLoad(
                LoadKind.STARTUP,
                ++nextSequence,
                nanoClock.getAsLong(),
                profile,
                profile.id(),
                "Game startup",
                boxed(baselines.startupAverage(profile)),
                LoadActivity.STARTUP_RESOURCES
        );
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

    //? if >=26.1 {
    /*public synchronized void startWorld(Minecraft client, String folderName, String displayName) {
    *///?} else {
    public synchronized void startWorld(MinecraftClient client, String folderName, String displayName) {
    //?}
        ProfileFingerprint profile = EnvironmentFingerprint.capture(client);
        String worldKey = profile.id() + ':' + EnvironmentFingerprint.shortSha256(folderName);
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
        if (active != null && active.kind == LoadKind.WORLD) {
            playableFrameGate.reset();
            updateActivity(activity);
        }
    }

    public synchronized void noteWorldActivity(LoadActivity activity) {
        if (active != null && active.kind == LoadKind.WORLD) {
            updateActivity(activity);
        }
    }

    //? if >=26.1 {
    /*public synchronized boolean observeHudFrame(Minecraft client) {
    *///?} else {
    public synchronized boolean observeHudFrame(MinecraftClient client) {
    //?}
        if (active == null || active.kind != LoadKind.WORLD) {
            return false;
        }

        //? if >=26.2 {
        /*boolean playableFrame = client.level != null
                && client.player != null
                && client.gui.screen() == null;
        *///?} else if >=26.1 {
        /*boolean playableFrame = client.level != null
                && client.player != null
                && client.screen == null;
        *///?} else {
        boolean playableFrame = client.world != null
                && client.player != null
                && client.currentScreen == null;
        //?}
        if (!playableFrameGate.observe(playableFrame)) {
            return false;
        }

        long elapsedMillis = elapsedMillis(active);
        baselines.recordWorld(active.profile, active.key, active.label, elapsedMillis);
        active = null;
        playableFrameGate.reset();
        return true;
    }

    public synchronized LoadSnapshot snapshot(LoadStage stage) {
        if (active == null || !stageMatchesActiveLoad(stage, active.kind)) {
            return LoadSnapshot.inactive(stage);
        }
        return new LoadSnapshot(
                true,
                stage,
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
        return (stage == LoadStage.STARTUP) == (kind == LoadKind.STARTUP);
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
