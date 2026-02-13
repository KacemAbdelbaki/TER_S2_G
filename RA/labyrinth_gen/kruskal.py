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


class DisjointSet:
    def __init__(self, n):
        self.parent = list(range(n))

    def find(self, i):
        if self.parent[i] == i:
            return i
        self.parent[i] = self.find(self.parent[i])
        return self.parent[i]

    def union(self, i, j):
        root_i = self.find(i)
        root_j = self.find(j)
        if root_i != root_j:
            self.parent[root_i] = root_j
            return True
        return False


def kruskal_algorithm(grid):
    H = len(grid)
    W = len(grid[0])
    ds = DisjointSet(H * W)

    walls = []
    for r in range(H):
        for c in range(W):
            # Check Right neighbor
            if c + 1 < W:
                walls.append(
                    ((r, c), (r, c + 1), 1, 3)
                )  # (cell1, cell2, wall_idx1, wall_idx2)
            # Check Down neighbor
            if r + 1 < H:
                walls.append(((r, c), (r + 1, c), 2, 0))

    for r in range(H):
        for c in range(W):
            if grid[r][c].visited:
                # Find adjacent visited cells and union them
                for dr, dc in [(0, 1), (1, 0)]:
                    nr, nc = r + dr, c + dc
                    if 0 <= nr < H and 0 <= nc < W and grid[nr][nc].visited:
                        ds.union(r * W + c, nr * W + nc)

    random.shuffle(walls)

    for (r1, c1), (r2, c2), w1, w2 in walls:
        set1 = r1 * W + c1
        set2 = r2 * W + c2

        if ds.union(set1, set2):
            grid[r1][c1].walls[w1] = False
            grid[r2][c2].walls[w2] = False
            grid[r1][c1].visited = True
            grid[r2][c2].visited = True


def mirror_map(grid):
    H, W = len(grid), len(grid[0])
    newW = W * 2
    new = [[None for _ in range(newW)] for _ in range(H)]
    for r in range(H):
        for c in range(W):
            orig = grid[r][c]
            left_cell = Cell(r, c)
            left_cell.walls = orig.walls.copy()
            left_cell.isGhost = orig.isGhost
            new[r][c] = left_cell

            new_c = W + (W - 1 - c)
            right_cell = Cell(r, new_c)
            right_cell.walls = [
                orig.walls[0],
                orig.walls[3],
                orig.walls[2],
                orig.walls[1],
            ]
            right_cell.isGhost = orig.isGhost
            new[r][new_c] = right_cell

    # Connect halves
    for r in range(H):
        new[r][W - 1].walls[1] = False
        new[r][W].walls[3] = False
    return new


def print_map(grid):
    h, w = len(grid), len(grid[0])
    for r in range(h):
        top, mid = "", ""
        for c in range(w):
            cell = grid[r][c]
            top += "+" + ("---" if cell.walls[0] else "   ")
            content = " ¥ " if cell.isGhost else "   "
            mid += ("|" if cell.walls[3] else " ") + content
        print(top + "+")
        print(mid + ("|" if grid[r][-1].walls[1] else " "))
    print("+" + "---+" * w)


if __name__ == "__main__":
    HEIGHT, WIDTH = 5, 6
    grid = [[Cell(r, c) for c in range(WIDTH)] for r in range(HEIGHT)]
    middleY = HEIGHT // 2
    grid[middleY][WIDTH - 2].visited = True
    grid[middleY][WIDTH - 2].isGhost = True
    grid[middleY][WIDTH - 1].visited = True
    grid[middleY][WIDTH - 1].isGhost = True

    kruskal_algorithm(grid)

    final_map = mirror_map(grid)
    print_map(final_map)
