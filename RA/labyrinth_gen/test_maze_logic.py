import urllib.request
import json

def test_maze(algo, height, width):
    url = f"http://localhost:5000/labyrinth?algorithm={algo}&height={height}&width={width}"
    try:
        with urllib.request.urlopen(url) as response:
            data = json.loads(response.read().decode())
            
            rows = data['rows']
            h = data['height']
            w = data['width']
            
            ghost_cells = []
            for r in range(h):
                for c in range(w):
                    if rows[r][c]['isGhost']:
                        ghost_cells.append(rows[r][c])
            
            print(f"Algorithm: {algo}, Size: {h}x{w}")
            print(f"  Ghost cells found: {len(ghost_cells)}")
            
            # Check for 3 ghost cells
            if len(ghost_cells) != 3:
                print(f"  [X] FAILED: Expected 3 ghost cells, found {len(ghost_cells)}")
                return False
            
            # Check if they are in the same row
            rs = [c['x'] for c in ghost_cells]
            if len(set(rs)) != 1:
                print(f"  [X] FAILED: Ghost cells are not in the same row: {rs}")
                return False
            
            # Sort by column
            ghost_cells.sort(key=lambda c: c['y'])
            cs = [c['y'] for c in ghost_cells]
            
            # Check if they are contiguous
            if cs[1] != cs[0] + 1 or cs[2] != cs[1] + 1:
                print(f"  [X] FAILED: Ghost cells are not contiguous: {cs}")
                return False
            
            # Check for exactly one exit (Up wall is False)
            exits = []
            for i, cell in enumerate(ghost_cells):
                if cell['walls'][0] == False:
                    exits.append(i)
            
            if len(exits) != 1:
                print(f"  [X] FAILED: Expected 1 exit (Up), found {len(exits)} at indices {exits}")
                return False
            
            if exits[0] != 1: # Middle cell should be the door
                print(f"  [X] FAILED: Exit should be in the middle cell (index 1), found at index {exits[0]}")
                return False

            # Check if the cell above the door has Down wall open
            door_cell = ghost_cells[1]
            dr, dc = door_cell['x'], door_cell['y']
            if dr > 0:
                above_cell = rows[dr-1][dc]
                if above_cell['walls'][2]:
                    print(f"  [X] FAILED: Cell above door ({dr-1}, {dc}) has Down wall closed")
                    # Note: Prim/Kruskal might not have visited the cell above yet? 
                    # No, the full_maze functions run the algorithms fully.
                    return False
            else:
                print(f"  [X] FAILED: Ghost room is on the top row, no room for exit")
                return False
                
            print(f"  [V] PASSED")
            return True
            
    except Exception as e:
        print(f"Error testing {algo}: {e}")
        return False

algos = ["dfs", "kruskal", "prim", "tetris"]
sizes = [(5, 6), (10, 10), (20, 15)]

all_passed = True
for algo in algos:
    for h, w in sizes:
        if not test_maze(algo, h, w):
            all_passed = False

if all_passed:
    print("\nALL TESTS PASSED!")
else:
    print("\nSOME TESTS FAILED.")
