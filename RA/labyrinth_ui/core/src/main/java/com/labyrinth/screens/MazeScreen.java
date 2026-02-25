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
    private final String host;
    private final int port;

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

    // Camera drag
    private boolean dragging = false;

    public MazeScreen(Main game, String algorithm, int height, int width,
                      boolean animated, String host, int port) {
        this.game = game;
        this.algorithm = algorithm;
        this.requestHeight = height;
        this.requestWidth = width;
        this.animated = animated;
        this.host = host;
        this.port = port;
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
        ApiClient client = new ApiClient(host, port);
        client.fetchMaze(algorithm, requestHeight, requestWidth, animated, new ApiClient.MazeCallback() {
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
        if (playerActive) {
            player.update(delta);
            float tx = mazeRenderer.gridToScreenX(player.gridCol) + mazeRenderer.getCellSize() / 2f;
            float ty = mazeRenderer.gridToScreenY(player.gridRow, maze.height) + mazeRenderer.getCellSize() / 2f;
            player.setTarget(tx, ty);
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

        // HUD
        float elapsed = playerActive ? player.getElapsedTime() : 0f;
        int score = playerActive ? player.score : 0;
        int dotsLeft = playerActive ? player.getDotsRemaining() : 0;
        hud.update(algorithm, maze.height, maze.width, elapsed, score, dotsLeft,
                animationMode ? animator : null, playerActive, player.isWon());
        hud.getStage().act(delta);
        hud.getStage().draw();
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
