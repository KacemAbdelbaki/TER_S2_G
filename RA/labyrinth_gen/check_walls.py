from DFS import full_maze
import json

def check_maze_walls(maze_json):
    data = json.loads(maze_json)
    rows = data['rows']
    H = data['height']
    W = data['width']
    
    mismatches = []
    
    for r in range(H):
        for c in range(W):
            cell = rows[r][c]
            # check UP
            if r > 0:
                if cell['walls'][0] != rows[r-1][c]['walls'][2]:
                    mismatches.append(f"Mismatch UP: ({r},{c}) holds {cell['walls'][0]} but ({r-1},{c}) holds {rows[r-1][c]['walls'][2]}")
            # check DOWN
            if r < H - 1:
                if cell['walls'][2] != rows[r+1][c]['walls'][0]:
                    mismatches.append(f"Mismatch DOWN: ({r},{c}) holds {cell['walls'][2]} but ({r+1},{c}) holds {rows[r+1][c]['walls'][0]}")
            # check LEFT
            if c > 0:
                if cell['walls'][3] != rows[r][c-1]['walls'][1]:
                    mismatches.append(f"Mismatch LEFT: ({r},{c}) holds {cell['walls'][3]} but ({r},{c-1}) holds {rows[r][c-1]['walls'][1]}")
            # check RIGHT
            if c < W - 1:
                if cell['walls'][1] != rows[r][c+1]['walls'][3]:
                    mismatches.append(f"Mismatch RIGHT: ({r},{c}) holds {cell['walls'][1]} but ({r},{c+1}) holds {rows[r][c+1]['walls'][3]}")
    return list(set(mismatches))

if __name__ == "__main__":
    for i in range(10):
        j = full_maze(15, 15, False)
        m = check_maze_walls(j)
        if m:
            print("FOUND MISMATCHES on iteration", i)
            for x in m: print(" ", x)
    print("Done checking.")
