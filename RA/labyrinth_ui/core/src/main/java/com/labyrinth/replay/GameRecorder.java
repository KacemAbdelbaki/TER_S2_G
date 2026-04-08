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
        public float[] ghostVisualX;
        public float[] ghostVisualY;

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

    // Metadata for serialization
    private int finalScore;
    private int finalDotsEaten;
    private boolean won;
    private float durationSeconds;
    private String playerType = "human";
    private String botStrategy;
    private boolean originalHadGhosts = true;
    private String pacmanAgentType = "human";

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
                            int[] ghostRows, int[] ghostCols, int[] ghostModes,
                            float[] ghostVisualX, float[] ghostVisualY) {
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
        frame.ghostVisualX = ghostVisualX != null ? ghostVisualX.clone() : new float[0];
        frame.ghostVisualY = ghostVisualY != null ? ghostVisualY.clone() : new float[0];

        frames.add(frame);
    }

    /** Stop recording. */
    public void stop() {
        recording = false;
    }

    public void setMazeId(String mazeId) {
        this.mazeId = mazeId;
    }

    public void setBotStrategy(String botStrategy) {
        this.botStrategy = botStrategy;
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

    public void setFinalScore(int s) { this.finalScore = s; }
    public void setFinalDotsEaten(int d) { this.finalDotsEaten = d; }
    public void setWon(boolean w) { this.won = w; }
    public void setDurationSeconds(float s) { this.durationSeconds = s; }
    public void setPlayerType(String t) { this.playerType = t; }
    public boolean hadOriginalGhosts() { return originalHadGhosts; }
    public void setOriginalHadGhosts(boolean v) { this.originalHadGhosts = v; }
    public String getPacmanAgentType() { return pacmanAgentType; }
    public void setPacmanAgentType(String t) { this.pacmanAgentType = t; }

    /** Serialize the entire recording to a standard JSON string. */
    public String toJson() {
        Json json = new Json();
        json.setOutputType(JsonWriter.OutputType.json);

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"maze_id\":\"").append(mazeId != null ? mazeId : "").append("\",");
        sb.append("\"algorithm\":\"").append(algorithm != null ? algorithm : "").append("\",");
        sb.append("\"maze_width\":").append(mazeWidth).append(",");
        sb.append("\"maze_height\":").append(mazeHeight).append(",");
        sb.append("\"player_type\":\"").append(playerType).append("\",");
        if (botStrategy != null) {
            sb.append("\"bot_strategy\":\"").append(botStrategy).append("\",");
        }
        sb.append("\"original_had_ghosts\":").append(originalHadGhosts).append(",");
        sb.append("\"pacman_agent_type\":\"").append(pacmanAgentType).append("\",");
        sb.append("\"final_score\":").append(finalScore).append(",");
        sb.append("\"final_dots_eaten\":").append(finalDotsEaten).append(",");
        sb.append("\"won\":").append(won).append(",");
        sb.append("\"duration_seconds\":").append(durationSeconds).append(",");
        sb.append("\"frame_count\":").append(frames.size).append(",");
        
        // Frames - use LibGDX Json for correctly quoted standard JSON array
        sb.append("\"frames\":");
        sb.append(json.toJson(frames, Array.class, FrameRecord.class));

        // Inputs
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

        String mazeId = root.has("maze_id") ? root.getString("maze_id") : root.getString("mazeId", "");
        String algorithm = root.getString("algorithm", "");
        int w = root.has("maze_width") ? root.getInt("maze_width") : root.getInt("mazeWidth", 0);
        int h = root.has("maze_height") ? root.getInt("maze_height") : root.getInt("mazeHeight", 0);

        GameRecorder recorder = new GameRecorder(mazeId, algorithm, w, h);
        recorder.recording = false;
        recorder.setFinalScore(root.getInt("final_score", root.getInt("finalScore", 0)));
        recorder.setFinalDotsEaten(root.getInt("final_dots_eaten", root.getInt("finalDotsEaten", 0)));
        recorder.setWon(root.getBoolean("won", false));
        recorder.setDurationSeconds(root.getFloat("duration_seconds", root.getFloat("durationSeconds", 0f)));
        recorder.setPlayerType(root.getString("player_type", root.getString("playerType", "human")));
        recorder.setBotStrategy(root.getString("bot_strategy", root.getString("botStrategy", null)));
        recorder.setOriginalHadGhosts(root.getBoolean("original_had_ghosts", true));
        recorder.setPacmanAgentType(root.getString("pacman_agent_type", "human"));

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
                JsonValue gx = fv.get("ghostVisualX");
                JsonValue gy = fv.get("ghostVisualY");
                frame.ghostRows = gr != null ? gr.asIntArray() : new int[0];
                frame.ghostCols = gc != null ? gc.asIntArray() : new int[0];
                frame.ghostModes = gm != null ? gm.asIntArray() : new int[0];
                frame.ghostVisualX = gx != null ? gx.asFloatArray() : new float[0];
                frame.ghostVisualY = gy != null ? gy.asFloatArray() : new float[0];

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
