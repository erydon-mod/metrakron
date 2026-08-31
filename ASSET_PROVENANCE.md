# Asset provenance

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
