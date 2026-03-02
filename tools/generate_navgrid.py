#!/usr/bin/env python3
"""
NavGrid Generator — Terrain image pixel analysis → walkability grid.

For each of the 27 terrain backgrounds, combines the two 512×1024 halves
into a 512×2048 image, divides it into 32×32 tiles (16 cols × 64 rows),
and classifies each tile as walkable or blocked.

Output per terrain:
  - JSON grid:   core/assets/data/navgrid/terrain-{NN}.json
  - Debug image: tools/navgrid-debug/terrain-{NN}.png  (overlay: green=walk, red=blocked)

Classification:
  - General pixel analysis rules (dark ratio, cyan, brightness variance)
  - Per-terrain overrides based on human review
  - Terrain 22: brightness-based bridge detection

Validation:
  - BFS from any walkable tile on row 0 to any on row 63 (Y-axis connectivity)
  - Every row must have at least one walkable tile (no full X-axis blockage)
"""

import json
import os
import sys
from collections import deque
from pathlib import Path

from PIL import Image, ImageDraw

# --- Config ---
TILE_SIZE = 32
TERRAIN_W = 512
TERRAIN_H = 2048
COLS = TERRAIN_W // TILE_SIZE   # 16
ROWS = TERRAIN_H // TILE_SIZE   # 64

TERRAINS_DIR = Path(__file__).parent.parent / "core" / "assets" / "gfx" / "terrains"
OUTPUT_JSON_DIR = Path(__file__).parent.parent / "core" / "assets" / "data" / "navgrid"
OUTPUT_DEBUG_DIR = Path(__file__).parent / "navgrid-debug"

# ============================================================
# Per-terrain overrides (based on human visual review)
# ============================================================

# These terrains have no obstacles — force all tiles walkable.
# Terrain 25: factory buildings rendered as foreground overlay (units pass behind)
FORCE_ALL_WALKABLE = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 25}

# Rectangular region overrides: (row_start, col_start, row_end, col_end) inclusive.
# Applied AFTER general classification.
#   "force_walkable": list of regions to force walkable
#   "force_blocked":  list of regions to force blocked
#   "all_walkable_then_block": force all walkable first, then block listed regions
OVERRIDES = {
    # NOTE: All row coordinates below match the GAME coordinate system:
    #   Row 0  = game top (enemy spawn, 02.jpg visual top)
    #   Row 31 = game middle (02.jpg visual bottom / 01.jpg visual top)
    #   Row 32 = game middle (01.jpg visual top)
    #   Row 63 = game bottom (ally base, 01.jpg visual bottom)
    #
    # Original annotations were done on combined images with 01.jpg on top
    # (old order). Coordinates have been transformed: new_row = (old_row+32)%64.
    17: {
        # Military compound — based on human-annotated image.
        "all_walkable_then_block": [
            (7, 15, 16, 15),    # Right edge structures
            (10, 14, 12, 14),   # Right edge col 14
            (13, 0, 31, 0),     # Bottom-left edge col 0
            (16, 1, 31, 1),     # Bottom-left edge col 1
            (31, 9, 31, 10),    # Bottom-right corner
            (32, 0, 32, 1),     # Top-left corner
            (32, 9, 39, 11),    # Upper right building complex
            (33, 0, 33, 0),     # Left edge row 1
            (40, 9, 40, 10),    # Below upper right building
            (46, 0, 56, 0),     # Left edge structures
            (48, 1, 54, 1),     # Left structures col 1
            (50, 10, 52, 12),   # Center-right structure
            (51, 9, 52, 9),     # Center-right extension
            (53, 11, 53, 11),   # Small structure
        ],
    },
    18: {
        # Snow terrain — based on human-annotated image.
        "all_walkable_then_block": [
            (17, 3, 18, 5),     # Round building 1
            (19, 4, 19, 4),     # Round building 1 extension
            (23, 3, 24, 5),     # Round building 2
            (25, 4, 25, 4),     # Round building 2 extension
            (28, 0, 31, 0),     # Bottom-left cliff
            (28, 15, 31, 15),   # Bottom-right cliff
            (29, 1, 31, 1),     # Bottom-left col 1
            (29, 14, 31, 14),   # Bottom-right col 14
            (30, 13, 31, 13),   # Bottom-right col 13
            (31, 2, 31, 2),     # Bottom-left col 2
            (32, 0, 39, 3),     # Top-left cliff
            (32, 12, 40, 15),   # Top-right cliff
            (35, 11, 38, 11),   # Right cliff extension
            (40, 0, 41, 2),     # Left cliff lower section
            (41, 13, 43, 15),   # Right cliff lower section
            (42, 0, 43, 1),     # Left edge rows 10-11
            (44, 0, 46, 0),     # Left edge rows 12-14
            (44, 14, 45, 15),   # Right edge rows 12-13
            (46, 15, 46, 15),   # Right edge row 14
            (51, 0, 57, 0),     # Left narrow section
            (51, 15, 57, 15),   # Right narrow section
            (52, 1, 56, 1),     # Left narrow col 1
            (52, 14, 56, 14),   # Right narrow col 14
        ],
    },
    20: {
        # Wasteland — based on human-annotated image.
        "all_walkable_then_block": [
            (16, 0, 24, 0),     # Bottom-left collapsed col 0
            (18, 1, 23, 1),     # Bottom-left collapsed col 1
            (28, 2, 30, 5),     # Bottom-left craters
            (28, 14, 31, 15),   # Bottom-right collapsed
            (30, 13, 31, 13),   # Bottom-right col 13
            (31, 3, 31, 5),     # Bottom-left crater extension
            (31, 12, 31, 12),   # Bottom-right extension
            (32, 11, 53, 15),   # Upper-right collapsed terrain (main body)
            (33, 10, 52, 10),   # Upper-right col 10 extension
            (36, 9, 43, 9),     # Upper-right col 9 extension
            (54, 12, 55, 15),   # Collapsed terrain tail
            (56, 13, 56, 15),   # Collapsed terrain tail
            (57, 15, 57, 15),   # Collapsed terrain tip
        ],
    },
    19: {
        # Grassland with water inlet and dark structures.
        # Water on right side (rows 11-28), water+structures on left (rows 31-49).
        "all_walkable_then_block": [
            # Right side water inlet
            (11, 15, 13, 15),   # Right edge col 15
            (14, 14, 16, 15),   # Right edge cols 14-15
            (17, 14, 17, 14),   # Right col 14
            (18, 14, 19, 15),   # Right cols 14-15
            (20, 14, 21, 14),   # Right col 14
            (22, 14, 23, 15),   # Right cols 14-15
            (24, 15, 28, 15),   # Right col 15
            # Left side water + structures complex
            (31, 0, 32, 0),     # Left col 0
            (33, 0, 33, 1),     # Left cols 0-1
            (34, 0, 34, 2),     # Left cols 0-2
            (35, 0, 35, 3),     # Left cols 0-3
            (36, 0, 36, 4),     # Left cols 0-4
            (37, 0, 38, 5),     # Water cols 0-5 (dark structures + water)
            (39, 2, 40, 6),     # Dark + water cols 2-6
            (41, 0, 42, 6),     # Dark + water cols 0-6
            (43, 0, 43, 5),     # Dark + water cols 0-5
            (44, 3, 44, 5),     # Water cols 3-5
            (45, 2, 45, 4),     # Water cols 2-4
            (46, 0, 46, 3),     # Dark + water cols 0-3
            (47, 0, 47, 3),     # Water cols 0-3
            (48, 0, 48, 1),     # Water cols 0-1
            (49, 0, 49, 0),     # Water col 0
        ],
    },
    21: {
        # Canyon with central bridge — precise water boundaries.
        # Water walls form a canyon shape; bridge spans the narrowest section.
        "all_walkable_then_block": [
            # Top half — left canyon water wall
            (4, 0, 4, 0),
            (5, 0, 5, 1),
            (6, 0, 6, 2),
            (7, 0, 8, 3),
            (9, 0, 22, 4),     # Main left wall (includes bridge area)
            (23, 0, 23, 2),
            (24, 0, 24, 1),
            (25, 0, 25, 0),
            # Top half — right canyon water wall
            (2, 15, 2, 15),
            (3, 14, 4, 15),
            (5, 13, 6, 15),
            (7, 12, 7, 15),
            (8, 11, 8, 15),
            (9, 10, 20, 15),   # Main right wall (includes bridge area)
            (21, 10, 21, 13),  # Widening below bridge
            (22, 10, 22, 14),
            (23, 11, 25, 15),
            (26, 12, 26, 15),
            (27, 13, 27, 15),
            (28, 14, 29, 15),
            (30, 15, 30, 15),
            # Bottom half — left canyon water wall
            (33, 0, 34, 0),
            (35, 0, 36, 1),
            (37, 0, 44, 2),
            (45, 1, 45, 3),    # Shifted right
            (46, 0, 50, 3),
            (51, 0, 51, 4),    # Wider at row 51
            (52, 0, 54, 3),
            (55, 0, 55, 2),
            (56, 0, 57, 1),
            (58, 0, 58, 0),
            # Bottom half — right canyon water wall
            (45, 15, 45, 15),
            (46, 14, 46, 15),
            (47, 13, 49, 15),
            (50, 12, 52, 15),
            (53, 13, 56, 15),
            (57, 14, 58, 15),
            (59, 15, 60, 15),
        ],
    },
    22: {
        # Bridge over city — based on human-annotated image.
        "all_walkable_then_block": [
            (5, 0, 31, 0),      # Bottom-left cliff col 0
            (5, 15, 31, 15),    # Bottom-right cliff col 15
            (6, 1, 31, 1),      # Bottom-left col 1
            (6, 14, 31, 14),    # Bottom-right col 14
            (7, 2, 22, 2),      # Bottom-left col 2
            (7, 13, 31, 13),    # Bottom-right col 13
            (8, 3, 17, 3),      # Bottom-left col 3
            (9, 12, 31, 12),    # Bottom-right col 12
            (15, 11, 31, 11),   # Bottom-right col 11
            (21, 10, 31, 10),   # Bottom-right col 10
            (25, 9, 29, 9),     # Bottom-right col 9
            (32, 0, 57, 1),     # Left cliff upper section
            (32, 10, 35, 15),   # Top-right cliff
            (35, 2, 56, 2),     # Left cliff col 2
            (36, 11, 40, 15),   # Right cliff upper
            (39, 3, 55, 3),     # Left cliff col 3
            (41, 12, 54, 15),   # Right cliff main body
            (52, 4, 53, 4),     # Left cliff col 4 extension
            (55, 13, 56, 15),   # Right cliff tail
            (57, 14, 57, 15),   # Right cliff tip
            (58, 0, 58, 0),     # Left edge
            (58, 15, 58, 15),   # Right edge
        ],
    },
    23: {
        # Desert craters — based on human-annotated image.
        "all_walkable_then_block": [
            (19, 6, 24, 8),     # Bottom crater center
            (20, 5, 23, 5),     # Bottom crater left
            (20, 9, 24, 9),     # Bottom crater right
            (21, 4, 22, 4),     # Bottom crater far left
            (21, 10, 22, 10),   # Bottom crater far right
            (39, 6, 46, 8),     # Top crater center
            (40, 4, 45, 5),     # Top crater left
            (40, 9, 46, 10),    # Top crater right
            (41, 3, 43, 3),     # Top crater far left
            (41, 11, 44, 11),   # Top crater far right
            (46, 5, 46, 5),     # Top crater bottom edge
        ],
    },
    24: {
        # Desert craters: three craters need expanded coverage.
        "force_blocked": [
            (10, 0, 18, 4),    # Bottom-left crater (expanded)
            (21, 8, 31, 15),   # Bottom-right crater (expanded)
            (37, 0, 45, 5),    # Top crater (expanded)
        ],
    },
    25: {
        # Industrial — based on human-annotated image.
        "all_walkable_then_block": [
            (4, 0, 31, 0),      # Lower-left pipeline col 0
            (5, 1, 31, 1),      # Lower-left pipeline col 1
            (6, 2, 31, 2),      # Lower-left pipeline col 2
            (7, 3, 31, 4),      # Lower-left pipeline cols 3-4
            (8, 5, 19, 5),      # Lower-left pipeline col 5
            (9, 6, 14, 6),      # Lower-left pipeline col 6
            (24, 14, 31, 15),   # Bottom-right structures cols 14-15
            (25, 12, 31, 13),   # Bottom-right structures cols 12-13
            (26, 11, 31, 11),   # Bottom-right structures col 11
            (28, 10, 30, 10),   # Bottom-right col 10
            (32, 0, 42, 3),     # Upper-left structure cluster
            (32, 11, 35, 15),   # Upper-right structure cluster
            (36, 4, 42, 4),     # Upper-left col 4 extension
            (36, 12, 40, 15),   # Upper-right col 12+ extension
            (38, 5, 41, 5),     # Upper-left col 5 extension
            (41, 13, 41, 15),   # Upper-right bottom edge
            (42, 14, 42, 15),   # Upper-right bottom tip
            (43, 0, 43, 2),     # Upper-left bottom edge
            (43, 15, 43, 15),   # Upper-right bottom tip
            (44, 0, 44, 0),     # Left edge
        ],
    },
    27: {
        # Grassland with lake/water on left side.
        "all_walkable_then_block": [
            (13, 0, 13, 2),    # Top edge
            (14, 0, 23, 3),    # Main lake body
            (24, 0, 25, 2),    # Narrowing
            (26, 0, 35, 1),    # Narrow left wall
            (36, 0, 37, 0),    # Bottom edge
        ],
    },
    26: {
        # Industrial with roads — based on human-annotated image.
        "all_walkable_then_block": [
            (7, 0, 11, 0),      # Mid-left col 0
            (8, 1, 11, 1),      # Mid-left col 1
            (14, 14, 20, 15),   # Right collapsed cols 14-15
            (15, 13, 20, 13),   # Right collapsed col 13
            (21, 15, 21, 15),   # Right collapsed tip
            (27, 0, 31, 1),     # Bottom-left cols 0-1
            (27, 12, 31, 15),   # Bottom-right cols 12-15
            (28, 2, 31, 3),     # Bottom-left cols 2-3
            (32, 0, 34, 3),     # Top-left collapsed
            (32, 12, 33, 15),   # Top-right collapsed
            (34, 13, 34, 15),   # Top-right row 2
            (35, 0, 35, 2),     # Top-left row 3
            (35, 15, 35, 15),   # Top-right edge
            (36, 0, 36, 1),     # Top-left row 4
        ],
    },
}

# All human-annotated terrains use "all_walkable_then_block" overrides.
# Image combine order: 02.jpg (game top) then 01.jpg (game bottom).


def load_terrain(terrain_id: int) -> Image.Image:
    """Load and combine two 512×1024 halves into 512×2048.

    In the game (LibGDX Scene2D):
      - 01.jpg is rendered at Y=0    (bottom half, ally base side)
      - 02.jpg is rendered at Y=1024 (top half, enemy spawn side)

    NavGrid row 0 must correspond to the game top (enemy spawn),
    and row 63 to the game bottom (ally base).

    In PIL, Y=0 is the image top, so we place:
      - 02.jpg at PIL Y=0    (game top → navgrid row 0)
      - 01.jpg at PIL Y=1024 (game bottom → navgrid row 32+)
    """
    folder = TERRAINS_DIR / f"{terrain_id:02d}"
    img1_path = folder / "01.jpg"
    img2_path = folder / "02.jpg"

    if not img1_path.exists() or not img2_path.exists():
        raise FileNotFoundError(f"Terrain {terrain_id:02d} images not found")

    img1 = Image.open(img1_path).convert("RGB")
    img2 = Image.open(img2_path).convert("RGB")

    combined = Image.new("RGB", (TERRAIN_W, TERRAIN_H))
    combined.paste(img2, (0, 0))       # 02.jpg = game top (enemy spawn side)
    combined.paste(img1, (0, 1024))    # 01.jpg = game bottom (ally base side)
    return combined


def analyze_tile(img: Image.Image, col: int, row: int) -> dict:
    """Analyze a 32×32 tile and return color statistics."""
    x0 = col * TILE_SIZE
    y0 = row * TILE_SIZE
    tile = img.crop((x0, y0, x0 + TILE_SIZE, y0 + TILE_SIZE))
    pixels = list(tile.getdata())

    total = len(pixels)
    sum_r = sum_g = sum_b = 0
    sum_h = sum_s = sum_v = 0
    dark_count = 0
    cyan_count = 0
    high_sat_count = 0

    for r, g, b in pixels:
        sum_r += r
        sum_g += g
        sum_b += b

        mx = max(r, g, b)
        mn = min(r, g, b)
        v = mx / 255.0
        s = (mx - mn) / mx if mx > 0 else 0
        if mx == mn:
            h = 0
        elif mx == r:
            h = 60 * ((g - b) / (mx - mn)) % 360
        elif mx == g:
            h = 60 * ((b - r) / (mx - mn)) + 120
        else:
            h = 60 * ((r - g) / (mx - mn)) + 240
        if h < 0:
            h += 360

        sum_h += h
        sum_s += s
        sum_v += v

        if v < 0.30:
            dark_count += 1
        if 140 <= h <= 210 and s > 0.25 and v > 0.3:
            cyan_count += 1
        if s > 0.5:
            high_sat_count += 1

    avg_r = sum_r / total
    avg_g = sum_g / total
    avg_b = sum_b / total
    avg_v = sum_v / total

    brightness_vals = [(r * 0.299 + g * 0.587 + b * 0.114) for r, g, b in pixels]
    avg_bright = sum(brightness_vals) / total
    variance = sum((x - avg_bright) ** 2 for x in brightness_vals) / total

    return {
        "avg_r": avg_r, "avg_g": avg_g, "avg_b": avg_b,
        "avg_v": avg_v,
        "dark_ratio": dark_count / total,
        "cyan_ratio": cyan_count / total,
        "high_sat_ratio": high_sat_count / total,
        "brightness_variance": variance,
        "avg_brightness": avg_bright,
    }


def classify_tile_general(stats: dict) -> bool:
    """General classification: Return True if walkable, False if blocked."""
    if stats["cyan_ratio"] > 0.35:
        return False
    if stats["dark_ratio"] > 0.45:
        return False
    if stats["dark_ratio"] > 0.25 and stats["brightness_variance"] > 1200:
        return False
    if stats["avg_v"] < 0.32 and stats["brightness_variance"] > 800:
        return False
    if stats["avg_brightness"] < 60:
        return False
    return True


def classify_tile_brightness(stats: dict, threshold: float) -> bool:
    """Brightness-based classification for bridge maps (terrain 22)."""
    return stats["avg_brightness"] > threshold


def apply_overrides(grid: list, terrain_id: int):
    """Apply per-terrain overrides to the grid (in-place)."""
    if terrain_id in FORCE_ALL_WALKABLE:
        for r in range(ROWS):
            for c in range(COLS):
                grid[r][c] = True
        return

    override = OVERRIDES.get(terrain_id)
    if not override:
        return

    # "all_walkable_then_block": set everything walkable, then block regions
    if "all_walkable_then_block" in override:
        for r in range(ROWS):
            for c in range(COLS):
                grid[r][c] = True
        for r1, c1, r2, c2 in override["all_walkable_then_block"]:
            for r in range(r1, r2 + 1):
                for c in range(c1, c2 + 1):
                    if 0 <= r < ROWS and 0 <= c < COLS:
                        grid[r][c] = False

    # "force_walkable": override specific regions to walkable
    if "force_walkable" in override:
        for r1, c1, r2, c2 in override["force_walkable"]:
            for r in range(r1, r2 + 1):
                for c in range(c1, c2 + 1):
                    if 0 <= r < ROWS and 0 <= c < COLS:
                        grid[r][c] = True

    # "force_blocked": override specific regions to blocked
    if "force_blocked" in override:
        for r1, c1, r2, c2 in override["force_blocked"]:
            for r in range(r1, r2 + 1):
                for c in range(c1, c2 + 1):
                    if 0 <= r < ROWS and 0 <= c < COLS:
                        grid[r][c] = False


def validate_grid(grid: list) -> dict:
    """
    Validate the grid:
    1. Every row has at least one walkable tile (no X-axis blockage)
    2. BFS path exists from bottom (row 0) to top (row ROWS-1)
    """
    issues = []

    blocked_rows = []
    for r in range(ROWS):
        if not any(grid[r][c] for c in range(COLS)):
            blocked_rows.append(r)
    if blocked_rows:
        issues.append(f"Fully blocked rows (no X passage): {blocked_rows}")

    visited = [[False] * COLS for _ in range(ROWS)]
    queue = deque()

    for c in range(COLS):
        if grid[0][c]:
            queue.append((0, c))
            visited[0][c] = True

    directions = [(-1, 0), (1, 0), (0, -1), (0, 1)]
    reachable_top = False

    while queue:
        r, c = queue.popleft()
        if r == ROWS - 1:
            reachable_top = True
            break
        for dr, dc in directions:
            nr, nc = r + dr, c + dc
            if 0 <= nr < ROWS and 0 <= nc < COLS and not visited[nr][nc] and grid[nr][nc]:
                visited[nr][nc] = True
                queue.append((nr, nc))

    if not reachable_top:
        issues.append("No path from bottom (row 0) to top (row 63) — Y-axis disconnected")

    return {
        "valid": len(issues) == 0,
        "issues": issues,
        "blocked_rows": blocked_rows,
    }


def generate_debug_image(terrain_img: Image.Image, grid: list, validation: dict) -> Image.Image:
    """Overlay walkability grid on terrain image."""
    debug = terrain_img.copy()
    overlay = Image.new("RGBA", debug.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(overlay)

    for r in range(ROWS):
        for c in range(COLS):
            x0 = c * TILE_SIZE
            y0 = r * TILE_SIZE
            if grid[r][c]:
                color = (0, 255, 0, 60)   # green = walkable
            else:
                color = (255, 0, 0, 100)   # red = blocked
            draw.rectangle([x0, y0, x0 + TILE_SIZE - 1, y0 + TILE_SIZE - 1], fill=color)

    for r in range(ROWS + 1):
        y = r * TILE_SIZE
        draw.line([(0, y), (TERRAIN_W, y)], fill=(255, 255, 255, 40))
    for c in range(COLS + 1):
        x = c * TILE_SIZE
        draw.line([(x, 0), (x, TERRAIN_H)], fill=(255, 255, 255, 40))

    debug = debug.convert("RGBA")
    debug = Image.alpha_composite(debug, overlay)
    return debug


def process_terrain(terrain_id: int) -> dict:
    """Process a single terrain: analyze, classify, apply overrides, validate, output."""
    print(f"  Processing terrain {terrain_id:02d}...", end=" ")

    img = load_terrain(terrain_id)

    # Build grid
    grid = [[False] * COLS for _ in range(ROWS)]

    for r in range(ROWS):
        for c in range(COLS):
            stats = analyze_tile(img, c, r)
            grid[r][c] = classify_tile_general(stats)

    # Apply per-terrain overrides
    apply_overrides(grid, terrain_id)

    # Validate
    validation = validate_grid(grid)

    # Count stats
    walkable = sum(sum(1 for c in range(COLS) if grid[r][c]) for r in range(ROWS))
    total = ROWS * COLS
    pct = walkable / total * 100

    status = "OK" if validation["valid"] else "FAIL"
    print(f"{walkable}/{total} walkable ({pct:.0f}%) — {status}")
    if not validation["valid"]:
        for issue in validation["issues"]:
            print(f"    WARNING: {issue}")

    # Output JSON
    OUTPUT_JSON_DIR.mkdir(parents=True, exist_ok=True)
    json_path = OUTPUT_JSON_DIR / f"terrain-{terrain_id:02d}.json"
    json_data = {
        "terrainId": terrain_id,
        "cols": COLS,
        "rows": ROWS,
        "tileSize": TILE_SIZE,
        "grid": grid,
    }
    with open(json_path, "w") as f:
        json.dump(json_data, f, separators=(",", ":"))

    # Output debug image
    OUTPUT_DEBUG_DIR.mkdir(parents=True, exist_ok=True)
    debug_img = generate_debug_image(img, grid, validation)
    debug_path = OUTPUT_DEBUG_DIR / f"terrain-{terrain_id:02d}.png"
    debug_img.save(debug_path)

    return {
        "terrain_id": terrain_id,
        "walkable": walkable,
        "total": total,
        "validation": validation,
    }


def main():
    print("NavGrid Generator (with overrides)")
    print(f"Grid: {COLS}×{ROWS} tiles ({TILE_SIZE}px each)")
    print(f"Terrains dir: {TERRAINS_DIR}")
    print(f"Output JSON:  {OUTPUT_JSON_DIR}")
    print(f"Output debug: {OUTPUT_DEBUG_DIR}")
    print(f"Force all walkable: {sorted(FORCE_ALL_WALKABLE)}")
    print(f"Custom overrides: {sorted(OVERRIDES.keys())}")
    print(f"Image order: 02.jpg (game top) + 01.jpg (game bottom)")
    print()

    results = []
    for tid in range(1, 28):
        try:
            result = process_terrain(tid)
            results.append(result)
        except FileNotFoundError as e:
            print(f"  Skipped terrain {tid:02d}: {e}")

    # Summary
    print()
    print("=== Summary ===")
    failed = [r for r in results if not r["validation"]["valid"]]
    if failed:
        print(f"FAILED: {len(failed)} terrains need manual review:")
        for r in failed:
            print(f"  terrain-{r['terrain_id']:02d}: {', '.join(r['validation']['issues'])}")
    else:
        print(f"All {len(results)} terrains passed validation.")

    avg_walkable = sum(r["walkable"] / r["total"] for r in results) / len(results) * 100
    print(f"Average walkable: {avg_walkable:.0f}%")


if __name__ == "__main__":
    main()
