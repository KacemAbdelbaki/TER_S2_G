import copy
import random
import json
import argparse


class Cell:
    def __init__(self, x, y):
        self.x = x
        self.y = y
        self.visited = False
        self.walls = [True, True, True, True]
        self.isTP = False
        self.TPCell = None
        self.isGhost = False


def initMap(height, width):
    map = [[Cell(r, c) for c in range(width)] for r in range(height)]
    return ghost_room_init(map, height, width)


def print_map(grid):
    h = len(grid)
    w = len(grid[0]) if h else 0
    for r in range(h):
        top = ""
        mid = ""
        for c in range(w):
            cell = grid[r][c]
            top += "+" + ("---" if cell.walls[0] else "   ")
            content = (
                " ¥ "
                if getattr(cell, "isGhost", False)
                else " @ " if getattr(cell, "isTP", False) else "   "
            )

            mid += ("|" if cell.walls[3] else " ") + content
        top += "+\n"
        mid += ("|" if grid[r][-1].walls[1] else " ") + "\n"
        print(top + mid, end="")
    if h:
        bottom = ""
        for c in range(w):
            bottom += "+" + ("---" if grid[-1][c].walls[2] else "   ")
        bottom += "+\n"
        print(bottom, end="")


def border(cell, grid):
    H = len(grid)
    W = len(grid[0]) if H else 0
    if cell.x == 0 or cell.x == H - 1 or cell.y == 0 or cell.y == W - 1:
        return True
    return False


def DFS(grid, r, c):
    H = len(grid)
    W = len(grid[0]) if H else 0
    cell = grid[r][c]
    cell.visited = True

    dirs = [(-1, 0, 0, 2), (0, 1, 1, 3), (1, 0, 2, 0), (0, -1, 3, 1)]
    random.shuffle(dirs)

    for dr, dc, w_cur, w_nei in dirs:
        nr, nc = r + dr, c + dc
        if 0 <= nr < H and 0 <= nc < W and not grid[nr][nc].visited:
            # remove wall between cell and neighbor
            cell.walls[w_cur] = False
            grid[nr][nc].walls[w_nei] = False
            DFS(grid, nr, nc)


def Tetris(grid, r, c, branch_bias=0.85):
    """Generate a maze using the classic "Tetris" / active-cells approach.

    This is the standard "growing list of active cells" maze generator:
    - Start from (r,c)
    - Maintain a list of active cells
    - Repeatedly pick a random active cell and carve to a random unvisited neighbor
    - If a cell has no unvisited neighbors, remove it from the active list

    Cells already marked visited (e.g. the ghost room) are treated as blocked.
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
            for (ar, ac) in active:
                if len(unvisited_neighbors(ar, ac)) >= 2:
                    branch_candidates.append((ar, ac))

        if branch_candidates and random.random() < branch_bias:
            cr, cc = random.choice(branch_candidates)
            # remove first occurrence to avoid duplicates; we'll re-add if still active
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

        # keep current cell active (it may have more neighbors later)
        active.append((cr, cc))
        # and add the newly carved cell
        active.append((nr, nc))


def add_cycles(grid, p=0.08):
    H = len(grid)
    W = len(grid[0]) if H else 0
    for r in range(H):
        for c in range(W):
            # right neighbor
            if c + 1 < W:
                cell = grid[r][c]
                nei = grid[r][c + 1]
                # if wall exists between them, randomly knock it down
                if cell.walls[1] and nei.walls[3] and random.random() < p:
                    cell.walls[1] = False
                    nei.walls[3] = False

            # down neighbor
            if r + 1 < H:
                cell = grid[r][c]
                nei = grid[r + 1][c]
                if cell.walls[2] and nei.walls[0] and random.random() < p:
                    cell.walls[2] = False
                    nei.walls[0] = False


def mirror_map(grid):
    H = len(grid)
    W = len(grid[0]) if H else 0
    newW = W * 2
    new = [[None for _ in range(newW)] for _ in range(H)]

    for r in range(H):
        for c in range(W):
            orig = grid[r][c]
            # copy original cell to the left half
            left_cell = Cell(r, c)
            left_cell.visited = orig.visited
            left_cell.walls = orig.walls.copy()
            new[r][c] = left_cell

            # mirrored cell goes to right half at index W + (W-1-c)
            new_c = W + (W - 1 - c)
            right_cell = Cell(r, new_c)
            right_cell.visited = orig.visited
            right_cell.walls = [
                orig.walls[0],
                orig.walls[3],
                orig.walls[2],
                orig.walls[1],
            ]
            new[r][new_c] = right_cell

    # remove the dividing wall between the left and right halves
    if W > 0:
        for r in range(H):
            left_cell = new[r][W - 1]
            right_cell = new[r][W]
            if left_cell is not None and right_cell is not None:
                left_cell.walls[1] = False
                right_cell.walls[3] = False

    return new


def ghost_room_init(map, height, width):
    middleY = height // 2
    # map[y][x].walls = [up, right, down, left]
    map[middleY][width - 2].visited = True
    map[middleY][width - 2].walls = [True, False, True, True]
    map[middleY][width - 2].isGhost = True
    map[middleY][width - 1].visited = True
    map[middleY][width - 1].walls = [False, False, True, False]
    map[middleY][width - 1].isGhost = True
    return map


def insert_midle_column(mapS, mapD):
    insert_x = len(mapD[0]) // 2
    source_x = -1

    for y in range(len(mapD)):
        new_cell = copy.deepcopy(mapS[y][source_x])
        mapD[y].insert(insert_x, new_cell)
        mapD[y][insert_x].walls[1] = mapD[y][insert_x].walls[3]
        mapD[y][insert_x + 1].walls[3] = mapD[y][insert_x].walls[1]

    # update cell.x and cell.y
    for r in range(len(mapD)):
        for c in range(len(mapD[0])):
            cell = mapD[r][c]
            if cell is not None:
                cell.x = r
                cell.y = c

    return mapD


def add_TP_Points(map):
    H = len(map)
    W = len(map[0]) if H else 0

    while True:
        r1, c1 = random.randint(0, H - 1), random.randint(0, W - 1)
        if not map[r1][c1].isTP and border(map[r1][c1], map):
            cell1 = map[r1][c1]
            break

    while True:
        r2, c2 = random.randint(0, H - 1), random.randint(0, W - 1)
        if (r2 != r1 or c2 != c1) and not map[r2][c2].isTP and border(map[r2][c2], map):
            cell2 = map[r2][c2]
            break

    cell1.isTP = True
    cell1.TPCell = cell2
    cell2.isTP = True
    cell2.TPCell = cell1


def pick_random_unvisited_cell(grid):
    H = len(grid)
    W = len(grid[0]) if H else 0
    candidates = [(r, c) for r in range(H) for c in range(W) if not grid[r][c].visited]
    if not candidates:
        raise ValueError("No unvisited cells available to start generation")
    return random.choice(candidates)


def maze_benchmarks(grid):
    """Return (num_cycles, num_hallways, hallway_lengths).

    - num_cycles: cyclomatic number = E - V + C
    - num_hallways: count of connected components formed by degree-2 cells
    - hallway_lengths: list of sizes (number of cells) for each hallway
    """
    H = len(grid)
    W = len(grid[0]) if H else 0

    def idx(r, c):
        return r * W + c

    # build adjacency for every non-None cell
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

    # connected components over adj keys
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

    # hallway detection: nodes with degree == 2
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

    num_hallways = len(hallway_lengths)
    return num_cycles, num_hallways, hallway_lengths


def maze_to_json(grid):
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
            if (
                getattr(cell, "isTP", False)
                and getattr(cell, "TPCell", None) is not None
            ):
                tp = cell.TPCell
                # TPCell may be a reference to an object with x,y set
                obj["TP"] = {"x": getattr(tp, "x", None), "y": getattr(tp, "y", None)}
            row.append(obj)
        rows.append(row)
    out = {"height": H, "width": W, "rows": rows}
    return json.dumps(out)


def maze_from_json(source):
    # load JSON data
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

    # create empty grid
    grid = [[Cell(r, c) for c in range(W)] for r in range(H)]

    # populate properties
    for r in range(H):
        for c in range(W):
            obj = rows[r][c]
            cell = grid[r][c]
            cell.walls = list(obj.get("walls", [True, True, True, True]))
            cell.isGhost = bool(obj.get("isGhost", False))
            cell.isTP = bool(obj.get("isTP", False))
            # TPCell will be linked after all cells are created
            cell.TPCell = None

    # link teleport pairs if present
    for r in range(H):
        for c in range(W):
            obj = rows[r][c]
            tp = obj.get("TP")
            if tp:
                tx, ty = tp.get("x"), tp.get("y")
                if tx is not None and ty is not None:
                    # guard indices
                    if 0 <= tx < H and 0 <= ty < W:
                        grid[r][c].TPCell = grid[tx][ty]

    return grid


def full_maze(height, width):
    map = initMap(height, width)

    # DFS
    map_dfs = [[copy.deepcopy(cell) for cell in row[:]] for row in map]
    # choose a start cell that's not inside the ghost room
    start_r, start_c = pick_random_unvisited_cell(map_dfs)
    Tetris(map_dfs, start_r, start_c)
    # create random cycles (destroy random walls)
    add_cycles(map_dfs, p=0.25)
    map = [[copy.deepcopy(cell) for cell in row[:-1]] for row in map_dfs]
    map = mirror_map(map)
    map = insert_midle_column(map_dfs, map)
    for _ in range(2):
        add_TP_Points(map)

    j = maze_to_json(map)
    return j


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Maze generator")
    parser.add_argument("--height", type=int, default=10, help="Grid height")
    parser.add_argument("--width", type=int, default=12, help="Grid width")
    args = parser.parse_args()

    HEIGHT = args.height
    WIDTH = args.width
    if HEIGHT < 1 or WIDTH < 2:
        raise SystemExit("height must be >= 1 and width must be >= 2")
    if HEIGHT * WIDTH <= 2:
        raise SystemExit("grid is too small; need more than 2 cells")

    map = initMap(HEIGHT, WIDTH)

    # DFS
    map_dfs = [[copy.deepcopy(cell) for cell in row[:]] for row in map]
    # choose a start cell that's not inside the ghost room
    start_r, start_c = pick_random_unvisited_cell(map_dfs)
    Tetris(map_dfs, start_r, start_c)
    # create random cycles (destroy random walls)
    add_cycles(map_dfs, p=0.25)
    map = [[copy.deepcopy(cell) for cell in row[:-1]] for row in map_dfs]
    map = mirror_map(map)
    map = insert_midle_column(map_dfs, map)
    for _ in range(2):
        add_TP_Points(map)

    cycles, num_hallways, hallway_lengths = maze_benchmarks(map)
    print("cycles:", cycles)
    print("hallways:", num_hallways)

    j = maze_to_json(map)
    print_map(maze_from_json(j))