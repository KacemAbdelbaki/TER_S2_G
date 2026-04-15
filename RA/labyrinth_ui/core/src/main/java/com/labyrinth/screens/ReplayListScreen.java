package com.labyrinth.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.labyrinth.Main;
import com.labyrinth.maze.MazeRenderer;
import com.labyrinth.net.ApiClient;
import com.labyrinth.ui.NeonSkin;

public class ReplayListScreen implements Screen {

    private final Main game;
    private final String baseUrl;
    private Stage stage;
    private Skin skin;
    private Table listTable;
    private Label statusLabel;

    public ReplayListScreen(Main game, String baseUrl) {
        this.game = game;
        this.baseUrl = baseUrl;
    }

    @Override
    public void show() {
        skin = game.skin;
        stage = new Stage(new ScreenViewport());

        Table root = new Table();
        root.setFillParent(true);
        root.top().pad(20);

        // Header
        Table header = new Table();
        TextButton backBtn = new TextButton("< Back", skin, "default");
        backBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new MenuScreen(game));
            }
        });

        Label titleLbl = new Label("SAVED REPLAYS", skin, "title");

        header.add(backBtn).left().width(100).padRight(20);
        header.add(titleLbl).expandX().center();
        header.add(new Container<>()).width(100); // Balance right side

        root.add(header).fillX().padBottom(20).row();

        // Status Label
        statusLabel = new Label("Loading replays...", skin, "small");
        statusLabel.setColor(NeonSkin.CYAN);
        root.add(statusLabel).padBottom(10).row();

        // Scrollable list container
        listTable = new Table();
        listTable.top();

        ScrollPane scrollPane = new ScrollPane(listTable, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setOverscroll(false, false);
        root.add(scrollPane).expand().fill().row();

        stage.addActor(root);

        InputMultiplexer mux = new InputMultiplexer();
        mux.addProcessor(stage);
        mux.addProcessor(new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.ESCAPE) {
                    game.setScreen(new MenuScreen(game));
                    return true;
                }
                return false;
            }
        });
        Gdx.input.setInputProcessor(mux);

        // Fetch replays
        ApiClient api = new ApiClient(baseUrl);
        api.listReplays(null, 50, new ApiClient.ReplayListCallback() {
            @Override
            public void onSuccess(String listJson) {
                try {
                    JsonValue arr = new JsonReader().parse(listJson);
                    populateList(arr, api);
                } catch (Exception e) {
                    statusLabel.setText("Failed to parse replays.");
                    statusLabel.setColor(NeonSkin.ERROR_RED);
                }
            }

            @Override
            public void onError(String msg) {
                statusLabel.setText("Error: " + msg);
                statusLabel.setColor(NeonSkin.ERROR_RED);
            }
        });
    }

    private void populateList(JsonValue arr, ApiClient api) {
        listTable.clearChildren();

        if (arr == null || arr.size == 0) {
            statusLabel.setText("No replays found in database.");
            statusLabel.setColor(NeonSkin.ERROR_RED);
            return;
        }

        statusLabel.setText("Found " + arr.size + " replay(s).");
        statusLabel.setColor(NeonSkin.ACCENT);

        for (JsonValue r : arr) {
            String id = r.getString("id", r.has("_id") ? r.getString("_id") : "unknown");
            String algo = r.getString("algorithm", "unknown");
            int w = r.getInt("maze_width", 0);
            int h = r.getInt("maze_height", 0);
            int score = r.getInt("final_score", 0);
            boolean won = r.getBoolean("won", false);
            String playerType = r.getString("player_type", "human");
            String date = r.getString("created_at", "unknown");

            Table row = new Table();
            row.pad(10);
            // Optionally add a background to the row

            // Left info
            Table info = new Table();
            info.left();
            String title = algo.toUpperCase() + " (" + w + "x" + h + ") - " + playerType.toUpperCase();
            Label titleObj = new Label(title, skin, "default");
            titleObj.setColor(NeonSkin.CYAN);
            info.add(titleObj).left().row();

            String sub = "Score: " + score + " | Result: " + (won ? "WON" : "LOST") + " | " + date;
            Label subObj = new Label(sub, skin, "small");
            subObj.setColor(won ? NeonSkin.ACCENT : NeonSkin.TEXT_GRAY);
            info.add(subObj).left();

            row.add(info).expandX().left();

            // Right button
            TextButton watchBtn = new TextButton("WATCH", skin, "accent");
            watchBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    loadAndPlay(id, api);
                }
            });
            row.add(watchBtn).width(120).height(45).right();

            listTable.add(row).expandX().fillX().padBottom(5).row();
        }
    }

    private void loadAndPlay(String replayId, ApiClient api) {
        statusLabel.setText("Loading replay data...");
        statusLabel.setColor(NeonSkin.CYAN);

        api.loadReplay(replayId, new ApiClient.ReplayLoadCallback() {
            @Override
            public void onSuccess(String replayJson) {
                // Parse to check metadata
                try {
                    com.badlogic.gdx.utils.JsonReader reader = new com.badlogic.gdx.utils.JsonReader();
                    com.badlogic.gdx.utils.JsonValue root = reader.parse(replayJson);
                    boolean hadGhosts = root.getBoolean("original_had_ghosts", true);

                    if (!hadGhosts) {
                        // Original run had ghosts disabled: keep replay ghost-free.
                        game.setScreen(new ReplayScreen(game, replayJson, baseUrl, false, null));
                    } else {
                        // Start replay immediately with original ghosts
                        game.setScreen(new ReplayScreen(game, replayJson, baseUrl, false, null));
                    }
                } catch (Exception e) {
                    statusLabel.setText("Failed to parse replay metadata.");
                    statusLabel.setColor(NeonSkin.ERROR_RED);
                    game.setScreen(new ReplayScreen(game, replayJson, baseUrl, false, null));
                }
            }

            @Override
            public void onError(String msg) {
                statusLabel.setText("Load error: " + msg);
                statusLabel.setColor(NeonSkin.ERROR_RED);
            }
        });
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(
                MazeRenderer.BG_COLOR.r,
                MazeRenderer.BG_COLOR.g,
                MazeRenderer.BG_COLOR.b,
                1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        if (stage != null)
            stage.dispose();
    }
}
