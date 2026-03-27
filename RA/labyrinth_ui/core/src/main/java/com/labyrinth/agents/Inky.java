package com.labyrinth.agents;

import com.labyrinth.maze.MazeData;

/**
 * Inky (Cyan) — The unpredictable one.
 * In CHASE mode, targets a position computed from Blinky + Pac-Man:
 *   1. Find the tile 2 ahead of Pac-Man.
 *   2. Draw a vector from Blinky to that tile.
 *   3. Double the vector → that's Inky's target.
 * Scatter corner: bottom-right.
 */
public class Inky extends Ghost {

    private static final int[] DR = {0, -1, 0, 1};
    private static final int[] DC = {1, 0, -1, 0};

    private Blinky blinky; // needs reference to Blinky's position

    public Inky(MazeData maze, int startRow, int startCol, Blinky blinky) {
        super(maze, startRow, startCol, maze.height - 1, maze.width - 1);
        this.blinky = blinky;
    }

    @Override
    public int[] getTargetTile(int pacmanRow, int pacmanCol, int pacmanDir) {
        switch (mode) {
            case CHASE:
                // 2 tiles ahead of Pac-Man
                int aheadR = pacmanRow + DR[pacmanDir] * 2;
                int aheadC = pacmanCol + DC[pacmanDir] * 2;
                // Vector from Blinky to that position, doubled
                int targetR = aheadR + (aheadR - blinky.getGridRow());
                int targetC = aheadC + (aheadC - blinky.getGridCol());
                targetR = Math.max(0, Math.min(maze.height - 1, targetR));
                targetC = Math.max(0, Math.min(maze.width - 1, targetC));
                return new int[]{targetR, targetC};
            case SCATTER:
                return new int[]{scatterRow, scatterCol};
            case FRIGHTENED:
                return new int[]{scatterRow, scatterCol};
            case EATEN:
                return new int[]{maze.height / 2, maze.width / 2};
            default:
                return new int[]{pacmanRow, pacmanCol};
        }
    }
}
