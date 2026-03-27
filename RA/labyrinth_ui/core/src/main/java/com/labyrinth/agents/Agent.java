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
}
