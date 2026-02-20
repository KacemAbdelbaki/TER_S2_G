from DFS import full_maze, maze_to_json, maze_from_json, print_map
from flask import request, jsonify
from flask import Flask
import json


app = Flask(__name__)

@app.route("/labyrinth", methods=["GET"])
def get_labyrinth():
    maze = full_maze(5, 6)
    grid = maze_from_json(maze)
    print_map(grid)
    return json.loads(maze)


if __name__ == "__main__":
    app.run(debug=False, host="0.0.0.0")
