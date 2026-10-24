# Asset provenance

The Portorium logo and emblem use Oliver's photograph of real Portoro marble,
upscaled with GPT, then mapped onto the approved Blender plaques. The metal,
Cinzel SemiBold lettering and reflection loop were rendered from geometry with
Codex assistance. This is an upscale of an owned photograph, not a newly
generated stone texture (confirmed by Oliver on 13 September 2026). The PNG
exports retain the approved texture crops; see `BRANDING.md` for placement.
The runtime logo animation uses 512x152-pixel frames downsampled directly from
the lossless 2060x614 marketing PNG sequence. `tools/import_logo_animation.py`
preserves transparency and samples the 24-fps master at the existing 20-fps
runtime cadence: 174 frames, 8.7 seconds. The intermediate atlas is 4096x3344;
it is not an enlargement of the former 256x76 runtime frames.
Cinzel lettering is a rendered image, under the same OFL font attribution as
the existing Cinzel assets. Oliver-owned branding remains CC BY-SA 4.0.

The compact menu emblem is an exact copy of the approved 64x2560 integration
sheet, containing 40 transparent 64x64 frames at 20 fps. The custom GUI selects
each frame and uses smooth filtering; its texture metadata deliberately has no
second animation controller. The README banner is an exact copy of the approved
480x143 animated full-logo GIF, kept in documentation rather than runtime
resources. Both exports share the existing branding provenance and licences.

The Portorium control background and replacement `portorium_panel.png` use the
approved owned Portoro photograph, resampled to 1024x1024. They are an exception
to the unresized gallery crops below and must be retained when regenerating those
crops. The transparent `portorium_control_frame.png` is the authored ERYDON
control chamfer and recessed polished bronze inlay; the silver variant applies
the shared silver palette while preserving its alpha and geometry. Both loading
renderers and the appearance panel reuse this frame. These assets remain
CC BY-SA 4.0.

Appearance choices include all 27 ERYDON gallery stones, using centre crops of
Oliver's full-size stone masters under CC BY-SA 4.0. The complete source-to-output
mapping is recorded in `tools/import_stone_panels.ps1`. Most panels are 960x384,
except Striatus (676x270), whose supplied master is 676x676.
These gallery crops preserve exact source pixels, without resizing, tinting, or embedded
editing metadata. Regenerate with `tools/import_stone_panels.ps1 -MasterDirectory
<full-size-master-folder>` on Windows. Most stones use
`textures/gui/<stone>_panel.png`; Kelastrion
and Selenephos use `textures/gui/<stone>_quiet_panel.png`. They are self-contained;
the ERYDON mod is not required. Pale Glacium is the default; saved choices are
preserved. Original Nerium retains its original high-resolution panel crop. All stones retain their colours without a
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
