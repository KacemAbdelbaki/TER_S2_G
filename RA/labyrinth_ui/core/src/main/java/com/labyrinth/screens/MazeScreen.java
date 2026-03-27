package com.labyrinth.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.labyrinth.Main;
import com.labyrinth.maze.GenerationAnimator;
import com.labyrinth.maze.MazeData;
import com.labyrinth.maze.MazeRenderer;
import com.labyrinth.net.ApiClient;
import com.labyrinth.player.Player;
import com.labyrinth.ui.MazeHud;
import com.labyrinth.agents.Ghost;
import com.labyrinth.agents.Blinky;
import com.labyrinth.agents.Pinky;
import com.labyrinth.agents.Inky;
import com.labyrinth.agents.Clyde;
import com.labyrinth.replay.GameRecorder;

/**
 * Main Pac-Man game screen.
 * Renders labyrinth, animates generation, then lets the player
 * navigate as Pac-Man eating dots.
 */
public class MazeScreen implements Screen {

    private final Main game;
    private final String algorithm;
    private final int requestHeight, requestWidth;
    private final boolean animated;
    private final String baseUrl;

    // Rendering
    private OrthographicCamera camera;
    private FitViewport viewport;
    private MazeRenderer mazeRenderer;
    private ShapeRenderer pacmanSr;

    // Data
    private MazeData maze;
    private boolean loading = true;
    private String errorMessage = null;

    // Animation
    private GenerationAnimator animator;
    private boolean animationMode;

    // Player (Pac-Man)
    private Player player;
    private boolean playerActive = false;

    // HUD
    private MazeHud hud;

    // API client (kept for rating calls)
    private ApiClient apiClient;

    // Ghosts
    private Blinky blinky;
    private Pinky pinky;
    private Inky inky;
    private Clyde clyde;
    private Ghost[] ghosts;

    // Ghost mode timer
    private float modeTimer = 0f;
    private int modePhase = 0;
    // Classic Pac-Man scatter/chase durations (seconds)
    private static final float[] MODE_DURATIONS = {7, 20, 7, 20, 5, 20, 5, Float.MAX_VALUE};
    private boolean frightenedActive = false;
    private float frightenedTimer = 0f;
    private static final float FRIGHTENED_DURATION = 6f;

    // Replay recorder
    private GameRecorder recorder;

    // Game over flag
    private boolean gameOver = false;

    // Camera drag
    private boolean dragging = false;

    public MazeScreen(Main game, String algorithm, int height, int width,
                      boolean animated, String baseUrl) {
        this.game = game;
        this.algorithm = algorithm;
        this.requestHeight = height;
        this.requestWidth = width;
        this.animated = animated;
        this.baseUrl = baseUrl;
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(1280, 720, camera);
        camera.position.set(640, 360, 0);
        camera.update();

        mazeRenderer = new MazeRenderer();
        pacmanSr = new ShapeRenderer();
        animator = new GenerationAnimator();
        player = new Player();

        hud = new MazeHud(game.skin, () -> game.setScreen(new MenuScreen(game)));

        // Wire rating listener
        hud.setRatingListener(rating -> {
            Gdx.app.log("MazeScreen", "Rating clicked: " + rating
                + ", maze.id=" + (maze != null ? maze.id : "null")
                + ", apiClient=" + (apiClient != null)
                + ", baseUrl=" + (apiClient != null ? apiClient.getBaseUrl() : "null"));
            if (maze != null && maze.id != null && apiClient != null) {
                apiClient.rateMaze(maze.id, rating, new ApiClient.RatingCallback() {
                    @Override
                    public void onSuccess(float newRating, int totalVotes) {
                        hud.showRatingResult(newRating, totalVotes);
                    }
                    @Override
                    public void onError(String message) {
                        hud.showRatingError(message);
                    }
                });
            } else {
                hud.showRatingError("No maze ID — server may be offline or DB unavailable");
            }
        });

        // Wire animation controls
        hud.getPauseButton().addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent e, Actor a) { animator.togglePause(); }
        });
        hud.getRestartButton().addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent e, Actor a) {
                animator.restart();
                playerActive = false;
            }
        });
        hud.getSkipButton().addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent e, Actor a) {
                animator.skipToEnd();
                activatePlayer();
            }
        });
        hud.getSpeedSlider().addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent e, Actor a) {
                animator.setSpeed(hud.getSpeedSlider().getValue());
            }
        });

        InputMultiplexer mux = new InputMultiplexer();
        mux.addProcessor(hud.getStage());
        mux.addProcessor(new GameInput());
        Gdx.input.setInputProcessor(mux);

        // Fetch the maze
        hud.showLoading();
        apiClient = new ApiClient(baseUrl);
        apiClient.fetchMaze(algorithm, requestHeight, requestWidth, animated, new ApiClient.MazeCallback() {
            @Override
            public void onSuccess(MazeData data) {
                maze = data;
                loading = false;
                setupMaze();
            }

            @Override
            public void onError(String message) {
                loading = false;
                errorMessage = message;
                hud.showError(message);
            }
        });
    }

    private void setupMaze() {
        mazeRenderer.computeLayout(maze, viewport.getWorldWidth(), viewport.getWorldHeight());

        if (animated && maze.steps != null && !maze.steps.isEmpty()) {
            animationMode = true;
            animator.start(maze.steps);
            animator.setSpeed(hud.getSpeedSlider().getValue());
        } else {
            animationMode = false;
            activatePlayer();
        }
    }

    /**
     * Find Pac-Man start position: below the ghost room center, or center-bottom.
     */
    private int[] findPacManStart() {
        // Find ghost room center column
        int ghostMinR = maze.height, ghostMaxR = 0;
        int ghostMinC = maze.width, ghostMaxC = 0;
        boolean hasGhostRoom = false;
        for (int r = 0; r < maze.height; r++) {
            for (int c = 0; c < maze.width; c++) {
                if (maze.grid[r][c] != null && maze.grid[r][c].isGhost) {
                    hasGhostRoom = true;
                    ghostMinR = Math.min(ghostMinR, r);
                    ghostMaxR = Math.max(ghostMaxR, r);
                    ghostMinC = Math.min(ghostMinC, c);
                    ghostMaxC = Math.max(ghostMaxC, c);
                }
            }
        }

        if (hasGhostRoom) {
            int centerCol = (ghostMinC + ghostMaxC) / 2;
            // Start 2 rows below ghost room
            for (int r = ghostMaxR + 1; r < maze.height; r++) {
                if (maze.grid[r][centerCol] != null && !maze.grid[r][centerCol].isGhost) {
                    return new int[]{r, centerCol};
                }
            }
        }

        // Fallback: center bottom
        return new int[]{maze.height - 1, maze.width / 2};
    }

    private void activatePlayer() {
        if (maze == null) return;
        playerActive = true;

        int[] start = findPacManStart();
        player.init(maze, start[0], start[1]);

        float px = mazeRenderer.gridToScreenX(start[1]) + mazeRenderer.getCellSize() / 2f;
        float py = mazeRenderer.gridToScreenY(start[0], maze.height) + mazeRenderer.getCellSize() / 2f;
        player.setVisualPosition(px, py);

        // Spawn ghosts inside the ghost room
        spawnGhosts();

        // Start recording
        recorder = new GameRecorder(
            maze.id, algorithm, maze.width, maze.height);
    }

    private void spawnGhosts() {
        // Find ghost room center
        int gMinR = maze.height, gMaxR = 0, gMinC = maze.width, gMaxC = 0;
        boolean hasGhostRoom = false;
        for (int r = 0; r < maze.height; r++) {
            for (int c = 0; c < maze.width; c++) {
                if (maze.grid[r][c] != null && maze.grid[r][c].isGhost) {
                    hasGhostRoom = true;
                    gMinR = Math.min(gMinR, r);
                    gMaxR = Math.max(gMaxR, r);
                    gMinC = Math.min(gMinC, c);
                    gMaxC = Math.max(gMaxC, c);
                }
            }
        }

        int centerR = hasGhostRoom ? (gMinR + gMaxR) / 2 : maze.height / 2;
        int centerC = hasGhostRoom ? (gMinC + gMaxC) / 2 : maze.width / 2;

        blinky = new Blinky(maze, centerR, Math.min(centerC + 1, maze.width - 1));
        pinky  = new Pinky(maze, centerR, Math.max(centerC - 1, 0));
        inky   = new Inky(maze, centerR, Math.min(centerC + 2, maze.width - 1), blinky);
        clyde  = new Clyde(maze, centerR, Math.max(centerC - 2, 0));
        ghosts = new Ghost[]{blinky, pinky, inky, clyde};

        float cs = mazeRenderer.getCellSize();
        for (Ghost g : ghosts) {
            float gx = mazeRenderer.gridToScreenX(g.getGridCol()) + cs / 2f;
            float gy = mazeRenderer.gridToScreenY(g.getGridRow(), maze.height) + cs / 2f;
            g.setVisualPosition(gx, gy);
        }

        modeTimer = 0f;
        modePhase = 0;
        frightenedActive = false;
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(
                MazeRenderer.BG_COLOR.r,
                MazeRenderer.BG_COLOR.g,
                MazeRenderer.BG_COLOR.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (loading || maze == null) {
            hud.getStage().act(delta);
            hud.getStage().draw();
            return;
        }

        // Update animation
        if (animationMode) {
            animator.update(delta);
            if (animator.isFinished() && !playerActive) {
                activatePlayer();
            }
        }

        // Update Pac-Man
        if (playerActive && !gameOver) {
            player.update(delta);

            // Detect power pellet eat via flag set by Player
            if (ghosts != null && player.powerPelletEaten) {
                player.powerPelletEaten = false;
                activateFrightened();
            }

            float tx = mazeRenderer.gridToScreenX(player.gridCol) + mazeRenderer.getCellSize() / 2f;
            float ty = mazeRenderer.gridToScreenY(player.gridRow, maze.height) + mazeRenderer.getCellSize() / 2f;
            player.setTarget(tx, ty);

            // Update ghosts
            if (ghosts != null && !player.isWon() && !gameOver) {
                updateGhosts(delta);
            }

            // Record frame
            if (recorder != null && recorder.isRecording()) {
                int[] gRows = new int[4], gCols = new int[4], gModes = new int[4];
                if (ghosts != null) {
                    for (int i = 0; i < ghosts.length; i++) {
                        gRows[i] = ghosts[i].getGridRow();
                        gCols[i] = ghosts[i].getGridCol();
                        gModes[i] = ghosts[i].getMode().ordinal();
                    }
                }
                recorder.recordFrame(player.getElapsedTime(),
                    player.gridRow, player.gridCol, player.direction,
                    player.visualX, player.visualY,
                    player.score, player.dotsEaten, player.lives,
                    player.isWon(), gRows, gCols, gModes);
            }
            if (player.isWon() && recorder != null && recorder.isRecording()) {
                recorder.stop();
            }
        }

        camera.update();

        // Render maze with dots
        int animProgress = animationMode ? animator.getProgress() : -1;
        mazeRenderer.render(maze, camera, animProgress,
                playerActive ? player.gridRow : -1,
                playerActive ? player.gridCol : -1,
                playerActive ? player.visitedCells : null,
                playerActive ? player.dots : null,
                playerActive ? player.powerPellets : null);

        // Render Pac-Man
        if (playerActive) {
            renderPacMan();
        }

        // Render active ghosts (drawn over the maze, not the static ghost-room ghosts)
        if (playerActive && ghosts != null) {
            mazeRenderer.renderActiveGhosts(ghosts, camera);
        }

        // HUD
        float elapsed = playerActive ? player.getElapsedTime() : 0f;
        int score = playerActive ? player.score : 0;
        int dotsLeft = playerActive ? player.getDotsRemaining() : 0;
        int lives = playerActive ? player.lives : 3;
        hud.update(algorithm, maze.height, maze.width, elapsed, score, dotsLeft, lives,
                animationMode ? animator : null, playerActive, player.isWon(), gameOver);
        hud.getStage().act(delta);
        hud.getStage().draw();
    }

    /**
     * Update ghost AI, mode switching, movement, and collision.
     */
    private void updateGhosts(float delta) {
        float cs = mazeRenderer.getCellSize();

        // Scatter / Chase mode switching
        if (!frightenedActive) {
            modeTimer += delta;
            if (modePhase < MODE_DURATIONS.length && modeTimer >= MODE_DURATIONS[modePhase]) {
                modeTimer = 0f;
                modePhase++;
            }
            boolean scatter = (modePhase % 2 == 0);
            for (Ghost g : ghosts) {
                if (g.getMode() != Ghost.GhostMode.EATEN) {
                    g.setMode(scatter ? Ghost.GhostMode.SCATTER : Ghost.GhostMode.CHASE);
                }
            }
        } else {
            frightenedTimer -= delta;
            if (frightenedTimer <= 0) {
                frightenedActive = false;
                player.resetGhostCombo();
            }
        }

        // Check if player just ate a power pellet (score jumped by 50)
        // We detect it by checking if player is on a power pellet position
        // Simpler: check frightenedActive transition from Player side
        // Actually, let's detect power pellet via powerPellets grid change
        // We track via a simple flag on the maze
        // For now, trigger frightened when player eats a power pellet:
        // The player already sets score += 50 for power pellets.
        // We'll check if the player just landed on a power pellet cell.

        // Move each ghost
        for (Ghost g : ghosts) {
            g.update(delta);
            if (g.isReadyToMove()) {
                if (g.isInGhostRoom()) {
                    // Float upward out of ghost room (ignoring walls)
                    g.exitGhostRoom();
                } else {
                    int[] target = g.getTargetTile(player.gridRow, player.gridCol, player.direction);
                    int dir = g.chooseDirection(target[0], target[1]);
                    g.moveInDirection(dir);
                }
                g.resetMoveTimer();

                float gx = mazeRenderer.gridToScreenX(g.getGridCol()) + cs / 2f;
                float gy = mazeRenderer.gridToScreenY(g.getGridRow(), maze.height) + cs / 2f;
                g.setVisualTarget(gx, gy);
            }
        }

        // Collision detection: ghost touches Pac-Man
        for (Ghost g : ghosts) {
            if (g.getGridRow() == player.gridRow && g.getGridCol() == player.gridCol) {
                if (g.getMode() == Ghost.GhostMode.FRIGHTENED) {
                    // Eat ghost
                    player.eatGhost();
                    g.setMode(Ghost.GhostMode.EATEN);
                } else if (g.getMode() != Ghost.GhostMode.EATEN) {
                    // Player dies
                    if (player.loseLife()) {
                        // Game over
                        gameOver = true;
                        player.gameOver = true;
                        if (recorder != null && recorder.isRecording()) {
                            recorder.stop();
                        }
                        return;
                    } else {
                        // Respawn player + ghosts
                        int[] start = findPacManStart();
                        player.gridRow = start[0];
                        player.gridCol = start[1];
                        float px = mazeRenderer.gridToScreenX(start[1]) + cs / 2f;
                        float py = mazeRenderer.gridToScreenY(start[0], maze.height) + cs / 2f;
                        player.setVisualPosition(px, py);
                        spawnGhosts();
                    }
                    break; // only process one collision per frame
                }
            }
        }
    }

    /**
     * Called by Player when a power pellet is eaten. Activates FRIGHTENED mode.
     */
    public void activateFrightened() {
        frightenedActive = true;
        frightenedTimer = FRIGHTENED_DURATION;
        player.resetGhostCombo();
        for (Ghost g : ghosts) {
            if (g.getMode() != Ghost.GhostMode.EATEN) {
                g.setMode(Ghost.GhostMode.FRIGHTENED);
            }
        }
    }

    /**
     * Render Pac-Man as a yellow circle with an animated mouth.
     */
    private void renderPacMan() {
        float size = mazeRenderer.getCellSize() * 0.42f;
        float cx = player.visualX;
        float cy = player.visualY;
        float mouthDeg = player.mouthAngle;

        pacmanSr.setProjectionMatrix(camera.combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // Glow
        pacmanSr.begin(ShapeRenderer.ShapeType.Filled);
        pacmanSr.setColor(new Color(1f, 1f, 0f, 0.15f));
        pacmanSr.circle(cx, cy, size * 1.3f, 24);
        pacmanSr.end();

        // Pac-Man body (filled arc with mouth cutout)
        pacmanSr.begin(ShapeRenderer.ShapeType.Filled);
        pacmanSr.setColor(new Color(1f, 1f, 0f, 1f)); // Pac-Man yellow

        // Base rotation depending on direction
        float baseAngle;
        switch (player.direction) {
            case Player.DIR_UP:    baseAngle = 90;  break;
            case Player.DIR_LEFT:  baseAngle = 180; break;
            case Player.DIR_DOWN:  baseAngle = 270; break;
            default:               baseAngle = 0;   break; // RIGHT
        }

        // Draw pie-slice arc (full circle minus mouth wedge)
        float startAngle = baseAngle + mouthDeg;
        float sweep = 360f - 2f * mouthDeg;
        int segments = 32;
        float segAngle = sweep / segments;

        for (int i = 0; i < segments; i++) {
            float a1 = (float) Math.toRadians(startAngle + segAngle * i);
            float a2 = (float) Math.toRadians(startAngle + segAngle * (i + 1));
            float x1 = cx + (float) Math.cos(a1) * size;
            float y1 = cy + (float) Math.sin(a1) * size;
            float x2 = cx + (float) Math.cos(a2) * size;
            float y2 = cy + (float) Math.sin(a2) * size;
            pacmanSr.triangle(cx, cy, x1, y1, x2, y2);
        }
        pacmanSr.end();
    }

    private class GameInput extends InputAdapter {
        @Override
        public boolean scrolled(float amountX, float amountY) {
            camera.zoom += amountY * 0.1f;
            camera.zoom = MathUtils.clamp(camera.zoom, 0.2f, 4f);
            return true;
        }

        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) {
            if (button == Input.Buttons.MIDDLE || button == Input.Buttons.RIGHT) {
                dragging = true;
                return true;
            }
            return false;
        }

        @Override
        public boolean touchUp(int screenX, int screenY, int pointer, int button) {
            if (button == Input.Buttons.MIDDLE || button == Input.Buttons.RIGHT) {
                dragging = false;
                return true;
            }
            return false;
        }

        @Override
        public boolean touchDragged(int screenX, int screenY, int pointer) {
            if (dragging) {
                camera.translate(-Gdx.input.getDeltaX() * camera.zoom,
                                  Gdx.input.getDeltaY() * camera.zoom);
                return true;
            }
            return false;
        }

        @Override
        public boolean keyDown(int keycode) {
            if (keycode == Input.Keys.ESCAPE) {
                game.setScreen(new MenuScreen(game));
                return true;
            }
            return false;
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        camera.position.set(viewport.getWorldWidth() / 2f, viewport.getWorldHeight() / 2f, 0);
        hud.resize(width, height);
        if (maze != null) {
            mazeRenderer.computeLayout(maze, viewport.getWorldWidth(), viewport.getWorldHeight());
            if (playerActive) {
                float px = mazeRenderer.gridToScreenX(player.gridCol) + mazeRenderer.getCellSize() / 2f;
                float py = mazeRenderer.gridToScreenY(player.gridRow, maze.height) + mazeRenderer.getCellSize() / 2f;
                player.setVisualPosition(px, py);
            }
        }
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() { dispose(); }

    @Override
    public void dispose() {
        if (mazeRenderer != null) mazeRenderer.dispose();
        if (pacmanSr != null) pacmanSr.dispose();
        if (hud != null) hud.dispose();
    }
}
