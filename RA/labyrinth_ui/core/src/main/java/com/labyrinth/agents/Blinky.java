package com.labyrinth.agents;

import com.labyrinth.maze.MazeData;

/**
 * Blinky (Red) — The direct chaser.
 * In CHASE mode, targets Pac-Man's current tile.
 * Scatter corner: top-right.
 */
public class Blinky extends Ghost {

    public Blinky(MazeData maze, int startRow, int startCol) {
        super(maze, startRow, startCol, 0, maze.width - 1);
    }

    @Override
    public int[] getTargetTile(int pacmanRow, int pacmanCol, int pacmanDir) {
        switch (mode) {
            case CHASE:
                return new int[]{pacmanRow, pacmanCol};
            case SCATTER:
                return new int[]{scatterRow, scatterCol};
            case FRIGHTENED:
                // Random target — handled by caller via random direction
                return new int[]{scatterRow, scatterCol};
            case EATEN:
                // Return to ghost house center
                return new int[]{maze.height / 2, maze.width / 2};
            default:
                return new int[]{pacmanRow, pacmanCol};
        }
    }
}
