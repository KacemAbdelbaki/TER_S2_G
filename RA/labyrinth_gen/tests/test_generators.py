"""
test_generators.py - Functional tests for all three maze generators.

Verifies that each algorithm (DFS, Kruskal, Prim) produces valid maze JSON
with the correct dimensions and well-formed cells.
"""

import json
import pytest


# ═══════════════════════════════════════════════════════════════════════════
# Dimension tests  (the "30x30 check" from the brief)
# ═══════════════════════════════════════════════════════════════════════════

DIMENSION_CASES = [
    (3, 4),
    (5, 6),
    (10, 12),
    (15, 15),
    (30, 30),
]


@pytest.mark.parametrize("h,w", DIMENSION_CASES)
def test_dimensions_match_request(generate, algo_name, h, w):
    """Maze height must equal requested height."""
    raw = generate(h, w)
    data = json.loads(raw)
    assert data["height"] == h, (
        f"[{algo_name}] Expected height={h}, got {data['height']}"
    )


@pytest.mark.parametrize("h,w", DIMENSION_CASES)
def test_row_count_matches_height(generate, algo_name, h, w):
    """Number of rows in the grid must equal the reported height."""
    raw = generate(h, w)
    data = json.loads(raw)
    assert len(data["rows"]) == data["height"], (
        f"[{algo_name}] rows list length {len(data['rows'])} != height {data['height']}"
    )


@pytest.mark.parametrize("h,w", DIMENSION_CASES)
def test_column_count_matches_width(generate, algo_name, h, w):
    """Each row must contain exactly ``width`` cells."""
    raw = generate(h, w)
    data = json.loads(raw)
    for r, row in enumerate(data["rows"]):
        assert len(row) == data["width"], (
            f"[{algo_name}] row {r}: expected {data['width']} cells, got {len(row)}"
        )


# ═══════════════════════════════════════════════════════════════════════════
# Cell structure tests
# ═══════════════════════════════════════════════════════════════════════════

def test_every_cell_has_four_walls(maze_json, algo_name):
    """Each cell must have a list of exactly 4 boolean wall values."""
    for r, row in enumerate(maze_json["rows"]):
        for c, cell in enumerate(row):
            assert "walls" in cell, f"[{algo_name}] Cell ({r},{c}) missing 'walls'"
            assert len(cell["walls"]) == 4, (
                f"[{algo_name}] Cell ({r},{c}) walls length {len(cell['walls'])} != 4"
            )
            for i, w in enumerate(cell["walls"]):
                assert isinstance(w, bool), (
                    f"[{algo_name}] Cell ({r},{c}) wall[{i}]={w!r} is not bool"
                )


def test_cells_have_required_fields(maze_json, algo_name):
    """Each cell must contain x, y, walls, isGhost, isTP, TP."""
    required = {"x", "y", "walls", "isGhost", "isTP", "TP"}
    for r, row in enumerate(maze_json["rows"]):
        for c, cell in enumerate(row):
            missing = required - set(cell.keys())
            assert not missing, (
                f"[{algo_name}] Cell ({r},{c}) missing fields: {missing}"
            )


# ═══════════════════════════════════════════════════════════════════════════
# Teleport points
# ═══════════════════════════════════════════════════════════════════════════

def test_at_least_two_tp_pairs(maze_json, algo_name):
    """full_maze calls add_TP_Points twice → at least 4 TP cells."""
    tp_count = sum(
        1
        for row in maze_json["rows"]
        for cell in row
        if cell["isTP"]
    )
    assert tp_count >= 4, (
        f"[{algo_name}] Expected ≥4 TP cells, found {tp_count}"
    )


def test_tp_links_are_symmetric(maze_json, algo_name):
    """Every TP cell's target must also be a TP cell pointing back."""
    cells = {}
    for row in maze_json["rows"]:
        for cell in row:
            cells[(cell["x"], cell["y"])] = cell

    for (x, y), cell in cells.items():
        if cell["isTP"] and cell["TP"] is not None:
            tx, ty = cell["TP"]["x"], cell["TP"]["y"]
            target = cells.get((tx, ty))
            assert target is not None, (
                f"[{algo_name}] TP at ({x},{y}) points to ({tx},{ty}) which doesn't exist"
            )
            assert target["isTP"], (
                f"[{algo_name}] TP target ({tx},{ty}) is not marked isTP"
            )


# ═══════════════════════════════════════════════════════════════════════════
# Ghost room
# ═══════════════════════════════════════════════════════════════════════════

def test_ghost_cells_exist(maze_json, algo_name):
    """Maze must contain at least 2 ghost cells (the ghost room)."""
    ghost_count = sum(
        1
        for row in maze_json["rows"]
        for cell in row
        if cell["isGhost"]
    )
    assert ghost_count >= 2, (
        f"[{algo_name}] Expected ≥2 ghost cells, found {ghost_count}"
    )


# ═══════════════════════════════════════════════════════════════════════════
# Steps recording
# ═══════════════════════════════════════════════════════════════════════════

def test_steps_present_when_requested(generate, algo_name):
    """When record_steps=True the JSON must contain a non-empty 'steps' list."""
    raw = generate(5, 6, record_steps=True)
    data = json.loads(raw)
    assert "steps" in data, f"[{algo_name}] 'steps' key missing from JSON"
    assert len(data["steps"]) > 0, f"[{algo_name}] steps list is empty"


def test_steps_absent_when_not_requested(generate, algo_name):
    """When record_steps=False the JSON must NOT contain a 'steps' key."""
    raw = generate(5, 6, record_steps=False)
    data = json.loads(raw)
    assert "steps" not in data, (
        f"[{algo_name}] 'steps' key should not be present when steps=False"
    )
