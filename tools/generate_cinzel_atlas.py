#!/usr/bin/env python3
"""Generate Metrakron's early-loading Cinzel atlas from the licensed TTF."""

from __future__ import annotations

import hashlib
import sys
import json
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


PROJECT_ROOT = Path(__file__).resolve().parents[1]
FONT_PATH = PROJECT_ROOT / "src/client/resources/assets/metrakron/font/cinzel.ttf"
OUTPUT_DIRECTORY = PROJECT_ROOT / "src/client/resources/assets/metrakron/textures/gui"
PNG_PATH = OUTPUT_DIRECTORY / "cinzel_atlas.png"
JSON_PATH = OUTPUT_DIRECTORY / "cinzel_atlas.json"
TIMER_PNG_PATH = OUTPUT_DIRECTORY / "cinzel_timer_atlas.png"
TIMER_PROPERTIES_PATH = OUTPUT_DIRECTORY / "cinzel_timer_atlas.properties"

FONT_SIZE = 40
TEXTURE_WIDTH = 1024
PADDING = 2
TIMER_FONT_SIZE = 160
TIMER_TEXTURE_WIDTH = 1024
TIMER_PADDING = 4
TIMER_CHARACTERS = "+:0123456789"


def next_power_of_two(value: int) -> int:
    result = 1
    while result < value:
        result *= 2
    return result


def generate_timer_atlas() -> None:
    """Bake large timer glyphs through FreeType to preserve Cinzel overlaps."""
    font = ImageFont.truetype(str(FONT_PATH), TIMER_FONT_SIZE)
    ascent, descent = font.getmetrics()
    prepared: list[dict[str, object]] = []

    for character in TIMER_CHARACTERS:
        left, top, right, bottom = font.getbbox(character, anchor="ls")
        width = max(0, right - left)
        height = max(0, bottom - top)
        glyph_image = Image.new(
            "RGBA",
            (width + TIMER_PADDING * 2, height + TIMER_PADDING * 2),
            (255, 255, 255, 0),
        )
        draw = ImageDraw.Draw(glyph_image)
        draw.text(
            (TIMER_PADDING - left, TIMER_PADDING - top),
            character,
            font=font,
            fill=(255, 255, 255, 255),
            anchor="ls",
        )
        prepared.append(
            {
                "codepoint": ord(character),
                "image": glyph_image,
                "width": glyph_image.width,
                "height": glyph_image.height,
                "offsetX": left - TIMER_PADDING,
                "offsetY": top - TIMER_PADDING,
                "advance": round(float(font.getlength(character)), 3),
            }
        )

    make_digits_tabular(prepared)

    x = TIMER_PADDING
    y = TIMER_PADDING
    row_height = 0
    for glyph in prepared:
        width = int(glyph["width"])
        height = int(glyph["height"])
        if x + width + TIMER_PADDING > TIMER_TEXTURE_WIDTH:
            x = TIMER_PADDING
            y += row_height + TIMER_PADDING
            row_height = 0
        glyph["x"] = x
        glyph["y"] = y
        x += width + TIMER_PADDING
        row_height = max(row_height, height)

    texture_height = next_power_of_two(y + row_height + TIMER_PADDING)
    atlas = Image.new(
        "RGBA",
        (TIMER_TEXTURE_WIDTH, texture_height),
        (255, 255, 255, 0),
    )
    property_lines = [
        "# Deterministic Cinzel timer atlas metrics",
        f"textureWidth={TIMER_TEXTURE_WIDTH}",
        f"textureHeight={texture_height}",
        f"fontSize={TIMER_FONT_SIZE}",
        f"ascent={ascent}",
        f"descent={descent}",
        f"sourceFontSha256={hashlib.sha256(FONT_PATH.read_bytes()).hexdigest()}",
    ]

    for glyph in prepared:
        glyph_image = glyph.pop("image")
        atlas.alpha_composite(glyph_image, (int(glyph["x"]), int(glyph["y"])))
        property_lines.append(
            "glyph.{codepoint}={x},{y},{width},{height},{offsetX},{offsetY},{advance}".format(
                **glyph
            )
        )

    atlas.save(TIMER_PNG_PATH, optimize=True)
    TIMER_PROPERTIES_PATH.write_text(
        "\n".join(property_lines) + "\n",
        encoding="ascii",
    )


def main() -> None:
    font = ImageFont.truetype(str(FONT_PATH), FONT_SIZE)
    ascent, descent = font.getmetrics()
    characters = [chr(codepoint) for codepoint in range(32, 127)]
    characters.extend(chr(codepoint) for codepoint in range(160, 256))

    prepared: list[dict[str, object]] = []
    for character in characters:
        bbox = font.getbbox(character, anchor="ls")
        left, top, right, bottom = bbox
        width = max(0, right - left)
        height = max(0, bottom - top)
        advance = float(font.getlength(character))
        glyph_image = None

        if width > 0 and height > 0:
            glyph_image = Image.new(
                "RGBA",
                (width + PADDING * 2, height + PADDING * 2),
                (255, 255, 255, 0),
            )
            draw = ImageDraw.Draw(glyph_image)
            draw.text(
                (PADDING - left, PADDING - top),
                character,
                font=font,
                fill=(255, 255, 255, 255),
                anchor="ls",
            )

        prepared.append(
            {
                "codepoint": ord(character),
                "image": glyph_image,
                "width": 0 if glyph_image is None else glyph_image.width,
                "height": 0 if glyph_image is None else glyph_image.height,
                "offsetX": left - PADDING,
                "offsetY": top - PADDING,
                "advance": round(advance, 3),
            }
        )

    make_digits_tabular(prepared)
    x = PADDING
    y = PADDING
    row_height = 0
    for glyph in prepared:
        width = int(glyph["width"])
        height = int(glyph["height"])
        if width == 0 or height == 0:
            glyph["x"] = 0
            glyph["y"] = 0
            continue
        if x + width + PADDING > TEXTURE_WIDTH:
            x = PADDING
            y += row_height + PADDING
            row_height = 0
        glyph["x"] = x
        glyph["y"] = y
        x += width + PADDING
        row_height = max(row_height, height)

    texture_height = next_power_of_two(y + row_height + PADDING)
    atlas = Image.new("RGBA", (TEXTURE_WIDTH, texture_height), (255, 255, 255, 0))
    metadata_glyphs: dict[str, dict[str, int | float]] = {}

    for glyph in prepared:
        glyph_image = glyph.pop("image")
        if glyph_image is not None:
            atlas.alpha_composite(glyph_image, (int(glyph["x"]), int(glyph["y"])))
        metadata_glyphs[str(glyph.pop("codepoint"))] = glyph  # type: ignore[assignment]

    OUTPUT_DIRECTORY.mkdir(parents=True, exist_ok=True)
    atlas.save(PNG_PATH, optimize=True)
    metadata = {
        "textureWidth": TEXTURE_WIDTH,
        "textureHeight": texture_height,
        "fontSize": FONT_SIZE,
        "ascent": ascent,
        "descent": descent,
        "sourceFontSha256": hashlib.sha256(FONT_PATH.read_bytes()).hexdigest(),
        "glyphs": metadata_glyphs,
    }
    JSON_PATH.write_text(
        json.dumps(metadata, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )
    generate_timer_atlas()


def make_digits_tabular(glyphs):
    digits = [g for g in glyphs if 48 <= g["codepoint"] <= 57]
    advance = max(g["advance"] for g in digits)
    for glyph in digits:
        glyph["offsetX"] += round((advance - glyph["advance"]) / 2)
        glyph["advance"] = advance


if __name__ == "__main__":
    if len(sys.argv) > 1:
        name = sys.argv[1]
        if name not in ("cinzel", "lato", "spacemono"):
            raise SystemExit("Expected cinzel, lato, or spacemono")
        FONT_PATH = PROJECT_ROOT / f"src/client/resources/assets/metrakron/font/{name}.ttf"
        PNG_PATH = OUTPUT_DIRECTORY / f"{name}_atlas.png"
        JSON_PATH = OUTPUT_DIRECTORY / f"{name}_atlas.json"
        TIMER_PNG_PATH = OUTPUT_DIRECTORY / f"{name}_timer_atlas.png"
        TIMER_PROPERTIES_PATH = OUTPUT_DIRECTORY / f"{name}_timer_atlas.properties"
    main()
