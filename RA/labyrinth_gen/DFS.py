import random

class Cell:
    def __init__(self, x, y):
        self.x = x
        self.y = y
        self.visited = False
        self.walls = [True, True, True, True]

def initMap(height, width):
    grid = [[Cell(r, c) for c in range(width)] for r in range(height)]
    return grid

def print_map(grid):
    h = len(grid)
    w = len(grid[0]) if h else 0
    for r in range(h):
        top = ""
        mid = ""
        for c in range(w):
            cell = grid[r][c]
            top += "+" + ("---" if cell.walls[0] else "   ")
            mid += ("|" if cell.walls[3] else " ") + "   "
        top += "+\n"
        mid += ("|" if grid[r][-1].walls[1] else " ") + "\n"
        print(top + mid, end="")
    if h:
        bottom = ""
        for c in range(w):
            bottom += "+" + ("---" if grid[-1][c].walls[2] else "   ")
        bottom += "+\n"
        print(bottom, end="")

def border(cell):
    if cell.x == 0 or cell.x == HEIGHT - 1 or cell.y == 0 or cell.y == WIDTH - 1:
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
            right_cell.walls = [orig.walls[0], orig.walls[3], orig.walls[2], orig.walls[1]]
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


if __name__ == "__main__":
    HEIGHT = 5
    WIDTH = 6
    map = initMap(HEIGHT, WIDTH)
    # choose a random starting cell (anywhere inside the grid)
    start_r = random.randint(0, HEIGHT - 1)
    start_c = random.randint(0, WIDTH - 1)
    DFS(map, start_r, start_c)
    print_map(map)
    map = mirror_map(map)
    print_map(map)