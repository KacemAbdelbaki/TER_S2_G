"""
common.py - Shared data model and utilities for all maze generation algorithms.

Provides:
    - Cell (data model for a single maze cell)
    - Grid helpers: initMap, ghost_room_init, border
    - Post-processing: mirror_map, insert_middle_column, add_TP_Points
    - Serialization: maze_to_json, maze_from_json
    - Benchmarks: maze_benchmarks
    - ASCII display: print_map
"""

import copy
import json
import random


# ---------------------------------------------------------------------------
# Data model
# ---------------------------------------------------------------------------

class Cell:
    """Represents a single cell in the labyrinth grid."""

    def __init__(self, x, y):
        self.x = x          # row index
        self.y = y          # column index
        self.visited = False
        self.walls = [True, True, True, True]  # [Up, Right, Down, Left]
        self.isTP = False
        self.TPCell = None
        self.isGhost = False


# ---------------------------------------------------------------------------
# Grid initialisation
# ---------------------------------------------------------------------------

def initMap(height, width):
    """Create an H×W grid of Cells and initialise the ghost room."""
    grid = [[Cell(r, c) for c in range(width)] for r in range(height)]
    return ghost_room_init(grid)


def ghost_room_init(grid):
    """Set up the ghost room in the middle-right of *grid*.
    Since the grid is half the final width, configuring cols W-2 and W-1
    on a single row will create a 1x3 room when mirrored:
    Left (W-2), Door (W-1), and Right (Mirrored W-2).
    """
    H = len(grid)
    W = len(grid[0]) if H else 0
    middleY = H // 2
    if H < 2 or W < 2:
        return grid
        
    # Left ghost cell (becomes Left boundary)
    grid[middleY][W - 2].visited = True
    grid[middleY][W - 2].walls = [True, False, True, True]
    grid[middleY][W - 2].isGhost = True
    
    # Right ghost cell (becomes the Center door cell)
    grid[middleY][W - 1].visited = True
    grid[middleY][W - 1].walls = [False, False, True, False]
    grid[middleY][W - 1].isGhost = True
    
    # Ensure symmetry for the door (the cell above must have its down wall open)
    grid[middleY - 1][W - 1].walls[2] = False
    
    return grid


def border(cell, grid):
    """Return True if *cell* lies on the border of the grid."""
    H = len(grid)
    W = len(grid[0]) if H else 0
    return cell.x == 0 or cell.x == H - 1 or cell.y == 0 or cell.y == W - 1


# ---------------------------------------------------------------------------
# Wall helpers
# ---------------------------------------------------------------------------

def remove_wall_between(c1, c2):
    """Remove walls between two adjacent cells *c1* and *c2*."""
    if c1.x > c2.x:      # c1 is below c2 → Up
        c1.walls[0] = False
        c2.walls[2] = False
    elif c1.x < c2.x:    # c1 is above c2 → Down
        c1.walls[2] = False
        c2.walls[0] = False
    elif c1.y < c2.y:    # c1 is left of c2 → Right
        c1.walls[1] = False
        c2.walls[3] = False
    elif c1.y > c2.y:    # c1 is right of c2 → Left
        c1.walls[3] = False
        c2.walls[1] = False


# ---------------------------------------------------------------------------
# Post-processing
# ---------------------------------------------------------------------------

def add_cycles(grid, p=0.08):
    """Randomly knock down walls to create extra cycles (loops)."""
    H = len(grid)
    W = len(grid[0]) if H else 0
    for r in range(H):
        for c in range(W):
            if c + 1 < W:
                cell, nei = grid[r][c], grid[r][c + 1]
                if not (cell.isGhost or nei.isGhost):
                    if cell.walls[1] and nei.walls[3] and random.random() < p:
                        cell.walls[1] = False
                        nei.walls[3] = False
            if r + 1 < H:
                cell, nei = grid[r][c], grid[r + 1][c]
                if not (cell.isGhost or nei.isGhost):
                    if cell.walls[2] and nei.walls[0] and random.random() < p:
                        cell.walls[2] = False
                        nei.walls[0] = False


def seal_borders(grid):
    """Ensure all outer boundary walls are closed.

    Post-processing steps (mirror, insert_middle_column, add_cycles) may
    inadvertently open walls on the grid perimeter.  This pass forces every
    border cell's outward-facing wall to True so nothing can escape.
    """
    H = len(grid)
    W = len(grid[0]) if H else 0
    for c in range(W):
        grid[0][c].walls[0] = True       # top row  → Up wall
        grid[H - 1][c].walls[2] = True   # bottom row → Down wall
    for r in range(H):
        grid[r][0].walls[3] = True       # left col → Left wall
        grid[r][W - 1].walls[1] = True   # right col → Right wall


def mirror_map(grid):
    """Mirror *grid* horizontally, producing a symmetric maze of double width."""
    H = len(grid)
    W = len(grid[0]) if H else 0
    newW = W * 2
    new = [[None for _ in range(newW)] for _ in range(H)]

    for r in range(H):
        for c in range(W):
            orig = grid[r][c]
            # Left half - copy
            left = Cell(r, c)
            left.visited = orig.visited
            left.walls = orig.walls.copy()
            left.isGhost = orig.isGhost
            new[r][c] = left

            # Right half - mirror (swap Left↔Right walls)
            nc = W + (W - 1 - c)
            right = Cell(r, nc)
            right.visited = orig.visited
            right.walls = [orig.walls[0], orig.walls[3],
                           orig.walls[2], orig.walls[1]]
            right.isGhost = orig.isGhost
            new[r][nc] = right

    # Remove dividing wall between left and right halves
    if W > 0:
        for r in range(H):
            new[r][W - 1].walls[1] = False
            new[r][W].walls[3] = False

    return new


def insert_middle_column(mapS, mapD):
    """Insert the last column of *mapS* into the centre of *mapD*."""
    insert_x = len(mapD[0]) // 2
    source_x = -1

    for y in range(len(mapD)):
        new_cell = copy.deepcopy(mapS[y][source_x])
        mapD[y].insert(insert_x, new_cell)
        
        # Sync the left neighboring cell (undoing the clear from mirror_map)
        mapD[y][insert_x - 1].walls[1] = new_cell.walls[3]
        
        # Make the inserted cell symmetric and sync the right neighboring cell
        new_cell.walls[1] = new_cell.walls[3]
        mapD[y][insert_x + 1].walls[3] = new_cell.walls[1]

    # Re-index every cell
    for r in range(len(mapD)):
        for c in range(len(mapD[0])):
            cell = mapD[r][c]
            if cell is not None:
                cell.x = r
                cell.y = c
    return mapD


def add_TP_Points(grid):
    """Place one pair of linked teleport points on border cells."""
    H = len(grid)
    W = len(grid[0]) if H else 0

    while True:
        r1, c1 = random.randint(0, H - 1), random.randint(0, W - 1)
        if not grid[r1][c1].isTP and not grid[r1][c1].isGhost and border(grid[r1][c1], grid):
            break

    while True:
        r2, c2 = random.randint(0, H - 1), random.randint(0, W - 1)
        if (r2, c2) != (r1, c1) and not grid[r2][c2].isTP and not grid[r2][c2].isGhost and border(grid[r2][c2], grid):
            break

    grid[r1][c1].isTP = True
    grid[r1][c1].TPCell = grid[r2][c2]
    grid[r2][c2].isTP = True
    grid[r2][c2].TPCell = grid[r1][c1]


# ---------------------------------------------------------------------------
# Serialisation
# ---------------------------------------------------------------------------

def maze_to_json(grid, steps=None):
    """Serialise *grid* to a JSON string.

    If *steps* is provided it must be a list of dicts describing each wall
    removal event, and will be included under the ``"steps"`` key.
    """
    H = len(grid)
    W = len(grid[0]) if H else 0
    rows = []
    for r in range(H):
        row = []
        for c in range(W):
            cell = grid[r][c]
            obj = {
                "x": r,
                "y": c,
                "walls": list(cell.walls),
                "isGhost": bool(getattr(cell, "isGhost", False)),
                "isTP": bool(getattr(cell, "isTP", False)),
                "TP": None,
            }
            if getattr(cell, "isTP", False) and getattr(cell, "TPCell", None) is not None:
                tp = cell.TPCell
                obj["TP"] = {"x": getattr(tp, "x", None),
                             "y": getattr(tp, "y", None)}
            row.append(obj)
        rows.append(row)

    out = {"height": H, "width": W, "rows": rows}
    if steps is not None:
        out["steps"] = steps
    return json.dumps(out)


def maze_from_json(source):
    """Deserialise a maze from a JSON string, dict, or filename."""
    if isinstance(source, str):
        try:
            data = json.loads(source)
        except Exception:
            with open(source, "r", encoding="utf-8") as f:
                data = json.load(f)
    elif isinstance(source, dict):
        data = source
    else:
        raise TypeError("source must be JSON string, dict, or filename")

    H = data.get("height")
    W = data.get("width")
    rows = data.get("rows")
    if H is None or W is None or rows is None:
        raise ValueError("Invalid maze JSON: missing height/width/rows")

    grid = [[Cell(r, c) for c in range(W)] for r in range(H)]
    for r in range(H):
        for c in range(W):
            obj = rows[r][c]
            cell = grid[r][c]
            cell.walls = list(obj.get("walls", [True, True, True, True]))
            cell.isGhost = bool(obj.get("isGhost", False))
            cell.isTP = bool(obj.get("isTP", False))
            cell.TPCell = None

    # Link teleport pairs
    for r in range(H):
        for c in range(W):
            tp = rows[r][c].get("TP")
            if tp:
                tx, ty = tp.get("x"), tp.get("y")
                if tx is not None and ty is not None and 0 <= tx < H and 0 <= ty < W:
                    grid[r][c].TPCell = grid[tx][ty]

    return grid


# ---------------------------------------------------------------------------
# Benchmarks
# ---------------------------------------------------------------------------

def maze_benchmarks(grid):
    """Return ``(num_cycles, num_hallways, hallway_lengths)``."""
    H = len(grid)
    W = len(grid[0]) if H else 0

    def idx(r, c):
        return r * W + c

    adj = {}
    for r in range(H):
        for c in range(W):
            cell = grid[r][c]
            if cell is None:
                continue
            v = idx(r, c)
            neighs = []
            if not cell.walls[0] and r - 1 >= 0:
                neighs.append(idx(r - 1, c))
            if not cell.walls[1] and c + 1 < W:
                neighs.append(idx(r, c + 1))
            if not cell.walls[2] and r + 1 < H:
                neighs.append(idx(r + 1, c))
            if not cell.walls[3] and c - 1 >= 0:
                neighs.append(idx(r, c - 1))
            adj[v] = neighs

    V = len(adj)
    E = sum(len(n) for n in adj.values()) // 2

    visited = set()
    components = 0
    for v in adj:
        if v in visited:
            continue
        components += 1
        stack = [v]
        while stack:
            u = stack.pop()
            if u in visited:
                continue
            visited.add(u)
            for w in adj.get(u, ()):
                if w not in visited:
                    stack.append(w)

    num_cycles = E - V + components

    degree2 = {v for v, n in adj.items() if len(n) == 2}
    visited2 = set()
    hallway_lengths = []
    for v in degree2:
        if v in visited2:
            continue
        size = 0
        stack = [v]
        while stack:
            u = stack.pop()
            if u in visited2:
                continue
            visited2.add(u)
            size += 1
            for w in adj.get(u, ()):
                if w in degree2 and w not in visited2:
                    stack.append(w)
        hallway_lengths.append(size)

    return num_cycles, len(hallway_lengths), hallway_lengths


# ---------------------------------------------------------------------------
# ASCII display
# ---------------------------------------------------------------------------

def print_map(grid):
    """Pretty-print *grid* to stdout (¥ = ghost, @ = teleport)."""
    h = len(grid)
    w = len(grid[0]) if h else 0
    for r in range(h):
        top = ""
        mid = ""
        for c in range(w):
            cell = grid[r][c]
            top += "+" + ("---" if cell.walls[0] else "   ")
            content = (
                " ¥ " if getattr(cell, "isGhost", False)
                else " @ " if getattr(cell, "isTP", False)
                else "   "
            )
            mid += ("|" if cell.walls[3] else " ") + content
        top += "+"
        mid += ("|" if grid[r][-1].walls[1] else " ")
        print(top)
        print(mid)
    if h:
        bottom = ""
        for c in range(w):
            bottom += "+" + ("---" if grid[-1][c].walls[2] else "   ")
        bottom += "+"
        print(bottom)
