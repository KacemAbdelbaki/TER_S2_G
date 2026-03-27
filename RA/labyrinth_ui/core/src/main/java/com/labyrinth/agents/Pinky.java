package com.labyrinth.agents;

import com.labyrinth.maze.MazeData;

/**
 * Pinky (Pink) — The ambusher.
 * In CHASE mode, targets 4 tiles ahead of Pac-Man in his current direction.
 * Scatter corner: top-left.
 */
public class Pinky extends Ghost {

    // Direction deltas: Right(0), Up(1), Left(2), Down(3)
    private static final int[] DR = {0, -1, 0, 1};
    private static final int[] DC = {1, 0, -1, 0};

    public Pinky(MazeData maze, int startRow, int startCol) {
        super(maze, startRow, startCol, 0, 0);
    }

    @Override
    public int[] getTargetTile(int pacmanRow, int pacmanCol, int pacmanDir) {
        switch (mode) {
            case CHASE:
                // 4 tiles ahead of Pac-Man
                int targetR = pacmanRow + DR[pacmanDir] * 4;
                int targetC = pacmanCol + DC[pacmanDir] * 4;
                // Clamp to maze bounds
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
