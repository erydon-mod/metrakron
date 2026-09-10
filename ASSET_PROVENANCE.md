# Asset provenance

Appearance choices additionally bundle unchanged ERYDON `textures/block/aganite_block.png`,
`glacium_block.png`, `kelastrion_block.png`, and `selenephos_block.png` under
CC BY-SA 4.0. Aganite and Glacium use `textures/gui/<stone>_panel.png`; Kelastrion
and Selenephos use `textures/gui/<stone>_quiet_panel.png`. They are self-contained;
the ERYDON mod is not required. Original Nerium remains the default, using its
original high-resolution panel crop. All stones retain their colours without a
tint or wash; an opposing text drop shadow provides separation from veins.

Lato Regular is an unmodified font from `google/fonts`, `ofl/lato/Lato-Regular.ttf`,
under SIL OFL 1.1 (see `LICENSES/Lato-OFL.txt`). Space Mono Regular is unmodified
from `google/fonts`, `ofl/spacemono/SpaceMono-Regular.ttf`, also under SIL OFL 1.1
(see `LICENSES/SpaceMono-OFL.txt`). All three fonts' generated atlases use equal
digit advances for stable timers. Regenerate with
`python tools/generate_cinzel_atlas.py cinzel` and
`python tools/generate_cinzel_atlas.py lato`, and
`python tools/generate_cinzel_atlas.py spacemono`.

The interface deliberately reuses three assets from the public
ERYDON source owned by Oliver:

- `textures/gui/nerium_panel.png` is a pixel-identical copy of ERYDON's
  `textures/gui/nerium_control_background.png`; embedded Adobe editing history
  was removed before public distribution.
- `textures/gui/bronze_matte.png` derives from ERYDON's
  `textures/block/cover_bronze_matte.png`.
- `font/cinzel.ttf` is the unmodified Cinzel Regular 2.000 font already used by
  ERYDON. It is separately licensed under OFL-1.1.

The generated `textures/gui/cinzel_atlas.png` is a deterministic rasterization
of that TTF for the early splash screen. The generated high-resolution
`textures/gui/cinzel_timer_atlas.png` contains only timer characters and avoids
platform TrueType overlap differences in the independent desktop renderer. The
ERYDON-owned nerium and bronze textures remain CC BY-SA 4.0. Cinzel's exact
third-party notice is in `THIRD_PARTY_NOTICES.md`.
