package com.oliver.metrakron.timing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuickPlayLaunchTest {
    @Test
    void detectsTheNativeWorldArgumentUsedByCurseForgeAndModrinth() {
        assertEquals(new QuickPlayLaunch(true, "Generated World"), QuickPlayLaunch.fromArguments(
                new String[]{"--version", "1.20.1", "--quickPlaySingleplayer", "Generated World"}
        ));
        assertEquals(new QuickPlayLaunch(true, "Erydanis"), QuickPlayLaunch.fromArguments(
                new String[]{"--quickPlaySingleplayer", "Erydanis", "--width", "1280"}
        ));
    }

    @Test
    void acceptsInlineValuesAndPreservesExactSaveFolderNames() {
        String folder = "  Érydon 世界  ";
        assertEquals(new QuickPlayLaunch(true, folder), QuickPlayLaunch.fromArguments(
                new String[]{"--quickPlaySingleplayer=" + folder}
        ));
        assertEquals(new QuickPlayLaunch(true, folder), QuickPlayLaunch.fromArguments(
                new String[]{"--quickPlaySingleplayer", folder}
        ));
    }

    @Test
    void leavesBareLatestWorldRequestsForTheWorldOpenHookToResolve() {
        for (String[] arguments : new String[][]{
                {"--quickPlaySingleplayer"},
                {"--quickPlaySingleplayer="},
                {"--quickPlaySingleplayer", ""},
                {"--quickPlaySingleplayer", "--width", "1280"}
        }) {
            assertEquals(new QuickPlayLaunch(true, null), QuickPlayLaunch.fromArguments(arguments));
        }
    }

    @Test
    void doesNotMistakeLoggingMultiplayerRealmsOrNormalLaunchesForWorldShortcuts() {
        for (String[] arguments : new String[][]{
                {},
                {"--version", "1.20.1"},
                {"--quickPlayPath", "quickPlay/log.json"},
                {"--quickPlayMultiplayer", "example.invalid"},
                {"--quickPlayRealms", "1234"},
                {"--server", "example.invalid", "--port", "25565"},
                {"--quickPlaySingleplayerOther", "World"},
                {"--", "--quickPlaySingleplayer", "World"}
        }) {
            assertEquals(QuickPlayLaunch.none(), QuickPlayLaunch.fromArguments(arguments));
        }
    }
}
