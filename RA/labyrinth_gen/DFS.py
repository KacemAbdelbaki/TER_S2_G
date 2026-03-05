"""
DFS.py - Depth-First Search maze generation algorithm.

Uses shared utilities from common.py.
Provides:
    - DFS(grid, r, c, steps=None)   - recursive carving
    - full_maze(height, width, record_steps=False) - complete pipeline → JSON
"""

import copy
import random

from common import (
    Cell,
    initMap,
    add_cycles,
    mirror_map,
    insert_middle_column,
    add_TP_Points,
    maze_to_json,
    maze_from_json,
    maze_benchmarks,
    print_map,
)


# ---------------------------------------------------------------------------
# Algorithm
# ---------------------------------------------------------------------------

def DFS(grid, r, c, steps=None):
    """Recursive DFS maze carving.

    If *steps* is a list, each wall-removal event is appended as a dict:
    ``{"r1": …, "c1": …, "w1": …, "r2": …, "c2": …, "w2": …}``
    """
    H = len(grid)
    W = len(grid[0]) if H else 0
    cell = grid[r][c]
    cell.visited = True

    dirs = [(-1, 0, 0, 2), (0, 1, 1, 3), (1, 0, 2, 0), (0, -1, 3, 1)]
    random.shuffle(dirs)

    for dr, dc, w_cur, w_nei in dirs:
        nr, nc = r + dr, c + dc
        if 0 <= nr < H and 0 <= nc < W and not grid[nr][nc].visited:
            cell.walls[w_cur] = False
            grid[nr][nc].walls[w_nei] = False
            if steps is not None:
                steps.append({
                    "r1": r, "c1": c, "w1": w_cur,
                    "r2": nr, "c2": nc, "w2": w_nei,
                })
            DFS(grid, nr, nc, steps)


# ---------------------------------------------------------------------------
# Full pipeline
# ---------------------------------------------------------------------------

def full_maze(height, width, record_steps=False):
    """Generate a complete symmetric maze and return its JSON string.

    Parameters
    ----------
    height, width : int
        Half-grid dimensions (final maze will be roughly height x (2*width-1)).
    record_steps : bool
        If True, generation steps are included in the JSON under "steps".
    """
    grid = initMap(height, width)

    steps = [] if record_steps else None

    # Deep copy so ghost-room state is preserved for later column insertion
    grid_dfs = [[copy.deepcopy(cell) for cell in row[:]] for row in grid]

    # Choose a start cell that is not inside the ghost room
    start_r = random.randint(0, height - 1)
    start_c = random.randint(0, width - 1)
    while grid_dfs[start_r][start_c].visited:
        start_r = random.randint(0, height - 1)
        start_c = random.randint(0, width - 1)

    DFS(grid_dfs, start_r, start_c, steps)

    # Extra cycles for playability
    add_cycles(grid_dfs, p=0.25)

    # Strip last column, mirror, reinsert middle column
    half = [[copy.deepcopy(cell) for cell in row[:-1]] for row in grid_dfs]
    mirrored = mirror_map(half)
    final = insert_middle_column(grid_dfs, mirrored)

    # Two pairs of teleport points
    for _ in range(2):
        add_TP_Points(final)

    return maze_to_json(final, steps=steps)


# ---------------------------------------------------------------------------
# Standalone
# ---------------------------------------------------------------------------

if __name__ == "__main__":
    HEIGHT, WIDTH = 10, 12
    j = full_maze(HEIGHT, WIDTH, record_steps=False)
    grid = maze_from_json(j)

    cycles, num_hallways, hallway_lengths = maze_benchmarks(grid)
    print("cycles:", cycles)
    print("hallways:", num_hallways)
    print()
    print_map(grid)
