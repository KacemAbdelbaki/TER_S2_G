package com.labyrinth.agents;

/**
 * Abstraction for any entity that navigates the maze (Pac-Man or Ghost).
 * All agents share a grid position, visual position, and direction.
 */
public interface Agent {

    /** Called every frame with the time delta. */
    void update(float delta);

    /** Current grid row. */
    int getGridRow();

    /** Current grid column. */
    int getGridCol();

    /** Current facing direction (0=Right, 1=Up, 2=Left, 3=Down). */
    int getDirection();

    /** Smoothly interpolated screen X. */
    float getVisualX();

    /** Smoothly interpolated screen Y. */
    float getVisualY();

    /** Whether this agent has a pending movement request this frame. */
    default boolean hasPendingMove() { return false; }

    /** Requested row delta (-1=up, 1=down, 0=none). */
    default int getPendingDR() { return 0; }

    /** Requested column delta (-1=left, 1=right, 0=none). */
    default int getPendingDC() { return 0; }

    /** Acknowledge that the pending move has been consumed. */
    default void consumeMove() {}
}
