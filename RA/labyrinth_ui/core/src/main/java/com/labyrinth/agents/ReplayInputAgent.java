package com.labyrinth.agents;

import com.badlogic.gdx.utils.Array;
import com.labyrinth.replay.GameRecorder;

/**
 * Agent that replays pre-recorded inputs at their original timestamps,
 * enabling deterministic re-simulation of a recorded game.
 */
public class ReplayInputAgent implements Agent {

    private final Array<GameRecorder.InputRecord> inputs;
    private int nextIndex = 0;
    private float elapsed = 0f;

    private boolean hasPending = false;
    private int pendingDR, pendingDC;

    // Interface fields (unused for navigation)
    private int gridRow, gridCol, direction;
    private float visualX, visualY;

    public ReplayInputAgent(Array<GameRecorder.InputRecord> inputs) {
        this.inputs = inputs;
    }

    @Override
    public void update(float delta) {
        elapsed += delta;
        hasPending = false;

        // Feed the next input if its timestamp has been reached
        if (nextIndex < inputs.size && inputs.get(nextIndex).timestamp <= elapsed) {
            GameRecorder.InputRecord ir = inputs.get(nextIndex);
            pendingDR = ir.dr;
            pendingDC = ir.dc;
            hasPending = true;
            nextIndex++;
        }
    }

    /** Reset playback to the beginning. */
    public void reset() {
        nextIndex = 0;
        elapsed = 0f;
        hasPending = false;
    }

    public boolean isFinished() {
        return nextIndex >= inputs.size;
    }

    @Override public boolean hasPendingMove() { return hasPending; }
    @Override public int getPendingDR() { return pendingDR; }
    @Override public int getPendingDC() { return pendingDC; }
    @Override public void consumeMove() { hasPending = false; }

    @Override public int getGridRow() { return gridRow; }
    @Override public int getGridCol() { return gridCol; }
    @Override public int getDirection() { return direction; }
    @Override public float getVisualX() { return visualX; }
    @Override public float getVisualY() { return visualY; }
}
