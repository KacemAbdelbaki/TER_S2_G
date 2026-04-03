package com.labyrinth.replay;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;

/**
 * Records each frame of a Pac-Man game for later replay.
 * Stores player position, direction, score, ghost positions and modes.
 */
public class GameRecorder {

    /** A single snapshot of game state. */
    public static class FrameRecord {
        public float timestamp;
        public int playerRow, playerCol, playerDir;
        public float playerVisualX, playerVisualY;
        public int score, dotsEaten, lives;
        public boolean won;
        // Ghost data: arrays indexed by ghost index (0=Blinky, 1=Pinky, 2=Inky, 3=Clyde)
        public int[] ghostRows;
        public int[] ghostCols;
        public int[] ghostModes; // ordinal of GhostMode enum

        public FrameRecord() {}
    }

    /** A single player input event (successful move). */
    public static class InputRecord {
        public float timestamp;
        public int dr, dc;

        public InputRecord() {}
        public InputRecord(float timestamp, int dr, int dc) {
            this.timestamp = timestamp;
            this.dr = dr;
            this.dc = dc;
        }
    }

    private Array<FrameRecord> frames = new Array<>();
    private Array<InputRecord> inputs = new Array<>();
    private String mazeId;
    private String algorithm;
    private int mazeWidth, mazeHeight;
    private boolean recording = true;

    public GameRecorder(String mazeId, String algorithm, int mazeWidth, int mazeHeight) {
        this.mazeId = mazeId;
        this.algorithm = algorithm;
        this.mazeWidth = mazeWidth;
        this.mazeHeight = mazeHeight;
    }

    /**
     * Record a single frame of gameplay.
     */
    public void recordFrame(float timestamp,
                            int playerRow, int playerCol, int playerDir,
                            float playerVisualX, float playerVisualY,
                            int score, int dotsEaten, int lives, boolean won,
                            int[] ghostRows, int[] ghostCols, int[] ghostModes) {
        if (!recording) return;

        FrameRecord frame = new FrameRecord();
        frame.timestamp = timestamp;
        frame.playerRow = playerRow;
        frame.playerCol = playerCol;
        frame.playerDir = playerDir;
        frame.playerVisualX = playerVisualX;
        frame.playerVisualY = playerVisualY;
        frame.score = score;
        frame.dotsEaten = dotsEaten;
        frame.lives = lives;
        frame.won = won;
        frame.ghostRows = ghostRows != null ? ghostRows.clone() : new int[0];
        frame.ghostCols = ghostCols != null ? ghostCols.clone() : new int[0];
        frame.ghostModes = ghostModes != null ? ghostModes.clone() : new int[0];

        frames.add(frame);
    }

    /** Stop recording. */
    public void stop() {
        recording = false;
    }

    /**
     * Record a successful player input (called by Player on each tryMove success).
     */
    public void recordInput(float timestamp, int dr, int dc) {
        if (!recording) return;
        inputs.add(new InputRecord(timestamp, dr, dc));
    }

    public boolean isRecording() { return recording; }
    public int getFrameCount() { return frames.size; }
    public Array<FrameRecord> getFrames() { return frames; }
    public Array<InputRecord> getInputs() { return inputs; }
    public String getMazeId() { return mazeId; }
    public String getAlgorithm() { return algorithm; }
    public int getMazeWidth() { return mazeWidth; }
    public int getMazeHeight() { return mazeHeight; }

    /** Serialize the entire recording to a JSON string. */
    public String toJson() {
        Json json = new Json();
        json.setOutputType(JsonWriter.OutputType.json);

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"mazeId\":\"").append(mazeId != null ? mazeId : "").append("\",");
        sb.append("\"algorithm\":\"").append(algorithm != null ? algorithm : "").append("\",");
        sb.append("\"mazeWidth\":").append(mazeWidth).append(",");
        sb.append("\"mazeHeight\":").append(mazeHeight).append(",");
        sb.append("\"frameCount\":").append(frames.size).append(",");
        sb.append("\"frames\":");
        sb.append(json.toJson(frames, Array.class, FrameRecord.class));
        sb.append(",\"inputs\":[");
        for (int i = 0; i < inputs.size; i++) {
            InputRecord ir = inputs.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"timestamp\":").append(ir.timestamp)
              .append(",\"dr\":").append(ir.dr)
              .append(",\"dc\":").append(ir.dc).append("}");
        }
        sb.append("]");
        sb.append("}");

        return sb.toString();
    }

    /** Deserialize a recording from a JSON string. Returns a new GameRecorder (non-recording). */
    public static GameRecorder fromJson(String jsonString) {
        //Json json = new Json();
        JsonValue root = new com.badlogic.gdx.utils.JsonReader().parse(jsonString);

        String mazeId = root.getString("mazeId", "");
        String algorithm = root.getString("algorithm", "");
        int w = root.getInt("mazeWidth", 0);
        int h = root.getInt("mazeHeight", 0);

        GameRecorder recorder = new GameRecorder(mazeId, algorithm, w, h);
        recorder.recording = false;

        JsonValue framesArray = root.get("frames");
        if (framesArray != null) {
            for (JsonValue fv = framesArray.child; fv != null; fv = fv.next) {
                FrameRecord frame = new FrameRecord();
                frame.timestamp = fv.getFloat("timestamp", 0);
                frame.playerRow = fv.getInt("playerRow", 0);
                frame.playerCol = fv.getInt("playerCol", 0);
                frame.playerDir = fv.getInt("playerDir", 0);
                frame.playerVisualX = fv.getFloat("playerVisualX", 0);
                frame.playerVisualY = fv.getFloat("playerVisualY", 0);
                frame.score = fv.getInt("score", 0);
                frame.dotsEaten = fv.getInt("dotsEaten", 0);
                frame.lives = fv.getInt("lives", 3);
                frame.won = fv.getBoolean("won", false);

                JsonValue gr = fv.get("ghostRows");
                JsonValue gc = fv.get("ghostCols");
                JsonValue gm = fv.get("ghostModes");
                frame.ghostRows = gr != null ? gr.asIntArray() : new int[0];
                frame.ghostCols = gc != null ? gc.asIntArray() : new int[0];
                frame.ghostModes = gm != null ? gm.asIntArray() : new int[0];

                recorder.frames.add(frame);
            }
        }

        JsonValue inputsArray = root.get("inputs");
        if (inputsArray != null) {
            for (JsonValue iv = inputsArray.child; iv != null; iv = iv.next) {
                InputRecord input = new InputRecord();
                input.timestamp = iv.getFloat("timestamp", 0);
                input.dr = iv.getInt("dr", 0);
                input.dc = iv.getInt("dc", 0);
                recorder.inputs.add(input);
            }
        }

        return recorder;
    }
}
