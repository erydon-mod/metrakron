# Portorium branding

The polished stone plaques, metal inlays and Cinzel lettering form the shared
ERYDON family branding. Wide spaces use the full logo; compact square slots use
the emblem. Use the animated version wherever the surface supports it, sized for
the space and display scale. Static-only external slots, including Mod Menu
icons, use a static emblem. Keep full-resolution masters outside runtime
resources; ship purpose-sized exports. Collection 32x and 64x retain separate
resolution lettering.

## Written names

Use **ERYDON**, **ERYDON Themelios**, **ERYDON Daedalon** and
**ERYDON Metrakron** for official mod names, listings and full titles.
Themelios, Daedalon and Metrakron may be shortened in descriptive prose.
Always write ERYDON in capitals. Pack names are **ERYDON Collection 32x**
and **ERYDON Collection 64x**.

The approved plaque artwork retains its engraved capital-letter typography.
Written names follow the convention above. Technical mod IDs, namespaces,
commands, URLs and artifact filenames retain their existing spelling and case.

Oliver owns the source photograph of real Portoro marble. GPT was used to
upscale that photograph; it was not generated as a new stone texture. Oliver
confirmed this provenance on 13 September 2026. The approved plaque geometry,
lettering and lighting were assembled and rendered in Blender with Codex
assistance. Exported images retain the approved appearance and texture crops.

Lettering uses Cinzel SemiBold by the Cinzel Project Authors (Natanael Gama),
under the SIL Open Font License 1.1: https://github.com/NDISCOVER/Cinzel.
These branding exports contain rendered lettering, not an embedded font file.
Existing repository asset licences and third-party notices continue to apply.

Creative-tab animation uses transparent 64-pixel square PNG frames and one
Minecraft tick per frame, without interpolation. GUI logos use a filtered
20-fps sheet drawn only while the relevant menu is visible. Mod Menu icons
and resource-pack selection icons use static square PNGs.

Metrakron uses the approved 174-frame, 20-fps reflection loop (8.7 seconds)
in both loading displays and the full appearance-screen header. Each runtime
frame is 512x152 pixels, downsampled from the lossless marketing master, with
the same on-screen size as before. The 4096x3344 atlas is shared by both renderers.
The compact appearance-screen header uses the approved 64x64 animated emblem:
40 frames at 20 fps, preserving its two-second loop, drawn in the existing
14x14 square with smooth filtering only while that menu is visible. The
512-pixel static emblem remains for static-only external icon slots.
The README banner is the approved 480x143 animated full-logo GIF in
`docs/branding/`; documentation assets are not included in runtime resources.
The independent desktop helper starts with a 16 MiB heap and allows growth up
to 128 MiB to accommodate the approximately 53 MiB decoded animation.
The default loading-panel stone is pale Glacium, giving the Portorium plaque clear separation.
Portorium controls and the outside panel border share one authored bevel and
recessed polished metal frame, with bronze and silver options. Existing saved
appearance choices are preserved. Marketing renderer captures use sample timer
readings and must not be described as live Minecraft screenshots.

## Branding validation

Use the approved animated full logo for wide README and custom GUI headers,
and the animated emblem for compact custom GUI slots wherever animation is
supported. Keep static square emblems in standard Fabric, Mod Menu and resource-
pack selection slots. Preserve aspect ratio and the approved playback timing.
Use purpose-sized 8-bit runtime exports; keep full-resolution artwork masters
outside runtime resource directories. Documentation GIFs stay in `docs/branding`.

Run `python -B tools/audit_branding.py` for the portable source audit. Its
`config/branding-assets.json` records approved hashes, dimensions, frame counts,
timing and consumers. Update that contract only when deliberately replacing an
approved export; never resize an entire animation atlas as if it were one frame.
The matching `tools/test_branding.py` checks rejection of damaged, mistimed,
unreferenced or oversized branding assets. Gradle source/resource checks run
these gates automatically; resource-pack CI runs the same source audit.
