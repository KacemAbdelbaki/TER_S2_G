"""
prim.py - Prim's algorithm for maze generation.

Uses shared utilities from common.py.
Provides:
    - prim_algorithm(grid, start_r, start_c, steps=None)
    - full_maze(height, width, record_steps=False) - complete pipeline → JSON
"""

import copy
import random

from common import (
    Cell,
    initMap,
    add_cycles,
    seal_borders,
    mirror_map,
    insert_middle_column,
    add_TP_Points,
    remove_wall_between,
    maze_to_json,
    maze_from_json,
    print_map,
)


# ---------------------------------------------------------------------------
# Algorithm
# ---------------------------------------------------------------------------

def prim_algorithm(grid, start_r, start_c, steps=None):
    """Grow a maze outward from (start_r, start_c) using Prim's algorithm.

    If *steps* is a list, each wall-removal event is appended.
    """
    H = len(grid)
    W = len(grid[0])

    grid[start_r][start_c].visited = True
    frontier = []  # (target_r, target_c, parent_r, parent_c)

    def _add_frontier(r, c):
        for dr, dc in [(-1, 0), (0, 1), (1, 0), (0, -1)]:
            nr, nc = r + dr, c + dc
            if 0 <= nr < H and 0 <= nc < W and not grid[nr][nc].visited:
                frontier.append((nr, nc, r, c))

    _add_frontier(start_r, start_c)

    while frontier:
        idx = random.randint(0, len(frontier) - 1)
        nr, nc, pr, pc = frontier.pop(idx)

        if not grid[nr][nc].visited:
            grid[nr][nc].visited = True
            parent = grid[pr][pc]
            child = grid[nr][nc]
            remove_wall_between(parent, child)

            if steps is not None:
                # Determine which wall indices were removed
                if parent.x > child.x:
                    w1, w2 = 0, 2
                elif parent.x < child.x:
                    w1, w2 = 2, 0
                elif parent.y < child.y:
                    w1, w2 = 1, 3
                else:
                    w1, w2 = 3, 1
                steps.append({
                    "r1": pr, "c1": pc, "w1": w1,
                    "r2": nr, "c2": nc, "w2": w2,
                })

            _add_frontier(nr, nc)


# ---------------------------------------------------------------------------
# Full pipeline
# ---------------------------------------------------------------------------

def full_maze(height, width, record_steps=False):
    """Generate a complete symmetric maze and return its JSON string."""
    grid = initMap(height, width)
    steps = [] if record_steps else None

    grid_p = [[copy.deepcopy(cell) for cell in row[:]] for row in grid]

    # Find a non-ghost start cell
    start_r, start_c = 0, 0
    while grid_p[start_r][start_c].visited:
        start_r = random.randint(0, height - 1)
        start_c = random.randint(0, width - 1)

    prim_algorithm(grid_p, start_r, start_c, steps)

    add_cycles(grid_p, p=0.05)

    half = [[copy.deepcopy(cell) for cell in row[:-1]] for row in grid_p]
    mirrored = mirror_map(half)
    final = insert_middle_column(grid_p, mirrored)

    for _ in range(2):
        add_TP_Points(final)

    seal_borders(final)
    return maze_to_json(final, steps=steps)


# ---------------------------------------------------------------------------
# Standalone
# ---------------------------------------------------------------------------

if __name__ == "__main__":
    HEIGHT, WIDTH = 5, 6
    j = full_maze(HEIGHT, WIDTH, record_steps=False)
    print_map(maze_from_json(j))
