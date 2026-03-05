"""
conftest.py - Shared fixtures for all maze tests.

Adds the labyrinth_gen directory to sys.path so that
``import common``, ``import DFS``, etc. work correctly.
"""

import sys
import pathlib

# Insert the labyrinth_gen package dir at the front of sys.path
GEN_DIR = pathlib.Path(__file__).resolve().parent.parent
if str(GEN_DIR) not in sys.path:
    sys.path.insert(0, str(GEN_DIR))

import pytest
import json

from DFS import full_maze as dfs_full_maze
from kruskal import full_maze as kruskal_full_maze
from prim import full_maze as prim_full_maze
from common import maze_from_json


# ---- Fixtures ------------------------------------------------------------

ALGORITHMS = {
    "dfs": dfs_full_maze,
    "kruskal": kruskal_full_maze,
    "prim": prim_full_maze,
}


@pytest.fixture(params=["dfs", "kruskal", "prim"])
def algo_name(request):
    """Yields each algorithm name in turn."""
    return request.param


@pytest.fixture
def generate(algo_name):
    """Return the ``full_maze`` callable for the current algorithm."""
    return ALGORITHMS[algo_name]


@pytest.fixture
def maze_json(generate):
    """Generate a 10x12 maze and return the parsed JSON dict."""
    raw = generate(10, 12, record_steps=False)
    return json.loads(raw)


@pytest.fixture
def maze_grid(maze_json):
    """Return the Cell grid deserialized from the JSON."""
    return maze_from_json(maze_json)


@pytest.fixture
def flask_client():
    """Provide a Flask test client for API tests."""
    from labyrinthWebService import app
    app.config["TESTING"] = True
    with app.test_client() as client:
        yield client
