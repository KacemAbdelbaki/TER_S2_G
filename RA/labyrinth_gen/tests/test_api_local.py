"""
test_api_local.py - Tests for the Flask HTTP API (local / test-client).

Uses Flask's built-in test client so no real server needs to be running.
Covers:
    - Successful requests with default and explicit parameters
    - Dimension validation (the "30×30 check")
    - Invalid parameter handling (400 errors)
    - Steps recording via query parameter
"""

import json
import pytest


# ═══════════════════════════════════════════════════════════════════════════
# Happy-path requests
# ═══════════════════════════════════════════════════════════════════════════

def test_default_parameters(flask_client):
    """GET /labyrinth with no params → 200 and valid JSON (default: dfs, 5×6)."""
    resp = flask_client.get("/labyrinth")
    assert resp.status_code == 200
    data = resp.get_json()
    assert data["height"] == 5
    # Width varies after mirroring, but rows/cells should be present
    assert len(data["rows"]) == 5
    assert len(data["rows"][0]) == data["width"]


@pytest.mark.parametrize("algo", ["dfs", "kruskal", "prim"])
def test_each_algorithm(flask_client, algo):
    """All three algorithm names must be accepted."""
    resp = flask_client.get(f"/labyrinth?algorithm={algo}&height=5&width=6")
    assert resp.status_code == 200
    data = resp.get_json()
    assert data["height"] == 5


@pytest.mark.parametrize("h,w", [(3, 4), (10, 12), (30, 30)])
def test_requested_dimensions(flask_client, h, w):
    """Response height must match requested height."""
    resp = flask_client.get(f"/labyrinth?height={h}&width={w}")
    assert resp.status_code == 200
    data = resp.get_json()
    assert data["height"] == h, (
        f"Requested height={h}, got {data['height']}"
    )


def test_steps_true(flask_client):
    """steps=true must include a non-empty steps list."""
    resp = flask_client.get("/labyrinth?steps=true")
    assert resp.status_code == 200
    data = resp.get_json()
    assert "steps" in data
    assert len(data["steps"]) > 0


def test_steps_false(flask_client):
    """steps=false must NOT include a steps key."""
    resp = flask_client.get("/labyrinth?steps=false")
    assert resp.status_code == 200
    data = resp.get_json()
    assert "steps" not in data


# ═══════════════════════════════════════════════════════════════════════════
# Error handling (400 responses)
# ═══════════════════════════════════════════════════════════════════════════

def test_unknown_algorithm(flask_client):
    """An unrecognized algorithm name must return 400."""
    resp = flask_client.get("/labyrinth?algorithm=bogus")
    assert resp.status_code == 400
    data = resp.get_json()
    assert "error" in data


def test_non_integer_height(flask_client):
    """Non-integer height must return 400."""
    resp = flask_client.get("/labyrinth?height=abc")
    assert resp.status_code == 400
    data = resp.get_json()
    assert "error" in data


def test_non_integer_width(flask_client):
    """Non-integer width must return 400."""
    resp = flask_client.get("/labyrinth?width=xyz")
    assert resp.status_code == 400
    data = resp.get_json()
    assert "error" in data


@pytest.mark.parametrize("h,w", [(0, 6), (2, 6), (51, 6), (5, 0), (5, 2), (5, 51)])
def test_out_of_range_dimensions(flask_client, h, w):
    """Heights/widths outside 3..50 must return 400."""
    resp = flask_client.get(f"/labyrinth?height={h}&width={w}")
    assert resp.status_code == 400
    data = resp.get_json()
    assert "error" in data


# ═══════════════════════════════════════════════════════════════════════════
# Response structure
# ═══════════════════════════════════════════════════════════════════════════

def test_response_contains_top_level_keys(flask_client):
    """Response JSON must have at least height, width, rows."""
    resp = flask_client.get("/labyrinth")
    data = resp.get_json()
    for key in ("height", "width", "rows"):
        assert key in data, f"Top-level key '{key}' missing from response"


def test_cells_are_well_formed(flask_client):
    """Spot-check that returned cells contain the expected fields."""
    resp = flask_client.get("/labyrinth?height=5&width=6")
    data = resp.get_json()
    cell = data["rows"][0][0]
    for field in ("x", "y", "walls", "isGhost", "isTP", "TP"):
        assert field in cell, f"Cell field '{field}' missing"
