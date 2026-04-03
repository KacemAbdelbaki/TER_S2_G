"""
kruskal.py - Kruskal's algorithm for maze generation.

Uses shared utilities from common.py.
Provides:
    - DisjointSet                   - Union-Find data structure
    - kruskal_algorithm(grid, steps=None) - spanning-tree carving
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
    maze_to_json,
    maze_from_json,
    print_map,
)


# ---------------------------------------------------------------------------
# Union-Find
# ---------------------------------------------------------------------------

class DisjointSet:
    def __init__(self, n):
        self.parent = list(range(n))

    def find(self, i):
        if self.parent[i] == i:
            return i
        self.parent[i] = self.find(self.parent[i])
        return self.parent[i]

    def union(self, i, j):
        ri, rj = self.find(i), self.find(j)
        if ri != rj:
            self.parent[ri] = rj
            return True
        return False


# ---------------------------------------------------------------------------
# Algorithm
# ---------------------------------------------------------------------------

def kruskal_algorithm(grid, steps=None):
    """Generate a spanning-tree maze with Kruskal's algorithm.

    If *steps* is a list, each wall-removal event is appended.
    """
    H = len(grid)
    W = len(grid[0])
    ds = DisjointSet(H * W)

    # List every internal wall as an edge
    walls = []
    for r in range(H):
        for c in range(W):
            if c + 1 < W:
                walls.append(((r, c), (r, c + 1), 1, 3))
            if r + 1 < H:
                walls.append(((r, c), (r + 1, c), 2, 0))

    # Pre-union ghost-room cells
    for r in range(H):
        for c in range(W):
            if grid[r][c].visited:
                for dr, dc in [(0, 1), (1, 0)]:
                    nr, nc = r + dr, c + dc
                    if 0 <= nr < H and 0 <= nc < W and grid[nr][nc].visited:
                        ds.union(r * W + c, nr * W + nc)

    random.shuffle(walls)

    for (r1, c1), (r2, c2), w1, w2 in walls:
        if ds.union(r1 * W + c1, r2 * W + c2):
            grid[r1][c1].walls[w1] = False
            grid[r2][c2].walls[w2] = False
            grid[r1][c1].visited = True
            grid[r2][c2].visited = True
            if steps is not None:
                steps.append({
                    "r1": r1, "c1": c1, "w1": w1,
                    "r2": r2, "c2": c2, "w2": w2,
                })


# ---------------------------------------------------------------------------
# Full pipeline
# ---------------------------------------------------------------------------

def full_maze(height, width, record_steps=False):
    """Generate a complete symmetric maze and return its JSON string."""
    grid = initMap(height, width)
    steps = [] if record_steps else None

    grid_k = [[copy.deepcopy(cell) for cell in row[:]] for row in grid]
    kruskal_algorithm(grid_k, steps)

    add_cycles(grid_k, p=0.10)

    half = [[copy.deepcopy(cell) for cell in row[:-1]] for row in grid_k]
    mirrored = mirror_map(half)
    final = insert_middle_column(grid_k, mirrored)

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
