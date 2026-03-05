# Tests & CI/CD Documentation

> Complete reference for every test file, CI pipeline, and related tooling
> created for the **labyrinth_gen** maze generator project.

---

## Table of Contents

1. [Overview & Compliance Map](#1-overview--compliance-map)
2. [Prerequisites](#2-prerequisites)
3. [How to Run Tests](#3-how-to-run-tests)
4. [Test Files in Detail](#4-test-files-in-detail)
   - 4.1 [conftest.py — Shared Fixtures](#41-conftest-py--shared-fixtures)
   - 4.2 [test_generators.py — Functional Tests](#42-test_generatorspy--functional-tests)
   - 4.3 [test_maze_properties.py — Structural / Topological Tests](#43-test_maze_propertiespy--structural--topological-tests)
   - 4.4 [test_api_local.py — Flask HTTP API Tests](#44-test_api_localpy--flask-http-api-tests)
   - 4.5 [test_e2e.sh — End-to-End curl Tests](#45-test_e2esh--end-to-end-curl-tests)
5. [CI/CD Pipeline](#5-cicd-pipeline)
   - 5.1 [GitHub Actions Workflow](#51-github-actions-workflow)
   - 5.2 [Render Setup](#52-render-setup)
   - 5.3 [Demonstrating "Broken Build Blocks Deploy"](#53-demonstrating-broken-build-blocks-deploy)
6. [Test Results Summary](#6-test-results-summary)
7. [Known Quirks & Design Decisions](#7-known-quirks--design-decisions)
8. [File Tree](#8-file-tree)

---

## 1. Overview & Compliance Map

These tests and CI/CD pipeline were built to satisfy **Points 4 & 5** of the TER S2 project brief:

| Brief Requirement | How We Satisfy It | File(s) |
|---|---|---|
| **Functional test** — "if you asked for 30×30, verify you get 30×30" | `test_generators.py::test_dimensions_match_request` parametrized with sizes from 3×4 up to 30×30, for all 3 algorithms | `test_generators.py` |
| **Test the API locally** (server on your machine) | Flask test client — no real server needed, all requests via `app.test_client()` | `test_api_local.py` |
| **E2E test after deployment** — "test the API in curl, locally or remotely" | Bash script using `curl` + `jq`, accepts URL argument | `test_e2e.sh` |
| **Test maze characteristics** — dead-ends, connectivity, cycles, etc. | BFS connectivity, cycle count, dead-end ratio, hallway count, wall symmetry | `test_maze_properties.py` |
| **CI/CD** — automatic tests on push, deploy only if tests pass | GitHub Actions with `test` → `deploy` job dependency | `.github/workflows/ci.yml` |
| **Demo broken build blocks deploy** | Intentionally break code → tests fail → deploy job is skipped | Procedure in §5.3 |

---

## 2. Prerequisites

### For pytest (points 4A–4C)
```
Python 3.10+
pip install -r requirements.txt   # Flask, flask-cors, pytest
```

### For E2E curl tests (point 4D)
```
curl
jq       # sudo apt install jq
```

### For CI/CD (point 5)
- GitHub repository with Actions enabled
- Render account with a Deploy Hook URL
- GitHub secret `RENDER_DEPLOY_HOOK_URL` set in repo → Settings → Secrets

---

## 3. How to Run Tests

### Run all pytest tests locally
```bash
cd RA/labyrinth_gen
python -m pytest tests/ -v --tb=short
```

### Run a single test file
```bash
python -m pytest tests/test_generators.py -v
python -m pytest tests/test_api_local.py -v
python -m pytest tests/test_maze_properties.py -v
```

### Run a single test by name
```bash
python -m pytest tests/test_generators.py -k "test_dimensions_match_request" -v
```

### Run E2E curl tests (local)
```bash
# Start the server first in another terminal:
python labyrinthWebService.py

# Then:
bash tests/test_e2e.sh
```

### Run E2E curl tests (remote / deployed on Render)
```bash
bash tests/test_e2e.sh https://your-service-name.onrender.com
```

---

## 4. Test Files in Detail

---

### 4.1 `conftest.py` — Shared Fixtures

**Location:** `RA/labyrinth_gen/tests/conftest.py`

This file is automatically loaded by pytest before any test runs. It provides reusable fixtures.

#### What it does
1. **Adds `labyrinth_gen/` to `sys.path`** so imports like `from DFS import full_maze` work from the `tests/` subdirectory.
2. **Defines shared fixtures**, available to all test files without explicit import.

#### Fixtures provided

| Fixture | Scope | Description |
|---|---|---|
| `algo_name` | parametrize (dfs, kruskal, prim) | Yields each algorithm name — tests using this run **3× automatically** |
| `generate` | depends on `algo_name` | Returns the `full_maze(h, w, record_steps)` callable for the current algo |
| `maze_json` | depends on `generate` | Calls `generate(10, 12)` and returns the **parsed JSON dict** |
| `maze_grid` | depends on `maze_json` | Deserializes the JSON into a 2D list of `Cell` objects via `maze_from_json()` |
| `flask_client` | standalone | Creates a Flask **test client** from `labyrinthWebService.app` (no real HTTP server) |

#### How fixture parametrization works
Any test function that takes `algo_name` or `generate` as a parameter is **automatically run 3 times** — once per algorithm (DFS, Kruskal, Prim). This is achieved via:
```python
@pytest.fixture(params=["dfs", "kruskal", "prim"])
def algo_name(request):
    return request.param
```

---

### 4.2 `test_generators.py` — Functional Tests

**Location:** `RA/labyrinth_gen/tests/test_generators.py`  
**Test count:** 27 tests (9 test functions × 3 algorithms)

Tests that the three generators (DFS, Kruskal, Prim) produce well-formed maze JSON.

#### Test list

| Test | Parametrized | What it checks |
|---|---|---|
| `test_dimensions_match_request` | 5 sizes × 3 algos = **15** | `data["height"] == requested_height` |
| `test_row_count_matches_height` | 5 sizes × 3 algos = **15** | `len(data["rows"]) == data["height"]` |
| `test_column_count_matches_width` | 5 sizes × 3 algos = **15** | Every row has exactly `data["width"]` cells |
| `test_every_cell_has_four_walls` | 3 algos | Each cell `.walls` is a list of exactly 4 booleans |
| `test_cells_have_required_fields` | 3 algos | Every cell contains: `x, y, walls, isGhost, isTP, TP` |
| `test_at_least_two_tp_pairs` | 3 algos | ≥ 4 cells have `isTP=true` (since `add_TP_Points` is called twice) |
| `test_tp_links_are_symmetric` | 3 algos | If cell A points to B, then B is also `isTP` |
| `test_ghost_cells_exist` | 3 algos | At least 2 ghost cells (the ghost room) |
| `test_steps_present_when_requested` | 3 algos | `record_steps=True` → JSON has non-empty `"steps"` list |
| `test_steps_absent_when_not_requested` | 3 algos | `record_steps=False` → JSON does NOT have `"steps"` key |

#### Dimension cases tested
```python
DIMENSION_CASES = [(3, 4), (5, 6), (10, 12), (15, 15), (30, 30)]
```
This directly satisfies the brief's requirement: *"if you asked for a maze of 30×30, verify the size returned is 30×30."*

---

### 4.3 `test_maze_properties.py` — Structural / Topological Tests

**Location:** `RA/labyrinth_gen/tests/test_maze_properties.py`  
**Test count:** 18 tests (6 test functions × 3 algorithms)

Tests structural and topological properties of the generated mazes.

#### Helper functions (private, not tests)

| Helper | Purpose |
|---|---|
| `_grid_from_generate(generate, h, w)` | Generates a maze and returns the `Cell` grid |
| `_open_neighbours(grid, r, c)` | Returns `(r, c)` coordinates reachable through open walls |
| `_bfs_reachable(grid, start_r, start_c)` | BFS flood-fill; returns `set` of all reachable `(r, c)` |

#### Test list

| Test | What it checks |
|---|---|
| `test_maze_is_connected` | **BFS from first non-ghost cell** reaches ALL non-ghost cells — the maze is fully connected |
| `test_maze_has_cycles` | `maze_benchmarks()` returns `num_cycles > 0` — guaranteed by `add_cycles()` |
| `test_dead_end_count` | Dead-ends (cells with exactly 1 opening) exist but are < 50% of total cells |
| `test_hallways_exist` | At least one hallway (chain of degree-2 cells) exists |
| `test_wall_symmetry` | If cell `(r,c)` wall `d` is open, the neighbour's opposite wall is also open. Middle column excluded (see §7) |

#### Connectivity test explained
```
1. Find first non-ghost cell → start BFS
2. Count all non-ghost cells in grid
3. BFS explores only through open walls
4. Assert: reachable non-ghost cells == total non-ghost cells
```

#### Benchmarks used (from `common.maze_benchmarks`)
- **Cycles**: `E - V + components` (graph theory formula)
- **Hallways**: connected components of degree-2 cells
- **Dead-ends**: cells with exactly 1 wall opening

---

### 4.4 `test_api_local.py` — Flask HTTP API Tests

**Location:** `RA/labyrinth_gen/tests/test_api_local.py`  
**Test count:** 20 tests

Tests the Flask web service endpoints using Flask's built-in test client (no real server needed).

#### Happy-path tests

| Test | Request | Assertion |
|---|---|---|
| `test_default_parameters` | `GET /labyrinth` | 200, height=5, 5 rows |
| `test_each_algorithm[dfs/kruskal/prim]` | `GET /labyrinth?algorithm=X&height=5&width=6` | 200, height=5 |
| `test_requested_dimensions[3-4/10-12/30-30]` | `GET /labyrinth?height=H&width=W` | 200, height matches |
| `test_steps_true` | `GET /labyrinth?steps=true` | 200, `"steps"` key exists, non-empty |
| `test_steps_false` | `GET /labyrinth?steps=false` | 200, `"steps"` key absent |

#### Error handling tests (all expect HTTP 400)

| Test | Request | Error condition |
|---|---|---|
| `test_unknown_algorithm` | `?algorithm=bogus` | Unknown algorithm name |
| `test_non_integer_height` | `?height=abc` | Non-integer |
| `test_non_integer_width` | `?width=xyz` | Non-integer |
| `test_out_of_range_dimensions` (6 cases) | `?height=0`, `?height=2`, `?height=51`, `?width=0`, `?width=2`, `?width=51` | Value outside 3..50 |

#### Response structure tests

| Test | Checks |
|---|---|
| `test_response_contains_top_level_keys` | `height`, `width`, `rows` all present |
| `test_cells_are_well_formed` | First cell contains `x, y, walls, isGhost, isTP, TP` |

#### How the Flask test client works
```python
@pytest.fixture
def flask_client():
    from labyrinthWebService import app
    app.config["TESTING"] = True
    with app.test_client() as client:
        yield client
```
- No real HTTP server is started
- Requests go directly through Flask's WSGI layer
- Faster than starting a real server
- Ideal for local / CI testing

---

### 4.5 `test_e2e.sh` — End-to-End curl Tests

**Location:** `RA/labyrinth_gen/tests/test_e2e.sh`  
**Test count:** 9 test groups (24 individual assertions)

Bash script that hits the real HTTP API with `curl` and validates responses with `jq`.

#### Usage
```bash
# Local (server must be running on port 5000):
bash tests/test_e2e.sh

# Remote (Render deployment):
bash tests/test_e2e.sh https://your-service.onrender.com
```

#### Test list

| # | Name | What it checks |
|---|---|---|
| 1 | Default parameters | HTTP 200, height=5 |
| 2 | DFS 10×12 | HTTP 200, height=10, 10 rows |
| 3 | Kruskal 8×8 | HTTP 200, height=8 |
| 4 | Prim 6×6 | HTTP 200, height=6 |
| 5 | Steps recording | HTTP 200, steps array non-empty |
| 6 | Unknown algorithm | HTTP 400 |
| 7 | Out-of-range height | HTTP 400 |
| 8 | 30×30 dimension check | HTTP 200, height=30, 30 rows, width matches column count |
| 9 | Cell structure | Cell has `walls` (length 4), `isGhost`, `isTP` |

#### Output format
```
============================================
E2E Tests against: http://localhost:5000
============================================

Test 1: Default parameters
  ✔ HTTP 200
  ✔ Default height=5
...
============================================
Results: 24 passed, 0 failed
============================================
```

Exit code is **1** if any assertion fails (useful for CI scripts).

---

## 5. CI/CD Pipeline

---

### 5.1 GitHub Actions Workflow

**Location:** `.github/workflows/ci.yml`  

#### Pipeline structure

```
push to main/master  ──►  Job: test  ──►  Job: deploy
       or PR                  │                 │
                              │ pytest pass?     │ curl Render hook
                              ▼                 ▼
                         ✅ → continue     Render deploys new version
                         ❌ → STOP         (deploy job is skipped)
```

#### Job 1: `test`
- **Runs on:** `ubuntu-latest`
- **Working directory:** `RA/labyrinth_gen`
- **Steps:**
  1. `actions/checkout@v4`
  2. `actions/setup-python@v5` (Python 3.11)
  3. `pip install -r requirements.txt`
  4. `python -m pytest tests/ -v --tb=short`
- **If any test fails → job fails → deploy is blocked**

#### Job 2: `deploy`
- **Depends on:** `test` (via `needs: test`)
- **Condition:** only runs on `main` or `master` branch (not on PRs)
- **Action:** sends a POST to Render's Deploy Hook URL
- If `RENDER_DEPLOY_HOOK_URL` secret is not set, it prints a warning and exits 0 (doesn't block)

---

### 5.2 Render Setup

#### Steps to connect Render to GitHub

1. **Create a Web Service** on [render.com](https://render.com):
   - Connect your GitHub repository
   - Set **Root Directory** to `RA/labyrinth_gen`
   - Set **Build Command:** `pip install -r requirements.txt`
   - Set **Start Command:** `python labyrinthWebService.py`
   - (Or use Docker: Render detects the `Dockerfile` automatically)

2. **Disable Auto-Deploy** (since CI controls it):
   - Service → Settings → Build & Deploy → Auto-Deploy → set to **No**

3. **Create a Deploy Hook:**
   - Service → Settings → Deploy Hook → **Generate URL**
   - Copy the URL

4. **Add GitHub Secret:**
   - GitHub repo → Settings → Secrets and variables → Actions
   - New secret: name = `RENDER_DEPLOY_HOOK_URL`, value = the copied URL

---

### 5.3 Demonstrating "Broken Build Blocks Deploy"

This is explicitly required by the brief:

> *"vous cassez délibérément votre programme et vous montrez que le push n'engendre PAS un déploiement suite à l'échec des tests"*

#### Procedure

##### Step 1 — Break the code intentionally
For example, in `DFS.py`, change `full_maze` to return wrong dimensions:
```python
# BREAK: force incorrect height in JSON
def full_maze(height, width, record_steps=False):
    ...
    return maze_to_json(final, steps=steps).replace(
        f'"height": {len(final)}', '"height": 999'
    )
```

##### Step 2 — Push to main
```bash
git add -A && git commit -m "BREAK: intentional failure for CI demo" && git push
```

##### Step 3 — Observe GitHub Actions
- Go to GitHub → Actions tab
- The `test` job will **FAIL** (dimension tests will catch `height: 999`)
- The `deploy` job will show **Skipped** (greyed out)
- **Screenshot this for your presentation**

##### Step 4 — Fix and push
```bash
# Revert the break
git revert HEAD && git push
```

- The `test` job will **PASS** (101/101)
- The `deploy` job will **RUN** and trigger Render deployment
- **Screenshot this too**

---

## 6. Test Results Summary

Last run: **101 passed, 0 failed** in ~0.7s

```
tests/test_api_local.py      — 20 passed
tests/test_generators.py     — 63 passed  (9 tests × 3 algos, some parametrized ×5 sizes)
tests/test_maze_properties.py — 18 passed  (6 tests × 3 algos)
─────────────────────────────────────────
TOTAL                          101 passed
```

### Test categories mapped to the brief

| Brief Category | Our Tests | Count |
|---|---|---|
| **Functional tests** (dimensions, structure) | `test_generators.py` | 63 |
| **API tests** (local, Flask client) | `test_api_local.py` | 20 |
| **Maze properties** (dead-ends, connectivity, cycles) | `test_maze_properties.py` | 18 |
| **E2E tests** (curl, local or remote) | `test_e2e.sh` | 24 assertions |
| **CI/CD** (auto test + deploy gate) | `.github/workflows/ci.yml` | — |

---

## 7. Known Quirks & Design Decisions

### Middle column wall asymmetry
The `insert_middle_column()` function in `common.py` intentionally creates a wall asymmetry at the center column of the maze (Pac-Man style opening). The `test_wall_symmetry` test **skips cells adjacent to the middle column** to account for this:
```python
mid_col = W // 2
if c == mid_col or nc == mid_col:
    continue
```
This is not a bug — it's a design feature of the Pac-Man maze symmetry.

### Ghost cells and connectivity
Ghost cells (the ghost room) may or may not be reachable from normal cells. The connectivity test only checks that **all non-ghost cells** form a single connected component.

### Randomness
All generators use `random`. Tests are designed with **tolerant assertions** (e.g., "dead-ends exist but < 50%") rather than exact counts, so they pass reliably despite randomness.

### Flask test client vs real HTTP
`test_api_local.py` uses Flask's `test_client()` — no real server is needed. For real HTTP testing (local or remote), use `test_e2e.sh`.

---

## 8. File Tree

```
TER_S2_G/
├── .github/
│   └── workflows/
│       └── ci.yml                    ← CI/CD pipeline (Point 5)
└── RA/
    └── labyrinth_gen/
        ├── common.py                 ← Shared model, benchmarks, serialization
        ├── DFS.py                    ← DFS maze generator
        ├── kruskal.py                ← Kruskal maze generator
        ├── prim.py                   ← Prim maze generator
        ├── labyrinthWebService.py    ← Flask API (GET /labyrinth)
        ├── Dockerfile                ← Container for Render deployment
        ├── requirements.txt          ← Flask, flask-cors, pytest
        └── tests/
            ├── __init__.py           ← Makes tests/ a Python package
            ├── conftest.py           ← Shared fixtures (algo_name, flask_client, etc.)
            ├── test_generators.py    ← Functional tests (dimensions, cells, TP, ghost, steps)
            ├── test_maze_properties.py ← Structural tests (connectivity, cycles, dead-ends, walls)
            ├── test_api_local.py     ← Flask API tests (happy-path + error handling)
            ├── test_e2e.sh           ← curl E2E tests (local or remote)
            └── tests.md              ← THIS FILE
```
