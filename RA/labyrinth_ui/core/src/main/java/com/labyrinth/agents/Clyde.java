package com.labyrinth.agents;

import com.labyrinth.maze.MazeData;

/**
 * Clyde (Orange) — The shy one.
 * In CHASE mode:
 *   - If farther than 8 tiles from Pac-Man → targets Pac-Man directly.
 *   - If within 8 tiles → retreats to scatter corner.
 * Scatter corner: bottom-left.
 */
public class Clyde extends Ghost {

    private static final int FLEE_DISTANCE = 8;

    public Clyde(MazeData maze, int startRow, int startCol) {
        super(maze, startRow, startCol, maze.height - 1, 0);
    }

    @Override
    public int[] getTargetTile(int pacmanRow, int pacmanCol, int pacmanDir) {
        switch (mode) {
            case CHASE:
                double dist = Math.sqrt(
                    Math.pow(gridRow - pacmanRow, 2) + Math.pow(gridCol - pacmanCol, 2)
                );
                if (dist > FLEE_DISTANCE) {
                    return new int[]{pacmanRow, pacmanCol};
                } else {
                    return new int[]{scatterRow, scatterCol};
                }
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
