"""
labyrinthWebService.py - Flask REST API for labyrinth generation.

Endpoints
---------
GET  /labyrinth          - Generate a maze (+ auto-save to MongoDB)
POST /labyrinth/rate     - Rate a maze by ID (0-5)
GET  /labyrinth/<id>     - Retrieve a specific maze by ID
GET  /mazes              - List all saved mazes (summary)
"""

import json

from flask import Flask, request, jsonify
from flask_cors import CORS
from bson.errors import InvalidId

from DFS import full_maze as dfs_full_maze
from kruskal import full_maze as kruskal_full_maze
from prim import full_maze as prim_full_maze
from Tetris import full_maze as tetris_full_maze
from common import maze_from_json, print_map
from db import save_maze, rate_maze, get_maze_by_id, list_mazes
from db import save_replay, get_replay_by_id, list_replays

app = Flask(__name__)
CORS(app)

ALGORITHMS = {
    "dfs": dfs_full_maze,
    "kruskal": kruskal_full_maze,
    "prim": prim_full_maze,
    "tetris": tetris_full_maze,
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
    maze_dict = json.loads(maze_json_str)

    # --- Persist to MongoDB -------------------------------------------------
    maze_id = save_maze(maze_dict, algo_name, height, width)
    maze_dict["id"] = maze_id  # None when DB is unavailable

    # Pretty-print to server console
    grid = maze_from_json(maze_json_str)
    print(f"\n[{algo_name.upper()}] {height}x{width}  (steps={record_steps})  id={maze_id}")
    print_map(grid)

    return jsonify(maze_dict)


@app.route("/labyrinth/rate", methods=["POST"])
def rate_labyrinth():
    """Rate a maze.  Expects JSON body: {"maze_id": "<id>", "rating": 0-5}"""
    data = request.get_json(silent=True)
    if not data:
        return jsonify({"error": "JSON body required"}), 400

    maze_id = data.get("maze_id")
    rating = data.get("rating")

    if not maze_id:
        return jsonify({"error": "maze_id is required"}), 400
    if rating is None or not isinstance(rating, (int, float)):
        return jsonify({"error": "rating must be a number (0-5)"}), 400
    if not (0 <= rating <= 5):
        return jsonify({"error": "rating must be between 0 and 5"}), 400

    try:
        result = rate_maze(maze_id, rating)
    except InvalidId:
        return jsonify({"error": f"Invalid maze ID format: '{maze_id}'"}), 400

    if result is None:
        return jsonify({"error": f"Maze '{maze_id}' not found"}), 404

    return jsonify(result)


@app.route("/labyrinth/<maze_id>", methods=["GET"])
def get_labyrinth_by_id(maze_id):
    """Retrieve a previously saved maze by its MongoDB ID."""
    doc = get_maze_by_id(maze_id)
    if doc is None:
        return jsonify({"error": f"Maze '{maze_id}' not found"}), 404

    return jsonify({
        "id": doc["_id"],
        "algorithm": doc.get("algorithm"),
        "height": doc.get("height"),
        "width": doc.get("width"),
        "rating": doc.get("rating"),
        "rating_count": doc.get("rating_count", 0),
        "created_at": str(doc.get("created_at", "")),
        **doc.get("maze", {}),
    })


@app.route("/mazes", methods=["GET"])
def get_all_mazes():
    """List recent saved mazes (metadata only, no grid data)."""
    limit = request.args.get("limit", 50, type=int)
    docs = list_mazes(limit)
    results = []
    for doc in docs:
        results.append({
            "id": doc["_id"],
            "algorithm": doc.get("algorithm"),
            "height": doc.get("height"),
            "width": doc.get("width"),
            "rating": doc.get("rating"),
            "rating_count": doc.get("rating_count", 0),
            "created_at": str(doc.get("created_at", "")),
        })
    return jsonify(results)


# ---------------------------------------------------------------------------
# Replay endpoints
# ---------------------------------------------------------------------------

@app.route("/replay", methods=["POST"])
def save_game_replay():
    """Save a game replay.  Expects a JSON body with replay data."""
    data = request.get_json(silent=True)
    if not data:
        return jsonify({"error": "JSON body required"}), 400

    maze_id = data.get("mazeId") or data.get("maze_id")
    if not maze_id or maze_id == "null":
        return jsonify({"error": "maze_id is required and cannot be empty"}), 400

    # Validate maze exists
    maze_doc = get_maze_by_id(maze_id)
    if maze_doc is None:
        print(f"Warning: Replay refers to non-existent maze_id: {maze_id}")
        return jsonify({"error": f"Maze '{maze_id}' not found in database. Replays must be linked to a valid maze."}), 404

    # Build replay document
    replay_doc = {
        "maze_id": maze_id,
        "algorithm": data.get("algorithm", ""),
        "maze_width": data.get("maze_width") or data.get("mazeWidth", 0),
        "maze_height": data.get("maze_height") or data.get("mazeHeight", 0),
        "player_type": data.get("player_type") or data.get("playerType", "human"),
        "bot_strategy": data.get("bot_strategy") or data.get("botStrategy"),
        "final_score": data.get("final_score") or data.get("finalScore", 0),
        "final_dots_eaten": data.get("final_dots_eaten") or data.get("finalDotsEaten", 0),
        "won": data.get("won") if "won" in data else data.get("won", False),
        "duration_seconds": data.get("duration_seconds") or data.get("durationSeconds", 0),
        "frame_count": data.get("frame_count") or data.get("frameCount", 0),
        "frames": data.get("frames", []),
        "inputs": data.get("inputs", []),
    }

    replay_id = save_replay(replay_doc)
    if replay_id is None:
        return jsonify({"error": "Failed to save replay (DB unavailable)"}), 503

    return jsonify({"id": replay_id, "maze_id": maze_id}), 201


@app.route("/replay/<replay_id>", methods=["GET"])
def get_game_replay(replay_id):
    """Retrieve a previously saved replay by its ID."""
    doc = get_replay_by_id(replay_id)
    if doc is None:
        return jsonify({"error": f"Replay '{replay_id}' not found"}), 404

    doc["id"] = doc.pop("_id")
    return jsonify(doc)


@app.route("/replays", methods=["GET"])
def get_all_replays():
    """List recent replays (metadata only, no frames/inputs)."""
    maze_id = request.args.get("maze_id")
    limit = request.args.get("limit", 50, type=int)
    docs = list_replays(maze_id=maze_id, limit=limit)
    results = []
    for doc in docs:
        results.append({
            "id": doc["_id"],
            "maze_id": doc.get("maze_id"),
            "algorithm": doc.get("algorithm"),
            "maze_width": doc.get("maze_width"),
            "maze_height": doc.get("maze_height"),
            "player_type": doc.get("player_type"),
            "bot_strategy": doc.get("bot_strategy"),
            "final_score": doc.get("final_score", 0),
            "won": doc.get("won", False),
            "duration_seconds": doc.get("duration_seconds", 0),
            "frame_count": doc.get("frame_count", 0),
            "created_at": str(doc.get("created_at", "")),
        })
    return jsonify(results)


if __name__ == "__main__":
    app.run(debug=False, host="0.0.0.0")
