import java.awt.Color;
import java.awt.Font;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/** Headless production-JAR check: isolated adapter + outer bundle, exactly like the helper. */
public class VerifySharedAssets {
    public static void main(String[] args) throws Exception {
        Path bundle = Path.of(args[0]).toAbsolutePath();
        Path scratch = Files.createTempDirectory("metrakron-assets-test-");
        int adapters = 0;
        try (ZipFile zip = new ZipFile(bundle.toFile())) {
            var assets = zip.stream().filter(e -> !e.isDirectory() && e.getName().startsWith("assets/"))
                    .map(ZipEntry::getName).toList();
            var nested = zip.stream().filter(e -> e.getName().startsWith("META-INF/jars/") && e.getName().endsWith(".jar")).toList();
            for (var entry : nested) {
                Path adapter = scratch.resolve("adapter.jar");
                try (var input = zip.getInputStream(entry)) { Files.copy(input, adapter, StandardCopyOption.REPLACE_EXISTING); }
                try (var loader = new URLClassLoader(new java.net.URL[]{adapter.toUri().toURL(), bundle.toUri().toURL()},
                        ClassLoader.getPlatformClassLoader())) {
                    for (String asset : assets) {
                        var urls = Collections.list(loader.getResources(asset));
                        if (urls.size() != 1) throw new AssertionError(asset + " has " + urls.size() + " copies");
                        try (var actual = loader.getResourceAsStream(asset); var expected = zip.getInputStream(zip.getEntry(asset))) {
                            if (!Arrays.equals(actual.readAllBytes(), expected.readAllBytes())) throw new AssertionError(asset);
                        }
                    }
                    Class<?> timer = Class.forName("com.oliver.metrakron.overlay.BitmapCinzelTimer", true, loader);
                    var load = timer.getDeclaredMethod("load", String.class, Color.class, Color.class);
                    load.setAccessible(true);
                    for (String font : List.of("cinzel", "lato", "spacemono")) {
                        if (load.invoke(null, font, Color.WHITE, Color.BLACK) == null) throw new AssertionError(font);
                        try (var input = loader.getResourceAsStream("assets/metrakron/font/" + font + ".ttf")) {
                            Font.createFont(Font.TRUETYPE_FONT, input);
                        }
                    }
                }
                Files.delete(adapter);
                adapters++;
            }
            if (adapters != 12 || assets.size() < 27) throw new AssertionError("Incomplete bundle");
            System.out.println("Verified " + assets.size() + " shared assets, all fonts and bitmap timers across " + adapters + " isolated adapters.");
        } finally {
            Files.deleteIfExists(scratch.resolve("adapter.jar"));
            Files.delete(scratch);
        }
    }
}
