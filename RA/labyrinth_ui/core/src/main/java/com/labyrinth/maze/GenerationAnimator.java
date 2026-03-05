package com.labyrinth.maze;

import com.labyrinth.maze.MazeData.GenerationStep;

import java.util.List;

/**
 * Controls step-by-step animation of maze generation.
 */
public class GenerationAnimator {

    private List<GenerationStep> steps;
    private int currentStep;
    private float timer;
    private float stepInterval; // seconds per step
    private boolean playing;
    private boolean finished;

    public GenerationAnimator() {
        this.stepInterval = 0.04f; // 40ms default
        this.playing = false;
        this.finished = true;
    }

    /**
     * Start the animation with the given steps.
     */
    public void start(List<GenerationStep> steps) {
        this.steps = steps;
        this.currentStep = 0;
        this.timer = 0f;
        this.playing = true;
        this.finished = (steps == null || steps.isEmpty());
    }

    /**
     * Update the animation timer.
     * @param delta frame delta time in seconds
     */
    public void update(float delta) {
        if (!playing || finished || steps == null) return;

        timer += delta;
        while (timer >= stepInterval && currentStep < steps.size()) {
            currentStep++;
            timer -= stepInterval;
        }
        if (currentStep >= steps.size()) {
            finished = true;
            playing = false;
        }
    }

    /** Get the number of steps revealed so far. -1 if not animating. */
    public int getProgress() {
        if (finished && steps != null) return -1; // show all
        return currentStep;
    }

    public void togglePause() {
        if (!finished) {
            playing = !playing;
        }
    }

    public void restart() {
        if (steps != null) start(steps);
    }

    public boolean isPlaying() { return playing; }
    public boolean isFinished() { return finished; }

    public float getStepInterval() { return stepInterval; }

    public void setStepInterval(float interval) {
        this.stepInterval = Math.max(0.005f, Math.min(0.5f, interval));
    }

    /** Speed multiplier: 0.005 .. 0.5 mapped to slider 0..1 */
    public void setSpeed(float normalised) {
        // normalised 0 = slow (0.2s), 1 = fast (0.005s)
        this.stepInterval = 0.2f - normalised * 0.195f;
    }

    public int getTotalSteps() {
        return steps != null ? steps.size() : 0;
    }

    public int getCurrentStep() {
        return currentStep;
    }

    /** Force-finish: reveal all walls instantly. */
    public void skipToEnd() {
        if (steps != null) {
            currentStep = steps.size();
        }
        finished = true;
        playing = false;
    }
}
