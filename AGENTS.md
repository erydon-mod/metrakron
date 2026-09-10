# ERYDON Metrakron project instructions

## Scope

- This is the authoritative standalone ERYDON Metrakron source root.
- Mod id: `metrakron`; outer bundle id: `metrakron_bundle`; package:
  `com.oliver.metrakron`.
- Target: client-side Fabric with exact adapters for Minecraft 1.18.2, 1.19.2,
  1.19.4, 1.20.1, 1.20.4, 1.20.6, 1.21.1, 1.21.4, 1.21.8, 1.21.11, 26.1.2,
  and 26.2. Do not advertise or broaden a target until its adapter compiles,
  tests, and passes the universal audit.
- Adapters use Java 17 through 1.20.4, Java 21 from 1.20.6 through 1.21.11,
  and Java 25 for 26.1.2 and 26.2.
- Fabric Loader minima follow the selected Fabric API: 0.16.10 through 1.21.4,
  0.16.13 for 1.21.8, 0.17.3 for 1.21.11, 0.18.4 for 26.1.2, and the
  officially recommended 0.19.3 for 26.2. The classless outer bundle must
  retain the lowest common minimum, 0.16.10.
- The published artifact is one classless deterministic outer JAR containing
  exact, pairwise-disjoint nested adapters. Never merge their class trees.
- Metrakron must not depend on the ERYDON mod. Its selected ERYDON visual assets
  are local, attributed copies with their own licence records.
- Never alter the protected CurseForge instance while developing or testing
  this project without a new explicit request.

## Timing contract

- Use a monotonic clock owned by Metrakron.
- Never read Minecraft's percentage, progress-bar position, elapsed time,
  estimated time, or time-remaining values.
- Loading lifecycle stages may be observed only to keep the overlay visible and
  decide when Metrakron's own measurement ends.
- An instance or save with no prior measurement shows a learning count-up.
  Later loads count down from the average of up to the latest three completed
  measurements. Once a learned countdown is exceeded, show relearning with live
  overtime.
- Retain only the latest three completed measurements for each key. Never add a
  cancelled or incomplete load to the history.
- On Windows, macOS, and Linux/X11, presentation may use a per-launch child
  process so the visible timer continues when Minecraft's window thread is
  unresponsive. Linux/Wayland and desktops without the required Java AWT window
  support must fall back honestly to the in-game panel. The helper must remain
  presentation-only, use Metrakron's own monotonic clock, exit with Minecraft,
  and never become a service, launcher hook, or pre-launch timer.
- Keep recording the exact mod/resource/settings fingerprint for diagnostics,
  but carry the latest startup runs across profile changes in the same game
  instance. Carry world runs across profile changes only for the same save.

## Working style

- Keep the interface simple, readable, and ERYDON-like: compact upper-left
  placement, original stone colours without a tint or colour wash, and
  dimensional bronze or silver trim. Use the selected font consistently,
  with distinct classical, sans-serif, and monospaced choices and tabular timer digits.
  Keep original Nerium as the default; do not exclude veined stones. Select light
  or dark text automatically and use a contrasting drop shadow for legibility.
- Keep `en_us`, `de_de`, and `es_es` player text aligned.
- Keep the exact Minecraft, Java, Fabric Loader, and Fabric API matrix
  synchronized across `settings.gradle`, `stonecutter.properties.toml`,
  `stonecutter.gradle`, and `tools/audit_universal.py`.
- Use `gradlew.bat checkAdapters` for routine validation; it must not package
  release or test JARs.
- Do not run `universalJar`, `auditUniversalJar`, adapter packaging tasks, or
  create a test JAR unless Oliver explicitly asks for a JAR, release, or
  universal-package deliverable.
- When packaging is authorised, run `auditUniversalJar`, build twice from the
  same inputs, and prove identical SHA-256 hashes before handoff.
- Use the isolated smoke clients for representative runtime checks. They must
  preserve the full production candidate list, replace only the selected
  adapter with its development-mapped equivalent, and run at that adapter's
  declared Loader floor. They do not replace the production-JAR audit. Never
  use or alter a normal CurseForge instance unless Oliver explicitly requests
  that exact instance.
- Keep build output, run folders, logs, saves, caches, and IntelliJ files out of
  Git.
