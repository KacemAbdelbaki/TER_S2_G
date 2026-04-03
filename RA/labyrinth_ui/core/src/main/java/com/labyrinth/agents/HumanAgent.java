package com.labyrinth.agents;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
//import com.labyrinth.maze.MazeData;

/**
 * Human-controlled agent that reads keyboard input (WASD / Arrows).
 * Designed to be used as the input provider for the Player entity.
 */
public class HumanAgent implements Agent {

    private int gridRow, gridCol;
    private float visualX, visualY;
    private int direction = 0; // DIR_RIGHT

    private int pendingDR = 0;
    private int pendingDC = 0;
    private boolean hasPendingMove = false;

    public HumanAgent() {}

    @Override
    public void update(float delta) {
        hasPendingMove = false;
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) {
            pendingDR = -1; pendingDC = 0; hasPendingMove = true;
        } else if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            pendingDR = 1; pendingDC = 0; hasPendingMove = true;
        } else if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            pendingDR = 0; pendingDC = -1; hasPendingMove = true;
        } else if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            pendingDR = 0; pendingDC = 1; hasPendingMove = true;
        }
    }

    /** Whether the human pressed a movement key this frame. */
    @Override public boolean hasPendingMove() { return hasPendingMove; }

    /** Requested row delta (-1=up, 1=down, 0=none). */
    @Override public int getPendingDR() { return pendingDR; }

    /** Requested column delta (-1=left, 1=right, 0=none). */
    @Override public int getPendingDC() { return pendingDC; }

    @Override
    public void consumeMove() { hasPendingMove = false; }

    public void setPosition(int row, int col) { this.gridRow = row; this.gridCol = col; }
    public void setVisualPosition(float x, float y) { this.visualX = x; this.visualY = y; }
    public void setDirection(int dir) { this.direction = dir; }

    @Override public int getGridRow() { return gridRow; }
    @Override public int getGridCol() { return gridCol; }
    @Override public int getDirection() { return direction; }
    @Override public float getVisualX() { return visualX; }
    @Override public float getVisualY() { return visualY; }
}
