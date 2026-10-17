package com.oliver.metrakron.timing;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/** A real second JVM for history-lock tests; never loads Minecraft or creates a window. */
public final class BaselineStoreProcess {
    public static void main(String[] args) throws Exception {
        Path file = Path.of(args[0]);
        Path markers = Path.of(args[1]);
        String worker = args[2];
        long duration = Long.parseLong(args[3]);
        BaselineStore store = new BaselineStore(file);
        Files.writeString(markers.resolve(worker + ".ready"), "ready");
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        while (!Files.exists(markers.resolve("go"))) {
            if (System.nanoTime() > deadline) throw new IllegalStateException("Parent did not release worker");
            Thread.sleep(10L);
        }
        Files.writeString(markers.resolve(worker + ".attempting"), "attempting");
        ProfileFingerprint profile = new ProfileFingerprint("shared-profile", "History process test");
        store.recordStartup(profile, duration);
        for (int index = 0; index < 4; index++) {
            String key = profile.id() + ":save-" + worker + '-' + index;
            store.recordWorld(profile, key, key, duration + index);
            store.recordQuickPlay(profile, key, key, duration + index + 100L);
        }
        Files.writeString(markers.resolve(worker + ".done"), "done");
        System.out.println("History worker " + worker + " persisted startup and four independent saves/routes");
    }
}
