package com.labyrinth.maze;

/**
 * Represents a single cell in the labyrinth grid.
 * Mirrors the JSON cell structure from the Python API.
 */
public class MazeCell {
    /** Row index. */
    public int x;
    /** Column index. */
    public int y;
    /** Walls: [Up, Right, Down, Left]. true = wall present. */
    public boolean[] walls;
    /** Whether this cell is part of the ghost room. */
    public boolean isGhost;
    /** Whether this cell is a teleport point. */
    public boolean isTP;
    /** Teleport target row (-1 if none). */
    public int tpX = -1;
    /** Teleport target column (-1 if none). */
    public int tpY = -1;

    /** Whether this cell currently holds a fruit bonus. */
    public boolean hasFruit = false;
    /** Fruit type identifier (e.g. "cherry", "strawberry", "orange", "apple", "melon"). */
    public String fruitType = null;

    public MazeCell(int x, int y) {
        this.x = x;
        this.y = y;
        this.walls = new boolean[]{true, true, true, true};
        this.isGhost = false;
        this.isTP = false;
    }

    /** Check if the wall in the given direction exists. 0=Up, 1=Right, 2=Down, 3=Left. */
    public boolean hasWall(int direction) {
        return walls[direction];
    }
}
