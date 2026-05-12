# Labyrinth Generation Backend

Python Flask service that generates Pac Man style mazes, stores them in
MongoDB, and serves them as JSON to the desktop frontend or any HTTP client.

## Stack

* Python 3.10 or newer (tested with 3.12).
* Flask + flask cors + flasgger (Swagger UI).
* MongoDB (via `pymongo`) for maze and replay persistence.
* Algorithms: DFS, Kruskal, Prim, Tetris.

See [requirements.txt](requirements.txt) for exact versions.

## Quick start (local)

```bash
cd RA/labyrinth_gen
pip install -r requirements.txt
python3 labyrinthWebService.py
# Serving on http://127.0.0.1:5000
# Swagger UI on http://127.0.0.1:5000/apidocs/
```

Use `python3` explicitly: on some systems `python` still points to Python 2.

For a containerised run, see the [Dockerfile](Dockerfile) and the project
wide [DEPLOYMENT.md](../../DEPLOYMENT.md).

## Endpoints (overview)

| Method | Path                  | Purpose                                     |
| ------ | --------------------- | ------------------------------------------- |
| GET    | `/labyrinth`          | Generate a maze (also saves it to MongoDB). |
| POST   | `/labyrinth/rate`     | Submit a 0 to 5 rating for a saved maze.    |
| GET    | `/labyrinth/<id>`     | Retrieve one saved maze by id.              |
| GET    | `/mazes`              | List saved mazes (summary).                 |
| POST   | `/replay`             | Save a game replay.                         |
| GET    | `/replay/<id>`        | Retrieve one replay by id.                  |
| GET    | `/replays`            | List saved replays.                         |

Full request and response schemas live in the auto generated Swagger UI at
`/apidocs/`.

### `GET /labyrinth` parameters

| Parameter   | Type   | Range                         | Default | Description                          |
| ----------- | ------ | ----------------------------- | ------- | ------------------------------------ |
| `algorithm` | string | `dfs`, `kruskal`, `prim`, `tetris` | `dfs` | Generation algorithm.                |
| `height`    | int    | 3 to 50                       | 5       | Half grid height.                    |
| `width`     | int    | 3 to 50                       | 6       | Half grid width.                     |
| `steps`     | bool   | `true` or `false`             | `false` | Include the wall removal step list.  |

Final maze size after mirroring and the middle column is roughly
`2*height + 1` rows and `2*width - 1` columns.

Example:

```bash
curl "http://localhost:5000/labyrinth?algorithm=kruskal&height=5&width=6&steps=true"
```

Error responses:

* `400 Bad Request`: invalid algorithm name or out of range dimensions.
* `500 Internal Server Error`: generation or storage failure.

## JSON shape

```json
{
  "height": 11,
  "width": 11,
  "rows": [
    [
      {
        "x": 0, "y": 0,
        "walls": [true, false, true, true],
        "isGhost": false,
        "isTP": false,
        "TP": null
      }
    ]
  ],
  "steps": [
    {"r1": 0, "c1": 0, "w1": 2, "r2": 1, "c2": 0, "w2": 0}
  ]
}
```

Wall index convention (4 booleans per cell):

```
    0 (Up)
    +---+
3 (L|   |R) 1
    +---+
    2 (Down)
```

## Files

| File                       | Role                                           |
| -------------------------- | ---------------------------------------------- |
| `labyrinthWebService.py`   | Flask app, endpoints, Swagger config.          |
| `common.py`                | `Cell` class and shared post processing steps. |
| `DFS.py`, `prim.py`, `kruskal.py`, `Tetris.py` | The four generators.       |
| `db.py`                    | MongoDB persistence layer.                     |
| `check_walls.py`, `verify_fix.py` | Internal correctness utilities.         |
| `Dockerfile`               | Production container image.                    |
| `requirements.txt`         | Python dependencies.                           |
| `tests/`                   | Pytest and end to end test suite, see [tests/tests.md](tests/tests.md). |

## Algorithms

Algorithm explanations now live under [docs/algorithms/](../../docs/algorithms/README.md).

## Tests and CI

See [tests/tests.md](tests/tests.md) for the full test plan, fixtures, and
GitHub Actions integration.

## Deployment

Container image, GitHub Actions pipeline, Docker Hub, and the Render hosted
production endpoint are documented in the project wide
[DEPLOYMENT.md](../../DEPLOYMENT.md).
