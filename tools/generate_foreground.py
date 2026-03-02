#!/usr/bin/env python3
"""
Generate foreground overlay PNGs for terrains where units should
pass behind/under building structures.

Uses blocked tile regions to extract building structures from terrain images
and create transparent PNG overlays. These overlays are rendered above units
in the game, creating the illusion that units pass under the buildings.

Output per terrain:
  - 01-fg.png (bottom half, game Y=0 to Y=1024)
  - 02-fg.png (top half, game Y=1024 to Y=2048)
"""

from pathlib import Path

from PIL import Image

TILE_SIZE = 32
TERRAIN_W = 512
TERRAIN_H = 2048

TERRAINS_DIR = Path(__file__).parent.parent / "core" / "assets" / "gfx" / "terrains"

# Blocked tile regions per terrain (NavGrid coordinates):
# (row_start, col_start, row_end, col_end) inclusive
# Row 0 = game top (Y=2048), Row 63 = game bottom (Y=0)
FOREGROUND_REGIONS = {
    25: [
        # Industrial — building structures that units pass behind
        (4, 0, 31, 0),
        (5, 1, 31, 1),
        (6, 2, 31, 2),
        (7, 3, 31, 4),
        (8, 5, 19, 5),
        (9, 6, 14, 6),
        (24, 14, 31, 15),
        (25, 12, 31, 13),
        (26, 11, 31, 11),
        (28, 10, 30, 10),
        (32, 0, 42, 3),
        (32, 11, 35, 15),
        (36, 4, 42, 4),
        (36, 12, 40, 15),
        (38, 5, 41, 5),
        (41, 13, 41, 15),
        (42, 14, 42, 15),
        (43, 0, 43, 2),
        (43, 15, 43, 15),
        (44, 0, 44, 0),
    ],
}


def load_terrain(terrain_id: int) -> Image.Image:
    """Load and combine terrain halves into 512x2048 image.

    Combined image order matches generate_navgrid.py:
      - 02.jpg at PIL Y=0 (game top, NavGrid row 0)
      - 01.jpg at PIL Y=1024 (game bottom, NavGrid row 32+)
    """
    tid_str = str(terrain_id).zfill(2)
    d = TERRAINS_DIR / tid_str
    img1 = Image.open(d / "01.jpg").convert("RGBA")
    img2 = Image.open(d / "02.jpg").convert("RGBA")

    combined = Image.new("RGBA", (TERRAIN_W, TERRAIN_H))
    combined.paste(img2, (0, 0))       # 02.jpg = game top
    combined.paste(img1, (0, 1024))    # 01.jpg = game bottom
    return combined


def generate_foreground(terrain_id: int, regions: list):
    """Generate foreground overlay PNGs for a terrain."""
    terrain = load_terrain(terrain_id)
    foreground = Image.new("RGBA", (TERRAIN_W, TERRAIN_H), (0, 0, 0, 0))

    for r1, c1, r2, c2 in regions:
        for r in range(r1, r2 + 1):
            for c in range(c1, c2 + 1):
                x = c * TILE_SIZE
                y = r * TILE_SIZE
                tile = terrain.crop((x, y, x + TILE_SIZE, y + TILE_SIZE))
                foreground.paste(tile, (x, y))

    # Split: top half → 02-fg.png, bottom half → 01-fg.png
    fg2 = foreground.crop((0, 0, TERRAIN_W, 1024))
    fg1 = foreground.crop((0, 1024, TERRAIN_W, TERRAIN_H))

    tid_str = str(terrain_id).zfill(2)
    output_dir = TERRAINS_DIR / tid_str
    fg1.save(output_dir / "01-fg.png")
    fg2.save(output_dir / "02-fg.png")

    fg1_opaque = sum(1 for p in fg1.getdata() if p[3] > 0)
    fg2_opaque = sum(1 for p in fg2.getdata() if p[3] > 0)
    total = TERRAIN_W * 1024

    print(f"terrain-{tid_str}: Generated foreground overlays")
    print(f"  01-fg.png: {fg1_opaque}/{total} opaque ({100*fg1_opaque/total:.1f}%)")
    print(f"  02-fg.png: {fg2_opaque}/{total} opaque ({100*fg2_opaque/total:.1f}%)")


def main():
    for tid, regions in FOREGROUND_REGIONS.items():
        generate_foreground(tid, regions)


if __name__ == "__main__":
    main()
