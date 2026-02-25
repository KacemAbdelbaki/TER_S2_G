package com.labyrinth.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.MathUtils;
import com.labyrinth.maze.MazeCell;
import com.labyrinth.maze.MazeData;

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
    private float moveSpeed = 12f;

    // Trail & dots
    public boolean[][] visitedCells;
    public boolean[][] dots;           // true = dot still present
    public boolean[][] powerPellets;   // true = power pellet still present
    public int dotsEaten = 0;
    public int totalDots = 0;
    public int score = 0;

    // Maze reference
    private MazeData maze;

    // Timer
    private float elapsedTime;
    private boolean won;

    // Cooldown
    private float moveCooldown = 0f;
    private static final float MOVE_COOLDOWN_TIME = 0.10f;

    public Player() {
        this.won = false;
    }

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
        if (won) return;

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

        // Poll keyboard input
        if (!moving && moveCooldown <= 0) {
            if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) {
                tryMove(-1, 0);
            } else if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
                tryMove(1, 0);
            } else if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
                tryMove(0, -1);
            } else if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
                tryMove(0, 1);
            }
        }
    }

    private void tryMove(int dr, int dc) {
        if (maze == null) return;

        int wallDir;
        if (dr == -1) wallDir = 0;       // Up
        else if (dc == 1) wallDir = 1;    // Right
        else if (dr == 1) wallDir = 2;    // Down
        else wallDir = 3;                  // Left

        MazeCell current = maze.getCell(gridRow, gridCol);
        if (current == null) return;
        if (current.hasWall(wallDir)) return;

        int newRow = gridRow + dr;
        int newCol = gridCol + dc;
        MazeCell target = maze.getCell(newRow, newCol);
        if (target == null) return;

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
            } else {
                score += 10;
            }
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
    }

    public void setTarget(float tx, float ty) {
        this.targetX = tx;
        this.targetY = ty;
        this.moving = true;
    }

    public boolean isWon() { return won; }
    public boolean isMoving() { return moving; }
    public float getElapsedTime() { return elapsedTime; }
    public int getDotsRemaining() { return totalDots - dotsEaten; }
}
