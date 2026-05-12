# Pac Man Labyrinth

A Pac Man themed labyrinth generator and playable desktop game built for
the TER S2 (Travail d'Etude et de Recherche), Master 1 iA.

The project is split into two components that talk over HTTP:

* A **Python Flask backend** that generates symmetric Pac Man style mazes
  with four algorithms (DFS, Kruskal, Prim, Tetris), stores them in
  MongoDB, and exposes a REST + Swagger API.
* A **LibGDX desktop frontend** that fetches mazes, animates their
  generation step by step, and lets you play Pac Man on them with score,
  ghosts, dots, power pellets, and teleport tunnels.

```
 LibGDX Desktop Client  <-- HTTP / JSON -->  Flask API  -->  Algorithms (DFS, Kruskal, Prim, Tetris)
        Java 17+                              Python 3.10+         |
                                                                   v
                                                                MongoDB
```

## Download and play

You do not need Java, Python, or Gradle installed. GitHub Actions builds
self contained desktop packages for Windows, Linux, and macOS on every
tagged release.

1. Open the [latest GitHub Release](https://github.com/KacemAbdelbaki/TER_S2_G/releases/latest).
2. Download the archive for your operating system:

   | OS      | Asset                          |
   | ------- | ------------------------------ |
   | Windows | `PacmanLabyrinth-windows.zip`  |
   | Linux   | `PacmanLabyrinth-linux.zip`    |
   | macOS   | `PacmanLabyrinth-macos.zip`    |

3. Extract the archive anywhere (Desktop is fine).
4. Launch the game:

   | OS      | Action                                                                 |
   | ------- | ---------------------------------------------------------------------- |
   | Windows | Double click `PacmanLabyrinth\PacmanLabyrinth.exe`.                    |
   | Linux   | Run `./PacmanLabyrinth/bin/PacmanLabyrinth` (or double click it).      |
   | macOS   | Double click `PacmanLabyrinth.app`. First time, right click then **Open** to bypass Gatekeeper, or run `xattr -dr com.apple.quarantine PacmanLabyrinth.app` once. |

> The macOS bundle is ad hoc signed, not Developer ID signed, so the first
> launch may say "unidentified developer". This is expected for an unsigned
> student project build.

The game needs the Flask backend reachable. By default it points to
`localhost:5000`, but you can type any host (for example, the Render hosted
demo) into the server field on the menu screen.

## Controls (in game)

| Control                    | Action                              |
| -------------------------- | ----------------------------------- |
| `W` / `A` / `S` / `D` or arrow keys | Move Pac Man.                |
| Scroll wheel               | Zoom camera (0.2x to 4x).           |
| Right or middle click drag | Pan the camera.                     |
| `ESC`                      | Return to the menu.                 |

## Repository map

| Path                                | What lives there                                       |
| ----------------------------------- | ------------------------------------------------------ |
| [RA/labyrinth_gen/](RA/labyrinth_gen/)     | Python Flask backend, generators, Dockerfile, tests.  |
| [RA/labyrinth_ui/](RA/labyrinth_ui/)       | LibGDX desktop frontend, Gradle build, jpackage tasks. |
| [docs/algorithms/](docs/algorithms/)       | DFS, Prim, Kruskal explanations and comparison.        |
| [docs/research/](docs/research/)           | Per author research notes (labyrinth vs maze, etc.).   |
| [RA/Gallery/](RA/Gallery/)                 | Generated maze screenshots.                            |
| [RA/report/](RA/report/)                   | LaTeX project report.                                  |
| [.github/workflows/](.github/workflows/)   | CI (tests, Docker image) and release (jpackage) workflows. |
| [DEPLOYMENT.md](DEPLOYMENT.md)             | Docker, Docker Hub, GitHub Actions, Render setup.      |
| [ToDo.md](ToDo.md)                         | Backlog and notes.                                     |

## Developer quick start

Clone the repo, then run backend and frontend in two terminals.

```bash
# Terminal 1: backend
cd RA/labyrinth_gen
pip install -r requirements.txt
python3 labyrinthWebService.py
# http://localhost:5000  and Swagger at /apidocs/
```

```bash
# Terminal 2: frontend (Java 17+ required)
cd RA/labyrinth_ui
./gradlew lwjgl3:run
```

Full instructions live in the component READMEs.

## Documentation index

* [RA/labyrinth_gen/README.md](RA/labyrinth_gen/README.md), backend: API, endpoints, JSON shape.
* [RA/labyrinth_ui/README.md](RA/labyrinth_ui/README.md), frontend: build from source, jpackage, controls.
* [docs/algorithms/README.md](docs/algorithms/README.md), maze generation algorithms compared.
* [docs/algorithms/DFS.md](docs/algorithms/DFS.md), [docs/algorithms/Prim.md](docs/algorithms/Prim.md), [docs/algorithms/Kruskal.md](docs/algorithms/Kruskal.md).
* [RA/labyrinth_gen/tests/tests.md](RA/labyrinth_gen/tests/tests.md), test plan and CI.
* [DEPLOYMENT.md](DEPLOYMENT.md), Docker, GitHub Actions, Render.
* [docs/research/](docs/research/), per author research notes.
* [ToDo.md](ToDo.md), open tasks.

## Authors

* Abou Orm Daniel
* Kabban Joe
* Abdelbaki Kacem

TER S2, Master 1 iA, 2025 / 2026.
