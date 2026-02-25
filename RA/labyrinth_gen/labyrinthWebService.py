"""
labyrinthWebService.py - Flask REST API for labyrinth generation.

Endpoints
---------
GET /labyrinth
    Query parameters:
        algorithm  - dfs (default), kruskal, prim
        height     - int, 3..50 (default 5)
        width      - int, 3..50 (default 6)
        steps      - true/false (default false) - include generation steps
"""

import json

from flask import Flask, request, jsonify
from flask_cors import CORS

from DFS import full_maze as dfs_full_maze
from kruskal import full_maze as kruskal_full_maze
from prim import full_maze as prim_full_maze
from common import maze_from_json, print_map

app = Flask(__name__)
CORS(app)

ALGORITHMS = {
    "dfs": dfs_full_maze,
    "kruskal": kruskal_full_maze,
    "prim": prim_full_maze,
}


@app.route("/labyrinth", methods=["GET"])
def get_labyrinth():
    # --- Parse parameters ---------------------------------------------------
    algo_name = request.args.get("algorithm", "dfs").lower()
    if algo_name not in ALGORITHMS:
        return jsonify({"error": f"Unknown algorithm '{algo_name}'. "
                        f"Choose from: {', '.join(ALGORITHMS)}"}), 400

    try:
        height = int(request.args.get("height", 5))
        width = int(request.args.get("width", 6))
    except ValueError:
        return jsonify({"error": "height and width must be integers"}), 400

    if not (3 <= height <= 50) or not (3 <= width <= 50):
        return jsonify({"error": "height and width must be between 3 and 50"}), 400

    record_steps = request.args.get("steps", "false").lower() in ("true", "1", "yes")

    # --- Generate -----------------------------------------------------------
    generate = ALGORITHMS[algo_name]
    maze_json_str = generate(height, width, record_steps=record_steps)

    # Pretty-print to server console
    grid = maze_from_json(maze_json_str)
    print(f"\n[{algo_name.upper()}] {height}x{width}  (steps={record_steps})")
    print_map(grid)

    return json.loads(maze_json_str)


if __name__ == "__main__":
    app.run(debug=False, host="0.0.0.0")
