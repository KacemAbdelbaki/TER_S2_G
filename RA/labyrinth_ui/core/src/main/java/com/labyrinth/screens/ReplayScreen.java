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
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.labyrinth.Main;
import com.labyrinth.agents.Blinky;
import com.labyrinth.agents.Clyde;
import com.labyrinth.agents.Ghost;
import com.labyrinth.agents.Inky;
import com.labyrinth.agents.Pinky;
import com.labyrinth.maze.MazeData;
import com.labyrinth.maze.MazeRenderer;
import com.labyrinth.net.ApiClient;
import com.labyrinth.player.Player;
import com.labyrinth.replay.GameRecorder;
import com.labyrinth.replay.GameReplayer;
import com.labyrinth.ui.NeonSkin;

/**
 * Screen for watching a previously recorded game replay.
 * Uses observation-mode: steps through recorded frames and positions
 * the player/ghost sprites according to snapshot data.
 */
public class ReplayScreen implements Screen {

    private final Main game;
    private final String replayJson;

    // Replay data
    private GameReplayer replayer;
    private GameRecorder recorder;

    // Maze fetched from API
    private MazeData maze;
    private boolean loading = true;
    private String errorMessage;
    private ApiClient apiClient;
    private String baseUrl;

    // Rendering
    private OrthographicCamera camera;
    private FitViewport viewport;
    private MazeRenderer mazeRenderer;
    private ShapeRenderer pacmanSr;

    // Dots grid (updated from frame data)
    private boolean[][] dots;
    private boolean[][] powerPellets;
    private boolean[][] visitedCells;

    // HUD
    private Stage hudStage;
    private Label statusLbl, timeLbl, scoreLbl, speedLbl, frameLbl, metadataLbl;
    private Slider seekSlider;
    private boolean userSeeking = false;
    
    // Ghost simulation
    private boolean spawnGhosts = false;
    private Ghost.Difficulty ghostDifficulty = Ghost.Difficulty.CLASSIC;
    private Ghost[] replayGhosts;
    private float ghostModeTimer = 0f;
    private int ghostModePhase = 0;
    private boolean frightenedActive = false;
    private float frightenedTimer = 0f;

    // Ghost mode timer (same as MazeScreen)
    private static final float[] MODE_DURATIONS = {7, 20, 7, 20, 5, 20, 5, Float.MAX_VALUE};
    private static final float FRIGHTENED_DURATION = 6f;

    /**
     * @param game       main game instance
     * @param replayJson full JSON of the replay document from the cloud
     * @param baseUrl    API base URL for fetching the maze
     * @param spawnGhosts whether to spawn simulated ghosts
     * @param ghostDiff  ghost AI difficulty (if spawning ghosts)
     */
    public ReplayScreen(Main game, String replayJson, String baseUrl, boolean spawnGhosts, Ghost.Difficulty ghostDiff) {
        this.game = game;
        this.replayJson = replayJson;
        this.baseUrl = baseUrl;
        this.spawnGhosts = spawnGhosts;
        this.ghostDifficulty = ghostDiff != null ? ghostDiff : Ghost.Difficulty.CLASSIC;
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(1280, 720, camera);
        camera.position.set(640, 360, 0);
        mazeRenderer = new MazeRenderer();
        pacmanSr = new ShapeRenderer();

        // Parse replay
        try {
            recorder = GameRecorder.fromJson(replayJson);
            replayer = new GameReplayer(recorder);
        } catch (Exception e) {
            errorMessage = "Failed to parse replay: " + e.getMessage();
        }

        // Build HUD
        Skin skin = game.skin;
        hudStage = new Stage(new ScreenViewport());

        Table top = new Table();
        top.setFillParent(true);
        top.top().pad(10);

        TextButton backBtn = new TextButton("< Menu", skin, "default");
        backBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent e, Actor a) {
                game.setScreen(new MenuScreen(game));
            }
        });

        TextButton playPause = new TextButton("Pause", skin, "default");
        playPause.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent e, Actor a) {
                if (replayer == null) return;
                if (replayer.isPaused()) { replayer.resume(); playPause.setText("Pause"); }
                else { replayer.pause(); playPause.setText("Play"); }
            }
        });

        TextButton speedUp = new TextButton(">>", skin, "default");
        speedUp.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent e, Actor a) {
                if (replayer == null) return;
                float s = replayer.getPlaybackSpeed();
                replayer.setPlaybackSpeed(Math.min(s + 0.5f, 4f));
            }
        });

        TextButton speedDown = new TextButton("<<", skin, "default");
        speedDown.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent e, Actor a) {
                if (replayer == null) return;
                float s = replayer.getPlaybackSpeed();
                replayer.setPlaybackSpeed(Math.max(s - 0.5f, 0.25f));
            }
        });

        TextButton resetBtn = new TextButton("Reset", skin, "default");
        resetBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent e, Actor a) {
                if (replayer != null) replayer.reset();
            }
        });

        statusLbl = new Label("", skin, "default");
        timeLbl = new Label("0.0s", skin, "default");
        scoreLbl = new Label("SCORE: 0", skin, "default");
        speedLbl = new Label("1.0x", skin, "small");
        frameLbl = new Label("Frame 0/0", skin, "small");
        metadataLbl = new Label("", skin, "small");

        top.add(backBtn).padRight(12);
        top.add(scoreLbl).padRight(20);
        top.add(timeLbl).padRight(20);
        top.add(speedDown).padRight(4);
        top.add(playPause).padRight(4);
        top.add(speedUp).padRight(4);
        top.add(resetBtn).padRight(12);
        top.add(speedLbl).padRight(12);
        top.add(frameLbl).padRight(12);
        top.add(metadataLbl);
        hudStage.addActor(top);

        // Seek slider at bottom
        Table bot = new Table();
        bot.setFillParent(true);
        bot.bottom().pad(10);
        seekSlider = new Slider(0, 1, 0.001f, false, skin);
        seekSlider.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent e, Actor a) {
                if (replayer != null && userSeeking) {
                    float t = seekSlider.getValue() * replayer.getTotalDuration();
                    replayer.seekToTime(t);
                }
            }
        });
        // Detect touch begin/end for seeking
        seekSlider.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
            @Override public boolean touchDown(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                                                float x, float y, int pointer, int button) {
                userSeeking = true;
                return false; // let ChangeListener also handle it
            }
            @Override public void touchUp(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                                           float x, float y, int pointer, int button) {
                userSeeking = false;
            }
        });
        bot.add(seekSlider).width(600).padBottom(5);
        hudStage.addActor(bot);

        // Center status
        Table center = new Table();
        center.setFillParent(true);
        center.center();
        center.add(statusLbl);
        hudStage.addActor(center);

        InputMultiplexer mux = new InputMultiplexer();
        mux.addProcessor(hudStage);
        mux.addProcessor(new InputAdapter() {
            @Override public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.ESCAPE) { game.setScreen(new MenuScreen(game)); return true; }
                if (keycode == Input.Keys.SPACE && replayer != null) {
                    if (replayer.isPaused()) replayer.resume(); else replayer.pause();
                    return true;
                }
                return false;
            }
        });
        Gdx.input.setInputProcessor(mux);

        if (errorMessage != null) {
            statusLbl.setText(errorMessage);
            statusLbl.setColor(NeonSkin.ERROR_RED);
            loading = false;
            return;
        }

        // Fetch the maze for rendering
        statusLbl.setText("Loading maze...");
        statusLbl.setColor(NeonSkin.CYAN);
        apiClient = new ApiClient(baseUrl);
        String mazeId = recorder.getMazeId();
        if (mazeId != null && !mazeId.isEmpty()) {
            // Fetch the specific maze used in this recording
            apiClient.fetchMazeById(mazeId, new ApiClient.MazeCallback() {
                    @Override public void onSuccess(MazeData data) {
                        maze = data;
                        loading = false;
                        initReplay();
                    }
                    @Override public void onError(String msg) {
                        loading = false;
                        statusLbl.setText("Could not load maze: " + msg);
                        statusLbl.setColor(NeonSkin.ERROR_RED);
                    }
                });
        } else {
            loading = false;
            statusLbl.setText("No maze ID in replay — cannot render maze");
            statusLbl.setColor(NeonSkin.ERROR_RED);
        }
    }

    private void initReplay() {
        mazeRenderer.computeLayout(maze, viewport.getWorldWidth(), viewport.getWorldHeight());
        // Initialise dot/pellet grids from maze (they'll be updated from frame data)
        dots = new boolean[maze.height][maze.width];
        powerPellets = new boolean[maze.height][maze.width];
        visitedCells = new boolean[maze.height][maze.width];
        statusLbl.setText("");
        
        // Spawn simulated ghosts if requested
        if (spawnGhosts) {
            spawnReplayGhosts();
        }
        
        // Update metadata display
        updateMetadataDisplay();
    }
    
    private void updateMetadataDisplay() {
        String agentType = recorder.getPacmanAgentType();
        String ghostInfo;
        if (spawnGhosts) {
            ghostInfo = "Simulated (" + ghostDifficulty + ")";
        } else if (recorder.hadOriginalGhosts()) {
            ghostInfo = "Original";
        } else {
            ghostInfo = "None";
        }
        metadataLbl.setText("By: " + agentType.toUpperCase() + " | Ghosts: " + ghostInfo);
    }
    
    private void spawnReplayGhosts() {
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
        
        int topR = hasGhostRoom ? gMinR : maze.height / 2;
        int bottomR = hasGhostRoom ? gMaxR : maze.height / 2;
        int centerC = hasGhostRoom ? (gMinC + gMaxC) / 2 : maze.width / 2;
        
        // Blinky outside ghost room
        int outsideR = Math.max(topR - 1, 0);
        Blinky blinky = new Blinky(maze, outsideR, centerC);
        
        // Pinky, Inky, Clyde inside ghost room
        Pinky pinky = new Pinky(maze, bottomR, centerC);
        Inky inky = new Inky(maze, bottomR, Math.max(centerC - 1, 0), blinky);
        Clyde clyde = new Clyde(maze, bottomR, Math.min(centerC + 1, maze.width - 1));
        
        replayGhosts = new Ghost[]{blinky, pinky, inky, clyde};
        
        // Set visual positions
        float cs = mazeRenderer.getCellSize();
        for (Ghost g : replayGhosts) {
            float gx = mazeRenderer.gridToScreenX(g.getGridCol()) + cs / 2f;
            float gy = mazeRenderer.gridToScreenY(g.getGridRow(), maze.height) + cs / 2f;
            g.setVisualPosition(gx, gy);
            g.setDifficulty(ghostDifficulty);
        }
        
        ghostModeTimer = 0f;
        ghostModePhase = 0;
        frightenedActive = false;
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (loading || maze == null || replayer == null) {
            hudStage.act(delta);
            hudStage.draw();
            return;
        }

        // Advance replay
        GameRecorder.FrameRecord frame = replayer.update(delta);
        if (frame == null) frame = replayer.getCurrentFrame();
        if (frame == null) {
            hudStage.act(delta);
            hudStage.draw();
            return;
        }

        // Update simulated ghosts
        if (spawnGhosts && replayGhosts != null && !replayer.isFinished()) {
            updateReplayGhosts(delta, frame);
        }

        // Update HUD
        timeLbl.setText(String.format("%.1fs / %.1fs", replayer.getPlaybackTime(), replayer.getTotalDuration()));
        scoreLbl.setText("SCORE: " + frame.score + "  Lives: " + frame.lives);
        speedLbl.setText(String.format("%.1fx", replayer.getPlaybackSpeed()));
        frameLbl.setText("Frame " + (replayer.getCurrentIndex() + 1) + "/" + replayer.getTotalFrames());

        if (!userSeeking && replayer.getTotalDuration() > 0) {
            seekSlider.setValue(replayer.getPlaybackTime() / replayer.getTotalDuration());
        }

        if (replayer.isFinished()) {
            statusLbl.setText(frame.won ? "REPLAY COMPLETE — WIN!" : "REPLAY COMPLETE");
            statusLbl.setColor(frame.won ? NeonSkin.ACCENT : NeonSkin.CYAN);
        }

        // Render maze (dots not tracked frame-by-frame for observation mode, show static)
        camera.update();
        mazeRenderer.render(maze, camera, -1,
            frame.playerRow, frame.playerCol,
            visitedCells, dots, powerPellets);

        // Render Pac-Man at frame position
        float cs = mazeRenderer.getCellSize();
        float cx, cy;
        // Use visual position from frame for perfect alignment
        if (frame.playerVisualX != 0 || frame.playerVisualY != 0) {
            cx = frame.playerVisualX;
            cy = frame.playerVisualY;
        } else {
            cx = mazeRenderer.gridToScreenX(frame.playerCol) + cs / 2f;
            cy = mazeRenderer.gridToScreenY(frame.playerRow, maze.height) + cs / 2f;
        }
        renderPacManAt(cx, cy, frame.playerDir, cs * 0.42f, Color.YELLOW);

        // Render ghosts
        if (spawnGhosts && replayGhosts != null) {
            mazeRenderer.renderActiveGhosts(replayGhosts, camera);
        } else if (frame.ghostRows != null && frame.ghostRows.length > 0) {
            renderReplayGhosts(frame);
        }

        hudStage.act(delta);
        hudStage.draw();
    }

    private void renderPacManAt(float cx, float cy, int dir, float size, Color color) {
        pacmanSr.setProjectionMatrix(camera.combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        pacmanSr.begin(ShapeRenderer.ShapeType.Filled);
        pacmanSr.setColor(new Color(color.r, color.g, color.b, 0.15f));
        pacmanSr.circle(cx, cy, size * 1.3f, 24);
        pacmanSr.end();

        pacmanSr.begin(ShapeRenderer.ShapeType.Filled);
        pacmanSr.setColor(color);

        float baseAngle;
        switch (dir) {
            case Player.DIR_UP:    baseAngle = 90;  break;
            case Player.DIR_LEFT:  baseAngle = 180; break;
            case Player.DIR_DOWN:  baseAngle = 270; break;
            default:               baseAngle = 0;   break;
        }

        float mouthDeg = 30f; // fixed mouth opening for replay
        float startAngle = baseAngle + mouthDeg;
        float sweep = 360f - 2f * mouthDeg;
        int segments = 32;
        float segAngle = sweep / segments;

        for (int i = 0; i < segments; i++) {
            float a1 = (float) Math.toRadians(startAngle + segAngle * i);
            float a2 = (float) Math.toRadians(startAngle + segAngle * (i + 1));
            pacmanSr.triangle(cx, cy,
                cx + (float) Math.cos(a1) * size, cy + (float) Math.sin(a1) * size,
                cx + (float) Math.cos(a2) * size, cy + (float) Math.sin(a2) * size);
        }
        pacmanSr.end();
    }
    
    /**
     * Update simulated ghosts using real-time AI targeting the recorded Pacman position.
     */
    private void updateReplayGhosts(float delta, GameRecorder.FrameRecord frame) {
        float cs = mazeRenderer.getCellSize();
        
        // Scatter/Chase mode switching (simplified - no frightened mode in replay)
        ghostModeTimer += delta;
        if (ghostModePhase < MODE_DURATIONS.length && ghostModeTimer >= MODE_DURATIONS[ghostModePhase]) {
            ghostModeTimer = 0f;
            ghostModePhase++;
        }
        boolean scatter = (ghostModePhase % 2 == 0);
        for (Ghost g : replayGhosts) {
            if (g.getMode() != Ghost.GhostMode.EATEN) {
                g.setMode(scatter ? Ghost.GhostMode.SCATTER : Ghost.GhostMode.CHASE);
            }
        }
        
        // Move ghosts using AI targeting recorded Pacman position
        for (Ghost g : replayGhosts) {
            g.update(delta);
            if (g.isReadyToMove()) {
                if (g.isInGhostRoom()) {
                    g.exitGhostRoom();
                } else if (g.getMode() == Ghost.GhostMode.EATEN) {
                    g.moveTowardGhostRoom();
                } else {
                    // Target recorded Pacman position from frame
                    int[] target = g.getTargetTile(frame.playerRow, frame.playerCol, frame.playerDir);
                    int dir = g.chooseDirection(target[0], target[1]);
                    g.moveInDirection(dir);
                }
                g.resetMoveTimer();
                
                float gx = mazeRenderer.gridToScreenX(g.getGridCol()) + cs / 2f;
                float gy = mazeRenderer.gridToScreenY(g.getGridRow(), maze.height) + cs / 2f;
                g.setVisualTarget(gx, gy);
            }
        }
        
        // Collision detection with recorded Pacman (just mark ghost as eaten, can't kill Pacman in replay)
        for (Ghost g : replayGhosts) {
            if (g.getGridRow() == frame.playerRow && g.getGridCol() == frame.playerCol) {
                if (g.getMode() == Ghost.GhostMode.FRIGHTENED) {
                    g.setMode(Ghost.GhostMode.EATEN);
                }
            }
        }
    }

    private static final Color[] GHOST_COLORS = {
        new Color(1f, 0f, 0f, 0.85f),      // Blinky red
        new Color(1f, 0.72f, 0.84f, 0.85f), // Pinky pink
        new Color(0f, 1f, 1f, 0.85f),       // Inky cyan
        new Color(1f, 0.6f, 0f, 0.85f)      // Clyde orange
    };
    private static final Color FRIGHTENED_COLOR = new Color(0.2f, 0.2f, 1f, 0.85f);

    private void renderReplayGhosts(GameRecorder.FrameRecord frame) {
        float cs = mazeRenderer.getCellSize();
        float size = cs * 0.40f;

        pacmanSr.setProjectionMatrix(camera.combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);

        for (int i = 0; i < frame.ghostRows.length && i < 4; i++) {
            float gx, gy;
            // Use recorded visual positions for perfect alignment
            if (frame.ghostVisualX != null && i < frame.ghostVisualX.length && frame.ghostVisualX[i] != 0) {
                gx = frame.ghostVisualX[i];
                gy = frame.ghostVisualY[i];
            } else {
                gx = mazeRenderer.gridToScreenX(frame.ghostCols[i]) + cs / 2f;
                gy = mazeRenderer.gridToScreenY(frame.ghostRows[i], maze.height) + cs / 2f;
            }

            // Mode: 0=CHASE, 1=SCATTER, 2=FRIGHTENED, 3=EATEN
            Color c;
            if (frame.ghostModes != null && i < frame.ghostModes.length && frame.ghostModes[i] == 2) {
                c = FRIGHTENED_COLOR;
            } else if (frame.ghostModes != null && i < frame.ghostModes.length && frame.ghostModes[i] == 3) {
                c = new Color(1f, 1f, 1f, 0.3f); // eaten = faint
            } else {
                c = GHOST_COLORS[i];
            }

            pacmanSr.begin(ShapeRenderer.ShapeType.Filled);
            pacmanSr.setColor(c);
            // Ghost body (rounded rectangle + bottom)
            pacmanSr.rect(gx - size / 2, gy - size / 2, size, size);
            pacmanSr.circle(gx, gy + size / 2 - size * 0.05f, size / 2, 16);

            // Eyes
            pacmanSr.setColor(Color.WHITE);
            float eyeR = size * 0.12f;
            pacmanSr.circle(gx - size * 0.18f, gy + size * 0.15f, eyeR, 8);
            pacmanSr.circle(gx + size * 0.18f, gy + size * 0.15f, eyeR, 8);
            pacmanSr.setColor(Color.BLACK);
            pacmanSr.circle(gx - size * 0.18f, gy + size * 0.15f, eyeR * 0.5f, 6);
            pacmanSr.circle(gx + size * 0.18f, gy + size * 0.15f, eyeR * 0.5f, 6);
            pacmanSr.end();
        }
    }

    @Override
    public void resize(int w, int h) {
        viewport.update(w, h, true);
        camera.position.set(viewport.getWorldWidth() / 2f, viewport.getWorldHeight() / 2f, 0);
        hudStage.getViewport().update(w, h, true);
        if (maze != null) mazeRenderer.computeLayout(maze, viewport.getWorldWidth(), viewport.getWorldHeight());
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() { dispose(); }

    @Override
    public void dispose() {
        if (mazeRenderer != null) mazeRenderer.dispose();
        if (pacmanSr != null) pacmanSr.dispose();
        if (hudStage != null) hudStage.dispose();
    }
}
