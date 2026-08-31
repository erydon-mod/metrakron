# ERYDON Metrakron

ERYDON Metrakron is a client-side Fabric mod that shows an honest,
self-measured loading clock. One universal JAR contains a small, tested adapter
for each supported Minecraft version; Fabric selects the one exact match and
rejects versions Metrakron has not tested.

Metrakron does not depend on the ERYDON mod.

Source and issue tracking are public at
[`erydon-mod/metrakron`](https://github.com/erydon-mod/metrakron).

## Install

Install `metrakron-fabric-universal-0.2.0.jar` and the matching Fabric API for
the game version. Do not install one of the internal adapter JARs separately.
Metrakron follows the minimum Loader version already required by that Fabric
API instead of forcing every older modpack onto the newest Loader.

The current universal download supports these exact releases:

| Java used by Minecraft | Minimum Fabric Loader | Supported Minecraft versions |
| --- | --- | --- |
| Java 17 | 0.16.10 | 1.18.2, 1.19.2, 1.19.4, 1.20.1, 1.20.4 |
| Java 21 | 0.16.10 | 1.20.6, 1.21.1, 1.21.4 |
| Java 21 | 0.16.13 | 1.21.8 |
| Java 21 | 0.17.3 | 1.21.11 |
| Java 25 | 0.18.4 | 26.1.2 |
| Java 25 | 0.19.3 | 26.2 |

Those are exact compatibility points, not an untested claim that every release
between 1.18.2 and 26.2 is interchangeable.

## What it measures

- Startup timing begins on the first rendered Mojang splash frame and ends on
  the first genuinely usable screen after the loading overlay has gone. This
  includes first-run accessibility screens and custom main-menu routes.
- Singleplayer timing begins when a world entry is played, remains visible
  through reading, generation, and joining screens, and ends only after three
  consecutive playable HUD frames.
- Elapsed time is displayed to whole-second precision.
- The clock uses Java's monotonic `System.nanoTime()` source. It never reads
  Minecraft's percentage, progress-bar value, elapsed time, estimated time, or
  time remaining.
- The smaller technical line describes the observed lifecycle stage and common
  work areas, such as registries, data packs, tags, recipes, chunks, terrain,
  lighting, player state, or the first render meshes. It is descriptive only;
  it is not treated as a progress source.

## Learning and countdowns

- A game instance or world with no completed history says `LEARNING` and counts
  elapsed time upwards.
- Later loads say `MAIN MENU IN` or `WORLD READY IN` and count down from
  Metrakron's own average of the latest three completed runs.
- If a load exceeds that average, the heading changes to
  `RELEARNING LOAD TIME` and continues upwards as overtime from `+00:00`.
- Startup history carries across modpack changes in the same game instance.
  World history carries across changes only for the same save folder.
- Cancelled and incomplete loads are never added to the average.

Earlier single measurements are migrated into the rolling history. Timing data
is local to the game instance at `config/metrakron/baselines.json`.

## Freeze-resistant desktop overlay

On Windows, macOS, and Linux/X11, a lightweight child Java process draws the
same upper-left panel and advances its own monotonic display clock even when
Minecraft's window thread stops responding. It starts and exits with Minecraft;
it is not a service, launcher hook, or pre-launch timer. On macOS it runs as a
background UI element so it does not request a Dock icon or menu focus.

The independent loading window includes an underlined `erydon.co.uk` footer.
Clicking it asks the desktop to open `https://erydon.co.uk` in the default
browser when one is available; the window itself remains non-focusable and
never opens the site without a deliberate click.

Linux/Wayland and desktops without dependable Java AWT positioning use the
in-game renderer instead. The measurement remains correct, but that fallback
cannot draw a new frame while Minecraft itself is frozen.

Runtime presentation files are kept under `config/metrakron/`: session-specific
state, helper status and helper logs, timing history, and a SHA-256-named cached
copy of the selected internal adapter. Session-specific names prevent concurrent
Minecraft clients from overwriting one another. They contain no Minecraft
progress estimate.

The compact interface uses a darkened nerium recessed centre, a burnished
bronze inlay with directional top-left highlights, and Cinzel for the heading,
timer, and activity line. A pre-baked Cinzel atlas makes the text available
before Minecraft's own fonts.

## Build and test

Open this folder as a Gradle project in IntelliJ IDEA. Gradle itself requires a
JDK 25 runtime for the newest adapter, plus Python 3 for the universal audit;
the build selects Java 17, 21, or 25 for each Minecraft target.

On Windows, use `gradlew.bat checkAdapters`; on macOS or Linux, use
`./gradlew checkAdapters`. This compiles and tests all advertised adapters
without packaging a JAR. Replace `checkAdapters` with `auditUniversalJar` to
build the universal download and audit its metadata, nested hashes, resources,
bytecode, and deterministic ZIP layout. Set `-Pmetrakron.python=<command>` if
Python 3 is not available as the platform's normal `python` or `python3`
command. The result is written to
`build/libs/metrakron-fabric-universal-0.2.0.jar`.

Disposable nested-bundle launch checks are available as
`:smoke1201:runClient`, `:smoke12111:runClient`, and `:smoke262:runClient`.
They use isolated folders under `build/smoke-runs` and do not alter normal
Minecraft or CurseForge instances. Each mirrors the full 12-candidate nested
layout while replacing only its selected adapter with a development-mapped copy
for the Loom client. These checks therefore exercise Loader selection as well
as startup, but complement rather than replace the strict audit of the
production-remapped universal JAR.

## Known compatibility edges

The tested path covers vanilla/Fabric startup, the first usable menu,
singleplayer world-list launches, vanilla's blocking world-data wait, all
intervening screen types, and the first playable world frames. A mod with a
completely custom world-launch route may need a small compatibility hook.
