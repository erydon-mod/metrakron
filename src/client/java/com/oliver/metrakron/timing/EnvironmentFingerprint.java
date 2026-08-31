package com.oliver.metrakron.timing;

import com.oliver.metrakron.MetrakronClient;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.SharedConstants;
//? if >=26.1 {
/*import net.minecraft.client.Minecraft;
*///?} else {
import net.minecraft.client.MinecraftClient;
//?}

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

final class EnvironmentFingerprint {
    private static final int PROFILE_FORMAT = 2;

    private EnvironmentFingerprint() {
    }

    //? if >=26.1 {
    /*static ProfileFingerprint capture(Minecraft client) {
    *///?} else {
    static ProfileFingerprint capture(MinecraftClient client) {
    //?}
        FabricLoader loader = FabricLoader.getInstance();
        List<ModContainer> mods = loader.getAllMods().stream()
                .sorted(Comparator.comparing(mod -> mod.getMetadata().getId()))
                .toList();

        //? if >=26.1 {
        /*String minecraftVersion = SharedConstants.getCurrentVersion().name();
        *///?} else if >=1.21.6 {
        /*String minecraftVersion = SharedConstants.getGameVersion().name();
        *///?} else {
        String minecraftVersion = SharedConstants.getGameVersion().getName();
        //?}
        String loaderVersion = loader.getModContainer("fabricloader")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
        StringBuilder source = new StringBuilder(4096)
                .append("profileFormat=").append(PROFILE_FORMAT).append('\n')
                .append("minecraft=").append(minecraftVersion).append('\n')
                .append("java=").append(System.getProperty("java.version", "unknown")).append('\n')
                .append("vm=").append(System.getProperty("java.vm.name", "unknown")).append('\n')
                .append("os=").append(System.getProperty("os.name", "unknown")).append('|')
                .append(System.getProperty("os.arch", "unknown")).append('\n')
                .append("processors=").append(Runtime.getRuntime().availableProcessors()).append('\n')
                .append("maxMemory=").append(Runtime.getRuntime().maxMemory()).append('\n');

        for (ModContainer mod : mods) {
            // Metrakron updates must not make the game being measured look new.
            if (MetrakronClient.MOD_ID.equals(mod.getMetadata().getId())) {
                continue;
            }
            source.append("mod=")
                    .append(mod.getMetadata().getId())
                    .append('@')
                    .append(mod.getMetadata().getVersion().getFriendlyString())
                    .append('\n');
        }

        int resourcePackCount = 0;
        if (client != null && client.options != null) {
            resourcePackCount = client.options.resourcePacks.size();
            source.append("resourcePacks=").append(String.join("\u001f", client.options.resourcePacks)).append('\n')
                    .append("incompatiblePacks=")
                    .append(String.join("\u001f", client.options.incompatibleResourcePacks))
                    .append('\n')
                    //? if >=26.1 {
                    /*.append("viewDistance=").append(client.options.renderDistance().get()).append('\n')
                    .append("simulationDistance=").append(client.options.simulationDistance().get()).append('\n')
                    .append("mipmapLevels=").append(client.options.mipmapLevels().get()).append('\n')
                    .append("graphicsMode=").append(client.options.graphicsPreset().get()).append('\n')
                    .append("biomeBlend=").append(client.options.biomeBlendRadius().get()).append('\n');
                    *///?} else {
                    //? if <1.19 {
                    /*.append("viewDistance=").append(client.options.getViewDistance()).append('\n')
                    .append("simulationDistance=").append(client.options.simulationDistance).append('\n')
                    .append("mipmapLevels=").append(client.options.mipmapLevels).append('\n')
                    .append("graphicsMode=").append(client.options.graphicsMode).append('\n')
                    .append("biomeBlend=").append(client.options.biomeBlendRadius).append('\n');
                    *///?} else {
                    .append("viewDistance=").append(client.options.getViewDistance().getValue()).append('\n')
                    .append("simulationDistance=").append(client.options.getSimulationDistance().getValue()).append('\n')
                    .append("mipmapLevels=").append(client.options.getMipmapLevels().getValue()).append('\n')
                    //? if >=1.21.11 {
                    /*.append("graphicsMode=").append(client.options.getPreset().getValue()).append('\n')
                    *///?} else {
                    .append("graphicsMode=").append(client.options.getGraphicsMode().getValue()).append('\n')
                    //?}
                    .append("biomeBlend=").append(client.options.getBiomeBlendRadius().getValue()).append('\n');
                    //?}
                    //?}
        }

        String id = shortSha256(source.toString());
        long memoryGiB = Math.max(1L, Math.round(Runtime.getRuntime().maxMemory() / 1_073_741_824.0));
        String summary = String.format(
                Locale.ROOT,
                "Minecraft %s | Fabric Loader %s | %d mods | %d resource packs | Java %s | %d GiB",
                minecraftVersion,
                loaderVersion,
                mods.size(),
                resourcePackCount,
                System.getProperty("java.version", "unknown"),
                memoryGiB
        );
        return new ProfileFingerprint(id, summary);
    }

    static String shortSha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed, 0, 10);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }
}
