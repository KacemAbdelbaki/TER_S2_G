package com.labyrinth.maze;

import java.util.ArrayList;
import java.util.List;

/**
 * Complete labyrinth data model parsed from the API JSON response.
 */
public class MazeData {
    public String id;        // MongoDB ID from the API (null if DB unavailable)
    public int height;
    public int width;
    public MazeCell[][] grid;
    public List<GenerationStep> steps;

    public MazeData(int height, int width) {
        this.height = height;
        this.width = width;
        this.grid = new MazeCell[height][width];
        this.steps = new ArrayList<>();
    }

    /** Get cell at (row, col), or null if out of bounds. */
    public MazeCell getCell(int row, int col) {
        if (row < 0 || row >= height || col < 0 || col >= width) return null;
        return grid[row][col];
    }

    /**
     * Represents a single wall-removal step during maze generation.
     */
    public static class GenerationStep {
        public int r1, c1, w1; // first cell row/col and wall index removed
        public int r2, c2, w2; // second cell row/col and wall index removed

        public GenerationStep(int r1, int c1, int w1, int r2, int c2, int w2) {
            this.r1 = r1; this.c1 = c1; this.w1 = w1;
            this.r2 = r2; this.c2 = c2; this.w2 = w2;
        }
    }
}
