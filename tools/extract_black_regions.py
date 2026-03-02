#!/usr/bin/env python3
"""
Extract black-painted regions from user-annotated terrain images.
Black = impassable, everything else = walkable.
Outputs grid overrides for generate_navgrid.py.
"""

from PIL import Image

TILE_SIZE = 32
TERRAIN_W = 512
TERRAIN_H = 2048
COLS = TERRAIN_W // TILE_SIZE  # 16
ROWS = TERRAIN_H // TILE_SIZE  # 64

# Threshold: pixels darker than this are considered "black paint"
BLACK_THRESHOLD = 30  # RGB all < 30
# Minimum ratio of black pixels in a tile to mark it as blocked
BLACK_RATIO_THRESHOLD = 0.25


def extract_blocked_tiles(image_path: str, terrain_id: int):
    img = Image.open(image_path).convert("RGB")
    print(f"terrain-{terrain_id}: size={img.size} mode={img.mode}")

    # Resize if needed to match 512x2048
    if img.size != (TERRAIN_W, TERRAIN_H):
        print(f"  Resizing from {img.size} to ({TERRAIN_W}, {TERRAIN_H})")
        img = img.resize((TERRAIN_W, TERRAIN_H), Image.LANCZOS)

    blocked = []
    for r in range(ROWS):
        for c in range(COLS):
            x0 = c * TILE_SIZE
            y0 = r * TILE_SIZE
            tile = img.crop((x0, y0, x0 + TILE_SIZE, y0 + TILE_SIZE))
            pixels = list(tile.getdata())
            total = len(pixels)

            black_count = sum(
                1 for pr, pg, pb in pixels
                if pr < BLACK_THRESHOLD and pg < BLACK_THRESHOLD and pb < BLACK_THRESHOLD
            )
            ratio = black_count / total
            if ratio >= BLACK_RATIO_THRESHOLD:
                blocked.append((r, c, round(ratio, 2)))

    return blocked


def tiles_to_regions(blocked_tiles):
    """Convert list of (row, col) into rectangular regions for overrides."""
    if not blocked_tiles:
        return []

    coords = set((r, c) for r, c, _ in blocked_tiles)

    # Simple greedy rectangular merge
    visited = set()
    regions = []

    sorted_tiles = sorted(coords)
    for r, c in sorted_tiles:
        if (r, c) in visited:
            continue

        # Expand right
        c_end = c
        while c_end + 1 < COLS and (r, c_end + 1) in coords and (r, c_end + 1) not in visited:
            c_end += 1

        # Expand down
        r_end = r
        can_expand = True
        while can_expand and r_end + 1 < ROWS:
            for cc in range(c, c_end + 1):
                if (r_end + 1, cc) not in coords or (r_end + 1, cc) in visited:
                    can_expand = False
                    break
            if can_expand:
                r_end += 1

        # Mark visited
        for rr in range(r, r_end + 1):
            for cc in range(c, c_end + 1):
                visited.add((rr, cc))

        regions.append((r, c, r_end, c_end))

    return regions


def transform_row(old_row: int) -> int:
    """Transform row from annotation image order (01 on top, 02 on bottom)
    to game order (02 on top, 01 on bottom).

    User-annotated images have 01.jpg at the top and 02.jpg at the bottom.
    But in the game, 01.jpg is at the bottom (Y=0) and 02.jpg at the top (Y=1024).
    The navgrid uses game order: row 0 = game top (02.jpg), row 63 = game bottom (01.jpg).
    """
    return (old_row + 32) % 64


def main():
    for tid in [17, 18, 20, 22, 23, 25, 26]:
        image_path = f"/Users/sokol/Desktop/terrain-{tid}.jpg"
        print(f"\n=== Terrain {tid} ===")
        blocked = extract_blocked_tiles(image_path, tid)

        print(f"  Blocked tiles: {len(blocked)}/{ROWS * COLS}")
        print(f"  Blocked tile list (row, col, black_ratio):")
        for r, c, ratio in blocked:
            print(f"    ({r}, {c}) ratio={ratio}")

        regions = tiles_to_regions(blocked)

        # Transform coordinates from annotation order to game order
        transformed = []
        for r1, c1, r2, c2 in regions:
            tr1 = transform_row(r1)
            tr2 = transform_row(r2)
            # Ensure r1 <= r2 after transformation
            if tr1 > tr2:
                tr1, tr2 = tr2, tr1
            transformed.append((tr1, c1, tr2, c2))
        transformed.sort()

        print(f"\n  Merged regions ({len(transformed)}) [game coordinates]:")
        print(f"  \"all_walkable_then_block\": [")
        for r1, c1, r2, c2 in transformed:
            print(f"      ({r1}, {c1}, {r2}, {c2}),")
        print(f"  ],")


if __name__ == "__main__":
    main()
