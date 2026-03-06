"""
Tetris.py - "Growing active cells" maze generation algorithm.

Uses shared utilities from common.py.
Provides:
    - Tetris(grid, r, c, branch_bias=0.85, steps=None)
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


def Tetris(grid, r, c, branch_bias=0.85, steps=None):
    """Generate a maze using the classic "Tetris" / active-cells approach.

    If *steps* is a list, each wall-removal event is appended as a dict:
    ``{"r1": …, "c1": …, "w1": …, "r2": …, "c2": …, "w2": …}``
    """
    H = len(grid)
    W = len(grid[0]) if H else 0
    if H == 0 or W == 0:
        return

    if not (0 <= r < H and 0 <= c < W):
        return

    if grid[r][c].visited:
        return

    dirs = [(-1, 0, 0, 2), (0, 1, 1, 3), (1, 0, 2, 0), (0, -1, 3, 1)]

    grid[r][c].visited = True
    active = [(r, c)]

    def unvisited_neighbors(cr, cc):
        out = []
        for dr, dc, w_cur, w_nei in dirs:
            nr, nc = cr + dr, cc + dc
            if 0 <= nr < H and 0 <= nc < W and not grid[nr][nc].visited:
                out.append((nr, nc, w_cur, w_nei))
        return out

    while active:
        branch_candidates = []
        if branch_bias > 0:
            for ar, ac in active:
                if len(unvisited_neighbors(ar, ac)) >= 2:
                    branch_candidates.append((ar, ac))

        if branch_candidates and random.random() < branch_bias:
            cr, cc = random.choice(branch_candidates)
            active.remove((cr, cc))
        else:
            i = random.randrange(len(active))
            cr, cc = active.pop(i)

        neighbors = unvisited_neighbors(cr, cc)
        if not neighbors:
            continue

        nr, nc, w_cur, w_nei = random.choice(neighbors)
        grid[cr][cc].walls[w_cur] = False
        grid[nr][nc].walls[w_nei] = False
        grid[nr][nc].visited = True

        if steps is not None:
            steps.append(
                {
                    "r1": cr,
                    "c1": cc,
                    "w1": w_cur,
                    "r2": nr,
                    "c2": nc,
                    "w2": w_nei,
                }
            )

        active.append((cr, cc))
        active.append((nr, nc))


# ---------------------------------------------------------------------------
# Full pipeline
# ---------------------------------------------------------------------------


def full_maze(height, width, record_steps=False):
    """Generate a complete symmetric maze and return its JSON string."""
    grid = initMap(height, width)

    steps = [] if record_steps else None

    grid_t = [[copy.deepcopy(cell) for cell in row[:]] for row in grid]

    # Choose a start cell that is not inside the ghost room
    start_r = random.randint(0, height - 1)
    start_c = random.randint(0, width - 1)
    while grid_t[start_r][start_c].visited:
        start_r = random.randint(0, height - 1)
        start_c = random.randint(0, width - 1)

    Tetris(grid_t, start_r, start_c, steps=steps)

    add_cycles(grid_t, p=0.25)

    half = [[copy.deepcopy(cell) for cell in row[:-1]] for row in grid_t]
    mirrored = mirror_map(half)
    final = insert_middle_column(grid_t, mirrored)

    for _ in range(2):
        add_TP_Points(final)

    return maze_to_json(final, steps=steps)


# ---------------------------------------------------------------------------
# Standalone
# ---------------------------------------------------------------------------

if __name__ == "__main__":
    HEIGHT, WIDTH = 5, 6
    j = full_maze(HEIGHT, WIDTH, record_steps=False)
    print_map(maze_from_json(j))
