# Deployment Documentation

> **TER S2 – Pac-Man Labyrinth Generator**  
> Full reference for containerisation, CI/CD pipeline, and cloud deployment.

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Docker Containerisation](#2-docker-containerisation)
3. [Docker Hub](#3-docker-hub)
4. [CI/CD Pipeline – GitHub Actions](#4-cicd-pipeline--github-actions)
5. [Cloud Deployment – Render](#5-cloud-deployment--render)
6. [Required Secrets](#6-required-secrets)
7. [Running the Backend Locally](#7-running-the-backend-locally)
8. [Running the Frontend Locally](#8-running-the-frontend-locally)
9. [API Reference](#9-api-reference)
10. [End-to-End Deployment Flow](#10-end-to-end-deployment-flow)

---

## 1. Architecture Overview

The project is split into two independently deployable components:

```
 Developer pushes code
         │
         ▼
 GitHub Repository
         │
         ▼ GitHub Actions CI/CD
 ┌───────────────────────────────────────────────────┐
 │  1. Run Tests (pytest)                            │
 │  2. Build & Push Docker image → Docker Hub        │
 │  3. Trigger Render deploy hook                    │
 └───────────────────────────────────────────────────┘
         │                           │
         ▼                           ▼
 Docker Hub                   Render.com (cloud)
 (image registry)             (hosts Flask API)
                                     │
                                     │  HTTP GET /labyrinth
                                     ▼
                              LibGDX Java Client
                              (runs locally on
                               developer machine)
```

### Components

| Component | Technology | Deployment |
|-----------|-----------|------------|
| **Backend – Flask API** | Python 3.11, Flask >=2.2 | Docker → Render.com |
| **Frontend – Visualizer** | Java 17, LibGDX 1.12.1 | Desktop JAR (local) |

---

## 2. Docker Containerisation

The backend (`RA/labyrinth_gen`) is fully containerised using Docker.

### `Dockerfile` (located at `RA/labyrinth_gen/Dockerfile`)

```dockerfile
FROM python:3.11-slim          # Minimal Python 3.11 base image

WORKDIR /app

# Install runtime dependencies first (layer caching)
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

# Copy application source code
COPY . /app

WORKDIR /app

# Flask default port
EXPOSE 5000

# Start the Flask web service
CMD ["python", "labyrinthWebService.py"]
```

### Key design decisions

| Decision | Reason |
|----------|--------|
| `python:3.11-slim` base | Minimal image size; no unnecessary OS packages |
| `COPY requirements.txt` before `COPY . /app` | Docker layer caching — dependencies reinstall only when `requirements.txt` changes |
| `--no-cache-dir` on pip | Further reduces image size |
| `EXPOSE 5000` | Documents the port; required by Render to route traffic |
| `CMD ["python", "labyrinthWebService.py"]` | Runs Flask directly (not via `flask run`) for compatibility |

### Python dependencies (`requirements.txt`)

```
Flask>=2.2,<3
flask-cors>=4.0,<5
pytest>=7.0
```

| Package | Purpose |
|---------|---------|
| `Flask` | HTTP web framework powering the REST API |
| `flask-cors` | Adds CORS headers so the Java client (different origin) can call the API |
| `pytest` | Test runner (used in CI, not shipped to production) |

### Build & run the image locally

```bash
# Navigate to the backend directory
cd RA/labyrinth_gen

# Build the image
docker build -t labyrinth-api .

# Run the container
docker run -p 5000:5000 labyrinth-api

# Test it
curl "http://localhost:5000/labyrinth?algorithm=dfs&width=10&height=10"
```

---

## 3. Docker Hub

After a successful CI run, the built image is pushed to Docker Hub under:

```
<DOCKER_USERNAME>/dfs:latest
```

The image tag is fixed to `latest` so the deployment platform always pulls the most recent passing build.

### Pull and run the published image

```bash
# Pull from Docker Hub
docker pull <DOCKER_USERNAME>/dfs:latest

# Run
docker run -p 5000:5000 <DOCKER_USERNAME>/dfs:latest
```

---

## 4. CI/CD Pipeline – GitHub Actions

The pipeline is defined in `.github/workflows/ci.yml`.

### Triggers

| Event | Behaviour |
|-------|-----------|
| `push` to `main` or `master` | Full pipeline: test → build → deploy |
| `pull_request` to `main` or `master` | Tests only (no build/push/deploy) |

### Pipeline stages

```
 push to main
      │
      ▼
 ┌──────────────────────────────────┐
 │  Job 1 – test                    │
 │  • Checkout code                 │
 │  • Set up Python 3.11            │
 │  • pip install -r requirements   │
 │  • pytest tests/ -v --tb=short   │
 └──────────────┬───────────────────┘
                │ (only if tests PASS)
                ▼
 ┌──────────────────────────────────┐
 │  Job 2 – build-and-push          │
 │  • Checkout code                 │
 │  • Log in to Docker Hub          │
 │  • docker build ./RA/labyrinth_gen│
 │  • docker push <user>/dfs:latest │
 └──────────────┬───────────────────┘
                │ (only if build succeeds)
                ▼
 ┌──────────────────────────────────┐
 │  Job 3 – deploy                  │
 │  • Trigger Render deploy hook    │
 │    (HTTP POST to webhook URL)    │
 └──────────────────────────────────┘
```

### Full workflow file (`.github/workflows/ci.yml`)

```yaml
name: CI – Test & Deploy Maze Generator

on:
  push:
    branches: [main, master]
  pull_request:
    branches: [main, master]

jobs:
  test:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: RA/labyrinth_gen
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-python@v5
        with:
          python-version: "3.11"
      - run: pip install -r requirements.txt
      - run: python -m pytest tests/ -v --tb=short

  build-and-push:
    needs: test
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: docker/login-action@v3
        if: github.event_name == 'push'
        with:
          username: ${{ secrets.DOCKER_USERNAME }}
          password: ${{ secrets.DOCKER_PASSWORD }}
      - uses: docker/build-push-action@v5
        with:
          context: ./RA/labyrinth_gen
          push: ${{ github.event_name == 'push' }}
          tags: ${{ secrets.DOCKER_USERNAME }}/dfs:latest

  deploy:
    needs: build-and-push
    if: github.event_name == 'push'
    runs-on: ubuntu-latest
    steps:
      - name: Trigger Render deploy hook
        env:
          RENDER_DEPLOY_HOOK_URL: ${{ secrets.RENDER_DEPLOY_HOOK_URL }}
        run: |
          if [ -z "$RENDER_DEPLOY_HOOK_URL" ]; then
            echo "RENDER_DEPLOY_HOOK_URL not set – skipping deploy."
            exit 0
          fi
          curl -s -o /dev/null -w "HTTP %{http_code}" "$RENDER_DEPLOY_HOOK_URL"
          echo "Deploy hook triggered."
```

### "Broken build blocks deploy" guarantee

Because every later job declares `needs: <previous-job>`, a test failure in Job 1 prevents Jobs 2 and 3 from running. The deployed version is always one that passes all tests.

```
 Tests FAIL  →  build-and-push SKIPPED  →  deploy SKIPPED
 Tests PASS  →  build-and-push RUNS     →  deploy RUNS
```

---

## 5. Cloud Deployment – Render

The Flask API is deployed on **[Render.com](https://render.com)** – a managed Platform-as-a-Service (PaaS).

### How Render works for this project

1. Render watches for deploy hooks (HTTP calls from GitHub Actions).
2. On receiving a hook, it pulls the latest Docker image from Docker Hub.
3. It starts a new container, health-checks it, then switches traffic to it (zero-downtime deploy).

### Setting up Render (one-time)

1. Create a free account at [render.com](https://render.com).
2. **New → Web Service → Deploy an existing image**.
3. Enter Docker Hub image URL: `<DOCKER_USERNAME>/dfs:latest`.
4. Set **Port** to `5000`.
5. Set **Environment** to `Docker`.
6. Once created, go to **Settings → Deploy Hook** and copy the URL.
7. Save that URL as the `RENDER_DEPLOY_HOOK_URL` secret in GitHub (see §6).

### Render service configuration summary

| Setting | Value |
|---------|-------|
| Type | Web Service |
| Region | Frankfurt (EU) or Oregon (US) |
| Instance type | Free |
| Docker image | `<DOCKER_USERNAME>/dfs:latest` |
| Port | `5000` |
| Health check path | `/labyrinth` |
| Deploy trigger | Webhook from GitHub Actions |

### Deployed API base URL

```
https://<your-service-name>.onrender.com
```

Example request to the deployed API:

```bash
curl "https://<your-service-name>.onrender.com/labyrinth?algorithm=kruskal&height=10&width=10"
```

> **Note (Free tier):** Render free instances spin down after 15 minutes of inactivity. The first request after idle may take 30–60 seconds to respond while the container restarts.

---

## 6. Required Secrets

Configure these in **GitHub → Repository Settings → Secrets and variables → Actions**:

| Secret name | Value | Used by |
|-------------|-------|---------|
| `DOCKER_USERNAME` | Your Docker Hub username | `build-and-push` job |
| `DOCKER_PASSWORD` | Docker Hub password or access token | `build-and-push` job |
| `RENDER_DEPLOY_HOOK_URL` | Full webhook URL from Render dashboard | `deploy` job |

> **Best practice:** Use a Docker Hub **access token** (not your password) for `DOCKER_PASSWORD`. Create one at Docker Hub → Account Settings → Security → New Access Token.

---

## 7. Running the Backend Locally

### Option A – Plain Python

```bash
# Install dependencies
cd RA/labyrinth_gen
pip install -r requirements.txt

# Start the server
python labyrinthWebService.py
# Server listening on http://0.0.0.0:5000
```

### Option B – Docker

```bash
cd RA/labyrinth_gen

# Build image
docker build -t labyrinth-api .

# Run container (maps container port 5000 → host port 5000)
docker run -p 5000:5000 labyrinth-api
```

### Verify it's running

```bash
curl "http://localhost:5000/labyrinth?algorithm=dfs&height=5&width=6"
```

---

## 8. Running the Frontend Locally

The LibGDX Java desktop client connects to the Flask API to fetch mazes.

### Prerequisites

- Java 17+
- Gradle (or use the included `./gradlew` wrapper)

### Build & run

```bash
cd RA/labyrinth_ui

# Run directly (connects to local backend by default)
./gradlew lwjgl3:run

# Build a fat JAR (all dependencies included)
./gradlew lwjgl3:jar
# Output: RA/labyrinth_ui/lwjgl3/build/libs/labyrinth-visualizer.jar

# Run the fat JAR
java -jar lwjgl3/build/libs/labyrinth-visualizer.jar
```

> Make sure the Flask backend is running before starting the frontend.

---

## 9. API Reference

**Base URL (local):** `http://localhost:5000`  
**Base URL (deployed):** `https://<your-service-name>.onrender.com`

### `GET /labyrinth`

Generates and returns a maze as JSON.

#### Query parameters

| Parameter | Type | Default | Valid values | Description |
|-----------|------|---------|--------------|-------------|
| `algorithm` | string | `dfs` | `dfs`, `kruskal`, `prim`, `tetris` | Maze generation algorithm |
| `height` | int | `5` | `3` – `50` | Number of rows |
| `width` | int | `6` | `3` – `50` | Number of columns |
| `steps` | bool | `false` | `true`, `false` | Include step-by-step generation data |

#### Example requests

```bash
# Default (DFS, 5×6)
curl "http://localhost:5000/labyrinth"

# Kruskal, 15×15
curl "http://localhost:5000/labyrinth?algorithm=kruskal&height=15&width=15"

# Prim with generation steps
curl "http://localhost:5000/labyrinth?algorithm=prim&height=10&width=10&steps=true"
```

#### Response format (JSON)

```json
{
  "algorithm": "dfs",
  "height": 5,
  "width": 6,
  "rows": [
    [
      {
        "x": 0,
        "y": 0,
        "walls": [true, false, true, true],
        "isGhost": false,
        "isTP": false,
        "TP": null
      }
    ]
  ],
  "steps": []
}
```

| Field | Type | Description |
|-------|------|-------------|
| `algorithm` | string | Algorithm used |
| `height` | int | Maze height (rows) |
| `width` | int | Maze width (columns) |
| `rows` | array | 2-D array of Cell objects |
| `rows[r][c].walls` | bool[4] | `[top, right, bottom, left]` – `true` = wall present |
| `rows[r][c].isGhost` | bool | Cell is inside the ghost room |
| `rows[r][c].isTP` | bool | Cell is a teleport point |
| `rows[r][c].TP` | object/null | `{x, y}` coordinates of the linked teleport cell |
| `steps` | array | Generation snapshots (only if `steps=true`) |

#### Error responses

| HTTP code | Cause |
|-----------|-------|
| `400` | Unknown algorithm name |
| `400` | `height` or `width` not an integer |
| `400` | `height` or `width` outside 3–50 range |

---

## 10. End-to-End Deployment Flow

The following summarises the complete journey from a code change to a live deployed API:

```
 Developer
    │
    │  git push origin main
    ▼
 GitHub Repository
    │
    │  Webhook → GitHub Actions
    ▼
 ┌─────────────────────────────────────────────────────────┐
 │  GitHub Actions Runner (ubuntu-latest)                   │
 │                                                          │
 │  Job 1: test                                             │
 │    cd RA/labyrinth_gen                                   │
 │    pip install -r requirements.txt                       │
 │    python -m pytest tests/ -v                            │
 │    ✓ 30+ tests pass                                      │
 │                         │                                │
 │                         ▼ (tests passed)                 │
 │  Job 2: build-and-push                                   │
 │    docker build ./RA/labyrinth_gen → image               │
 │    docker push <user>/dfs:latest → Docker Hub            │
 │    ✓ Image published                                     │
 │                         │                                │
 │                         ▼ (build succeeded)              │
 │  Job 3: deploy                                           │
 │    curl POST $RENDER_DEPLOY_HOOK_URL                     │
 │    ✓ Render notified                                     │
 └─────────────────────────────────────────────────────────┘
                          │
                          ▼
 Render.com
    │  Pulls <user>/dfs:latest from Docker Hub
    │  Starts new container on port 5000
    │  Health-checks /labyrinth endpoint
    │  Switches live traffic → new container
    ▼
 Live API: https://<service>.onrender.com/labyrinth
    │
    │  HTTP GET /labyrinth?algorithm=...
    ▼
 Java LibGDX Client (runs on user machine)
    Renders maze with Pac-Man graphics
```

### Timeline (typical)

| Phase | Duration |
|-------|----------|
| GitHub Actions – tests | ~30 s |
| GitHub Actions – Docker build & push | ~60–90 s |
| GitHub Actions – deploy trigger | ~5 s |
| Render – pull image & start container | ~30–60 s |
| **Total end-to-end** | **~2–3 minutes** |
