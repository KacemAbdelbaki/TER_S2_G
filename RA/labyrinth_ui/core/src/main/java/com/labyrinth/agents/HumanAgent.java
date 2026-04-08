package com.labyrinth.agents;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

/**
 * Human-controlled agent that reads keyboard input (WASD / Arrows).
 *
 * Implements classic Pac-Man continuous movement:
 *   - Pac-Man keeps moving in his current direction until a wall stops him.
 *   - When the player presses a new direction key it becomes the "desired"
 *     direction.  On the next move tick the desired direction is tried first;
 *     if it is blocked, the current direction is continued instead.
 */
public class HumanAgent implements Agent {

    private int gridRow, gridCol;
    private float visualX, visualY;
    private int direction = 0; // DIR_RIGHT

    /** The direction Pac-Man is currently travelling. -1 = not moving yet. */
    private int currentDir = -1;

    /** The most recent direction requested by the player. -1 = none. */
    private int desiredDir = -1;

    private int pendingDR = 0;
    private int pendingDC = 0;
    private boolean hasPendingMove = false;

    /** Whether the agent has started moving (first key press received). */
    private boolean started = false;

    public HumanAgent() {}

    @Override
    public void update(float delta) {
        // Read keyboard — only update desiredDir, never clear it.
        // This lets the player "buffer" a turn while Pac-Man is still moving.
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) {
            desiredDir = 1; // UP  (dr=-1)
            started = true;
        } else if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            desiredDir = 3; // DOWN (dr=+1)
            started = true;
        } else if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            desiredDir = 2; // LEFT (dc=-1)
            started = true;
        } else if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            desiredDir = 0; // RIGHT (dc=+1)
            started = true;
        }

        if (!started) {
            hasPendingMove = false;
            return;
        }

        // Always produce a pending move so Pac-Man keeps moving continuously.
        // Player.tryMove() will reject it if a wall blocks the way, and we
        // fall back to currentDir in that case (handled below in consumeMove).
        hasPendingMove = true;
        applyDirection(desiredDir != -1 ? desiredDir : currentDir);
    }

    /**
     * Called by Player after a successful tryMove. We update currentDir to
     * whatever direction actually succeeded.
     */
    @Override
    public void consumeMove() {
        hasPendingMove = false;
    }

    /**
     * Notify the agent that the desired direction was blocked.
     * Fall back to continuing in the current direction.
     */
    public void desiredBlocked() {
        if (currentDir >= 0) {
            applyDirection(currentDir);
            hasPendingMove = true;
        } else {
            hasPendingMove = false;
        }
    }

    /** Record that a move in the given direction succeeded. */
    public void confirmDirection(int dir) {
        currentDir = dir;
    }

    // ── helpers ────────────────────────────────────────────────────

    private void applyDirection(int dir) {
        switch (dir) {
            case 0: pendingDR =  0; pendingDC =  1; break; // RIGHT
            case 1: pendingDR = -1; pendingDC =  0; break; // UP
            case 2: pendingDR =  0; pendingDC = -1; break; // LEFT
            case 3: pendingDR =  1; pendingDC =  0; break; // DOWN
            default: pendingDR = 0; pendingDC = 0; break;
        }
    }

    private int dirFromDeltas(int dr, int dc) {
        if (dc ==  1) return 0; // RIGHT
        if (dr == -1) return 1; // UP
        if (dc == -1) return 2; // LEFT
        if (dr ==  1) return 3; // DOWN
        return -1;
    }

    // ── Agent interface ───────────────────────────────────────────

    @Override public boolean hasPendingMove() { return hasPendingMove; }
    @Override public int getPendingDR() { return pendingDR; }
    @Override public int getPendingDC() { return pendingDC; }

    public int getDesiredDir() { return desiredDir; }
    public int getCurrentDir() { return currentDir; }

    public void setPosition(int row, int col) { this.gridRow = row; this.gridCol = col; }
    public void setVisualPosition(float x, float y) { this.visualX = x; this.visualY = y; }
    public void setDirection(int dir) { this.direction = dir; }

    @Override public int getGridRow() { return gridRow; }
    @Override public int getGridCol() { return gridCol; }
    @Override public int getDirection() { return direction; }
    @Override public float getVisualX() { return visualX; }
    @Override public float getVisualY() { return visualY; }
}
