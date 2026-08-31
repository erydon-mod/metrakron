package com.oliver.metrakron.overlay;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DesktopOverlayPlatformTest {
    @Test
    void supportsWindowsWithoutDockSuppression() {
        DesktopOverlayPlatform.Capability capability = detect("Windows 11", null, null, null, null, null);

        assertTrue(capability.supported());
        assertEquals(DesktopOverlayPlatform.Kind.WINDOWS, capability.kind());
        assertFalse(capability.suppressDockIcon());
    }

    @Test
    void supportsMacAndRequestsDockSuppression() {
        DesktopOverlayPlatform.Capability capability = detect("Mac OS X", null, null, null, null, null);

        assertTrue(capability.supported());
        assertEquals(DesktopOverlayPlatform.Kind.MACOS, capability.kind());
        assertTrue(capability.suppressDockIcon());
    }

    @Test
    void supportsLinuxWithAnX11Display() {
        DesktopOverlayPlatform.Capability capability = detect("Linux", null, null, "x11", null, ":0");

        assertTrue(capability.supported());
        assertEquals("Linux/X11", capability.displayName());
    }

    @Test
    void fallsBackOnWaylandEvenWhenXWaylandDisplayExists() {
        DesktopOverlayPlatform.Capability capability = detect(
                "Linux",
                null,
                null,
                "wayland",
                "wayland-0",
                ":0"
        );

        assertFalse(capability.supported());
        assertTrue(capability.reason().contains("Wayland"));
    }

    @Test
    void fallsBackWhenLinuxHasNoX11Display() {
        DesktopOverlayPlatform.Capability capability = detect("Linux", null, null, null, null, null);

        assertFalse(capability.supported());
        assertTrue(capability.reason().contains("X11"));
    }

    @Test
    void honoursTheCrossPlatformDisableProperty() {
        DesktopOverlayPlatform.Capability capability = detect("Mac OS X", "false", null, null, null, null);

        assertFalse(capability.supported());
        assertTrue(capability.reason().contains("metrakron.desktopOverlay"));
    }

    @Test
    void retainsTheLegacyWindowsDisableProperty() {
        DesktopOverlayPlatform.Capability windows = detect("Windows 11", null, "false", null, null, null);
        DesktopOverlayPlatform.Capability mac = detect("Mac OS X", null, "false", null, null, null);

        assertFalse(windows.supported());
        assertTrue(mac.supported());
    }

    @Test
    void fallsBackOnUnknownOperatingSystems() {
        DesktopOverlayPlatform.Capability capability = detect("Plan 9", null, null, null, null, null);

        assertFalse(capability.supported());
        assertEquals(DesktopOverlayPlatform.Kind.OTHER, capability.kind());
    }

    private static DesktopOverlayPlatform.Capability detect(
            String osName,
            String desktopSetting,
            String windowsSetting,
            String sessionType,
            String waylandDisplay,
            String x11Display
    ) {
        return DesktopOverlayPlatform.detect(
                osName,
                desktopSetting,
                windowsSetting,
                sessionType,
                waylandDisplay,
                x11Display
        );
    }
}
