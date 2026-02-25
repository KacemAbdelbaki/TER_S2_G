# Pac-Man Labyrinth Visualizer

A professional Pac-Man-themed graphical interface for generating, visualizing and playing through procedurally generated labyrinths. Built with **LibGDX 1.12.1** (LWJGL3 desktop backend) and **Java 17+**, communicating with a **Flask REST API** backend for maze generation.

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Maze Generation Algorithms](#maze-generation-algorithms)
4. [Python Backend (Flask API)](#python-backend-flask-api)
5. [JSON Format](#json-format)
6. [Java Frontend (LibGDX)](#java-frontend-libgdx)
7. [Project Structure](#project-structure)
8. [Prerequisites](#prerequisites)
9. [Installation & Running](#installation--running)
10. [Controls](#controls)
11. [Design Decisions](#design-decisions)
12. [Refactoring History](#refactoring-history)
13. [Troubleshooting](#troubleshooting)

---

## Overview

This project is part of the **TER S2 (Travail d'Etude et de Recherche)** for Master 1 iA. The goal is to develop a labyrinth generator suitable for a **Pac-Man game**, with:

- Three maze generation algorithms: **DFS**, **Kruskal**, and **Prim**
- A Pac-Man style symmetric maze layout with a **ghost room**, **teleport tunnels**, and **mirrored corridors**
- A graphical desktop client that renders the maze with dots, power pellets, animated ghost characters, and a playable Pac-Man character
- Step-by-step **animated generation** to visualize how each algorithm carves the maze

---

## Architecture

```
                                  HTTP GET
  +------------------+     /labyrinth?algo=...     +------------------+
  |                  | <-------------------------  |                  |
  |  Flask API       |                             |  LibGDX Client   |
  |  (Python 3.12)   | --------------------------> |  (Java 21)       |
  |                  |      JSON response          |                  |
  +------------------+                             +------------------+
        |                                                |
        |  Uses:                                         |  Uses:
        |  - common.py (shared module)                   |  - MazeRenderer (walls, dots, ghosts)
        |  - DFS.py / kruskal.py / prim.py               |  - Player (Pac-Man movement, score)
        |  - labyrinthWebService.py (Flask)               |  - GenerationAnimator (step replay)
        |                                                |  - NeonSkin (programmatic UI theme)
        +------------------------------------------------+
```

The system is a **client-server** architecture:
- **Backend**: Python Flask REST API generates mazes using one of three algorithms and returns JSON
- **Frontend**: Java LibGDX desktop application fetches maze data, renders it with Pac-Man aesthetics, and provides interactive gameplay

---

## Maze Generation Algorithms

All three algorithms share a common pipeline defined in **`common.py`**:

### Pipeline

```
initMap(height, width)          Create grid with all walls up
        |
algorithm(grid, ...)            Carve passages (DFS/Kruskal/Prim)
        |
add_cycles(grid, 0.12)         Randomly remove ~12% of remaining walls (multiple paths)
        |
strip last column               Remove rightmost column (prep for mirroring)
        |
mirror_map(grid)                Mirror left half to right (Pac-Man symmetric layout)
        |
insert_middle_column(grid)      Add a connecting column in the center
        |
add_TP_Points(grid) x2         Place 2 pairs of teleport points on edges
        |
ghost_room_init(grid)          Carve a ghost room in the center of the maze
        |
maze_to_json(grid, steps)      Serialize to JSON
```

### DFS (Depth-First Search / Recursive Backtracking)

- **File**: `DFS.py`
- **Method**: Start at a random cell, recursively visit unvisited neighbours by removing the wall between them. Backtrack when stuck.
- **Character**: Creates long, winding corridors with few branches. Produces mazes with a single solution path that tends to be very long.
- **Time complexity**: O(rows x cols)

### Kruskal's Algorithm

- **File**: `kruskal.py`
- **Method**: Treat each cell as a node. List all internal walls, shuffle them. Process walls one-by-one: if the two cells belong to different sets (Disjoint-Set / Union-Find), remove the wall and merge the sets.
- **Character**: Produces more uniform, balanced mazes. Walls are removed seemingly at random across the entire grid simultaneously.
- **Time complexity**: O(E * alpha(V)) where alpha is the inverse Ackermann function (nearly O(E))

### Prim's Algorithm

- **File**: `prim.py`
- **Method**: Start from a random cell, maintain a frontier of walls adjacent to the visited set. Randomly pick a frontier wall; if it leads to an unvisited cell, remove it and add the new cell's walls to the frontier.
- **Character**: Grows organically outward from the start point. Produces mazes with many short dead ends and a branching, radial structure.
- **Time complexity**: O(E log E) with the frontier list

### Step Recording

All three algorithms support an optional `record_steps=True` parameter. When enabled, each wall removal is recorded as a step event:

```python
{"r1": row1, "c1": col1, "w1": wall_index1, "r2": row2, "c2": col2, "w2": wall_index2}
```

This enables the **animated generation** feature in the GUI, replaying each wall removal one-by-one.

---

## Python Backend (Flask API)

### File: `labyrinthWebService.py`

A Flask REST API with CORS support, serving generated mazes.

### Endpoint

```
GET /labyrinth
```

### Query Parameters

| Parameter   | Type   | Range   | Default | Description |
|-------------|--------|---------|---------|-------------|
| `algorithm` | string | `dfs`, `kruskal`, `prim` | `dfs` | Which generation algorithm to use |
| `height`    | int    | 3 - 50  | `5`     | Half-grid height (actual maze height is `2*height + 1` after mirroring transformations) |
| `width`     | int    | 3 - 50  | `6`     | Half-grid width (actual maze width is `2*width - 1` after mirror + middle column) |
| `steps`     | string | `true` / `false` | `false` | Whether to include generation step data for animation |

### Example Request

```bash
curl "http://localhost:5000/labyrinth?algorithm=kruskal&height=5&width=6&steps=true"
```

### Error Responses

- `400 Bad Request`: Invalid algorithm name or out-of-range dimensions
- `500 Internal Server Error`: Generation failure

### Dependencies

```
flask>=2.2,<3
flask-cors>=4.0,<5
```

### Running the Server

```bash
cd RA/labyrinth_gen/
pip install -r requirements.txt
python3 labyrinthWebService.py
# Server starts on http://localhost:5000
```

> **Important**: Use `python3`, not `python` (which may point to Python 2.7 on some systems).

---

## JSON Format

The API returns a JSON object with this structure:

```json
{
  "height": 11,
  "width": 11,
  "rows": [
    [
      {
        "x": 0,
        "y": 0,
        "walls": [true, false, true, true],
        "isGhost": false,
        "isTP": false,
        "TP": null
      },
      {
        "x": 0,
        "y": 1,
        "walls": [true, true, false, false],
        "isGhost": false,
        "isTP": true,
        "TP": {"x": 5, "y": 10}
      }
    ]
  ],
  "steps": [
    {"r1": 0, "c1": 0, "w1": 2, "r2": 1, "c2": 0, "w2": 0}
  ]
}
```

### Cell Fields

| Field    | Type           | Description |
|----------|----------------|-------------|
| `x`      | int            | Row index |
| `y`      | int            | Column index |
| `walls`  | bool[4]        | `[Up, Right, Down, Left]` - `true` = wall present |
| `isGhost`| bool           | Whether cell is part of the ghost room |
| `isTP`   | bool           | Whether cell is a teleport point |
| `TP`     | object or null | Teleport destination `{x: row, y: col}` |

### Wall Index Convention

```
    0 (Up)
    -----
3  |     | 1
(L)|     |(R)
    -----
    2 (Down)
```

### Step Fields

Each step represents one wall removal during generation:

| Field | Description |
|-------|-------------|
| `r1`, `c1` | First cell (row, col) |
| `w1` | Wall index removed from first cell |
| `r2`, `c2` | Second cell (row, col) |
| `w2` | Wall index removed from second cell |

---

## Java Frontend (LibGDX)

### Technology Stack

| Component | Version | Purpose |
|-----------|---------|---------|
| LibGDX    | 1.12.1  | Game framework (rendering, input, UI) |
| LWJGL3    | (bundled) | Desktop OpenGL backend |
| gdx-freetype | 1.12.1 | Runtime TTF font generation |
| Gradle    | 8.5     | Build system |
| Java      | 17+     | (tested with OpenJDK 21.0.10) |

### Class Reference

#### `Main.java` (entry point)
- Extends `Game` for screen management
- Creates `SpriteBatch`, generates 3 `BitmapFont` instances via FreeType (DejaVuSans TTF at 48/22/16px)
- Builds the `NeonSkin` and sets `MenuScreen` as the initial screen

#### `screens/MenuScreen.java`
- Scene2D-based menu with:
  - `SelectBox` for algorithm (DFS / Kruskal / Prim)
  - Two `Slider` widgets for height (3-30) and width (3-30)
  - `CheckBox` for animated generation toggle
  - `TextField` for server address (default `localhost:5000`)
  - `TextButton` "GENERATE" that transitions to `MazeScreen`

#### `screens/MazeScreen.java`
- Main game screen integrating all components:
  - `OrthographicCamera` + `FitViewport` (1280x720 virtual resolution)
  - `MazeRenderer` for walls, dots, power pellets, ghost sprites
  - `GenerationAnimator` for step-by-step replay with play/pause/speed controls
  - `Player` (Pac-Man) entity with smooth movement and dot eating
  - `MazeHud` overlay for score, timer, animation controls
  - `InputMultiplexer` routing input to HUD stage first, then game controls
- Pac-Man spawns **below the ghost room** (or center-bottom if no ghost room found)
- Pac-Man is rendered as a yellow pie-slice with animated mouth that faces movement direction
- Camera supports scroll-wheel zoom (0.2x - 4x) and right/middle-click drag for panning

#### `maze/MazeCell.java`
- Data model mirroring the JSON cell: `x`, `y`, `walls[4]`, `isGhost`, `isTP`, `tpX`, `tpY`

#### `maze/MazeData.java`
- Container for the complete maze: `height`, `width`, `MazeCell[][] grid`, `List<GenerationStep> steps`
- Inner class `GenerationStep` holding `r1, c1, w1, r2, c2, w2`

#### `net/ApiClient.java`
- HTTP client using `Gdx.net.sendHttpRequest`
- Constructs URL with query parameters from user selections
- Parses JSON response using LibGDX `JsonReader` into `MazeData`
- Posts results to the GL thread via `Gdx.app.postRunnable()` for thread safety

#### `maze/MazeRenderer.java`
- Pac-Man themed renderer using `ShapeRenderer`:
  - **Walls**: Classic blue neon with glow effect (two-pass: thick translucent + thin sharp)
  - **Dots**: Small warm-yellow circles at cell centers
  - **Power pellets**: Larger pulsing yellow circles near maze corners
  - **Ghost room**: Faint red background tint
  - **Ghost sprites**: Up to 4 classic ghost shapes (red, pink, cyan, orange) with dome body, wavy bottom, white eyes with blue pupils, bobbing animation
  - **TP links**: Faint magenta lines between teleport pairs
  - **Cell backgrounds**: Visited cells get a subtle tint
  - **Animated grid**: Builds a temporary grid with all walls up, then progressively removes them based on step progress
- Coordinate helpers: `gridToScreenX()`, `gridToScreenY()`, `getCellSize()`

#### `maze/GenerationAnimator.java`
- Timer-based step-by-step animation controller
- Controls: play, pause, restart, skip-to-end, speed adjustment
- Speed: slider maps 0..1 to 0.2s..0.005s per step interval
- Reports `getProgress()` (number of steps revealed) for the renderer

#### `player/Player.java`
- Pac-Man-specific player entity:
  - **Direction tracking**: `DIR_RIGHT`, `DIR_UP`, `DIR_LEFT`, `DIR_DOWN` (for mouth orientation)
  - **Mouth animation**: Sinusoidal oscillation 0..45 degrees
  - **Dot eating**: Maintains `boolean[][] dots` and `boolean[][] powerPellets`; eats on cell entry
  - **Scoring**: 10 points per dot, 50 points per power pellet
  - **Win condition**: All dots eaten (not reaching a corner)
  - **Teleportation**: Warps to TP destination, eats dot at destination too
  - **Smooth movement**: Lerp-based visual interpolation with wall collision checking
  - **Power pellets**: Placed near the 4 corners of the maze
  - **Starting position**: Below ghost room center

#### `ui/NeonSkin.java`
- Fully programmatic `Skin` - no external JSON/atlas files needed
- Creates all UI textures from `Pixmap`: solid colours, slider knobs, checkbox states
- Colour palette:
  | Element | Hex | Description |
  |---------|-----|-------------|
  | BG_DARK | #141423 | Panel backgrounds |
  | BG_MID | #1E1E33 | Text field backgrounds |
  | BG_LIGHT | #2E2E47 | Button hover states |
  | ACCENT | #00E699 | Primary neon green |
  | CYAN | #00D4FF | Titles, highlights |
  | TEXT_WHITE | #EBEBF5 | Body text |
  | TEXT_GRAY | #9999AD | Secondary text |
  | ERROR_RED | #FF4D4D | Error messages |
- Registers styles for: `Label` (default/title/small), `TextButton` (default/accent), `TextField`, `Slider`, `CheckBox`, `SelectBox`, `List`, `ScrollPane`, `Window`

#### `ui/MazeHud.java`
- Scene2D `Stage` overlay on top of the game:
  - **Top bar**: Back button, SCORE display, dots remaining, algorithm name, grid size, timer
  - **Animation controls**: Pause/Play, Restart, Skip buttons + speed slider + step counter
  - **Center**: Status messages (loading, error, win)
  - **Bottom**: Control hints

#### `lwjgl3/Lwjgl3Launcher.java`
- Desktop launcher: 1280x720 window, VSync enabled, 60 FPS foreground limit, title "Labyrinth Visualizer"

---

## Project Structure

```
RA/
  labyrinth_gen/                 # Python backend
    common.py                    # Shared module (Cell class, grid ops, JSON serialization)
    DFS.py                       # DFS maze generation algorithm
    kruskal.py                   # Kruskal's algorithm (Union-Find)
    prim.py                      # Prim's algorithm (frontier growth)
    labyrinthWebService.py       # Flask REST API
    requirements.txt             # Python dependencies
    Dockerfile                   # Container config (original)
    README_DFS.md                # DFS algorithm documentation
    README_Kruskal.md            # Kruskal algorithm documentation
    README_Prim.md               # Prim algorithm documentation
    Gallery/                     # Generated maze images

  labyrinth_ui/                  # Java frontend
    build.gradle                 # Root Gradle build (gdxVersion = 1.12.1)
    settings.gradle              # Includes 'core' and 'lwjgl3' modules
    gradle.properties            # JVM args for Gradle
    gradlew / gradlew.bat        # Gradle wrapper scripts
    assets/                      # Runtime assets
      DejaVuSans.ttf             # Regular font
      DejaVuSans-Bold.ttf        # Bold font
    gradle/wrapper/              # Gradle wrapper JAR + properties
    core/                        # Core game module
      build.gradle               # java-library, gdx + gdx-freetype
      src/main/java/com/labyrinth/
        Main.java
        maze/
          MazeCell.java
          MazeData.java
          MazeRenderer.java
          GenerationAnimator.java
        net/
          ApiClient.java
        player/
          Player.java
        screens/
          MenuScreen.java
          MazeScreen.java
        ui/
          NeonSkin.java
          MazeHud.java
    lwjgl3/                      # Desktop launcher module
      build.gradle               # application plugin, fat JAR config
      src/main/java/com/labyrinth/lwjgl3/
        Lwjgl3Launcher.java
```

---

## Prerequisites

| Requirement | Version | Notes |
|-------------|---------|-------|
| Java (JDK)  | 17+     | Tested with OpenJDK 21.0.10 |
| Python      | 3.10+   | Tested with 3.12.3. **Must use `python3` command** |
| pip         | recent  | For installing Flask dependencies |
| Internet    | -       | Gradle downloads dependencies on first build |

---

## Installation & Running

### 1. Clone and setup

```bash
git clone https://github.com/KacemAbdelbaki/TER_S2_G.git
cd TER_S2_G
```

### 2. Install Python dependencies

```bash
cd RA/labyrinth_gen/
pip install -r requirements.txt
# Or in a virtual environment:
python3 -m venv venv && source venv/bin/activate && pip install -r requirements.txt
```

### 3. Start the API server

```bash
python3 labyrinthWebService.py
# Output: * Running on http://127.0.0.1:5000
```

### 4. Run the visualizer (option A: from source)

```bash
cd RA/labyrinth_ui/
./gradlew lwjgl3:run
```

### 5. Run the visualizer (option B: fat JAR)

```bash
cd RA/labyrinth_ui/
./gradlew lwjgl3:jar
java -jar lwjgl3/build/libs/labyrinth-visualizer.jar
```

### 6. Using the app

1. Select an algorithm (DFS / Kruskal / Prim)
2. Adjust height and width sliders
3. Toggle "Animated Generation" if you want to watch the maze being built
4. Verify the server address (default: `localhost:5000`)
5. Click **GENERATE**
6. If animated: watch the maze being carved, use Pause/Skip/Speed controls
7. Once generation completes, Pac-Man appears - navigate with WASD/arrows
8. Eat all dots to win!

---

## Controls

### Menu Screen

| Control | Action |
|---------|--------|
| Mouse | Select algorithm, adjust sliders, click Generate |
| Keyboard | Type in server address field |

### Maze Screen

| Control | Action |
|---------|--------|
| `W` / `Arrow Up` | Move Pac-Man up |
| `A` / `Arrow Left` | Move Pac-Man left |
| `S` / `Arrow Down` | Move Pac-Man down |
| `D` / `Arrow Right` | Move Pac-Man right |
| Scroll wheel | Zoom camera in/out (0.2x - 4x) |
| Right-click drag | Pan camera |
| Middle-click drag | Pan camera |
| `ESC` | Return to menu |

### Animation Controls (during generation)

| Button | Action |
|--------|--------|
| Pause / Play | Toggle animation playback |
| Restart | Replay animation from step 0 |
| Skip >> | Jump to completed maze |
| Speed slider | Adjust animation speed (left=slow, right=fast) |

---

## Design Decisions

### Why LibGDX over JavaFX/Swing?

- **GPU-accelerated** ShapeRenderer for smooth thick-line walls and glow effects
- Built-in **game loop** with delta-time passing, ideal for animation
- `FitViewport` handles resolution independence automatically
- Scene2D provides a mature widget toolkit for menus and HUD
- Cross-platform potential (Android/iOS/Web) if needed in the future

### Why Gradle?

- LibGDX's standard build system with multi-module support
- Handles dependency resolution (LibGDX, LWJGL3, FreeType) automatically
- Fat JAR packaging for single-file distribution

### Why a separate Python backend?

- The maze generation algorithms were already implemented in Python
- REST API allows any frontend to consume the maze data (web, mobile, desktop)
- Decoupled architecture: algorithms can be tested/modified independently
- `flask-cors` enables cross-origin requests for future web frontends

### Pac-Man maze characteristics

The mazes are specifically designed for Pac-Man gameplay:
- **Symmetric layout**: Left half mirrors to right half (like the original Pac-Man)
- **Ghost room**: Central enclosed area where ghosts spawn
- **Teleport tunnels**: Edge-to-edge warps (like the side tunnels in Pac-Man)
- **Cycle addition**: ~12% extra walls removed to create multiple paths (prevents single-corridor mazes)
- **Middle column**: Seamless connection between mirrored halves

### Font rendering

- FreeType generates bitmap fonts at exact pixel sizes from bundled DejaVuSans TTF
- No scaling artifacts - each font size (48, 22, 16) is rasterized natively
- Fallback to scaled default font if TTF fails to load

### Colour palette

| Element | Colour | Description |
|---------|--------|-------------|
| Background | Pure black `#000000` | Classic Pac-Man |
| Walls | Blue `#2121FF` | Classic Pac-Man blue walls |
| Wall glow | Blue `rgba(33,33,255,0.18)` | Neon glow effect |
| Dots | Warm yellow `#FFD98C` | Edible pellets |
| Power pellets | Bright yellow (pulsing) | Corner power-ups |
| Pac-Man | Yellow `#FFFF00` | The player character |
| Ghost (Blinky) | Red `#FF0000` | |
| Ghost (Pinky) | Pink `#FFB8D6` | |
| Ghost (Inky) | Cyan `#00FFFF` | |
| Ghost (Clyde) | Orange `#FF9900` | |
| Ghost eyes | White + blue pupils | Classic ghost design |
| TP links | Faint magenta | Teleport pair indicators |
| UI accent | Neon green `#00E699` | Buttons, score |
| UI text | Near-white `#EBEBF5` | Labels |

---

## Refactoring History

### What was done in this development session

#### Phase 1: Python Backend Refactoring

1. **Created `common.py`** - Extracted all shared code from the three algorithm files:
   - `Cell` class (row, col, walls, ghost/TP flags)
   - `initMap()` - grid initialization with all walls up
   - `ghost_room_init()` - ghost room carving
   - `border()` - ensure border walls
   - `remove_wall_between()` - safe wall removal between adjacent cells
   - `add_cycles()` - random wall removal for multiple paths
   - `mirror_map()` - left-to-right mirroring
   - `insert_middle_column()` - center column insertion
   - `add_TP_Points()` - teleport point placement
   - `maze_to_json()` - JSON serialization (with optional steps)
   - `maze_from_json()` - JSON deserialization
   - `maze_benchmarks()` - performance measurement
   - `print_map()` - ASCII visualization

2. **Rewrote `DFS.py`** - Imports from common.py, added step recording, `full_maze()` pipeline function
3. **Rewrote `kruskal.py`** - Imports from common.py, added `DisjointSet` class, step recording, `full_maze()` pipeline
4. **Rewrote `prim.py`** - Imports from common.py, added step recording, `full_maze()` pipeline
5. **Rewrote `labyrinthWebService.py`** - Added query parameter support (algorithm, height, width, steps), CORS with flask-cors, input validation, error handling
6. **Updated `requirements.txt`** - Added `flask-cors>=4.0,<5`

#### Phase 2: LibGDX Project Scaffolding

7. Created `RA/labyrinth_ui/` directory structure
8. **`build.gradle`** - Root build with `gdxVersion = '1.12.1'`, mavenCentral repository
9. **`settings.gradle`** - Includes `core` and `lwjgl3` sub-projects
10. **`gradle.properties`** - JVM memory settings
11. **`core/build.gradle`** - java-library plugin, gdx + gdx-freetype dependencies
12. **`lwjgl3/build.gradle`** - application plugin, LWJGL3 backend, native platform dependencies, fat JAR configuration
13. **`gradlew`** / wrapper - Gradle 8.5 wrapper (generated using downloaded Gradle distribution)
14. Bundled **DejaVuSans.ttf** and **DejaVuSans-Bold.ttf** in assets/ for crisp font rendering

#### Phase 3: Core Java Classes

15. **`Lwjgl3Launcher.java`** - 1280x720 window, VSync, 60fps
16. **`Main.java`** - Game entry, FreeType font generation (48/22/16px), NeonSkin creation
17. **`MazeCell.java`** - Cell data model (x, y, walls[4], isGhost, isTP, tpX, tpY)
18. **`MazeData.java`** - Grid container + GenerationStep inner class
19. **`ApiClient.java`** - Async HTTP fetch, JSON parsing with LibGDX JsonReader

#### Phase 4: Rendering & Animation

20. **`MazeRenderer.java`** - Pac-Man themed ShapeRenderer with:
    - Blue neon walls with glow (two-pass rendering)
    - Warm yellow dots at cell centers
    - Pulsing power pellets near corners
    - Ghost sprites with dome body, wavy bottom, eyes (red/pink/cyan/orange)
    - Faint ghost room background
    - TP link lines
    - Animated grid building (all-walls-up -> progressive removal)
21. **`GenerationAnimator.java`** - Timer-based step replay (play/pause/restart/skip/speed)

#### Phase 5: Player & Gameplay

22. **`Player.java`** - Pac-Man mechanics:
    - Direction-aware movement (4 directions)
    - Animated mouth (sinusoidal 0-45 degree oscillation)
    - Dot eating with score tracking (10 pts/dot, 50 pts/power pellet)
    - Win condition: all dots eaten
    - Wall collision checking
    - Teleport support (warps + eats dot at destination)
    - Lerp-based smooth visual movement
    - Spawn position: below ghost room center

#### Phase 6: UI

23. **`NeonSkin.java`** - Programmatic dark skin (no external files):
    - All widget styles from Pixmap-generated textures
    - Full colour palette for consistent dark neon theme
24. **`MazeHud.java`** - Game overlay:
    - Score display, dots remaining, timer
    - Animation controls (pause/restart/skip/speed)
    - Status messages (loading/error/win)
    - Navigation hints

#### Phase 7: Screens

25. **`MenuScreen.java`** - Algorithm selector, dimension sliders, animated toggle, server config, Generate button
26. **`MazeScreen.java`** - Main game screen:
    - Fetches maze from API on entry
    - Optional generation animation playback
    - Pac-Man rendering (yellow pie-slice with directional mouth)
    - Camera zoom/pan
    - Pac-Man spawns below ghost room
    - Passes dot/pellet state to renderer

#### Issues Encountered & Fixed

- **Python encoding**: `cat > file << 'EOF'` introduced Unicode em-dash and multiplication sign characters. Fixed with `sed -i 's/\xe2\x80\x93/-/g'`
- **Python version**: System `python` pointed to Python 2.7.18, which couldn't parse the files. All commands use `python3` (3.12.3)
- **pip PEP 668**: `pip install` blocked by externally-managed-environment. Resolved with `--break-system-packages` flag or virtual environment
- **Gradle wrapper**: No system Gradle installed. Downloaded Gradle 8.5 distribution, used it to generate a proper wrapper
- **Font blurriness**: Default BitmapFont scaled 3x was blurry. Fixed by using FreeType to generate fonts at native pixel sizes from bundled TTF

---

## Troubleshooting

### "Connection failed" when generating

The Python API server is not running. Start it:
```bash
cd RA/labyrinth_gen/ && python3 labyrinthWebService.py
```

### Blurry text

Ensure `DejaVuSans.ttf` and `DejaVuSans-Bold.ttf` exist in `RA/labyrinth_ui/assets/`. If missing, copy from system:
```bash
cp /usr/share/fonts/truetype/dejavu/DejaVuSans*.ttf RA/labyrinth_ui/assets/
```

### Gradle build fails

Ensure Java 17+ is installed:
```bash
java -version  # Should show 17+
```

### Python import errors

Ensure you're in the `RA/labyrinth_gen/` directory and using `python3`:
```bash
cd RA/labyrinth_gen/
python3 -c "from common import *; print('OK')"
```

### Port already in use

Change the server port or kill the existing process:
```bash
lsof -i :5000 | grep LISTEN
kill <PID>
```

---

## Build Commands

```bash
# Compile only (no JAR)
cd RA/labyrinth_ui/
./gradlew compileJava

# Build fat JAR (16 MB, includes all dependencies)
./gradlew lwjgl3:jar
# Output: lwjgl3/build/libs/labyrinth-visualizer.jar

# Run directly from source
./gradlew lwjgl3:run

# Clean build artifacts
./gradlew clean
```

---

## Authors

- **Abou Orm**
- **Kabbat**
- **Abdelbaki**

*TER S2 - Master 1 iA - 2025/2026*
