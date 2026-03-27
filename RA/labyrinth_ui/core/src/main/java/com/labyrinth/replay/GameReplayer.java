package com.labyrinth.replay;

import com.badlogic.gdx.utils.Array;

/**
 * Plays back a recorded game frame by frame.
 * Feed real elapsed time and it returns the appropriate frame.
 */
public class GameReplayer {

    private final GameRecorder recorder;
    private final Array<GameRecorder.FrameRecord> frames;
    private int currentIndex = 0;
    private float playbackTime = 0f;
    private float playbackSpeed = 1.0f;
    private boolean paused = false;
    private boolean finished = false;

    public GameReplayer(GameRecorder recorder) {
        this.recorder = recorder;
        this.frames = recorder.getFrames();
    }

    /** Create a replayer from a JSON string. */
    public static GameReplayer fromJson(String jsonString) {
        return new GameReplayer(GameRecorder.fromJson(jsonString));
    }

    /**
     * Advance playback by the given delta time.
     * Returns the current frame, or null if finished.
     */
    public GameRecorder.FrameRecord update(float delta) {
        if (finished || frames.size == 0) return null;
        if (paused) return frames.get(currentIndex);

        playbackTime += delta * playbackSpeed;

        // Advance to the correct frame
        while (currentIndex < frames.size - 1
               && frames.get(currentIndex + 1).timestamp <= playbackTime) {
            currentIndex++;
        }

        if (currentIndex >= frames.size - 1) {
            finished = true;
        }

        return frames.get(currentIndex);
    }

    /** Get current frame without advancing. */
    public GameRecorder.FrameRecord getCurrentFrame() {
        if (frames.size == 0) return null;
        return frames.get(currentIndex);
    }

    /** Reset playback to the beginning. */
    public void reset() {
        currentIndex = 0;
        playbackTime = 0f;
        finished = false;
        paused = false;
    }

    /** Jump to a specific frame index. */
    public void seekToFrame(int index) {
        if (index < 0 || index >= frames.size) return;
        currentIndex = index;
        playbackTime = frames.get(index).timestamp;
        finished = (index >= frames.size - 1);
    }

    /** Jump to a specific time. */
    public void seekToTime(float time) {
        playbackTime = time;
        currentIndex = 0;
        finished = false;
        while (currentIndex < frames.size - 1
               && frames.get(currentIndex + 1).timestamp <= playbackTime) {
            currentIndex++;
        }
        if (currentIndex >= frames.size - 1) {
            finished = true;
        }
    }

    public void setPlaybackSpeed(float speed) { this.playbackSpeed = speed; }
    public float getPlaybackSpeed() { return playbackSpeed; }
    public void pause() { this.paused = true; }
    public void resume() { this.paused = false; }
    public boolean isPaused() { return paused; }
    public boolean isFinished() { return finished; }
    public int getCurrentIndex() { return currentIndex; }
    public int getTotalFrames() { return frames.size; }
    public float getTotalDuration() {
        return frames.size > 0 ? frames.get(frames.size - 1).timestamp : 0f;
    }
    public float getPlaybackTime() { return playbackTime; }
    public String getMazeId() { return recorder.getMazeId(); }
}
