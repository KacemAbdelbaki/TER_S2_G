from labyrinthWebService import app
import threading
import time
import urllib.request
import json
import sys

def run_server():
    app.run(port=5001, debug=False)

# Start server in a thread
thread = threading.Thread(target=run_server, daemon=True)
thread.start()
time.sleep(2) # Wait for server to start

def test_maze(algo, height, width):
    url = f"http://localhost:5001/labyrinth?algorithm={algo}&height={height}&width={width}"
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
            
            # Check for 3 ghost cells
            if len(ghost_cells) != 3:
                return False, f"Expected 3 ghosts, found {len(ghost_cells)}"
            
            # Check for exactly one exit
            exits = [i for i, c in enumerate(sorted(ghost_cells, key=lambda x: x['y'])) if c['walls'][0] == False]
            if len(exits) != 1 or exits[0] != 1:
                return False, f"Expected 1 exit at index 1, found {exits}"
            
            return True, "Passed"
    except Exception as e:
        return False, str(e)

print("Verifying Kruskal fix...")
success, msg = test_maze("kruskal", 5, 6)
if success:
    print("Kruskal 5x11: PASSED")
else:
    print(f"Kruskal 5x11: FAILED - {msg}")

success, msg = test_maze("kruskal", 10, 10)
if success:
    print("Kruskal 10x19: PASSED")
else:
    print(f"Kruskal 10x19: FAILED - {msg}")

print("Verifying DFS...")
success, msg = test_maze("dfs", 10, 10)
print(f"DFS 10x19: {'PASSED' if success else 'FAILED - ' + msg}")

sys.exit(0)
