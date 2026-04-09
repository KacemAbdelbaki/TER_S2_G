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
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.labyrinth.Main;
import com.labyrinth.agents.*;
import com.labyrinth.maze.MazeData;
import com.labyrinth.maze.MazeRenderer;
import com.labyrinth.net.ApiClient;
import com.labyrinth.player.Player;
import com.labyrinth.replay.GameRecorder;
import com.labyrinth.ui.NeonSkin;

/**
 * Race mode: human vs AI bot on the same maze, rendered side-by-side
 * (split-screen) or overlaid with different colours.
 */
public class RaceScreen implements Screen {

    private final Main game;
    private final String algorithm;
    private final int requestHeight, requestWidth;
    private final String baseUrl;
    private final Ghost.Difficulty difficulty;
    private final BotAgent.BotStrategy botStrategy;
    private final boolean skipGhosts;

    // Display mode
    private boolean overlayMode = false;

    // Shared
    private MazeData maze;
    private boolean loading = true;
    private ApiClient apiClient;

    // Rendering
    private OrthographicCamera cameraLeft, cameraRight, cameraFull;
    private FitViewport vpLeft, vpRight, vpFull;
    private MazeRenderer rendererLeft, rendererRight, rendererFull;
    private ShapeRenderer pacmanSr;

    // HUD
    private Stage hudStage;
    private Label humanScoreLbl, botScoreLbl, timerLbl, statusLbl, modeLbl;

    // ---- Human lane ----
    private Player humanPlayer;
    private HumanAgent humanAgent;
    private Ghost[] humanGhosts;
    private GameRecorder humanRecorder;
    private float humanModeTimer;
    private int humanModePhase;
    private boolean humanFrightened;
    private float humanFrightTimer;
    private boolean humanGameOver;
    private int humanPrevRow = -1, humanPrevCol = -1;

    // ---- Bot lane ----
    private Player botPlayer;
    private BotAgent botAgent;
    private Ghost[] botGhosts;
    private GameRecorder botRecorder;
    private float botModeTimer;
    private int botModePhase;
    private boolean botFrightened;
    private float botFrightTimer;
    private boolean botGameOver;
    private int botPrevRow = -1, botPrevCol = -1;

    // Ghost mode schedule (same for both lanes)
    private static final float[] MODE_DURATIONS = {7, 20, 7, 20, 5, 20, 5, Float.MAX_VALUE};
    private static final float FRIGHTENED_DURATION = 6f;

    private boolean raceFinished = false;
    private String resultMessage = "";
    private boolean replaySaved = false;

    public RaceScreen(Main game, String algorithm, int height, int width,
                      String baseUrl, String diffString,
                      BotAgent.BotStrategy botStrategy, boolean skipGhosts) {
        this.game = game;
        this.algorithm = algorithm;
        this.requestHeight = height;
        this.requestWidth = width;
        this.baseUrl = baseUrl;
        this.difficulty = Ghost.Difficulty.valueOf(diffString);
        this.botStrategy = botStrategy;
        this.skipGhosts = skipGhosts;
    }

    @Override
    public void show() {
        // Cameras — split-screen uses half-width viewports
        float w = 1280, h = 720;

        cameraLeft = new OrthographicCamera();
        vpLeft = new FitViewport(w / 2, h, cameraLeft);
        cameraLeft.position.set(w / 4, h / 2, 0);

        cameraRight = new OrthographicCamera();
        vpRight = new FitViewport(w / 2, h, cameraRight);
        cameraRight.position.set(w / 4, h / 2, 0);

        cameraFull = new OrthographicCamera();
        vpFull = new FitViewport(w, h, cameraFull);
        cameraFull.position.set(w / 2, h / 2, 0);

        rendererLeft = new MazeRenderer();
        rendererRight = new MazeRenderer();
        rendererFull = new MazeRenderer();
        pacmanSr = new ShapeRenderer();

        // HUD
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

        TextButton toggleBtn = new TextButton("Toggle View", skin, "default");
        toggleBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent e, Actor a) {
                overlayMode = !overlayMode;
                modeLbl.setText(overlayMode ? "OVERLAY" : "SPLIT");
            }
        });

        humanScoreLbl = new Label("HUMAN: 0", skin, "default");
        humanScoreLbl.setColor(Color.YELLOW);
        botScoreLbl = new Label("BOT: 0", skin, "default");
        botScoreLbl.setColor(NeonSkin.CYAN);
        timerLbl = new Label("0.0s", skin, "default");
        statusLbl = new Label("", skin, "default");
        modeLbl = new Label("SPLIT", skin, "small");

        top.add(backBtn).padRight(15);
        top.add(humanScoreLbl).padRight(30);
        top.add(timerLbl).padRight(30);
        top.add(botScoreLbl).padRight(30);
        top.add(toggleBtn).padRight(10);
        top.add(modeLbl);
        hudStage.addActor(top);

        Table center = new Table();
        center.setFillParent(true);
        center.center();
        center.add(statusLbl);
        hudStage.addActor(center);

        InputMultiplexer mux = new InputMultiplexer();
        mux.addProcessor(hudStage);
        mux.addProcessor(new InputAdapter() {
            @Override public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.ESCAPE) {
                    game.setScreen(new MenuScreen(game));
                    return true;
                }
                if (keycode == Input.Keys.TAB) {
                    overlayMode = !overlayMode;
                    modeLbl.setText(overlayMode ? "OVERLAY" : "SPLIT");
                    return true;
                }
                return false;
            }
        });
        Gdx.input.setInputProcessor(mux);

        // Fetch maze
        statusLbl.setText("Loading maze...");
        statusLbl.setColor(NeonSkin.CYAN);
        apiClient = new ApiClient(baseUrl);
        apiClient.fetchMaze(algorithm, requestHeight, requestWidth, false, new ApiClient.MazeCallback() {
            @Override public void onSuccess(MazeData data) {
                maze = data;
                loading = false;
                initRace();
            }
            @Override public void onError(String message) {
                loading = false;
                statusLbl.setText("Error: " + message);
                statusLbl.setColor(NeonSkin.ERROR_RED);
            }
        });
    }

    // ------------------------------------------------------------------
    // Initialisation
    // ------------------------------------------------------------------

    private void initRace() {
        float halfW = vpLeft.getWorldWidth();
        float fullH = vpLeft.getWorldHeight();

        // Layout renderers
        rendererLeft.computeLayout(maze, halfW, fullH);
        rendererRight.computeLayout(maze, halfW, fullH);
        rendererFull.computeLayout(maze, vpFull.getWorldWidth(), vpFull.getWorldHeight());

        int[] start = findPacManStart();

        // Human lane
        humanPlayer = new Player();
        humanAgent = new HumanAgent();
        humanPlayer.init(maze, start[0], start[1]);
        humanPlayer.setInputAgent(humanAgent);
        float cs = rendererLeft.getCellSize();
        float px = rendererLeft.gridToScreenX(start[1]) + cs / 2f;
        float py = rendererLeft.gridToScreenY(start[0], maze.height) + cs / 2f;
        humanPlayer.setVisualPosition(px, py);
        humanPrevRow = start[0]; humanPrevCol = start[1];
        humanRecorder = new GameRecorder(maze.id, algorithm, maze.width, maze.height);
        humanPlayer.setRecorder(humanRecorder);
        if (!skipGhosts) {
            humanGhosts = spawnGhosts(rendererLeft);
        }
        humanModeTimer = 0; humanModePhase = 0;
        humanFrightened = false; humanGameOver = false;

        // Bot lane
        botPlayer = new Player();
        botAgent = new BotAgent(maze, botStrategy);
        botPlayer.init(maze, start[0], start[1]);
        botPlayer.setInputAgent(botAgent);
        cs = rendererRight.getCellSize();
        px = rendererRight.gridToScreenX(start[1]) + cs / 2f;
        py = rendererRight.gridToScreenY(start[0], maze.height) + cs / 2f;
        botPlayer.setVisualPosition(px, py);
        botPrevRow = start[0]; botPrevCol = start[1];
        botRecorder = new GameRecorder(maze.id, algorithm, maze.width, maze.height);
        botPlayer.setRecorder(botRecorder);
        if (!skipGhosts) {
            botGhosts = spawnGhosts(rendererRight);
        }
        botModeTimer = 0; botModePhase = 0;
        botFrightened = false; botGameOver = false;

        statusLbl.setText("");
    }

    private int[] findPacManStart() {
        int ghostMinR = maze.height, ghostMaxR = 0;
        int ghostMinC = maze.width, ghostMaxC = 0;
        boolean hasGR = false;
        for (int r = 0; r < maze.height; r++)
            for (int c = 0; c < maze.width; c++)
                if (maze.grid[r][c] != null && maze.grid[r][c].isGhost) {
                    hasGR = true;
                    ghostMinR = Math.min(ghostMinR, r); ghostMaxR = Math.max(ghostMaxR, r);
                    ghostMinC = Math.min(ghostMinC, c); ghostMaxC = Math.max(ghostMaxC, c);
                }
        if (hasGR) {
            int cc = (ghostMinC + ghostMaxC) / 2;
            for (int r = ghostMaxR + 1; r < maze.height; r++)
                if (maze.grid[r][cc] != null && !maze.grid[r][cc].isGhost)
                    return new int[]{r, cc};
        }
        return new int[]{maze.height - 1, maze.width / 2};
    }

    private Ghost[] spawnGhosts(MazeRenderer renderer) {
        int gMinR = maze.height, gMaxR = 0, gMinC = maze.width, gMaxC = 0;
        boolean hasGR = false;
        for (int r = 0; r < maze.height; r++)
            for (int c = 0; c < maze.width; c++)
                if (maze.grid[r][c] != null && maze.grid[r][c].isGhost) {
                    hasGR = true;
                    gMinR = Math.min(gMinR, r); gMaxR = Math.max(gMaxR, r);
                    gMinC = Math.min(gMinC, c); gMaxC = Math.max(gMaxC, c);
                }
        int cR = hasGR ? (gMinR + gMaxR) / 2 : maze.height / 2;
        int cC = hasGR ? (gMinC + gMaxC) / 2 : maze.width / 2;

        Blinky b = new Blinky(maze, cR, Math.min(cC + 1, maze.width - 1));
        Pinky  p = new Pinky(maze, cR, Math.max(cC - 1, 0));
        Inky   i = new Inky(maze, Math.min(cR + 1, maze.height - 1), Math.min(cC + 2, maze.width - 1), b);
        Clyde  cl = new Clyde(maze, cR, Math.max(cC - 2, 0));
        Ghost[] ghosts = {b, p, i, cl};

        float cs = renderer.getCellSize();
        for (Ghost g : ghosts) {
            float gx = renderer.gridToScreenX(g.getGridCol()) + cs / 2f;
            float gy = renderer.gridToScreenY(g.getGridRow(), maze.height) + cs / 2f;
            g.setVisualPosition(gx, gy);
            g.setDifficulty(difficulty);
        }
        return ghosts;
    }

    // ------------------------------------------------------------------
    // Render loop
    // ------------------------------------------------------------------

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (loading || maze == null) {
            hudStage.act(delta);
            hudStage.draw();
            return;
        }

        // Update both lanes
        updateLane(delta, humanPlayer, humanAgent, humanGhosts, rendererLeft, true);
        updateLane(delta, botPlayer, botAgent, botGhosts, rendererRight, false);
        checkRaceResult();

        // Draw
        int screenW = Gdx.graphics.getWidth();
        int screenH = Gdx.graphics.getHeight();

        if (overlayMode) {
            renderOverlay(screenW, screenH);
        } else {
            renderSplitScreen(screenW, screenH);
        }

        // HUD (always full screen)
        float elapsed = Math.max(humanPlayer.getElapsedTime(), botPlayer.getElapsedTime());
        humanScoreLbl.setText("HUMAN: " + humanPlayer.score +
            "  Dots:" + humanPlayer.getDotsRemaining() +
            "  Lives:" + humanPlayer.lives);
        botScoreLbl.setText("BOT(" + botStrategy.name() + "): " + botPlayer.score +
            "  Dots:" + botPlayer.getDotsRemaining() +
            "  Lives:" + botPlayer.lives);
        timerLbl.setText(String.format("%.1fs", elapsed));

        hudStage.act(delta);
        hudStage.draw();
    }

    // ------------------------------------------------------------------
    // Lane update (shared logic for human and bot)
    // ------------------------------------------------------------------

    private void updateLane(float delta, Player player, Agent agent,
                            Ghost[] ghosts, MazeRenderer renderer, boolean isHuman) {
        boolean laneGameOver = isHuman ? humanGameOver : botGameOver;
        if (laneGameOver || player.isWon()) return;

        // Feed bot agent with current game state
        if (!isHuman && agent instanceof BotAgent) {
            ((BotAgent) agent).setGameState(player.dots, ghosts,
                player.gridRow, player.gridCol);
        }

        player.update(delta);

        // Power pellet
        if (player.powerPelletEaten) {
            player.powerPelletEaten = false;
            if (isHuman) { humanFrightened = true; humanFrightTimer = FRIGHTENED_DURATION; }
            else { botFrightened = true; botFrightTimer = FRIGHTENED_DURATION; }
            player.resetGhostCombo();
            if (!skipGhosts && ghosts != null) {
                for (Ghost g : ghosts)
                    if (g.getMode() != Ghost.GhostMode.EATEN) g.setMode(Ghost.GhostMode.FRIGHTENED);
            }
        }

        // Grid move visual update
        int prevR = isHuman ? humanPrevRow : botPrevRow;
        int prevC = isHuman ? humanPrevCol : botPrevCol;
        if (player.gridRow != prevR || player.gridCol != prevC) {
            float cs = renderer.getCellSize();
            float tx = renderer.gridToScreenX(player.gridCol) + cs / 2f;
            float ty = renderer.gridToScreenY(player.gridRow, maze.height) + cs / 2f;
            player.setTarget(tx, ty);
            if (isHuman) { humanPrevRow = player.gridRow; humanPrevCol = player.gridCol; }
            else { botPrevRow = player.gridRow; botPrevCol = player.gridCol; }
        }

        // Ghost mode timer
        boolean frightened = isHuman ? humanFrightened : botFrightened;
        if (!frightened) {
            float modeTimer = isHuman ? humanModeTimer : botModeTimer;
            int modePhase = isHuman ? humanModePhase : botModePhase;
            modeTimer += delta;
            if (modePhase < MODE_DURATIONS.length && modeTimer >= MODE_DURATIONS[modePhase]) {
                modeTimer = 0; modePhase++;
            }
            if (isHuman) { humanModeTimer = modeTimer; humanModePhase = modePhase; }
            else { botModeTimer = modeTimer; botModePhase = modePhase; }
            
            if (!skipGhosts && ghosts != null) {
                boolean scatter = (modePhase % 2 == 0);
                for (Ghost g : ghosts)
                    if (g.getMode() != Ghost.GhostMode.EATEN)
                        g.setMode(scatter ? Ghost.GhostMode.SCATTER : Ghost.GhostMode.CHASE);
            }
        } else {
            float ft = isHuman ? humanFrightTimer : botFrightTimer;
            ft -= delta;
            if (ft <= 0) {
                if (isHuman) humanFrightened = false; else botFrightened = false;
                player.resetGhostCombo();
            }
            if (isHuman) humanFrightTimer = ft; else botFrightTimer = ft;
        }
        // Move and collide ghosts
        if (!skipGhosts && ghosts != null) {
            float cs = renderer.getCellSize();
            for (Ghost g : ghosts) {
                g.update(delta);
                if (g.isReadyToMove()) {
                    if (g.isInGhostRoom()) {
                        g.exitGhostRoom();
                    } else if (g.getMode() == Ghost.GhostMode.EATEN) {
                        g.moveTowardGhostRoom();
                    } else {
                        int[] target = g.getTargetTile(player.gridRow, player.gridCol, player.direction);
                        int dir = g.chooseDirection(target[0], target[1]);
                        g.moveInDirection(dir);
                    }
                    g.resetMoveTimer();
                    float gx = renderer.gridToScreenX(g.getGridCol()) + cs / 2f;
                    float gy = renderer.gridToScreenY(g.getGridRow(), maze.height) + cs / 2f;
                    g.setVisualTarget(gx, gy);
                }
            }
        }

        // Collision
        if (!skipGhosts && ghosts != null) {
            for (Ghost g : ghosts) {
                if (g.getGridRow() == player.gridRow && g.getGridCol() == player.gridCol) {
                    if (g.getMode() == Ghost.GhostMode.FRIGHTENED) {
                        player.eatGhost();
                        g.setMode(Ghost.GhostMode.EATEN);
                    } else if (g.getMode() != Ghost.GhostMode.EATEN) {
                        if (player.loseLife()) {
                            if (isHuman) humanGameOver = true; else botGameOver = true;
                            player.gameOver = true;
                        } else {
                            int[] start = findPacManStart();
                            player.gridRow = start[0]; player.gridCol = start[1];
                            float cs = renderer.getCellSize();
                            float px = renderer.gridToScreenX(start[1]) + cs / 2f;
                            float py = renderer.gridToScreenY(start[0], maze.height) + cs / 2f;
                            player.setVisualPosition(px, py);
                            // Respawn this lane's ghosts
                            Ghost[] newGhosts = spawnGhosts(renderer);
                            System.arraycopy(newGhosts, 0, ghosts, 0, ghosts.length);
                            break;
                        }
                    }
                }
            }
        }

        // Record frame
        if (isHuman && humanRecorder != null && humanRecorder.isRecording()) {
            recordFrame(humanRecorder, player, ghosts);
            if (player.isWon()) humanRecorder.stop();
        }
        if (!isHuman && botRecorder != null && botRecorder.isRecording()) {
            recordFrame(botRecorder, player, ghosts);
            if (player.isWon()) botRecorder.stop();
        }
    }

    private void recordFrame(GameRecorder rec, Player p, Ghost[] ghosts) {
        int[] gR = new int[4], gC = new int[4], gM = new int[4];
        float[] gVX = new float[4], gVY = new float[4];
        if (!skipGhosts && ghosts != null) {
            for (int i = 0; i < ghosts.length; i++) {
                gR[i] = ghosts[i].getGridRow();
                gC[i] = ghosts[i].getGridCol();
                gM[i] = ghosts[i].getMode().ordinal();
                gVX[i] = ghosts[i].getVisualX();
                gVY[i] = ghosts[i].getVisualY();
            }
        }
        rec.recordFrame(p.getElapsedTime(), p.gridRow, p.gridCol, p.direction,
            p.visualX, p.visualY, p.score, p.dotsEaten, p.lives, p.isWon(),
            gR, gC, gM, gVX, gVY);
    }

    private void checkRaceResult() {
        if (raceFinished) return;
        boolean hDone = humanPlayer.isWon() || humanGameOver;
        boolean bDone = botPlayer.isWon() || botGameOver;

        if (humanPlayer.isWon() && !botPlayer.isWon() && !botGameOver) {
            raceFinished = true;
            resultMessage = "HUMAN WINS!  Score: " + humanPlayer.score;
            statusLbl.setText(resultMessage);
            statusLbl.setColor(Color.YELLOW);
        } else if (botPlayer.isWon() && !humanPlayer.isWon() && !humanGameOver) {
            raceFinished = true;
            resultMessage = "BOT WINS!  Score: " + botPlayer.score;
            statusLbl.setText(resultMessage);
            statusLbl.setColor(NeonSkin.CYAN);
        } else if (hDone && bDone) {
            raceFinished = true;
            if (humanPlayer.score > botPlayer.score) {
                resultMessage = "HUMAN WINS by score! " + humanPlayer.score + " vs " + botPlayer.score;
                statusLbl.setColor(Color.YELLOW);
            } else if (botPlayer.score > humanPlayer.score) {
                resultMessage = "BOT WINS by score! " + botPlayer.score + " vs " + humanPlayer.score;
                statusLbl.setColor(NeonSkin.CYAN);
            } else {
                resultMessage = "TIE! Both scored " + humanPlayer.score;
                statusLbl.setColor(NeonSkin.ACCENT);
            }
            statusLbl.setText(resultMessage);
        }

        if (raceFinished && !replaySaved) {
            saveReplayToCloud(humanRecorder, humanPlayer, true);
        }
    }

    /** Upload finished replay to the cloud (fire-and-forget). */
    private void saveReplayToCloud(GameRecorder recorderToSave, Player playerToSave, boolean isHuman) {
        if (replaySaved || recorderToSave == null || apiClient == null) {
            return;
        }

        if (maze == null || maze.id == null) {
            Gdx.app.log("RaceScreen", "Cannot save replay: missing maze ID.");
            return;
        }

        // Finalize recorder metadata
        recorderToSave.setFinalScore(playerToSave.score);
        recorderToSave.setFinalDotsEaten(playerToSave.dotsEaten);
        recorderToSave.setWon(playerToSave.isWon());
        recorderToSave.setDurationSeconds(playerToSave.getElapsedTime());
        recorderToSave.setPlayerType(isHuman ? "human" : "bot");
        if (!isHuman && botStrategy != null) {
            recorderToSave.setBotStrategy(botStrategy.name());
        }
        recorderToSave.setMazeId(maze.id);

        String replayJson = recorderToSave.toJson();
        replaySaved = true;

        apiClient.saveReplay(replayJson, new ApiClient.ReplaySaveCallback() {
            @Override
            public void onSuccess(String replayId) {
                Gdx.app.log("RaceScreen", "Replay saved: " + replayId);
            }
            @Override
            public void onError(String message) {
                Gdx.app.log("RaceScreen", "Replay save failed: " + message);
            }
        });
    }

    // ------------------------------------------------------------------
    // Split-screen rendering
    // ------------------------------------------------------------------

    private void renderSplitScreen(int screenW, int screenH) {
        int halfW = screenW / 2;
        float worldW = vpLeft.getWorldWidth();
        float worldH = vpLeft.getWorldHeight();

        // ---- Left half — human ----
        Gdx.gl.glViewport(0, 0, halfW, screenH);
        cameraLeft.viewportWidth = worldW;
        cameraLeft.viewportHeight = worldH;
        cameraLeft.position.set(worldW / 2f, worldH / 2f, 0);
        cameraLeft.update();

        rendererLeft.render(maze, cameraLeft, -1,
            humanPlayer.gridRow, humanPlayer.gridCol,
            humanPlayer.visitedCells, humanPlayer.dots, humanPlayer.powerPellets);
        rendererLeft.renderActiveGhosts(humanGhosts, cameraLeft);
        renderPacMan(cameraLeft, humanPlayer, Color.YELLOW, rendererLeft);

        // ---- Right half — bot ----
        Gdx.gl.glViewport(halfW, 0, screenW - halfW, screenH);
        cameraRight.viewportWidth = worldW;
        cameraRight.viewportHeight = worldH;
        cameraRight.position.set(worldW / 2f, worldH / 2f, 0);
        cameraRight.update();

        rendererRight.render(maze, cameraRight, -1,
            botPlayer.gridRow, botPlayer.gridCol,
            botPlayer.visitedCells, botPlayer.dots, botPlayer.powerPellets);
        rendererRight.renderActiveGhosts(botGhosts, cameraRight);
        renderPacMan(cameraRight, botPlayer, NeonSkin.CYAN, rendererRight);

        // Restore full viewport for HUD
        Gdx.gl.glViewport(0, 0, screenW, screenH);
    }

    // ------------------------------------------------------------------
    // Overlay rendering
    // ------------------------------------------------------------------

    private void renderOverlay(int screenW, int screenH) {
        Gdx.gl.glViewport(0, 0, screenW, screenH);
        vpFull.update(screenW, screenH, true);
        cameraFull.update();

        // Render maze once (use human's dot state as "primary")
        rendererFull.render(maze, cameraFull, -1,
            humanPlayer.gridRow, humanPlayer.gridCol,
            humanPlayer.visitedCells, humanPlayer.dots, humanPlayer.powerPellets);

        // Ghosts — rendered at grid positions via the full renderer so coords match the camera
        rendererFull.renderActiveGhostsAtGrid(humanGhosts, cameraFull, maze.height);

        // Bot pac-man first (semi-transparent so human is always visible)
        renderPacManOverlay(cameraFull, botPlayer,
            new Color(NeonSkin.CYAN.r, NeonSkin.CYAN.g, NeonSkin.CYAN.b, 0.55f),
            rendererRight);
        // Human pac-man last (on top, full opacity, always visible)
        renderPacManOverlay(cameraFull, humanPlayer, Color.YELLOW, rendererLeft);
    }

    /**
     * Render Pac-Man for split-screen (uses the lane's renderer coordinates).
     */
    private void renderPacMan(OrthographicCamera cam, Player p, Color color, MazeRenderer renderer) {
        float size = renderer.getCellSize() * 0.42f;
        drawPacManShape(cam, p, color, size);
    }

    /**
     * Render Pac-Man for overlay mode. Remaps the player's smooth visual
     * position from the lane renderer's coordinate space into the full
     * renderer's coordinate space so movement is smooth, not grid-snapped.
     */
    private void renderPacManOverlay(OrthographicCamera cam, Player p, Color color,
                                      MazeRenderer laneRenderer) {
        float size = rendererFull.getCellSize() * 0.42f;
        float cx = remapX(p.visualX, laneRenderer, rendererFull);
        float cy = remapY(p.visualY, laneRenderer, rendererFull);
        drawPacManShapeAt(cam, cx, cy, p.direction, p.mouthAngle, color, size);
    }

    /** Remap an X coordinate from one renderer's space to another's. */
    private float remapX(float x, MazeRenderer from, MazeRenderer to) {
        float span = from.getCellSize() * maze.width;
        if (span < 0.001f) return x;
        float fraction = (x - from.getOriginX()) / span;
        return to.getOriginX() + fraction * to.getCellSize() * maze.width;
    }

    /** Remap a Y coordinate from one renderer's space to another's. */
    private float remapY(float y, MazeRenderer from, MazeRenderer to) {
        float span = from.getCellSize() * maze.height;
        if (span < 0.001f) return y;
        float fraction = (y - from.getOriginY()) / span;
        return to.getOriginY() + fraction * to.getCellSize() * maze.height;
    }

    private void drawPacManShape(OrthographicCamera cam, Player p, Color color, float size) {
        drawPacManShapeAt(cam, p.visualX, p.visualY, p.direction, p.mouthAngle, color, size);
    }

    private void drawPacManShapeAt(OrthographicCamera cam, float cx, float cy,
                                    int direction, float mouthAngle, Color color, float size) {
        pacmanSr.setProjectionMatrix(cam.combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // Glow
        pacmanSr.begin(ShapeRenderer.ShapeType.Filled);
        pacmanSr.setColor(new Color(color.r, color.g, color.b, 0.15f));
        pacmanSr.circle(cx, cy, size * 1.3f, 24);
        pacmanSr.end();

        // Body
        pacmanSr.begin(ShapeRenderer.ShapeType.Filled);
        pacmanSr.setColor(color);

        float baseAngle;
        switch (direction) {
            case Player.DIR_UP:    baseAngle = 90;  break;
            case Player.DIR_LEFT:  baseAngle = 180; break;
            case Player.DIR_DOWN:  baseAngle = 270; break;
            default:               baseAngle = 0;   break;
        }

        float startAngle = baseAngle + mouthAngle;
        float sweep = 360f - 2f * mouthAngle;
        int segments = 32;
        float segAngle = sweep / segments;

        for (int i = 0; i < segments; i++) {
            float a1 = (float) Math.toRadians(startAngle + segAngle * i);
            float a2 = (float) Math.toRadians(startAngle + segAngle * (i + 1));
            pacmanSr.triangle(
                cx, cy,
                cx + (float) Math.cos(a1) * size, cy + (float) Math.sin(a1) * size,
                cx + (float) Math.cos(a2) * size, cy + (float) Math.sin(a2) * size);
        }
        pacmanSr.end();
    }

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    @Override
    public void resize(int width, int height) {
        hudStage.getViewport().update(width, height, true);
        if (maze != null) {
            int halfW = width / 2;
            vpLeft.update(halfW, height, true);
            vpRight.update(width - halfW, height, true);
            vpFull.update(width, height, true);
            rendererLeft.computeLayout(maze, vpLeft.getWorldWidth(), vpLeft.getWorldHeight());
            rendererRight.computeLayout(maze, vpRight.getWorldWidth(), vpRight.getWorldHeight());
            rendererFull.computeLayout(maze, vpFull.getWorldWidth(), vpFull.getWorldHeight());
        }
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() { dispose(); }

    @Override
    public void dispose() {
        if (rendererLeft != null) rendererLeft.dispose();
        if (rendererRight != null) rendererRight.dispose();
        if (rendererFull != null) rendererFull.dispose();
        if (pacmanSr != null) pacmanSr.dispose();
        if (hudStage != null) hudStage.dispose();
    }
}
