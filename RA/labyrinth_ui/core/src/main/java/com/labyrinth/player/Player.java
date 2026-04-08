package com.labyrinth.player;

import com.badlogic.gdx.math.MathUtils;
import com.labyrinth.agents.Agent;
import com.labyrinth.maze.MazeCell;
import com.labyrinth.maze.MazeData;
import com.labyrinth.replay.GameRecorder;

/**
 * Pac-Man player entity.
 *
 * Navigates the labyrinth eating dots, supports smooth interpolated movement,
 * wall collision, teleportation, direction-aware mouth animation, and score tracking.
 */
public class Player {

    /** Movement direction constants. */
    public static final int DIR_RIGHT = 0;
    public static final int DIR_UP    = 1;
    public static final int DIR_LEFT  = 2;
    public static final int DIR_DOWN  = 3;

    // Logical position (grid coordinates)
    public int gridRow, gridCol;

    // Visual position (world coordinates, for smooth rendering)
    public float visualX, visualY;
    private float targetX, targetY;
    private boolean moving;

    // Direction Pac-Man is facing
    public int direction = DIR_RIGHT;

    // Mouth animation
    public float mouthAngle = 0f;     // current mouth opening in degrees
    private float mouthTimer = 0f;
    private static final float MOUTH_SPEED = 12f;  // oscillation speed
    public static final float MOUTH_MAX = 45f;     // max mouth opening degrees

    // Movement settings
    private float moveSpeed = 18f;

    // Trail & dots
    public boolean[][] visitedCells;
    public boolean[][] dots;           // true = dot still present
    public boolean[][] powerPellets;   // true = power pellet still present
    public int dotsEaten = 0;
    public int totalDots = 0;
    public int score = 0;

    // Lives & ghost combo
    public int lives = 3;
    private boolean extraLifeAwarded = false;
    private static final int EXTRA_LIFE_THRESHOLD = 10000;
    public int ghostCombo = 0;  // resets each power pellet
    private static final int[] GHOST_KILL_POINTS = {200, 400, 800, 1600};

    // Fruit values by type
    private static final String[] FRUIT_TYPES = {"cherry", "strawberry", "orange", "apple", "melon"};
    private static final int[] FRUIT_VALUES = {100, 300, 500, 700, 1000};

    // Maze reference
    private MazeData maze;

    // Input agent (human keyboard or AI bot)
    private Agent inputAgent;

    // Replay recorder (for input-based recording)
    private GameRecorder recorder;

    // Timer
    private float elapsedTime;
    private boolean won;

    // Power pellet flag — set to true when a power pellet is eaten, reset by game loop
    public boolean powerPelletEaten = false;

    // Game over flag
    public boolean gameOver = false;

    // Cooldown
    private float moveCooldown = 0f;
    private static final float MOVE_COOLDOWN_TIME = 0.10f;

    public Player() {
        this.won = false;
    }

    /** Set the agent that provides movement input (human or bot). */
    public void setInputAgent(Agent agent) { this.inputAgent = agent; }

    /** Set the recorder for input-level replay recording. */
    public void setRecorder(GameRecorder recorder) { this.recorder = recorder; }

    /**
     * Initialize Pac-Man for a new maze.
     * Places dots in every non-ghost corridor cell.
     * Places power pellets near the 4 corners.
     */
    public void init(MazeData maze, int startRow, int startCol) {
        this.maze = maze;
        this.gridRow = startRow;
        this.gridCol = startCol;
        this.visitedCells = new boolean[maze.height][maze.width];
        this.dots = new boolean[maze.height][maze.width];
        this.powerPellets = new boolean[maze.height][maze.width];
        this.visitedCells[startRow][startCol] = true;
        this.elapsedTime = 0;
        this.won = false;
        this.moving = false;
        this.dotsEaten = 0;
        this.totalDots = 0;
        this.score = 0;
        this.direction = DIR_RIGHT;

        // Place dots in every non-ghost cell (except start)
        for (int r = 0; r < maze.height; r++) {
            for (int c = 0; c < maze.width; c++) {
                MazeCell cell = maze.getCell(r, c);
                if (cell != null && !cell.isGhost && !(r == startRow && c == startCol)) {
                    dots[r][c] = true;
                    totalDots++;
                }
            }
        }

        // Place power pellets near 4 corners (replacing normal dots)
        placePowerPelletNear(1, 1);
        placePowerPelletNear(1, maze.width - 2);
        placePowerPelletNear(maze.height - 2, 1);
        placePowerPelletNear(maze.height - 2, maze.width - 2);
    }

    private void placePowerPelletNear(int r, int c) {
        // Search a small area around the target for a valid dot cell
        for (int dr = 0; dr <= 2; dr++) {
            for (int dc = 0; dc <= 2; dc++) {
                int rr = r + dr, cc = c + dc;
                if (rr >= 0 && rr < maze.height && cc >= 0 && cc < maze.width && dots[rr][cc]) {
                    powerPellets[rr][cc] = true;
                    return;
                }
            }
        }
    }

    public void setVisualPosition(float x, float y) {
        this.visualX = x;
        this.targetX = x;
        this.visualY = y;
        this.targetY = y;
    }

    public void update(float delta) {
        if (won || gameOver) return;

        elapsedTime += delta;

        // Mouth animation (oscillates open/closed)
        mouthTimer += delta * MOUTH_SPEED;
        mouthAngle = Math.abs(MathUtils.sin(mouthTimer)) * MOUTH_MAX;

        // Smooth movement
        if (moving) {
            visualX = MathUtils.lerp(visualX, targetX, moveSpeed * delta);
            visualY = MathUtils.lerp(visualY, targetY, moveSpeed * delta);
            if (Math.abs(visualX - targetX) < 0.5f && Math.abs(visualY - targetY) < 0.5f) {
                visualX = targetX;
                visualY = targetY;
                moving = false;
            }
        }

        // Movement cooldown
        if (moveCooldown > 0) {
            moveCooldown -= delta;
        }

        // Gate input on VISUAL arrival — sprite must reach its target before
        // the next grid-level move is allowed.  This prevents the logical
        // position from racing ahead of the visual, which looked like phasing
        // through walls.
        boolean visuallyArrived = Math.abs(visualX - targetX) < 0.5f
                               && Math.abs(visualY - targetY) < 0.5f;

        if (visuallyArrived && moveCooldown <= 0 && inputAgent != null) {
            inputAgent.update(delta);
            if (inputAgent.hasPendingMove()) {
                int dr = inputAgent.getPendingDR();
                int dc = inputAgent.getPendingDC();
                boolean moved = tryMove(dr, dc);
                if (moved) {
                    // Tell HumanAgent what direction succeeded so it becomes
                    // the "current direction" for continuous movement.
                    if (inputAgent instanceof com.labyrinth.agents.HumanAgent) {
                        int succeededDir = dirFromDeltas(dr, dc);
                        ((com.labyrinth.agents.HumanAgent) inputAgent).confirmDirection(succeededDir);
                    }
                    inputAgent.consumeMove();
                } else {
                    // The desired direction was blocked.
                    // For HumanAgent, fall back to current (continuous) direction.
                    if (inputAgent instanceof com.labyrinth.agents.HumanAgent) {
                        ((com.labyrinth.agents.HumanAgent) inputAgent).desiredBlocked();
                        if (inputAgent.hasPendingMove()) {
                            int dr2 = inputAgent.getPendingDR();
                            int dc2 = inputAgent.getPendingDC();
                            boolean moved2 = tryMove(dr2, dc2);
                            if (moved2) {
                                int succeededDir = dirFromDeltas(dr2, dc2);
                                ((com.labyrinth.agents.HumanAgent) inputAgent).confirmDirection(succeededDir);
                            }
                            inputAgent.consumeMove();
                        }
                    } else {
                        inputAgent.consumeMove();
                    }
                }
            }
        }
    }

    /** Convert dr/dc deltas to a direction constant. */
    private int dirFromDeltas(int dr, int dc) {
        if (dc ==  1) return DIR_RIGHT;
        if (dr == -1) return DIR_UP;
        if (dc == -1) return DIR_LEFT;
        if (dr ==  1) return DIR_DOWN;
        return DIR_RIGHT;
    }

    private boolean tryMove(int dr, int dc) {
        if (maze == null) return false;

        int wallDir;
        if (dr == -1) wallDir = 0;       // Up
        else if (dc == 1) wallDir = 1;    // Right
        else if (dr == 1) wallDir = 2;    // Down
        else wallDir = 3;                  // Left

        MazeCell current = maze.getCell(gridRow, gridCol);
        if (current == null) return false;
        if (current.hasWall(wallDir)) return false;

        int newRow = gridRow + dr;
        int newCol = gridCol + dc;
        MazeCell target = maze.getCell(newRow, newCol);
        if (target == null) return false;

        // Record successful input for deterministic replay
        if (recorder != null && recorder.isRecording()) {
            recorder.recordInput(elapsedTime, dr, dc);
        }

        // Update direction
        if (dc == 1) direction = DIR_RIGHT;
        else if (dc == -1) direction = DIR_LEFT;
        else if (dr == -1) direction = DIR_UP;
        else if (dr == 1) direction = DIR_DOWN;

        gridRow = newRow;
        gridCol = newCol;
        visitedCells[gridRow][gridCol] = true;
        moveCooldown = MOVE_COOLDOWN_TIME;

        // Eat dot
        if (dots[gridRow][gridCol]) {
            dots[gridRow][gridCol] = false;
            dotsEaten++;
            if (powerPellets[gridRow][gridCol]) {
                powerPellets[gridRow][gridCol] = false;
                score += 50;  // power pellet bonus
                ghostCombo = 0; // reset combo on new power pellet
                powerPelletEaten = true; // signal for MazeScreen to activate frightened
            } else {
                score += 10;
            }
            checkExtraLife();
            checkFruitSpawn();
        }

        // Eat fruit if present
        MazeCell landed = maze.getCell(gridRow, gridCol);
        if (landed != null && landed.hasFruit) {
            eatFruit(landed.fruitType);
            landed.hasFruit = false;
            landed.fruitType = null;
        }

        // Teleport
        if (target.isTP && target.tpX >= 0 && target.tpY >= 0) {
            gridRow = target.tpX;
            gridCol = target.tpY;
            visitedCells[gridRow][gridCol] = true;
            // Eat dot at TP destination too
            if (dots[gridRow][gridCol]) {
                dots[gridRow][gridCol] = false;
                dotsEaten++;
                score += 10;
            }
        }

        moving = true;

        // Win condition: all dots eaten
        if (dotsEaten >= totalDots) {
            won = true;
        }
        return true;
    }

    public void setTarget(float tx, float ty) {
        this.targetX = tx;
        this.targetY = ty;
        // Only flag as moving when the visual hasn't reached the target yet.
        if (Math.abs(visualX - tx) > 0.5f || Math.abs(visualY - ty) > 0.5f) {
            this.moving = true;
        }
    }

    public boolean isWon() { return won; }
    public boolean isMoving() { return moving; }
    public float getElapsedTime() { return elapsedTime; }
    public int getDotsRemaining() { return totalDots - dotsEaten; }

    /** Called when Pac-Man eats a ghost during FRIGHTENED mode. */
    public int eatGhost() {
        int points = GHOST_KILL_POINTS[Math.min(ghostCombo, GHOST_KILL_POINTS.length - 1)];
        ghostCombo++;
        score += points;
        checkExtraLife();
        return points;
    }

    /** Reset ghost combo counter (call when power pellet effect ends). */
    public void resetGhostCombo() {
        ghostCombo = 0;
    }

    /** Called when Pac-Man picks up a fruit. */
    public int eatFruit(String fruitType) {
        int points = 100; // default
        for (int i = 0; i < FRUIT_TYPES.length; i++) {
            if (FRUIT_TYPES[i].equals(fruitType)) {
                points = FRUIT_VALUES[i];
                break;
            }
        }
        score += points;
        checkExtraLife();
        return points;
    }

    /** Spawn a fruit at the center of the maze if conditions are met. */
    public void checkFruitSpawn() {
        if (maze == null || totalDots <= 0) return;
        // Spawn fruit at ~30% and ~60% of total dots eaten
        int firstThreshold = Math.max(5, totalDots / 3);
        int secondThreshold = Math.max(10, (totalDots * 2) / 3);
        if (dotsEaten == firstThreshold || dotsEaten == secondThreshold) {
            // Try center, then search outward for a non-ghost walkable cell
            int cr = maze.height / 2;
            int cc = maze.width / 2;
            MazeCell cell = findFruitCell(cr, cc);
            if (cell != null && !cell.hasFruit) {
                cell.hasFruit = true;
                int idx = (dotsEaten == firstThreshold) ? 0
                    : Math.min(dotsEaten / (totalDots / 5 + 1), FRUIT_TYPES.length - 1);
                cell.fruitType = FRUIT_TYPES[idx];
            }
        }
    }

    private MazeCell findFruitCell(int startR, int startC) {
        // Search expanding rings from the center for a valid non-ghost cell
        for (int dist = 0; dist < Math.max(maze.height, maze.width); dist++) {
            for (int dr = -dist; dr <= dist; dr++) {
                for (int dc = -dist; dc <= dist; dc++) {
                    if (Math.abs(dr) != dist && Math.abs(dc) != dist) continue;
                    int r = startR + dr, c = startC + dc;
                    MazeCell cell = maze.getCell(r, c);
                    if (cell != null && !cell.isGhost && !cell.hasFruit) {
                        return cell;
                    }
                }
            }
        }
        return null;
    }

    private void checkExtraLife() {
        if (!extraLifeAwarded && score >= EXTRA_LIFE_THRESHOLD) {
            lives++;
            extraLifeAwarded = true;
        }
    }

    /** Lose a life. Returns true if game over (no lives left). */
    public boolean loseLife() {
        lives--;
        return lives <= 0;
    }
}
