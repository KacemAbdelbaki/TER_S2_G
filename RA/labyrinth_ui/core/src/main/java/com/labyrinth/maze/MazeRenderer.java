package com.labyrinth.maze;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.Gdx;
import com.labyrinth.agents.Ghost;

/**
 * Renders a Pac-Man style labyrinth with neon walls, dots, power pellets,
 * ghost room visualisation, and teleport links.
 */
public class MazeRenderer {

    // ----- Pac-Man Theme colours -----
    public static final Color BG_COLOR        = new Color(0.0f, 0.0f, 0.0f, 1f);      // Pure black
    public static final Color WALL_COLOR      = new Color(0.13f, 0.13f, 1.0f, 1f);     // Classic blue
    public static final Color WALL_GLOW       = new Color(0.13f, 0.13f, 1.0f, 0.18f);
    public static final Color GHOST_ROOM_COLOR= new Color(1.0f, 0.5f, 0.5f, 0.12f);    // Faint red
    public static final Color GHOST_DOOR_COLOR= new Color(1.0f, 0.7f, 0.8f, 0.9f);     // Pink door
    public static final Color TP_COLOR        = new Color(1.0f, 0.0f, 1.0f, 0.50f);
    public static final Color TP_LINK_COLOR   = new Color(1.0f, 0.0f, 1.0f, 0.15f);
    public static final Color DOT_COLOR       = new Color(1.0f, 0.85f, 0.55f, 0.9f);   // Warm yellow dots
    public static final Color POWER_COLOR     = new Color(1.0f, 0.85f, 0.55f, 1.0f);   // Bright pellets
    public static final Color VISITED_COLOR   = new Color(0.05f, 0.05f, 0.12f, 1.0f);  // Slightly lighter than BG
    public static final Color UNVISITED_COLOR = new Color(0.0f, 0.0f, 0.0f, 1.0f);

    // Ghost colours
    public static final Color GHOST_RED    = new Color(1.0f, 0.0f, 0.0f, 0.85f);
    public static final Color GHOST_PINK   = new Color(1.0f, 0.72f, 0.84f, 0.85f);
    public static final Color GHOST_CYAN   = new Color(0.0f, 1.0f, 1.0f, 0.85f);
    public static final Color GHOST_ORANGE = new Color(1.0f, 0.60f, 0.0f, 0.85f);
    private static final Color[] GHOST_COLORS = {GHOST_RED, GHOST_PINK, GHOST_CYAN, GHOST_ORANGE};

    private ShapeRenderer sr;
    private float cellSize;
    private float wallThickness = 3f;
    private float originX, originY;

    public MazeRenderer() {
        sr = new ShapeRenderer();
    }

    public void computeLayout(MazeData maze, float viewportW, float viewportH) {
        float marginFraction = 0.08f;
        float availW = viewportW * (1f - 2f * marginFraction);
        float availH = viewportH * (1f - 2f * marginFraction);
        cellSize = Math.min(availW / maze.width, availH / maze.height);
        float mazeW = maze.width * cellSize;
        float mazeH = maze.height * cellSize;
        originX = (viewportW - mazeW) / 2f;
        originY = (viewportH - mazeH) / 2f;
    }

    /**
     * Render the maze with Pac-Man elements.
     *
     * @param maze           the maze data
     * @param camera         current camera
     * @param animProgress   steps revealed (-1 = show all)
     * @param playerGridRow  Pac-Man row (-1 if inactive)
     * @param playerGridCol  Pac-Man col (-1 if inactive)
     * @param visitedCells   player trail (may be null)
     * @param dots           dot grid (may be null)
     * @param powerPellets   power pellet grid (may be null)
     */
    public void render(MazeData maze, OrthographicCamera camera,
                       int animProgress, int playerGridRow, int playerGridCol,
                       boolean[][] visitedCells, boolean[][] dots, boolean[][] powerPellets) {

        sr.setProjectionMatrix(camera.combined);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // --- Cell backgrounds (ghost room highlight, visited trail) ---
        sr.begin(ShapeRenderer.ShapeType.Filled);
        for (int r = 0; r < maze.height; r++) {
            for (int c = 0; c < maze.width; c++) {
                MazeCell cell = maze.grid[r][c];
                if (cell == null) continue;

                float x = originX + c * cellSize;
                float y = originY + (maze.height - 1 - r) * cellSize;

                if (cell.isGhost) {
                    sr.setColor(GHOST_ROOM_COLOR);
                    sr.rect(x, y, cellSize, cellSize);
                } else if (visitedCells != null && visitedCells[r][c]) {
                    sr.setColor(VISITED_COLOR);
                    sr.rect(x, y, cellSize, cellSize);
                }
            }
        }
        sr.end();

        // --- Dots and power pellets ---
        sr.begin(ShapeRenderer.ShapeType.Filled);
        for (int r = 0; r < maze.height; r++) {
            for (int c = 0; c < maze.width; c++) {
                float cx = originX + c * cellSize + cellSize / 2f;
                float cy = originY + (maze.height - 1 - r) * cellSize + cellSize / 2f;

                if (powerPellets != null && powerPellets[r][c]) {
                    // Pulsing power pellet
                    float pulse = 0.8f + 0.2f * (float) Math.sin(System.currentTimeMillis() * 0.006);
                    float pelletR = cellSize * 0.22f * pulse;
                    sr.setColor(POWER_COLOR);
                    sr.circle(cx, cy, pelletR, 16);
                } else if (dots != null && dots[r][c]) {
                    // Small dot
                    float dotR = cellSize * 0.07f;
                    sr.setColor(DOT_COLOR);
                    sr.circle(cx, cy, dotR, 10);
                }
            }
        }
        sr.end();

        // --- Fruits ---
        sr.begin(ShapeRenderer.ShapeType.Filled);
        for (int r = 0; r < maze.height; r++) {
            for (int c = 0; c < maze.width; c++) {
                MazeCell cell = maze.grid[r][c];
                if (cell != null && cell.hasFruit) {
                    float cx = originX + c * cellSize + cellSize / 2f;
                    float cy = originY + (maze.height - 1 - r) * cellSize + cellSize / 2f;
                    float fruitR = cellSize * 0.25f;
                    // Cherry red by default; green for melon, orange for orange
                    if ("orange".equals(cell.fruitType)) {
                        sr.setColor(new Color(1f, 0.6f, 0f, 1f));
                    } else if ("melon".equals(cell.fruitType)) {
                        sr.setColor(new Color(0.2f, 0.9f, 0.3f, 1f));
                    } else if ("strawberry".equals(cell.fruitType)) {
                        sr.setColor(new Color(1f, 0.2f, 0.4f, 1f));
                    } else if ("apple".equals(cell.fruitType)) {
                        sr.setColor(new Color(0.8f, 0.1f, 0.1f, 1f));
                    } else {
                        sr.setColor(new Color(1f, 0f, 0.2f, 1f)); // cherry
                    }
                    sr.circle(cx, cy, fruitR, 14);
                    // Small stem
                    sr.setColor(new Color(0.4f, 0.25f, 0.1f, 1f));
                    sr.rectLine(cx, cy + fruitR, cx + fruitR * 0.2f, cy + fruitR * 1.4f, 1.5f);
                }
            }
        }
        sr.end();

        // --- TP links ---
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(TP_LINK_COLOR);
        for (int r = 0; r < maze.height; r++) {
            for (int c = 0; c < maze.width; c++) {
                MazeCell cell = maze.grid[r][c];
                if (cell != null && cell.isTP && cell.tpX >= 0 && cell.tpY >= 0) {
                    if (r < cell.tpX || (r == cell.tpX && c < cell.tpY)) {
                        float x1 = originX + c * cellSize + cellSize / 2f;
                        float y1 = originY + (maze.height - 1 - r) * cellSize + cellSize / 2f;
                        float x2 = originX + cell.tpY * cellSize + cellSize / 2f;
                        float y2 = originY + (maze.height - 1 - cell.tpX) * cellSize + cellSize / 2f;
                        sr.line(x1, y1, x2, y2);
                    }
                }
            }
        }
        sr.end();

        // --- Ghost shapes inside ghost room (only shown before player starts) ---
        if (playerGridRow < 0) {
            renderGhosts(maze);
        }

        // --- Walls ---
        MazeCell[][] drawGrid;
        if (animProgress >= 0 && maze.steps != null && !maze.steps.isEmpty()) {
            drawGrid = buildAnimatedGrid(maze, animProgress);
        } else {
            drawGrid = maze.grid;
        }

        // Glow pass
        Gdx.gl.glLineWidth(wallThickness + 4f);
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(WALL_GLOW);
        drawWalls(drawGrid, maze.height, maze.width);
        sr.end();

        // Sharp pass
        Gdx.gl.glLineWidth(wallThickness);
        sr.begin(ShapeRenderer.ShapeType.Line);
        sr.setColor(WALL_COLOR);
        drawWalls(drawGrid, maze.height, maze.width);
        sr.end();

        Gdx.gl.glLineWidth(1f);
    }

    /**
     * Render ghost characters inside the ghost room cells.
     */
    private void renderGhosts(MazeData maze) {
        // Find ghost room bounds
        int minR = maze.height, maxR = 0, minC = maze.width, maxC = 0;
        int ghostCount = 0;
        for (int r = 0; r < maze.height; r++) {
            for (int c = 0; c < maze.width; c++) {
                MazeCell cell = maze.grid[r][c];
                if (cell != null && cell.isGhost) {
                    minR = Math.min(minR, r);
                    maxR = Math.max(maxR, r);
                    minC = Math.min(minC, c);
                    maxC = Math.max(maxC, c);
                    ghostCount++;
                }
            }
        }
        if (ghostCount == 0) return;

        // Place up to 4 ghosts spread across the ghost room
        float ghostSize = cellSize * 0.35f;
        int numGhosts = Math.min(4, (maxC - minC + 1));
        float roomCenterR = (minR + maxR) / 2f;

        sr.begin(ShapeRenderer.ShapeType.Filled);
        for (int g = 0; g < numGhosts; g++) {
            float col = minC + (g + 0.5f) * (maxC - minC + 1f) / numGhosts;
            float gx = originX + col * cellSize + cellSize / 2f;
            float gy = originY + (maze.height - 1 - roomCenterR) * cellSize + cellSize / 2f;

            // Slight bobbing animation
            float bob = (float) Math.sin(System.currentTimeMillis() * 0.003 + g * 1.5) * cellSize * 0.05f;
            gy += bob;

            drawGhostShape(gx, gy, ghostSize, GHOST_COLORS[g % GHOST_COLORS.length]);
        }
        sr.end();

        // Ghost eyes
        sr.begin(ShapeRenderer.ShapeType.Filled);
        for (int g = 0; g < numGhosts; g++) {
            float col = minC + (g + 0.5f) * (maxC - minC + 1f) / numGhosts;
            float gx = originX + col * cellSize + cellSize / 2f;
            float gy = originY + (maze.height - 1 - roomCenterR) * cellSize + cellSize / 2f;
            float bob = (float) Math.sin(System.currentTimeMillis() * 0.003 + g * 1.5) * cellSize * 0.05f;
            gy += bob;

            drawGhostEyes(gx, gy, ghostSize);
        }
        sr.end();
    }

    /**
     * Draw a classic Pac-Man ghost shape (dome top + wavy bottom).
     */
    private void drawGhostShape(float cx, float cy, float size, Color color) {
        sr.setColor(color);
        // Body (rectangle)
        sr.rect(cx - size, cy - size * 0.4f, size * 2, size * 1.2f);
        // Dome (semicircle top - approximated with a filled arc)
        for (float a = 0; a < 180; a += 5) {
            float rad = (float) Math.toRadians(a);
            float rad2 = (float) Math.toRadians(a + 5);
            float x1 = cx + (float) Math.cos(rad) * size;
            float y1 = cy + size * 0.8f + (float) Math.sin(rad) * size;
            float x2 = cx + (float) Math.cos(rad2) * size;
            float y2 = cy + size * 0.8f + (float) Math.sin(rad2) * size;
            sr.triangle(cx, cy + size * 0.8f, x1, y1, x2, y2);
        }
        // Wavy bottom (three triangular cutouts)
        sr.setColor(BG_COLOR);
        float waveW = size * 2f / 3f;
        for (int i = 0; i < 3; i++) {
            float bx = cx - size + waveW * i + waveW / 2f;
            float by = cy - size * 0.4f;
            sr.triangle(bx - waveW * 0.35f, by, bx + waveW * 0.35f, by, bx, by - size * 0.3f);
        }
    }

    /**
     * Draw ghost eyes (white sclera + blue pupils).
     */
    private void drawGhostEyes(float cx, float cy, float size) {
        float eyeOff = size * 0.35f;
        float eyeR = size * 0.28f;
        float pupilR = size * 0.15f;
        float eyeY = cy + size * 0.5f;

        // White sclera
        sr.setColor(Color.WHITE);
        sr.circle(cx - eyeOff, eyeY, eyeR, 12);
        sr.circle(cx + eyeOff, eyeY, eyeR, 12);

        // Blue pupils
        sr.setColor(new Color(0.1f, 0.1f, 0.7f, 1f));
        sr.circle(cx - eyeOff, eyeY, pupilR, 10);
        sr.circle(cx + eyeOff, eyeY, pupilR, 10);
    }

    private void drawWalls(MazeCell[][] grid, int H, int W) {
        for (int r = 0; r < H; r++) {
            for (int c = 0; c < W; c++) {
                MazeCell cell = grid[r][c];
                if (cell == null) continue;

                float x = originX + c * cellSize;
                float y = originY + (H - 1 - r) * cellSize;

                if (cell.walls[0]) sr.line(x, y + cellSize, x + cellSize, y + cellSize);
                if (cell.walls[1]) sr.line(x + cellSize, y, x + cellSize, y + cellSize);
                if (cell.walls[2]) sr.line(x, y, x + cellSize, y);
                if (cell.walls[3]) sr.line(x, y, x, y + cellSize);
            }
        }
    }

    private MazeCell[][] buildAnimatedGrid(MazeData maze, int progress) {
        int H = maze.height, W = maze.width;
        MazeCell[][] aGrid = new MazeCell[H][W];
        for (int r = 0; r < H; r++) {
            for (int c = 0; c < W; c++) {
                MazeCell orig = maze.grid[r][c];
                MazeCell copy = new MazeCell(r, c);
                copy.isGhost = orig.isGhost;
                copy.isTP = orig.isTP;
                copy.tpX = orig.tpX;
                copy.tpY = orig.tpY;
                copy.walls = new boolean[]{true, true, true, true};
                if (orig.isGhost) {
                    copy.walls = orig.walls.clone();
                }
                aGrid[r][c] = copy;
            }
        }
        int limit = Math.min(progress, maze.steps.size());
        for (int i = 0; i < limit; i++) {
            MazeData.GenerationStep step = maze.steps.get(i);
            if (step.r1 >= 0 && step.r1 < H && step.c1 >= 0 && step.c1 < W)
                aGrid[step.r1][step.c1].walls[step.w1] = false;
            if (step.r2 >= 0 && step.r2 < H && step.c2 >= 0 && step.c2 < W)
                aGrid[step.r2][step.c2].walls[step.w2] = false;
        }
        return aGrid;
    }

    public float getCellSize() { return cellSize; }
    public float getOriginX() { return originX; }
    public float getOriginY() { return originY; }

    public float gridToScreenY(int row, int mazeHeight) {
        return originY + (mazeHeight - 1 - row) * cellSize;
    }

    public float gridToScreenX(int col) {
        return originX + col * cellSize;
    }

    /**
     * Render the active ghost agents at their visual positions.
     * Uses FRIGHTENED blue colour or EATEN eyes-only.
     */
    public void renderActiveGhosts(Ghost[] ghosts, OrthographicCamera camera) {
        if (ghosts == null) return;
        sr.setProjectionMatrix(camera.combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        float ghostSize = cellSize * 0.35f;

        // Bodies
        sr.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < ghosts.length; i++) {
            Ghost g = ghosts[i];
            float gx = g.getVisualX();
            float gy = g.getVisualY();

            if (g.getMode() == Ghost.GhostMode.EATEN) {
                continue; // eyes only, drawn below
            } else if (g.getMode() == Ghost.GhostMode.FRIGHTENED) {
                drawGhostShape(gx, gy, ghostSize, new Color(0.2f, 0.2f, 1.0f, 0.85f));
            } else {
                drawGhostShape(gx, gy, ghostSize, GHOST_COLORS[i % GHOST_COLORS.length]);
            }
        }
        sr.end();

        // Eyes
        sr.begin(ShapeRenderer.ShapeType.Filled);
        for (Ghost g : ghosts) {
            drawGhostEyes(g.getVisualX(), g.getVisualY(), ghostSize);
        }
        sr.end();
    }

    /**
     * Render ghosts at their grid positions (not visual positions).
     * Useful when the ghost visual positions are in a different coordinate space
     * (e.g. half-width renderer) than the camera being drawn with.
     */
    public void renderActiveGhostsAtGrid(Ghost[] ghosts, OrthographicCamera camera, int mazeHeight) {
        if (ghosts == null) return;
        sr.setProjectionMatrix(camera.combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        float ghostSize = cellSize * 0.35f;

        // Bodies
        sr.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < ghosts.length; i++) {
            Ghost g = ghosts[i];
            if (g.getMode() == Ghost.GhostMode.EATEN) continue;
            float gx = gridToScreenX(g.getGridCol()) + cellSize / 2f;
            float gy = gridToScreenY(g.getGridRow(), mazeHeight) + cellSize / 2f;
            if (g.getMode() == Ghost.GhostMode.FRIGHTENED) {
                drawGhostShape(gx, gy, ghostSize, new Color(0.2f, 0.2f, 1.0f, 0.85f));
            } else {
                drawGhostShape(gx, gy, ghostSize, GHOST_COLORS[i % GHOST_COLORS.length]);
            }
        }
        sr.end();

        // Eyes
        sr.begin(ShapeRenderer.ShapeType.Filled);
        for (Ghost g : ghosts) {
            float gx = gridToScreenX(g.getGridCol()) + cellSize / 2f;
            float gy = gridToScreenY(g.getGridRow(), mazeHeight) + cellSize / 2f;
            drawGhostEyes(gx, gy, ghostSize);
        }
        sr.end();
    }

    /**
     * Renders a predicted path showing the exact tiles each ghost is planning to move to next.
     * This uses the same greedy pathfinding logic that ghosts use to decide their movements.
     */
    public void renderGhostPaths(Ghost[] ghosts, com.labyrinth.player.Player player, MazeData maze, OrthographicCamera camera) {
        if (ghosts == null || player == null || maze == null) return;
        
        sr.setProjectionMatrix(camera.combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        
        sr.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl.glLineWidth(3f);
        
        int[] DR = {0, -1, 0, 1};
        int[] DC = {1, 0, -1, 0};
        int[] DIR_TO_WALL = {1, 0, 3, 2};
        int[] priority = {1, 2, 3, 0}; // Up, Left, Down, Right 

        for (int i = 0; i < ghosts.length; i++) {
            Ghost g = ghosts[i];
            if (g.getMode() == Ghost.GhostMode.EATEN || g.getMode() == Ghost.GhostMode.FRIGHTENED) {
                continue; // Do not predict paths for eaten or frightened ghosts
            }
            if (g.isInGhostRoom()) continue; // Ghost is floating out, predicting path is tricky here
            
            Color color = GHOST_COLORS[i % GHOST_COLORS.length].cpy();
            color.a = 0.35f; // Semi-transparent
            sr.setColor(color);
            
            int[] target = g.getTargetTile(player.gridRow, player.gridCol, player.direction);
            
            // Simulate path for up to 12 steps
            int curRow = g.getGridRow();
            int curCol = g.getGridCol();
            int curDir = g.getDirection();
            
            float lastX = gridToScreenX(curCol) + cellSize / 2f;
            float lastY = gridToScreenY(curRow, maze.height) + cellSize / 2f;
            
            for (int step = 0; step < 12; step++) {
                if (curRow == target[0] && curCol == target[1]) {
                    sr.circle(lastX, lastY, cellSize * 0.25f);
                    break;
                }
                
                int reverseDir = (curDir + 2) % 4;
                int bestDir = -1;
                float bestDist = Float.MAX_VALUE;
                
                MazeCell current = maze.getCell(curRow, curCol);
                if (current == null) break;
                
                for (int d : priority) {
                    if (d == reverseDir) continue; 
                    if (current.hasWall(DIR_TO_WALL[d])) continue;
                    
                    int newR = curRow + DR[d];
                    int newC = curCol + DC[d];
                    float dist = (newR - target[0]) * (newR - target[0]) + (newC - target[1]) * (newC - target[1]);
                    
                    if (dist < bestDist) {
                        bestDist = dist;
                        bestDir = d;
                    }
                }
                
                // Allow reverse only if stuck (though classic ghosts don't do this, it's a safety)
                if (bestDir == -1) {
                    if (!current.hasWall(DIR_TO_WALL[reverseDir])) {
                        bestDir = reverseDir;
                    } else {
                        break; // Dead end
                    }
                }
                
                curDir = bestDir;
                curRow += DR[bestDir];
                curCol += DC[bestDir];
                
                float nextX = gridToScreenX(curCol) + cellSize / 2f;
                float nextY = gridToScreenY(curRow, maze.height) + cellSize / 2f;
                
                sr.line(lastX, lastY, nextX, nextY);
                lastX = nextX;
                lastY = nextY;
            }
            
            // Draw a cross indicating their true target tile, even if they haven't reached it in 12 steps
            float targetX = gridToScreenX(target[1]) + cellSize / 2f;
            float targetY = gridToScreenY(target[0], maze.height) + cellSize / 2f;
            color.a = 0.15f;
            sr.setColor(color);
            float crossR = cellSize * 0.4f;
            sr.line(targetX - crossR, targetY - crossR, targetX + crossR, targetY + crossR);
            sr.line(targetX - crossR, targetY + crossR, targetX + crossR, targetY - crossR);
        }
        
        sr.end();
        Gdx.gl.glLineWidth(1f);
    }
    public void dispose() {
        sr.dispose();
    }
}
