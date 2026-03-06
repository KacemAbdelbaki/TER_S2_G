package com.labyrinth.net;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.net.HttpRequestBuilder;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.labyrinth.maze.MazeCell;
import com.labyrinth.maze.MazeData;
import com.labyrinth.maze.MazeData.GenerationStep;

/**
 * HTTP client for the labyrinth Flask API.
 */
public class ApiClient {

    private String baseUrl;

    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public ApiClient(String host, int port) {
        this("http://" + host + ":" + port);
    }

    public ApiClient() {
        this("https://kacemdfs.onrender.com");
    }

    /**
     * Called when a maze has been fetched and parsed.
     */
    public interface MazeCallback {
        void onSuccess(MazeData maze);
        void onError(String message);
    }

    /**
     * Fetch a maze from the API asynchronously.
     *
     * @param algorithm "dfs", "kruskal", or "prim"
     * @param height    maze half-grid height
     * @param width     maze half-grid width
     * @param withSteps whether to request generation steps
     * @param callback  called on the GL thread when done
     */
    public void fetchMaze(String algorithm, int height, int width, boolean withSteps, MazeCallback callback) {
        String url = baseUrl + "/labyrinth"
                + "?algorithm=" + algorithm
                + "&height=" + height
                + "&width=" + width
                + (withSteps ? "&steps=true" : "");

        Net.HttpRequest request = new HttpRequestBuilder()
                .newRequest()
                .method(Net.HttpMethods.GET)
                .url(url)
                .timeout(15000)
                .build();

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                String json = httpResponse.getResultAsString();
                try {
                    MazeData maze = parseJson(json);
                    Gdx.app.postRunnable(() -> callback.onSuccess(maze));
                } catch (Exception e) {
                    Gdx.app.postRunnable(() -> callback.onError("Parse error: " + e.getMessage()));
                }
            }

            @Override
            public void failed(Throwable t) {
                Gdx.app.postRunnable(() -> callback.onError("Connection failed: " + t.getMessage()));
            }

            @Override
            public void cancelled() {
                Gdx.app.postRunnable(() -> callback.onError("Request cancelled"));
            }
        });
    }

    /**
     * Parse the JSON response into a MazeData object.
     */
    private MazeData parseJson(String jsonStr) {
        JsonReader reader = new JsonReader();
        JsonValue root = reader.parse(jsonStr);

        int height = root.getInt("height");
        int width = root.getInt("width");
        MazeData maze = new MazeData(height, width);

        JsonValue rows = root.get("rows");
        int r = 0;
        for (JsonValue row = rows.child; row != null; row = row.next, r++) {
            int c = 0;
            for (JsonValue cellJson = row.child; cellJson != null; cellJson = cellJson.next, c++) {
                MazeCell cell = new MazeCell(cellJson.getInt("x"), cellJson.getInt("y"));

                JsonValue wallsArr = cellJson.get("walls");
                int i = 0;
                for (JsonValue w = wallsArr.child; w != null; w = w.next, i++) {
                    cell.walls[i] = w.asBoolean();
                }

                cell.isGhost = cellJson.getBoolean("isGhost", false);
                cell.isTP = cellJson.getBoolean("isTP", false);

                JsonValue tp = cellJson.get("TP");
                if (tp != null && !tp.isNull()) {
                    cell.tpX = tp.getInt("x", -1);
                    cell.tpY = tp.getInt("y", -1);
                }

                maze.grid[r][c] = cell;
            }
        }

        // Parse generation steps if present
        JsonValue stepsArr = root.get("steps");
        if (stepsArr != null && stepsArr.isArray()) {
            for (JsonValue s = stepsArr.child; s != null; s = s.next) {
                maze.steps.add(new GenerationStep(
                        s.getInt("r1"), s.getInt("c1"), s.getInt("w1"),
                        s.getInt("r2"), s.getInt("c2"), s.getInt("w2")
                ));
            }
        }

        return maze;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void setBaseUrl(String host, int port) {
        this.baseUrl = "http://" + host + ":" + port;
    }
}
