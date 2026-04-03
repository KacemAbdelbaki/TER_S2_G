package com.labyrinth.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.labyrinth.Main;
import com.labyrinth.agents.BotAgent;
import com.labyrinth.maze.MazeRenderer;
import com.labyrinth.net.ApiClient;
import com.labyrinth.ui.NeonSkin;
import com.labyrinth.ui.NeonSkin;


/**
 * Main menu screen with algorithm selection, dimension controls,
 * server configuration, and a Generate button.
 */
public class MenuScreen implements Screen {

    private final Main game;
    private Stage stage;
    private Skin skin;

    // Widgets
    private SelectBox<String> algoSelect;
    private SelectBox<String> diffSelect;
    private Slider heightSlider;
    private Slider widthSlider;
    private Label heightValue;
    private Label widthValue;
    private CheckBox animatedCheck;
    private TextField serverField;
    private TextButton generateBtn;
    private Label statusLabel;
    private CheckBox raceCheck;
    private SelectBox<String> botStrategySelect;

    public MenuScreen(Main game) {
        this.game = game;
    }

    @Override
    public void show() {
        skin = game.skin;
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        buildUI();
    }

    private void buildUI() {
        Table root = new Table();
        root.setFillParent(true);
        root.center();
        root.defaults().pad(6).center();

        // Title
        Label title = new Label("PAC-MAN LABYRINTH", skin, "title");
        root.add(title).colspan(2).padBottom(30).row();

        // Algorithm
        Label algoLbl = new Label("Generation:", skin, "default");
        algoSelect = new SelectBox<>(skin);
        algoSelect.setItems("DFS", "Kruskal", "Prim", "Tetris");
        root.add(algoLbl).right().padRight(12);
        root.add(algoSelect).width(200).left().row();

        // Ghost Difficulty
        Label diffLbl = new Label("Ghost AI:", skin, "default");
        diffSelect = new SelectBox<>(skin);
        diffSelect.setItems("CLASSIC", "A_STAR");
        root.add(diffLbl).right().padRight(12);
        root.add(diffSelect).width(200).left().row();

        // Height slider
        Label hLbl = new Label("Height:", skin, "default");
        heightSlider = new Slider(3, 30, 1, false, skin);
        heightSlider.setValue(5);
        heightValue = new Label("5", skin, "default");
        heightSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                heightValue.setText(String.valueOf((int) heightSlider.getValue()));
            }
        });
        Table hRow = new Table();
        hRow.add(heightSlider).width(200).padRight(10);
        hRow.add(heightValue).width(30);
        root.add(hLbl).right().padRight(12);
        root.add(hRow).left().row();

        // Width slider
        Label wLbl = new Label("Width:", skin, "default");
        widthSlider = new Slider(3, 30, 1, false, skin);
        widthSlider.setValue(6);
        widthValue = new Label("6", skin, "default");
        widthSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                widthValue.setText(String.valueOf((int) widthSlider.getValue()));
            }
        });
        Table wRow = new Table();
        wRow.add(widthSlider).width(200).padRight(10);
        wRow.add(widthValue).width(30);
        root.add(wLbl).right().padRight(12);
        root.add(wRow).left().row();

        // Animated checkbox
        animatedCheck = new CheckBox("  Animated Generation", skin);
        animatedCheck.setChecked(true);
        root.add(animatedCheck).colspan(2).padTop(8).row();

        // Race mode
        raceCheck = new CheckBox("  Race vs Bot", skin);
        root.add(raceCheck).colspan(2).padTop(4).row();

        Label botLbl = new Label("Bot Strategy:", skin, "default");
        botStrategySelect = new SelectBox<>(skin);
        botStrategySelect.setItems("GREEDY", "GHOST_AVOIDANCE");
        botStrategySelect.setVisible(false);
        botLbl.setVisible(false);
        root.add(botLbl).right().padRight(12);
        root.add(botStrategySelect).width(200).left().row();

        raceCheck.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                boolean on = raceCheck.isChecked();
                botStrategySelect.setVisible(on);
                // find bot label sibling (previous row)
                botLbl.setVisible(on);
            }
        });

        // Server address
        Label serverLbl = new Label("Server:", skin, "default");
        serverField = new TextField("https://pacman-s2-g.onrender.com", skin);
        root.add(serverLbl).right().padRight(12);
        root.add(serverField).width(250).left().padTop(8).row();

        // Generate button
        generateBtn = new TextButton("GENERATE", skin, "accent");
        generateBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                onGenerate();
            }
        });
        root.add(generateBtn).colspan(2).width(280).height(55).padTop(24).row();

        // Watch Replay button
        TextButton replayBtn = new TextButton("WATCH LAST REPLAY", skin, "default");
        replayBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                onWatchReplay();
            }
        });
        root.add(replayBtn).colspan(2).width(280).height(45).padTop(10).row();

        // Status label
        statusLabel = new Label("", skin, "small");
        root.add(statusLabel).colspan(2).padTop(12).row();

        // Credits
        Label credits = new Label("TER S2 - Abou Orm | Kabban | Abdelbaki", skin, "small");
        root.add(credits).colspan(2).padTop(40).row();

        stage.addActor(root);
    }

    private void onGenerate() {
        String algo = algoSelect.getSelected().toLowerCase();
        int height = (int) heightSlider.getValue();
        int width = (int) widthSlider.getValue();
        boolean animated = animatedCheck.isChecked();
        String server = serverField.getText().trim();

        // Build base URL from the server field
        String baseUrl;
        if (server.startsWith("http://") || server.startsWith("https://")) {
            // Full URL provided – use as-is (strip trailing slash)
            baseUrl = server.endsWith("/") ? server.substring(0, server.length() - 1) : server;
        } else if (server.contains(":")) {
            // host:port format
            baseUrl = "http://" + server;
        } else {
            // Plain hostname – assume http on port 5000
            baseUrl = "http://" + server + ":5000";
        }

        statusLabel.setText("Generating...");
        statusLabel.setColor(NeonSkin.CYAN);

        String diff = diffSelect.getSelected();

        if (raceCheck.isChecked()) {
            BotAgent.BotStrategy strategy =
                BotAgent.BotStrategy.valueOf(botStrategySelect.getSelected());
            game.setScreen(new RaceScreen(game, algo, height, width, baseUrl, diff, strategy));
        } else {
            game.setScreen(new MazeScreen(game, algo, height, width, animated, baseUrl, diff));
        }
    }

    private void onWatchReplay() {
        String server = serverField.getText().trim();
        String baseUrl;
        if (server.startsWith("http://") || server.startsWith("https://")) {
            baseUrl = server.endsWith("/") ? server.substring(0, server.length() - 1) : server;
        } else if (server.contains(":")) {
            baseUrl = "http://" + server;
        } else {
            baseUrl = "http://" + server + ":5000";
        }

        statusLabel.setText("Fetching latest replay...");
        statusLabel.setColor(NeonSkin.CYAN);

        // Fetch replay list, pick first, then load full replay
        ApiClient api = new ApiClient(baseUrl);
        api.listReplays(null, 1, new ApiClient.ReplayListCallback() {
            @Override
            public void onSuccess(String listJson) {
                try {
                    com.badlogic.gdx.utils.JsonValue arr = new com.badlogic.gdx.utils.JsonReader().parse(listJson);
                    if (arr == null || arr.size == 0) {
                        statusLabel.setText("No replays found");
                        statusLabel.setColor(NeonSkin.ERROR_RED);
                        return;
                    }
                    String replayId = arr.get(0).getString("_id");
                    // Load full replay
                    api.loadReplay(replayId, new ApiClient.ReplayLoadCallback() {
                        @Override
                        public void onSuccess(String replayJson) {
                            game.setScreen(new ReplayScreen(game, replayJson, baseUrl));
                        }
                        @Override
                        public void onError(String msg) {
                            statusLabel.setText("Load error: " + msg);
                            statusLabel.setColor(NeonSkin.ERROR_RED);
                        }
                    });
                } catch (Exception e) {
                    statusLabel.setText("Parse error: " + e.getMessage());
                    statusLabel.setColor(NeonSkin.ERROR_RED);
                }
            }
            @Override
            public void onError(String msg) {
                statusLabel.setText("Replay list error: " + msg);
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

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() { dispose(); }
    @Override public void dispose() {
        if (stage != null) stage.dispose();
    }
}
