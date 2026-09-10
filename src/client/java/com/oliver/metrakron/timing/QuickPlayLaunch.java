package com.oliver.metrakron.timing;

/** Only the singleplayer Quick Play argument is retained, never the full launch command. */
public record QuickPlayLaunch(boolean requested, String folderName) {
    public static QuickPlayLaunch none() {
        return new QuickPlayLaunch(false, null);
    }

    public static QuickPlayLaunch fromArguments(String[] arguments) {
        String option = "--quickPlaySingleplayer";
        for (int index = 0; index < arguments.length; index++) {
            String argument = arguments[index];
            if (argument.equals("--")) {
                break;
            }
            if (argument.equals(option)) {
                String folder = index + 1 < arguments.length
                        && !arguments[index + 1].startsWith("--")
                        ? arguments[index + 1] : null;
                return new QuickPlayLaunch(true, nonBlank(folder));
            }
            if (argument.startsWith(option + '=')) {
                return new QuickPlayLaunch(true, nonBlank(argument.substring(option.length() + 1)));
            }
        }
        return none();
    }

    private static String nonBlank(String folder) {
        // Newer Minecraft versions accept the bare flag and resolve the latest save later.
        // Keep actual folder names, including spaces and non-ASCII characters, unchanged.
        return folder == null || folder.isBlank() ? null : folder;
    }
}
