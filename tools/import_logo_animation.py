"""Build the runtime animation atlas from the lossless marketing frame sequence.

Requires Pillow. This resamples existing artwork; it does not generate new frames.
Source: metrakron-logo's frame-0001.png through frame-0209.png, at 24 fps.
"""

import argparse
import hashlib
import json
from pathlib import Path

from PIL import Image

WIDTH, HEIGHT, COLUMNS, FRAMES = 512, 152, 8, 174
SOURCE_FPS, OUTPUT_FPS = 24, 20


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--frames-dir", required=True, type=Path)
    parser.add_argument("--output", type=Path, default=Path(__file__).resolve().parents[1]
                        / "src/client/resources/assets/metrakron/textures/gui/portorium_logo.png")
    args = parser.parse_args()
    sources = sorted(args.frames_dir.glob("frame-*.png"))
    if len(sources) != 209:
        parser.error(f"Expected 209 lossless marketing frames, found {len(sources)}")
    for index, source in enumerate(sources, 1):
        if source.name != f"frame-{index:04d}.png":
            parser.error("Source frames must form a continuous sequence starting at frame-0001.png")

    rows = (FRAMES + COLUMNS - 1) // COLUMNS
    atlas = Image.new("RGBA", (COLUMNS * WIDTH, rows * HEIGHT))
    for frame in range(FRAMES):
        # Nearest source frame at each existing 20-fps presentation timestamp.
        source_index = (frame * SOURCE_FPS * 2 + OUTPUT_FPS) // (OUTPUT_FPS * 2)
        with Image.open(sources[source_index]) as source:
            if source.size != (2060, 614) or source.mode != "RGBA":
                raise ValueError(f"Unexpected source format: {sources[source_index].name}")
            # Premultiplied alpha prevents a dark fringe at the transparent edge.
            resized = source.convert("RGBa").resize((WIDTH, HEIGHT), Image.Resampling.LANCZOS).convert("RGBA")
        atlas.paste(resized, (frame % COLUMNS * WIDTH, frame // COLUMNS * HEIGHT))
    args.output.parent.mkdir(parents=True, exist_ok=True)
    atlas.save(args.output, optimize=True)
    print(json.dumps({"frames": FRAMES, "fps": OUTPUT_FPS, "frame_size": [WIDTH, HEIGHT],
                      "atlas_size": list(atlas.size), "bytes": args.output.stat().st_size,
                      "sha256": hashlib.sha256(args.output.read_bytes()).hexdigest()}, indent=2))


if __name__ == "__main__":
    main()
