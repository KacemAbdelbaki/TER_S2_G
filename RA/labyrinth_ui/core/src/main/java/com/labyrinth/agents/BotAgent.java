package com.labyrinth.agents;

import com.labyrinth.maze.MazeCell;
import com.labyrinth.maze.MazeData;

import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * AI-controlled agent that navigates the maze automatically.
 * Supports two strategies: GREEDY (BFS to nearest dot) and
 * GHOST_AVOIDANCE (weighted pathfinding that penalises ghost proximity).
 */
public class BotAgent implements Agent {

    public enum BotStrategy { GREEDY, GHOST_AVOIDANCE }

    // Movement delta → wall index:  Up=0, Right=1, Down=2, Left=3
    private static final int[][] DIRS = {{-1, 0}, {0, 1}, {1, 0}, {0, -1}};
    private static final int[] WALL_IDX = {0, 1, 2, 3};
    private static final int GHOST_DANGER_RADIUS = 4;

    private final MazeData maze;
    private BotStrategy strategy;

    // Live references updated each frame by the game loop
    private boolean[][] dots;
    private Ghost[] ghosts;
    private int playerRow, playerCol;

    // Pending move output
    private int pendingDR, pendingDC;
    private boolean hasPendingMove;

    // Agent interface fields (not used for navigation but required by interface)
    private int gridRow, gridCol;
    private float visualX, visualY;
    private int direction;

    public BotAgent(MazeData maze, BotStrategy strategy) {
        this.maze = maze;
        this.strategy = strategy;
    }

    /** Update live game state — call before each update(). */
    public void setGameState(boolean[][] dots, Ghost[] ghosts, int playerRow, int playerCol) {
        this.dots = dots;
        this.ghosts = ghosts;
        this.playerRow = playerRow;
        this.playerCol = playerCol;
    }

    public void setStrategy(BotStrategy strategy) { this.strategy = strategy; }
    public BotStrategy getStrategy() { return strategy; }

    @Override
    public void update(float delta) {
        hasPendingMove = false;
        if (maze == null || dots == null) return;

        int[] move;
        if (strategy == BotStrategy.GHOST_AVOIDANCE && ghosts != null) {
            move = computeGhostAvoidanceMove();
        } else {
            move = computeGreedyMove();
        }

        if (move != null) {
            pendingDR = move[0];
            pendingDC = move[1];
            hasPendingMove = true;
        }
    }

    // ------------------------------------------------------------------
    // GREEDY: BFS to nearest uneaten dot — returns first-step (dr, dc)
    // ------------------------------------------------------------------
    private int[] computeGreedyMove() {
        int h = maze.height, w = maze.width;
        // firstStep[r][c] = direction index of the first step from player to reach (r,c)
        int[][] firstStep = new int[h][w];
        boolean[][] visited = new boolean[h][w];

        Queue<int[]> queue = new LinkedList<>();
        visited[playerRow][playerCol] = true;
        firstStep[playerRow][playerCol] = -1;

        // Seed neighbours
        for (int d = 0; d < 4; d++) {
            int nr = playerRow + DIRS[d][0];
            int nc = playerCol + DIRS[d][1];
            if (canMove(playerRow, playerCol, d) && nr >= 0 && nr < h && nc >= 0 && nc < w) {
                visited[nr][nc] = true;
                firstStep[nr][nc] = d;
                if (dots[nr][nc]) return new int[]{DIRS[d][0], DIRS[d][1]};
                queue.add(new int[]{nr, nc});
            }
        }

        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            for (int d = 0; d < 4; d++) {
                int nr = cur[0] + DIRS[d][0];
                int nc = cur[1] + DIRS[d][1];
                if (nr < 0 || nr >= h || nc < 0 || nc >= w) continue;
                if (visited[nr][nc]) continue;
                if (!canMove(cur[0], cur[1], d)) continue;

                visited[nr][nc] = true;
                firstStep[nr][nc] = firstStep[cur[0]][cur[1]];
                if (dots[nr][nc]) {
                    int fd = firstStep[nr][nc];
                    return new int[]{DIRS[fd][0], DIRS[fd][1]};
                }
                queue.add(new int[]{nr, nc});
            }
        }
        return null; // no reachable dots
    }

    // ------------------------------------------------------------------
    // GHOST_AVOIDANCE: Dijkstra with ghost proximity penalty
    // ------------------------------------------------------------------
    private int[] computeGhostAvoidanceMove() {
        int h = maze.height, w = maze.width;

        // Pre-compute ghost danger map (only for non-frightened, non-eaten ghosts)
        int[][] danger = new int[h][w];
        if (ghosts != null) {
            for (Ghost g : ghosts) {
                if (g.getMode() == Ghost.GhostMode.FRIGHTENED
                        || g.getMode() == Ghost.GhostMode.EATEN) continue;
                bfsDanger(g.getGridRow(), g.getGridCol(), danger);
            }
        }

        float[][] cost = new float[h][w];
        int[][] firstStep = new int[h][w];
        boolean[][] visited = new boolean[h][w];
        for (int r = 0; r < h; r++) {
            for (int c = 0; c < w; c++) {
                cost[r][c] = Float.MAX_VALUE;
                firstStep[r][c] = -1;
            }
        }
        cost[playerRow][playerCol] = 0;

        // PQ entries: {cost, row, col}
        PriorityQueue<float[]> pq = new PriorityQueue<>((a, b) -> Float.compare(a[0], b[0]));
        pq.add(new float[]{0, playerRow, playerCol});

        while (!pq.isEmpty()) {
            float[] cur = pq.poll();
            int cr = (int) cur[1], cc = (int) cur[2];
            if (visited[cr][cc]) continue;
            visited[cr][cc] = true;

            // Found a dot — trace back first step
            if (dots[cr][cc] && (cr != playerRow || cc != playerCol)) {
                int fd = firstStep[cr][cc];
                return new int[]{DIRS[fd][0], DIRS[fd][1]};
            }

            for (int d = 0; d < 4; d++) {
                int nr = cr + DIRS[d][0];
                int nc = cc + DIRS[d][1];
                if (nr < 0 || nr >= h || nc < 0 || nc >= w) continue;
                if (visited[nr][nc]) continue;
                if (!canMove(cr, cc, d)) continue;

                float edgeCost = 1.0f + danger[nr][nc] * 3.0f;
                float newCost = cost[cr][cc] + edgeCost;
                if (newCost < cost[nr][nc]) {
                    cost[nr][nc] = newCost;
                    firstStep[nr][nc] = (cr == playerRow && cc == playerCol) ? d : firstStep[cr][cc];
                    pq.add(new float[]{newCost, nr, nc});
                }
            }
        }

        // Fallback to greedy if ghost avoidance found nothing
        return computeGreedyMove();
    }

    /** BFS-based danger spread from a ghost position: cells within GHOST_DANGER_RADIUS get a penalty. */
    private void bfsDanger(int ghostRow, int ghostCol, int[][] danger) {
        int h = maze.height, w = maze.width;
        boolean[][] vis = new boolean[h][w];
        Queue<int[]> q = new LinkedList<>();
        q.add(new int[]{ghostRow, ghostCol, 0});
        vis[ghostRow][ghostCol] = true;

        while (!q.isEmpty()) {
            int[] cur = q.poll();
            int dist = cur[2];
            int penalty = GHOST_DANGER_RADIUS - dist;
            if (penalty > 0) danger[cur[0]][cur[1]] += penalty;
            if (dist >= GHOST_DANGER_RADIUS) continue;

            for (int d = 0; d < 4; d++) {
                int nr = cur[0] + DIRS[d][0];
                int nc = cur[1] + DIRS[d][1];
                if (nr < 0 || nr >= h || nc < 0 || nc >= w) continue;
                if (vis[nr][nc]) continue;
                if (!canMove(cur[0], cur[1], d)) continue;
                vis[nr][nc] = true;
                q.add(new int[]{nr, nc, dist + 1});
            }
        }
    }

    /** Check if movement in direction d from (r,c) is possible (no wall). */
    private boolean canMove(int r, int c, int dirIdx) {
        MazeCell cell = maze.getCell(r, c);
        if (cell == null) return false;
        return !cell.hasWall(WALL_IDX[dirIdx]);
    }

    // ------------------------------------------------------------------
    // Agent interface
    // ------------------------------------------------------------------
    @Override public boolean hasPendingMove() { return hasPendingMove; }
    @Override public int getPendingDR() { return pendingDR; }
    @Override public int getPendingDC() { return pendingDC; }
    @Override public void consumeMove() { hasPendingMove = false; }

    public void setPosition(int row, int col) { this.gridRow = row; this.gridCol = col; }
    public void setVisualPosition(float x, float y) { this.visualX = x; this.visualY = y; }
    public void setDirection(int dir) { this.direction = dir; }

    @Override public int getGridRow() { return gridRow; }
    @Override public int getGridCol() { return gridCol; }
    @Override public int getDirection() { return direction; }
    @Override public float getVisualX() { return visualX; }
    @Override public float getVisualY() { return visualY; }
}
