package com.labyrinth.agents;

import com.badlogic.gdx.math.MathUtils;
import com.labyrinth.maze.MazeCell;
import com.labyrinth.maze.MazeData;

/**
 * Abstract base class for all ghost entities.
 *
 * Each ghost subclass provides a target tile via {@link #getTargetTile}.
 * The base class handles movement toward the target using the classic
 * Pac-Man rules: never reverse direction, prefer Up > Left > Down > Right
 * when distances are tied.
 */
public abstract class Ghost implements Agent {

    public enum GhostMode { CHASE, SCATTER, FRIGHTENED, EATEN }
    public enum Difficulty { CLASSIC, A_STAR }

    protected Difficulty difficulty = Difficulty.CLASSIC;

    // Grid position
    protected int gridRow, gridCol;

    // Visual position (for smooth rendering)
    protected float visualX, visualY;
    protected float targetX, targetY;

    // Direction: 0=Right, 1=Up, 2=Left, 3=Down
    protected int direction = 2; // start facing left

    // Academic Metrics
    protected long lastComputeTimeNs = 0;
    protected int lastNodesEvaluated = 0;

    // Mode
    protected GhostMode mode = GhostMode.SCATTER;

    // Reference data
    protected MazeData maze;
    protected float moveTimer = 0f;
    protected float moveInterval = 0.22f; // seconds between grid moves

    // Scatter target corner (each ghost has its own)
    protected int scatterRow;
    protected int scatterCol;

    // Movement deltas indexed by direction: Right, Up, Left, Down
    private static final int[] DR = {0, -1, 0, 1};
    private static final int[] DC = {1, 0, -1, 0};

    // Ghost dir → MazeCell wall index mapping
    // Ghost:  0=Right, 1=Up, 2=Left, 3=Down
    // Walls:  0=Up, 1=Right, 2=Down, 3=Left
    private static final int[] DIR_TO_WALL = {1, 0, 3, 2};

    public Ghost(MazeData maze, int startRow, int startCol, int scatterRow, int scatterCol) {
        this.maze = maze;
        this.gridRow = startRow;
        this.gridCol = startCol;
        this.scatterRow = scatterRow;
        this.scatterCol = scatterCol;
    }

    /**
     * Return the target tile for this ghost in CHASE mode.
     * Subclasses implement their unique targeting strategy.
     */
    public abstract int[] getTargetTile(int pacmanRow, int pacmanCol, int pacmanDir);

    @Override
    public void update(float delta) {
        moveTimer += delta;
        // Do NOT consume the timer here — the game loop checks isReadyToMove()
        // and calls resetMoveTimer() after actually moving the ghost.

        // Smooth visual interpolation
        float speed = 10f;
        visualX = MathUtils.lerp(visualX, targetX, speed * delta);
        visualY = MathUtils.lerp(visualY, targetY, speed * delta);
    }

    /**
     * Choose the best direction toward the given target tile.
     * Classic Pac-Man rules: can't reverse, prefer Up > Left > Down > Right on tie.
     * If difficulty is A_STAR, uses A* pathfinding to find the shortest maze path.
     */
    public int chooseDirection(int targetRow, int targetCol) {
        long startTime = System.nanoTime();
        int dir;
        if (difficulty == Difficulty.A_STAR && mode != GhostMode.FRIGHTENED) {
            dir = chooseDirectionAStar(targetRow, targetCol);
        } else {
            // Eaten, scattered, frightened, or classic all fall under classic search
            dir = chooseDirectionClassic(targetRow, targetCol);
        }
        lastComputeTimeNs = System.nanoTime() - startTime;
        return dir;
    }

    /**
     * Finds the next move using A* to get the shortest path to the target tile.
     */
    private int chooseDirectionAStar(int targetRow, int targetCol) {
        // Simple A* implementation
        class Node implements Comparable<Node> {
            int r, c, g, h, dir;

            Node(int r, int c, int g, int h, int dir) {
                this.r = r; this.c = c; this.g = g; this.h = h; this.dir = dir;
            }

            @Override
            public int compareTo(Node other) {
                return (g + h) - (other.g + other.h);
            }
        }

        java.util.PriorityQueue<Node> open = new java.util.PriorityQueue<>();
        boolean[][] visited = new boolean[maze.height][maze.width];
        
        // Initial neighbors (must not reverse)
        int reverseDir = (direction + 2) % 4;
        MazeCell startCell = maze.getCell(gridRow, gridCol);
        
        for (int d = 0; d < 4; d++) {
            if (d == reverseDir || startCell.hasWall(DIR_TO_WALL[d])) continue;
            int nr = gridRow + DR[d], nc = gridCol + DC[d];
            if (maze.getCell(nr, nc) != null) {
                int h = Math.abs(nr - targetRow) + Math.abs(nc - targetCol);
                open.add(new Node(nr, nc, 1, h, d));
            }
        }

        //long startNs = System.nanoTime();
        int nodes = 0;
        while (!open.isEmpty()) {
            Node current = open.poll();
            nodes++;
            if (current.r == targetRow && current.c == targetCol) {
                lastNodesEvaluated = nodes;
                return current.dir;
            }
            if (visited[current.r][current.c]) continue;
            visited[current.r][current.c] = true;

            MazeCell cell = maze.getCell(current.r, current.c);
            for (int d = 0; d < 4; d++) {
                if (cell.hasWall(DIR_TO_WALL[d])) continue;
                int nr = current.r + DR[d], nc = current.c + DC[d];
                if (nr >= 0 && nr < maze.height && nc >= 0 && nc < maze.width && !visited[nr][nc]) {
                    int h = Math.abs(nr - targetRow) + Math.abs(nc - targetCol);
                    open.add(new Node(nr, nc, current.g + 1, h, current.dir));
                }
            }
        }

        lastNodesEvaluated = nodes;
        // Fallback to classic if no path found
        return chooseDirectionClassic(targetRow, targetCol);
    }

    private int chooseDirectionClassic(int targetRow, int targetCol) {
        lastNodesEvaluated = 0;
        int reverseDir = (direction + 2) % 4;
        int bestDir = -1;
        float bestDist = Float.MAX_VALUE;
        int[] priority = {1, 2, 3, 0};
        MazeCell current = maze.getCell(gridRow, gridCol);
        if (current == null) return direction;

        for (int dir : priority) {
            if (dir == reverseDir || current.hasWall(DIR_TO_WALL[dir])) continue;
            int newR = gridRow + DR[dir], newC = gridCol + DC[dir];
            if (maze.getCell(newR, newC) == null) continue;

            lastNodesEvaluated++;
            float dist = (newR - targetRow) * (newR - targetRow) + (newC - targetCol) * (newC - targetCol);
            if (dist < bestDist) { bestDist = dist; bestDir = dir; }
        }

        // If no non-reverse direction found, allow reverse as last resort
        if (bestDir < 0) {
            if (!current.hasWall(DIR_TO_WALL[reverseDir])) {
                int newR = gridRow + DR[reverseDir];
                int newC = gridCol + DC[reverseDir];
                if (maze.getCell(newR, newC) != null) {
                    lastNodesEvaluated++;
                    bestDir = reverseDir;
                }
            }
        }

        return bestDir >= 0 ? bestDir : -1;
    }

    /**
     * Move one step in the given direction (called by game loop).
     * Returns false if direction is invalid or movement would go through a wall.
     */
    public boolean moveInDirection(int dir) {
        if (dir < 0 || dir > 3) return false;
        MazeCell current = maze.getCell(gridRow, gridCol);
        if (current == null || current.hasWall(DIR_TO_WALL[dir])) return false;
        int newR = gridRow + DR[dir];
        int newC = gridCol + DC[dir];
        if (maze.getCell(newR, newC) == null) return false;
        this.direction = dir;
        this.gridRow = newR;
        this.gridCol = newC;
        return true;
    }

    /** Whether it's time for the next grid move. */
    public boolean isReadyToMove() {
        return moveTimer >= moveInterval;
    }

    public void resetMoveTimer() { moveTimer = 0f; }

    /** Returns true if the ghost is currently on a ghost-room cell. */
    public boolean isInGhostRoom() {
        MazeCell cell = maze.getCell(gridRow, gridCol);
        return cell != null && cell.isGhost;
    }

    /**
     * Move ghost upward one row toward the ghost-room exit, ignoring walls.
     * Used while the ghost is inside the ghost room.
     */
    public void exitGhostRoom() {
        int newR = gridRow - 1;
        if (newR >= 0 && maze.getCell(newR, gridCol) != null) {
            gridRow = newR;
            direction = 1; // Up
        }
    }

    /**
     * Move one step toward the ghost room center, ignoring walls.
     * Used when the ghost is EATEN and returning to respawn.
     */
    public void moveTowardGhostRoom() {
        int targetR = maze.height / 2;
        int targetC = maze.width / 2;

        int dr = Integer.compare(targetR, gridRow);
        int dc = Integer.compare(targetC, gridCol);

        // Prefer vertical movement first, then horizontal
        if (dr != 0) {
            int newR = gridRow + dr;
            if (newR >= 0 && newR < maze.height && maze.getCell(newR, gridCol) != null) {
                gridRow = newR;
                direction = (dr < 0) ? 1 : 3; // Up or Down
                return;
            }
        }
        if (dc != 0) {
            int newC = gridCol + dc;
            if (newC >= 0 && newC < maze.width && maze.getCell(gridRow, newC) != null) {
                gridCol = newC;
                direction = (dc > 0) ? 0 : 2; // Right or Left
                return;
            }
        }

        // Arrived at ghost room — switch back to normal mode
        if (gridRow == targetR && gridCol == targetC) {
            mode = GhostMode.SCATTER;
        }
    }

    public void setMode(GhostMode mode) { this.mode = mode; }
    public GhostMode getMode() { return mode; }

    public void setDifficulty(Difficulty difficulty) { this.difficulty = difficulty; }
    public Difficulty getDifficulty() { return difficulty; }

    public long getLastComputeTimeNs() { return lastComputeTimeNs; }
    public int getLastNodesEvaluated() { return lastNodesEvaluated; }

    public void setVisualTarget(float x, float y) { this.targetX = x; this.targetY = y; }
    public void setVisualPosition(float x, float y) { this.visualX = x; this.visualY = y; this.targetX = x; this.targetY = y; }

    @Override public int getGridRow() { return gridRow; }
    @Override public int getGridCol() { return gridCol; }
    @Override public int getDirection() { return direction; }
    @Override public float getVisualX() { return visualX; }
    @Override public float getVisualY() { return visualY; }
}
