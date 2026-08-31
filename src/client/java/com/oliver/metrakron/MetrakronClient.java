package com.oliver.metrakron;

import com.oliver.metrakron.overlay.DesktopOverlayBridge;
import com.oliver.metrakron.timing.LoadActivity;
import com.oliver.metrakron.timing.LoadSnapshot;
import com.oliver.metrakron.timing.LoadStage;
import com.oliver.metrakron.timing.TimingController;
import com.oliver.metrakron.ui.MetrakronOverlayRenderer;
import net.fabricmc.api.ClientModInitializer;
//? if >=26.1 {
/*import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.resources.Identifier;
*///?} else {
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
//? if <1.20 {
/*import com.oliver.metrakron.ui.LegacyDrawContext;
import net.minecraft.client.util.math.MatrixStack;
*///?} else {
import net.minecraft.client.gui.DrawContext;
//?}
//? if <1.21.6
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
//? if >=1.20.2 {
/*import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
*///?} else {
import net.minecraft.client.gui.screen.LevelLoadingScreen;
//?}
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
//?}
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public final class MetrakronClient implements ClientModInitializer {
    public static final String MOD_ID = "metrakron";
    public static final Logger LOGGER = LoggerFactory.getLogger("ERYDON Metrakron");

    private static volatile TimingController timing;

    @Override
    public void onInitializeClient() {
        timing();
        DesktopOverlayBridge.initialize(
                FabricLoader.getInstance().getConfigDir().resolve(MOD_ID)
        );
        //? if >=26.1 {
        /*Minecraft client = Minecraft.getInstance();
        ScreenEvents.AFTER_INIT.register((ignoredClient, screen, scaledWidth, scaledHeight) ->
                ScreenEvents.afterExtract(screen).register(MetrakronClient::renderScreenTimer)
        );
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(MOD_ID, "world_load_timer"),
                (graphics, deltaTracker) -> renderWorldTimer(client, graphics)
        );
        *///?} else {
        MinecraftClient client = MinecraftClient.getInstance();
        ScreenEvents.AFTER_INIT.register((ignoredClient, screen, scaledWidth, scaledHeight) ->
                ScreenEvents.afterRender(screen).register(MetrakronClient::renderScreenTimer)
        );
        HudRenderCallback.EVENT.register((context, tickDelta) -> renderWorldTimer(client, context));
        //?}
        LOGGER.info("ERYDON Metrakron loading timer initialized");
    }

    //? if >=26.1 {
    /*private static void renderScreenTimer(
            Screen screen,
            GuiGraphicsExtractor context,
            int mouseX,
            int mouseY,
            float tickDelta
    ) {
    *///?} else {
    private static void renderScreenTimer(
            Screen screen,
            //? if <1.20 {
            /*MatrixStack context,
            *///?} else {
            DrawContext context,
            //?}
            int mouseX,
            int mouseY,
            float tickDelta
    ) {
    //?}
        TimingController controller = timing();

        if (screen instanceof TitleScreen) {
            finishAtTitleScreen();
            return;
        }

        LoadSnapshot startup = controller.snapshot(LoadStage.STARTUP);
        if (startup.active()) {
            drawAndPublish(context, startup);
            return;
        }

        //? if >=26.1 {
        /*Minecraft client = Minecraft.getInstance();
        *///?} else {
        MinecraftClient client = MinecraftClient.getInstance();
        //?}
        LoadStage stage = stageForScreen(client, screen);
        controller.noteBlockingScreen(activityForStage(stage));
        drawAndPublish(context, controller.snapshot(stage));
    }

    //? if >=26.1 {
    /*private static void renderWorldTimer(Minecraft client, GuiGraphicsExtractor context) {
    *///?} else if >=1.20 {
    private static void renderWorldTimer(MinecraftClient client, DrawContext context) {
    //?} else {
    /*private static void renderWorldTimer(MinecraftClient client, MatrixStack context) {
    *///?}
        TimingController controller = timing();
        controller.noteWorldActivity(LoadActivity.WORLD_FIRST_FRAME);
        drawAndPublish(context, controller.snapshot(LoadStage.WORLD_FIRST_FRAME));
        if (controller.observeHudFrame(client)) {
            DesktopOverlayBridge.hide();
        }
    }

    //? if >=26.1 {
    /*public static void renderStartupTimer(GuiGraphicsExtractor context) {
    *///?} else if >=1.20 {
    public static void renderStartupTimer(DrawContext context) {
    //?} else {
    /*public static void renderStartupTimer(MatrixStack context) {
    *///?}
        TimingController controller = timing();
        //? if >=26.1 {
        /*controller.ensureStartupStarted(Minecraft.getInstance());
        *///?} else {
        controller.ensureStartupStarted(MinecraftClient.getInstance());
        //?}
        drawAndPublish(context, controller.snapshot(LoadStage.STARTUP));
    }

    public static void startWorldClock(String folderName, String displayName) {
        TimingController controller = timing();
        //? if >=26.1 {
        /*controller.startWorld(Minecraft.getInstance(), folderName, displayName);
        DesktopOverlayBridge.publish(
                controller.snapshot(LoadStage.WORLD_READING),
                Minecraft.getInstance().getWindow()
        );
        *///?} else {
        controller.startWorld(MinecraftClient.getInstance(), folderName, displayName);
        DesktopOverlayBridge.publish(
                controller.snapshot(LoadStage.WORLD_READING),
                MinecraftClient.getInstance().getWindow()
        );
        //?}
    }

    public static void finishAtTitleScreen() {
        timing().onTitleScreenRendered();
        DesktopOverlayBridge.hide();
    }

    public static void finishStartupIfReady() {
        //? if >=26.2 {
        /*if (Minecraft.getInstance().gui.overlay() != null) {
            return;
        }
        *///?} else if >=26.1 {
        /*if (Minecraft.getInstance().getOverlay() != null) {
            return;
        }
        *///?} else {
        if (MinecraftClient.getInstance().getOverlay() != null) {
            return;
        }
        //?}
        if (timing().snapshot(LoadStage.STARTUP).active()) {
            finishAtTitleScreen();
        }
    }

    //? if >=26.1 {
    /*private static void drawAndPublish(GuiGraphicsExtractor context, LoadSnapshot snapshot) {
        boolean desktopOverlayOwnsPresentation = DesktopOverlayBridge.publish(
                snapshot,
                Minecraft.getInstance().getWindow()
        );
    *///?} else if >=1.20 {
    private static void drawAndPublish(DrawContext context, LoadSnapshot snapshot) {
        boolean desktopOverlayOwnsPresentation = DesktopOverlayBridge.publish(
                snapshot,
                MinecraftClient.getInstance().getWindow()
        );
    //?} else {
    /*private static void drawAndPublish(MatrixStack context, LoadSnapshot snapshot) {
        boolean desktopOverlayOwnsPresentation = DesktopOverlayBridge.publish(
                snapshot,
                MinecraftClient.getInstance().getWindow()
        );
    *///?}
        if (desktopOverlayOwnsPresentation) {
            return;
        }
        //? if <1.20 {
        /*MetrakronOverlayRenderer.draw(new LegacyDrawContext(context), snapshot);
        *///?} else {
        MetrakronOverlayRenderer.draw(context, snapshot);
        //?}
    }

    //? if >=26.1 {
    /*private static LoadStage stageForScreen(Minecraft client, Screen screen) {
    *///?} else {
    private static LoadStage stageForScreen(MinecraftClient client, Screen screen) {
    //?}
        if (screen instanceof LevelLoadingScreen) {
            return LoadStage.WORLD_GENERATION;
        }
        //? if >=26.1 {
        /*if (client.level != null || client.player != null) {
        *///?} else {
        if (
                //? if <1.21.6
                screen instanceof DownloadingTerrainScreen ||
                client.world != null || client.player != null
        ) {
        //?}
            return LoadStage.WORLD_JOINING;
        }
        return LoadStage.WORLD_READING;
    }

    private static LoadActivity activityForStage(LoadStage stage) {
        return switch (stage) {
            case STARTUP -> LoadActivity.STARTUP_RESOURCES;
            case WORLD_READING -> LoadActivity.WORLD_DATA;
            case WORLD_GENERATION -> LoadActivity.WORLD_GENERATION;
            case WORLD_JOINING -> LoadActivity.WORLD_JOINING;
            case WORLD_FIRST_FRAME -> LoadActivity.WORLD_FIRST_FRAME;
        };
    }

    public static TimingController timing() {
        TimingController current = timing;
        if (current != null) {
            return current;
        }

        synchronized (MetrakronClient.class) {
            if (timing == null) {
                Path baselineFile = FabricLoader.getInstance()
                        .getConfigDir()
                        .resolve(MOD_ID)
                        .resolve("baselines.json");
                timing = new TimingController(baselineFile, System::nanoTime);
            }
            return timing;
        }
    }
}
