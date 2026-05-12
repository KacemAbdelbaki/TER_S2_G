# Pac Man Labyrinth Visualizer (Desktop Frontend)

LibGDX based desktop client that fetches mazes from the Flask backend and
renders them with Pac Man aesthetics, gameplay, and animated generation.

> **End users:** you do not need to build this. Download a pre packaged
> binary for your OS from the project root
> [readme.md](../../readme.md#download-and-play) (Releases page).

This README is for developers who want to build and run the frontend from
source.

## Stack

| Component    | Version                | Purpose                       |
| ------------ | ---------------------- | ----------------------------- |
| Java (JDK)   | 17 or newer            | Runtime, tested with JDK 21.  |
| LibGDX       | 1.12.1                 | Game framework.               |
| LWJGL3       | bundled with LibGDX    | Desktop OpenGL backend.       |
| gdx freetype | 1.12.1                 | TTF font rendering.           |
| Gradle       | 8.5 (via `gradlew`)    | Build system, no global install needed. |

The frontend also needs the [Flask backend](../labyrinth_gen/README.md)
reachable (default `localhost:5000`).

## Run from source

```bash
cd RA/labyrinth_ui
./gradlew lwjgl3:run
```

Build a fat JAR instead:

```bash
./gradlew lwjgl3:jar
java -jar lwjgl3/build/libs/labyrinth-visualizer.jar
```

Other useful tasks:

```bash
./gradlew compileJava   # Compile only.
./gradlew clean         # Remove build artifacts.
```

## Build native packages with jpackage

The `lwjgl3` module ships Gradle tasks that wrap `jpackage` (bundled with
JDK 14+) to produce self contained app images with an embedded JRE. End
users then just double click the executable.

```bash
# Linux host
./gradlew :lwjgl3:packageLinux

# Windows host
./gradlew :lwjgl3:packageWindows

# macOS host
./gradlew :lwjgl3:packageMac

# Auto pick the task for the current host OS
./gradlew :lwjgl3:packageNative
```

`jpackage` cannot cross compile. To ship all three platforms, run on three
machines or use the GitHub Actions matrix at
[.github/workflows/release.yml](../../.github/workflows/release.yml). To
cut a release:

```bash
git tag v1.0.0
git push origin v1.0.0
```

Roughly five to ten minutes later, the
[Releases page](https://github.com/KacemAbdelbaki/TER_S2_G/releases) gets
three zips and auto generated changelog notes.

### Output locations

All artifacts land under `lwjgl3/build/jpackage/`.

| Platform | Path                                          | How to launch                |
| -------- | --------------------------------------------- | ---------------------------- |
| Windows  | `PacmanLabyrinth/PacmanLabyrinth.exe`         | Double click the exe.        |
| Linux    | `PacmanLabyrinth/bin/PacmanLabyrinth`         | Run the binary.              |
| macOS    | `PacmanLabyrinth.app`                         | Double click the .app.       |

### Optional: real installers

Edit the relevant task in [lwjgl3/build.gradle](lwjgl3/build.gradle) and
replace `'app-image'` with `'exe'`, `'msi'`, `'deb'`, `'rpm'`, `'dmg'`, or
`'pkg'`. Each format has its own external tool requirement (WiX, dpkg,
rpm build, Xcode CLT).

### Optional: app icons

Drop the following files into [assets/icons/](assets/icons/) and the next
package build will pick them up automatically:

* `windows.ico`
* `linux.png` (256 by 256 recommended)
* `mac.icns`

## Controls

### Menu

| Control  | Action                                          |
| -------- | ----------------------------------------------- |
| Mouse    | Choose algorithm, set sliders, click GENERATE.  |
| Keyboard | Type into the server address field.             |

### Maze

| Control                    | Action                              |
| -------------------------- | ----------------------------------- |
| `W` or arrow up            | Move Pac Man up.                    |
| `A` or arrow left          | Move Pac Man left.                  |
| `S` or arrow down          | Move Pac Man down.                  |
| `D` or arrow right         | Move Pac Man right.                 |
| Scroll wheel               | Zoom camera in or out (0.2x to 4x). |
| Right or middle click drag | Pan the camera.                     |
| `ESC`                      | Return to the menu.                 |

### Animation (during generation)

| Button       | Action                                |
| ------------ | ------------------------------------- |
| Pause / Play | Toggle animation playback.            |
| Restart      | Replay animation from step 0.         |
| Skip         | Jump to the completed maze.           |
| Speed slider | Adjust animation speed.               |

## Code layout

```
core/src/main/java/com/labyrinth/
  Main.java                  Game entry, font and skin setup.
  maze/
    MazeCell.java            Cell model (x, y, walls[4], isGhost, isTP, tpX, tpY).
    MazeData.java            Grid + GenerationStep list.
    MazeRenderer.java        ShapeRenderer based renderer (walls, dots, ghosts).
    GenerationAnimator.java  Step replay controller.
  net/
    ApiClient.java           Async HTTP fetch + JSON parsing.
  player/
    Player.java              Pac Man movement, eating, scoring, teleport.
  screens/
    MenuScreen.java          Algorithm and dimension selection.
    MazeScreen.java          Main game screen.
  ui/
    NeonSkin.java            Programmatic Scene2D skin.
    MazeHud.java             Score, timer, animation controls.

lwjgl3/src/main/java/com/labyrinth/lwjgl3/
  Lwjgl3Launcher.java        Desktop launcher, 1280x720 window.
```

## Graphics

* Walls: classic Pac Man blue with a two pass neon glow.
* Dots: small warm yellow circles at cell centers (10 points).
* Power pellets: pulsing yellow circles near the corners (50 points).
* Ghost room: faint red background tint, four ghost sprites
  (red, pink, cyan, orange) with bobbing animation.
* Pac Man: yellow pie slice with a sinusoidal mouth that points in the
  movement direction.
* TP links: faint magenta lines between teleport pairs.
* Win condition: eat every dot.

## Why these choices

* **LibGDX over Swing or JavaFX**: GPU accelerated ShapeRenderer, built in
  game loop, FitViewport for resolution independence, Scene2D widgets, and a
  path to Android / iOS / Web if ever needed.
* **Gradle**: standard LibGDX build, multi module, fat JAR support.
* **Separate Python backend**: algorithms were already in Python, REST
  decouples them from the renderer, and CORS opens the door to a web
  frontend later.
* **Symmetric Pac Man layout**: half maze mirrored to the right, with a
  middle connector column, central ghost room, and edge teleport tunnels.

## Troubleshooting

| Symptom                                  | Fix                                                     |
| ---------------------------------------- | ------------------------------------------------------- |
| "Connection failed" on Generate          | Start the Flask backend, see [labyrinth_gen/README.md](../labyrinth_gen/README.md). |
| Blurry text                              | Make sure `assets/DejaVuSans*.ttf` exist.               |
| Gradle build fails                       | Check `java -version`, must be 17 or newer.             |
| Port 5000 already in use                 | `lsof -i :5000` then kill the listener.                 |

## Authors

Abou Orm, Kabbat, Abdelbaki. TER S2, Master 1 iA, 2025/2026.
