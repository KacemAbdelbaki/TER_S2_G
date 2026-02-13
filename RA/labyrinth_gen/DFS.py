import copy
import random


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
    return ghost_room_init(map)


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
                " ¥ " if getattr(cell, "isGhost", False) else " @ " if getattr(cell, "isTP", False) else "   "
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


def ghost_room_init(map):
    middleY = HEIGHT // 2
    # map[y][x].walls = [up, right, down, left]
    map[middleY][WIDTH - 2].visited = True
    map[middleY][WIDTH - 2].walls = [True, False, True, True]
    map[middleY][WIDTH - 2].isGhost = True
    map[middleY][WIDTH - 1].visited = True
    map[middleY][WIDTH - 1].walls = [False, False, True, False]
    map[middleY][WIDTH - 1].isGhost = True
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


if __name__ == "__main__":
    HEIGHT = 5
    WIDTH = 6
    map = initMap(HEIGHT, WIDTH)

    # DFS
    map_dfs = [[copy.deepcopy(cell) for cell in row[:]] for row in map]
    # choose a start cell that's not inside the ghost room (ghost_room_init marks those as visited)
    start_r = random.randint(0, HEIGHT - 1)
    start_c = random.randint(0, WIDTH - 1)
    while map_dfs[start_r][start_c].visited:
        start_r = random.randint(0, HEIGHT - 1)
        start_c = random.randint(0, WIDTH - 1)
    DFS(map_dfs, start_r, start_c)
    map = [[copy.deepcopy(cell) for cell in row[:-1]] for row in map_dfs]
    map = mirror_map(map)
    map = insert_midle_column(map_dfs, map)
    for _ in range(2):
        add_TP_Points(map)
    print_map(map)
