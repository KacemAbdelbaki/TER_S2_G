"""
test_maze_properties.py - Structural / topological tests for generated mazes.

Tests dead-ends, cycles, connectivity, hallway statistics, and wall
consistency using ``common.maze_benchmarks`` and BFS flood-fill.
"""

import json
import pytest
from collections import deque

from common import maze_from_json, maze_benchmarks


# ═══════════════════════════════════════════════════════════════════════════
# Helpers
# ═══════════════════════════════════════════════════════════════════════════

def _grid_from_generate(generate, h=10, w=12):
    raw = generate(h, w, record_steps=False)
    return maze_from_json(json.loads(raw))


def _open_neighbours(grid, r, c):
    """Return coordinates of cells reachable from (r, c) through open walls."""
    H, W = len(grid), len(grid[0])
    cell = grid[r][c]
    result = []
    if not cell.walls[0] and r - 1 >= 0:      # Up
        result.append((r - 1, c))
    if not cell.walls[1] and c + 1 < W:        # Right
        result.append((r, c + 1))
    if not cell.walls[2] and r + 1 < H:        # Down
        result.append((r + 1, c))
    if not cell.walls[3] and c - 1 >= 0:       # Left
        result.append((r, c - 1))
    return result


def _bfs_reachable(grid, start_r, start_c):
    """BFS from (start_r, start_c); return set of (r, c) visited."""
    visited = set()
    queue = deque([(start_r, start_c)])
    visited.add((start_r, start_c))
    while queue:
        r, c = queue.popleft()
        for nr, nc in _open_neighbours(grid, r, c):
            if (nr, nc) not in visited:
                visited.add((nr, nc))
                queue.append((nr, nc))
    return visited


# ═══════════════════════════════════════════════════════════════════════════
# Connectivity
# ═══════════════════════════════════════════════════════════════════════════

def test_maze_is_connected(generate, algo_name):
    """Every non-ghost cell must be reachable from every other non-ghost cell."""
    grid = _grid_from_generate(generate)
    H, W = len(grid), len(grid[0])

    # Find first non-ghost cell
    start = None
    non_ghost_count = 0
    for r in range(H):
        for c in range(W):
            if not grid[r][c].isGhost:
                non_ghost_count += 1
                if start is None:
                    start = (r, c)

    assert start is not None, f"[{algo_name}] No non-ghost cells found"

    reachable = _bfs_reachable(grid, *start)
    # Ghost cells may or may not be reachable; only check non-ghost ones
    reachable_non_ghost = {(r, c) for r, c in reachable if not grid[r][c].isGhost}
    assert len(reachable_non_ghost) == non_ghost_count, (
        f"[{algo_name}] Only {len(reachable_non_ghost)}/{non_ghost_count} "
        f"non-ghost cells are connected"
    )


# ═══════════════════════════════════════════════════════════════════════════
# Cycles (add_cycles guarantees loops exist)
# ═══════════════════════════════════════════════════════════════════════════

def test_maze_has_cycles(generate, algo_name):
    """After add_cycles the maze must contain at least one cycle."""
    grid = _grid_from_generate(generate)
    num_cycles, _, _ = maze_benchmarks(grid)
    assert num_cycles > 0, (
        f"[{algo_name}] Expected cycles > 0, got {num_cycles}"
    )


# ═══════════════════════════════════════════════════════════════════════════
# Dead-ends
# ═══════════════════════════════════════════════════════════════════════════

def test_dead_end_count(generate, algo_name):
    """Count cells with exactly one opening (dead-ends) — should exist but be limited."""
    grid = _grid_from_generate(generate)
    H, W = len(grid), len(grid[0])
    dead_ends = 0
    total = 0
    for r in range(H):
        for c in range(W):
            if grid[r][c].isGhost:
                continue
            total += 1
            openings = sum(1 for w in grid[r][c].walls if not w)
            if openings == 1:
                dead_ends += 1

    # Dead-ends should exist (a tree has many) but the fraction should be < 50%
    # This is a sanity check, not a strict threshold
    assert dead_ends > 0, f"[{algo_name}] Expected some dead-ends"
    ratio = dead_ends / total if total else 0
    assert ratio < 0.5, (
        f"[{algo_name}] Dead-end ratio {ratio:.2%} is unusually high"
    )


# ═══════════════════════════════════════════════════════════════════════════
# Hallway statistics
# ═══════════════════════════════════════════════════════════════════════════

def test_hallways_exist(generate, algo_name):
    """A maze of reasonable size should have at least one hallway segment."""
    grid = _grid_from_generate(generate)
    _, num_hallways, _ = maze_benchmarks(grid)
    assert num_hallways > 0, (
        f"[{algo_name}] Expected at least one hallway, got {num_hallways}"
    )


# ═══════════════════════════════════════════════════════════════════════════
# Wall consistency (if A→B is open, then B→A must be open)
# ═══════════════════════════════════════════════════════════════════════════

_OPPOSITE = {0: 2, 1: 3, 2: 0, 3: 1}  # Up↔Down, Right↔Left
_DELTA    = {0: (-1, 0), 1: (0, 1), 2: (1, 0), 3: (0, -1)}


def test_wall_symmetry(generate, algo_name):
    """If cell (r,c) wall[d] is open, the neighbour's opposite wall must also be open.

    The *middle column* inserted by ``insert_middle_column`` intentionally
    copies walls asymmetrically (design choice for the Pac-Man style opening),
    so Left/Right walls touching that column are excluded.
    """
    grid = _grid_from_generate(generate)
    H, W = len(grid), len(grid[0])
    mid_col = W // 2  # column inserted by insert_middle_column

    for r in range(H):
        for c in range(W):
            for d in range(4):
                if not grid[r][c].walls[d]:
                    dr, dc = _DELTA[d]
                    nr, nc = r + dr, c + dc
                    if 0 <= nr < H and 0 <= nc < W:
                        # Skip the known middle-column seam
                        if c == mid_col or nc == mid_col:
                            continue
                        opp = _OPPOSITE[d]
                        assert not grid[nr][nc].walls[opp], (
                            f"[{algo_name}] Wall asymmetry at ({r},{c}) dir={d}: "
                            f"open but ({nr},{nc}) dir={opp} is closed"
                        )
