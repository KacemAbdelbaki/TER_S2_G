package com.labyrinth.replay;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.labyrinth.agents.*;
import com.labyrinth.maze.MazeData;
import java.util.Locale;

public class AISimulator {

    public static void runSimulation(GameRecorder recorder, MazeData maze) {
        if (recorder == null || recorder.getFrameCount() == 0) return;
        
        System.out.println("Starting Academic Ghost AI Simulation...");

        // Setup CSV builder
        StringBuilder csv = new StringBuilder();
        csv.append("timestamp,difficulty,avg_distance_to_pacman,total_compute_time_ns,total_nodes_evaluated,caught\n");
        
        // Pass 1: CLASSIC
        runSimulationPass(recorder, maze, Ghost.Difficulty.CLASSIC, csv);
        
        // Pass 2: A_STAR
        runSimulationPass(recorder, maze, Ghost.Difficulty.A_STAR, csv);
        
        // Save CSV
        try {
            FileHandle file = Gdx.files.local("ghost_metrics_" + System.currentTimeMillis() + ".csv");
            file.writeString(csv.toString(), false);
            System.out.println("Academic Simulation Complete! Saved to: " + file.file().getAbsolutePath());
        } catch (Exception e) {
            System.err.println("Failed to write metrics CSV: " + e.getMessage());
        }
    }

    private static void runSimulationPass(GameRecorder recorder, MazeData maze, Ghost.Difficulty diff, StringBuilder csv) {
        // Find ghost room
        int gMinR = maze.height, gMaxR = 0, gMinC = maze.width, gMaxC = 0;
        boolean hasGhostRoom = false;
        for (int r = 0; r < maze.height; r++) {
            for (int c = 0; c < maze.width; c++) {
                if (maze.grid[r][c] != null && maze.grid[r][c].isGhost) {
                    hasGhostRoom = true;
                    gMinR = Math.min(gMinR, r);
                    gMaxR = Math.max(gMaxR, r);
                    gMinC = Math.min(gMinC, c);
                    gMaxC = Math.max(gMaxC, c);
                }
            }
        }
        int centerR = hasGhostRoom ? (gMinR + gMaxR) / 2 : maze.height / 2;
        int centerC = hasGhostRoom ? (gMinC + gMaxC) / 2 : maze.width / 2;

        Blinky blinky = new Blinky(maze, centerR, Math.min(centerC + 1, maze.width - 1));
        Pinky pinky  = new Pinky(maze, centerR, Math.max(centerC - 1, 0));
        Inky inky   = new Inky(maze, centerR, Math.min(centerC + 2, maze.width - 1), blinky);
        Clyde clyde  = new Clyde(maze, centerR, Math.max(centerC - 2, 0));
        Ghost[] ghosts = new Ghost[]{blinky, pinky, inky, clyde};
        
        for (Ghost g : ghosts) g.setDifficulty(diff);

        float moveTimer = 0f;
        float prevTimestamp = recorder.getFrames().get(0).timestamp;
        boolean caught = false;

        for (GameRecorder.FrameRecord f : recorder.getFrames()) {
            if (caught) break;

            float delta = f.timestamp - prevTimestamp;
            prevTimestamp = f.timestamp;
            moveTimer += delta;

            long frameComputeTime = 0;
            int frameNodesEvaluated = 0;
            
            // Assume move interval is around 0.22f based on Ghost logic
            if (moveTimer >= 0.22f) {
                moveTimer -= 0.22f; // reset
                
                for (int i = 0; i < ghosts.length; i++) {
                    Ghost g = ghosts[i];
                    if (g.isInGhostRoom()) {
                        g.exitGhostRoom();
                    } else {
                        // Safely apply recorded mode if available
                        if (f.ghostModes != null && f.ghostModes.length > i) {
                            try {
                                Ghost.GhostMode mode = Ghost.GhostMode.values()[f.ghostModes[i]];
                                g.setMode(mode);
                            } catch (Exception ignored) {}
                        }

                        if (g.getMode() == Ghost.GhostMode.EATEN) {
                            g.moveTowardGhostRoom();
                        } else {
                            int[] target = g.getTargetTile(f.playerRow, f.playerCol, f.playerDir);
                            int dir = g.chooseDirection(target[0], target[1]);
                            g.moveInDirection(dir);
                            
                            frameComputeTime += g.getLastComputeTimeNs();
                            frameNodesEvaluated += g.getLastNodesEvaluated();
                        }
                    }
                }
            }
            
            // Compute average physical distance and catch condition
            float totalDistance = 0f;
            for (Ghost g : ghosts) {
                float dist = (float) Math.sqrt(Math.pow(g.getGridRow() - f.playerRow, 2) + Math.pow(g.getGridCol() - f.playerCol, 2));
                totalDistance += dist;
                
                // If ghost is in same grid square and not eaten/frightened, pacman is caught
                if (dist == 0 && g.getMode() != Ghost.GhostMode.EATEN && g.getMode() != Ghost.GhostMode.FRIGHTENED) {
                    caught = true; 
                }
            }
            float avgDist = totalDistance / ghosts.length;
            
            // Locale US prevents comma decimals which breaks CSVs in some locales
            csv.append(String.format(Locale.US, "%f,%s,%f,%d,%d,%b\n", 
                f.timestamp, diff.name(), avgDist, frameComputeTime, frameNodesEvaluated, caught));
        }
    }
}
