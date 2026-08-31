package com.oliver.metrakron.overlay;

import java.util.Locale;
import java.util.Map;

final class DesktopOverlayPlatform {
    private static final String ENABLE_PROPERTY = "metrakron.desktopOverlay";
    private static final String LEGACY_WINDOWS_PROPERTY = "metrakron.windowsOverlay";

    private DesktopOverlayPlatform() {
    }

    static Capability current() {
        Map<String, String> environment = System.getenv();
        return detect(
                System.getProperty("os.name", ""),
                System.getProperty(ENABLE_PROPERTY),
                System.getProperty(LEGACY_WINDOWS_PROPERTY),
                environment.get("XDG_SESSION_TYPE"),
                environment.get("WAYLAND_DISPLAY"),
                environment.get("DISPLAY")
        );
    }

    static Capability detect(
            String osName,
            String desktopOverlaySetting,
            String legacyWindowsOverlaySetting,
            String sessionType,
            String waylandDisplay,
            String x11Display
    ) {
        Kind kind = operatingSystem(osName);
        if (isFalse(desktopOverlaySetting)) {
            return Capability.unsupported(kind, "disabled by metrakron.desktopOverlay=false");
        }
        if (kind == Kind.WINDOWS && isFalse(legacyWindowsOverlaySetting)) {
            return Capability.unsupported(kind, "disabled by metrakron.windowsOverlay=false");
        }

        return switch (kind) {
            case WINDOWS -> Capability.supported(kind, "Windows", false);
            case MACOS -> Capability.supported(kind, "macOS", true);
            case LINUX -> linuxCapability(sessionType, waylandDisplay, x11Display);
            case OTHER -> Capability.unsupported(kind, "this operating system is not supported");
        };
    }

    private static Capability linuxCapability(
            String sessionType,
            String waylandDisplay,
            String x11Display
    ) {
        if (equalsIgnoreCase(sessionType, "wayland") || !isBlank(waylandDisplay)) {
            return Capability.unsupported(
                    Kind.LINUX,
                    "Wayland does not guarantee independent overlay positioning"
            );
        }
        if (isBlank(x11Display)) {
            return Capability.unsupported(Kind.LINUX, "no X11 display is available");
        }
        return Capability.supported(Kind.LINUX, "Linux/X11", false);
    }

    private static Kind operatingSystem(String osName) {
        String normalized = osName == null ? "" : osName.toLowerCase(Locale.ROOT).trim();
        if (normalized.startsWith("windows")) {
            return Kind.WINDOWS;
        }
        if (normalized.startsWith("mac") || normalized.startsWith("darwin")) {
            return Kind.MACOS;
        }
        if (normalized.startsWith("linux")) {
            return Kind.LINUX;
        }
        return Kind.OTHER;
    }

    private static boolean isFalse(String value) {
        return equalsIgnoreCase(value, "false");
    }

    private static boolean equalsIgnoreCase(String left, String right) {
        return left != null && left.trim().equalsIgnoreCase(right);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    enum Kind {
        WINDOWS,
        MACOS,
        LINUX,
        OTHER
    }

    record Capability(
            Kind kind,
            boolean supported,
            String displayName,
            boolean suppressDockIcon,
            String reason
    ) {
        private static Capability supported(Kind kind, String displayName, boolean suppressDockIcon) {
            return new Capability(kind, true, displayName, suppressDockIcon, "available");
        }

        private static Capability unsupported(Kind kind, String reason) {
            return new Capability(kind, false, "in-game fallback", false, reason);
        }
    }
}
