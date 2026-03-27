"""
test_rating_api.py - Tests for the rating / retrieval / listing endpoints.

All MongoDB operations are mocked so no real database is needed.
"""

import json
import pytest
from unittest.mock import patch, MagicMock


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

@pytest.fixture
def client():
    """Flask test client with all db functions mocked."""
    with patch("labyrinthWebService.save_maze", return_value="abc123"):
        from labyrinthWebService import app
        app.config["TESTING"] = True
        with app.test_client() as c:
            yield c


# ---------------------------------------------------------------------------
# POST /labyrinth/rate
# ---------------------------------------------------------------------------

class TestRateEndpoint:

    def test_rate_success(self, client):
        fake_result = {"maze_id": "abc123", "new_rating": 4.0, "total_votes": 1}
        with patch("labyrinthWebService.rate_maze", return_value=fake_result):
            resp = client.post(
                "/labyrinth/rate",
                data=json.dumps({"maze_id": "abc123", "rating": 4}),
                content_type="application/json",
            )
        assert resp.status_code == 200
        data = resp.get_json()
        assert data["new_rating"] == 4.0
        assert data["total_votes"] == 1

    def test_rate_missing_body(self, client):
        resp = client.post("/labyrinth/rate")
        assert resp.status_code == 400

    def test_rate_missing_maze_id(self, client):
        resp = client.post(
            "/labyrinth/rate",
            data=json.dumps({"rating": 3}),
            content_type="application/json",
        )
        assert resp.status_code == 400

    def test_rate_missing_rating(self, client):
        resp = client.post(
            "/labyrinth/rate",
            data=json.dumps({"maze_id": "abc123"}),
            content_type="application/json",
        )
        assert resp.status_code == 400

    def test_rate_out_of_range(self, client):
        resp = client.post(
            "/labyrinth/rate",
            data=json.dumps({"maze_id": "abc123", "rating": 7}),
            content_type="application/json",
        )
        assert resp.status_code == 400

    def test_rate_negative(self, client):
        resp = client.post(
            "/labyrinth/rate",
            data=json.dumps({"maze_id": "abc123", "rating": -1}),
            content_type="application/json",
        )
        assert resp.status_code == 400

    def test_rate_not_found(self, client):
        with patch("labyrinthWebService.rate_maze", return_value=None):
            resp = client.post(
                "/labyrinth/rate",
                data=json.dumps({"maze_id": "abc123", "rating": 3}),
                content_type="application/json",
            )
        assert resp.status_code == 404

    def test_rate_invalid_id_format(self, client):
        from bson.errors import InvalidId
        with patch("labyrinthWebService.rate_maze", side_effect=InvalidId("bad")):
            resp = client.post(
                "/labyrinth/rate",
                data=json.dumps({"maze_id": "not-an-oid", "rating": 3}),
                content_type="application/json",
            )
        assert resp.status_code == 400


# ---------------------------------------------------------------------------
# GET /labyrinth/<maze_id>
# ---------------------------------------------------------------------------

class TestGetByIdEndpoint:

    def test_get_found(self, client):
        fake_doc = {
            "_id": "abc123",
            "algorithm": "dfs",
            "height": 5,
            "width": 12,
            "rating": 3.5,
            "rating_count": 2,
            "created_at": "2026-03-27 12:00:00",
            "maze": {"height": 5, "width": 12, "rows": []},
        }
        with patch("labyrinthWebService.get_maze_by_id", return_value=fake_doc):
            resp = client.get("/labyrinth/abc123")
        assert resp.status_code == 200
        data = resp.get_json()
        assert data["id"] == "abc123"
        assert data["algorithm"] == "dfs"
        assert "rows" in data

    def test_get_not_found(self, client):
        with patch("labyrinthWebService.get_maze_by_id", return_value=None):
            resp = client.get("/labyrinth/nonexistent")
        assert resp.status_code == 404


# ---------------------------------------------------------------------------
# GET /mazes
# ---------------------------------------------------------------------------

class TestListMazesEndpoint:

    def test_list_empty(self, client):
        with patch("labyrinthWebService.list_mazes", return_value=[]):
            resp = client.get("/mazes")
        assert resp.status_code == 200
        assert resp.get_json() == []

    def test_list_with_results(self, client):
        docs = [
            {"_id": "a1", "algorithm": "dfs", "height": 5, "width": 12,
             "rating": 4.0, "rating_count": 1, "created_at": "2026-03-27"},
            {"_id": "b2", "algorithm": "prim", "height": 10, "width": 20,
             "rating": None, "rating_count": 0, "created_at": "2026-03-26"},
        ]
        with patch("labyrinthWebService.list_mazes", return_value=docs):
            resp = client.get("/mazes")
        assert resp.status_code == 200
        data = resp.get_json()
        assert len(data) == 2
        assert data[0]["id"] == "a1"
        assert data[1]["id"] == "b2"


# ---------------------------------------------------------------------------
# GET /labyrinth  (verify ID is now present in response)
# ---------------------------------------------------------------------------

class TestGenerateHasId:

    def test_response_contains_id(self, client):
        resp = client.get("/labyrinth")
        assert resp.status_code == 200
        data = resp.get_json()
        assert "id" in data
        assert data["id"] == "abc123"
