import copy
import random


class Cell:
    def __init__(self, x, y):
        self.x = x
        self.y = y
        self.visited = False
        self.walls = [True, True, True, True]  # [Up, Right, Down, Left]
        self.isTP = False
        self.TPCell = None
        self.isGhost = False


def initMap(height, width):
    grid = [[Cell(r, c) for c in range(width)] for r in range(height)]
    return ghost_room_init(grid)


def ghost_room_init(grid):
    H = len(grid)
    W = len(grid[0])
    middleY = H // 2
    # Mark ghost room as visited so Prim's grows around/from it
    grid[middleY][W - 2].visited = True
    grid[middleY][W - 2].walls = [True, False, True, True]
    grid[middleY][W - 2].isGhost = True
    grid[middleY][W - 1].visited = True
    grid[middleY][W - 1].walls = [False, False, True, False]
    grid[middleY][W - 1].isGhost = True
    return grid


def remove_wall_between(c1, c2):
    """Removes walls between two adjacent cells."""
    if c1.x > c2.x:  # c1 is below c2 (Up)
        c1.walls[0] = False
        c2.walls[2] = False
    elif c1.x < c2.x:  # c1 is above c2 (Down)
        c1.walls[2] = False
        c2.walls[0] = False
    elif c1.y < c2.y:  # c1 is left of c2 (Right)
        c1.walls[1] = False
        c2.walls[3] = False
    elif c1.y > c2.y:  # c1 is right of c2 (Left)
        c1.walls[3] = False
        c2.walls[1] = False


def prim_algorithm(grid, start_r, start_c):
    H = len(grid)
    W = len(grid[0])

    grid[start_r][start_c].visited = True

    # Frontier stores (target_r, target_c, parent_r, parent_c)
    frontier = []

    def add_neighbors_to_frontier(r, c):
        for dr, dc in [(-1, 0), (0, 1), (1, 0), (0, -1)]:
            nr, nc = r + dr, c + dc
            if 0 <= nr < H and 0 <= nc < W and not grid[nr][nc].visited:
                frontier.append((nr, nc, r, c))

    add_neighbors_to_frontier(start_r, start_c)

    while frontier:
        idx = random.randint(0, len(frontier) - 1)
        nr, nc, pr, pc = frontier.pop(idx)

        if not grid[nr][nc].visited:
            grid[nr][nc].visited = True
            remove_wall_between(grid[pr][pc], grid[nr][nc])
            add_neighbors_to_frontier(nr, nc)


def add_cycles(grid, chance=0.1):
    H, W = len(grid), len(grid[0])
    for r in range(1, H - 1):
        for c in range(1, W - 1):
            if random.random() < chance:
                # Knock down a random wall to create a cycle
                dir_idx = random.randint(0, 3)
                dr, dc = [(-1, 0), (0, 1), (1, 0), (0, -1)][dir_idx]
                nr, nc = r + dr, c + dc
                remove_wall_between(grid[r][c], grid[nr][nc])


def mirror_map(grid):
    H = len(grid)
    W = len(grid[0])
    newW = W * 2
    new = [[None for _ in range(newW)] for _ in range(H)]

    for r in range(H):
        for c in range(W):
            orig = grid[r][c]
            left_cell = Cell(r, c)
            left_cell.visited = orig.visited
            left_cell.walls = orig.walls.copy()
            left_cell.isGhost = orig.isGhost
            new[r][c] = left_cell

            new_c = W + (W - 1 - c)
            right_cell = Cell(r, new_c)
            right_cell.visited = orig.visited
            right_cell.walls = [
                orig.walls[0],
                orig.walls[3],
                orig.walls[2],
                orig.walls[1],
            ]
            right_cell.isGhost = orig.isGhost
            new[r][new_c] = right_cell

    for r in range(H):
        new[r][W - 1].walls[1] = False
        new[r][W].walls[3] = False
    return new


def print_map(grid):
    h = len(grid)
    w = len(grid[0]) if h else 0
    for r in range(h):
        top, mid = "", ""
        for c in range(w):
            cell = grid[r][c]
            top += "+" + ("---" if cell.walls[0] else "   ")
            content = " ¥ " if cell.isGhost else " @ " if cell.isTP else "   "
            mid += ("|" if cell.walls[3] else " ") + content
        print(top + "+")
        print(mid + ("|" if grid[r][-1].walls[1] else " "))
    print("+" + "---+" * w)


if __name__ == "__main__":
    HEIGHT, WIDTH = 5, 6
    # Initialize with Ghost Room
    map_data = initMap(HEIGHT, WIDTH)

    # Run Prim's instead of DFS
    start_r, start_c = 0, 0
    while map_data[start_r][start_c].visited:  # Find non-ghost cell
        start_r, start_c = random.randint(0, HEIGHT - 1), random.randint(0, WIDTH - 1)

    prim_algorithm(map_data, start_r, start_c)

    # Optional: Add cycles for a less 'perfect' maze
    add_cycles(map_data, chance=0.05)

    # Apply Mirroring and Formatting
    final_map = mirror_map(map_data)
    print_map(final_map)
