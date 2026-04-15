package com.labyrinth.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.labyrinth.Main;
import com.labyrinth.agents.BotAgent;
import com.labyrinth.ui.NeonButton;
import com.labyrinth.ui.NeonSkin;
import com.labyrinth.ui.NeonSlider;
import com.labyrinth.ui.NeonToggle;

/**
 * Modern neon-themed main menu with glassmorphism card layout,
 * sectioned options, toggle switches, animated buttons, and custom sliders.
 */
public class MenuScreen implements Screen {

    private final Main game;
    private Stage stage;
    private Skin skin;

    // Widgets
    private SelectBox<String> algoSelect;
    private SelectBox<String> diffSelect;
    private NeonSlider heightSlider;
    private NeonSlider widthSlider;
    private Label heightValue;
    private Label widthValue;
    private NeonToggle animatedToggle;
    private NeonToggle ghostsToggle;
    private TextField serverField;
    private Label statusLabel;
    private NeonToggle raceToggle;
    private SelectBox<String> botStrategySelect;
    private NeonToggle aiAutoplayToggle;
    private SelectBox<String> pacmanStrategySelect;

    // Conditional labels (need visibility toggling)
    private Label pacmanStratLbl;
    private Label botLbl;

    private static final float CARD_WIDTH = 680f;

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

    // ===== UI construction =====

    private void buildUI() {
        // Outer root fills the screen, centres the card
        Table root = new Table();
        root.setFillParent(true);
        root.center();

        // Card panel with glassmorphism background
        Table card = new Table();
        Drawable panelBg = skin.getDrawable("panel-bg");
        card.setBackground(panelBg);
        card.defaults().pad(4, 32, 4, 32);
        card.pad(40, 0, 32, 0);

        // ── Title ──
        Label title = new Label("PAC-MAN Game", skin, "title");
        card.add(title).colspan(2).center().padBottom(6).row();
        Label subtitle = new Label("TER S2 - Abou Orm | Kabban | Abdelbaki | Radi", skin, "small");
        card.add(subtitle).colspan(2).center().padBottom(24).row();

        // ── GENERATION section ──
        addSectionHeader(card, "GENERATION");

        addLabeledRow(card, "Algorithm", null);
        algoSelect = new SelectBox<>(skin);
        algoSelect.setItems("DFS", "Kruskal", "Prim", "Tetris");
        card.add(algoSelect).width(240).left().padLeft(0).row();

        // Height slider
        Label hLbl = new Label("Height", skin, "default");
        heightSlider = new NeonSlider(3, 30, 1, false, skin);
        heightSlider.setValue(5);
        heightValue = new Label("5", skin, "default");
        heightSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                heightValue.setText(String.valueOf((int) heightSlider.getValue()));
            }
        });
        Table hRow = new Table();
        hRow.add(heightSlider).width(220).padRight(12);
        hRow.add(heightValue).width(30);
        card.add(hLbl).right().padRight(16);
        card.add(hRow).left().row();

        // Width slider
        Label wLbl = new Label("Width", skin, "default");
        widthSlider = new NeonSlider(3, 30, 1, false, skin);
        widthSlider.setValue(6);
        widthValue = new Label("6", skin, "default");
        widthSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                widthValue.setText(String.valueOf((int) widthSlider.getValue()));
            }
        });
        Table wRow = new Table();
        wRow.add(widthSlider).width(220).padRight(12);
        wRow.add(widthValue).width(30);
        card.add(wLbl).right().padRight(16);
        card.add(wRow).left().row();

        // Animated generation toggle
        addToggleRow(card, "Animated Generation", true);
        animatedToggle = lastToggle;

        // ── AI section ──
        addSectionHeader(card, "AI");

        addLabeledRow(card, "Ghost AI", null);
        diffSelect = new SelectBox<>(skin);
        diffSelect.setItems("CLASSIC", "A_STAR");
        card.add(diffSelect).width(240).left().padLeft(0).row();

        // AI Autoplay toggle
        addToggleRow(card, "AI Autoplay (Demo)", false);
        aiAutoplayToggle = lastToggle;

        // Pacman strategy (conditional)
        pacmanStratLbl = new Label("Pacman AI", skin, "default");
        pacmanStrategySelect = new SelectBox<>(skin);
        pacmanStrategySelect.setItems("GREEDY", "GHOST_AVOIDANCE");
        pacmanStrategySelect.setVisible(false);
        pacmanStratLbl.setVisible(false);
        card.add(pacmanStratLbl).right().padRight(16);
        card.add(pacmanStrategySelect).width(240).left().row();

        // ── GAMEPLAY section ──
        addSectionHeader(card, "GAMEPLAY");

        // Enable Ghosts toggle
        addToggleRow(card, "Enable Ghosts", true);
        ghostsToggle = lastToggle;

        // Race vs Bot toggle
        addToggleRow(card, "Race vs Bot", false);
        raceToggle = lastToggle;

        // Bot strategy (conditional)
        botLbl = new Label("Bot Strategy", skin, "default");
        botStrategySelect = new SelectBox<>(skin);
        botStrategySelect.setItems("GREEDY", "GHOST_AVOIDANCE");
        botStrategySelect.setVisible(false);
        botLbl.setVisible(false);
        card.add(botLbl).right().padRight(16);
        card.add(botStrategySelect).width(240).left().row();

        // ── Mutual exclusion listeners ──
        raceToggle.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                boolean on = raceToggle.isOn();
                botStrategySelect.setVisible(on);
                botLbl.setVisible(on);
                if (on) aiAutoplayToggle.setOn(false);
            }
        });
        aiAutoplayToggle.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                boolean on = aiAutoplayToggle.isOn();
                pacmanStrategySelect.setVisible(on);
                pacmanStratLbl.setVisible(on);
                if (on) raceToggle.setOn(false);
            }
        });

        // ── SERVER section ──
        addSectionHeader(card, "SERVER");
        Label serverLbl = new Label("Address", skin, "default");
        serverField = new TextField("https://pacman-s2-g.onrender.com", skin);
        card.add(serverLbl).right().padRight(16);
        card.add(serverField).width(360).left().row();

        // ── ACTIONS ──
        card.add(new Actor()).colspan(2).height(16).row(); // spacer

        NeonButton generateBtn = new NeonButton("GENERATE", "accent", skin);
        generateBtn.addChangeListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                onGenerate();
            }
        });
        card.add(generateBtn).colspan(2).width(340).height(54).center().padTop(8).row();

        NeonButton replayBtn = new NeonButton("BROWSE REPLAYS", "default", skin);
        replayBtn.addChangeListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                onWatchReplay();
            }
        });
        card.add(replayBtn).colspan(2).width(340).height(48).center().padTop(10).row();

        // Status label
        statusLabel = new Label("", skin, "small");
        card.add(statusLabel).colspan(2).center().padTop(12).row();

        // Add card to root inside a ScrollPane for safety at small resolutions
        ScrollPane scroll = new ScrollPane(card, skin);
        scroll.setFadeScrollBars(true);
        scroll.setScrollingDisabled(true, false);
        root.add(scroll).width(CARD_WIDTH).maxHeight(Gdx.graphics.getHeight() - 40).center();

        stage.addActor(root);
    }

    // ===== Helpers =====

    private NeonToggle lastToggle; // temp reference for buildUI convenience

    private void addSectionHeader(Table card, String text) {
        card.add(new Actor()).colspan(2).height(8).row(); // top spacer
        Label header = new Label(text, skin, "section");
        card.add(header).colspan(2).left().padLeft(32).padBottom(2).row();
        // Thin divider line
        Image divider = new Image(skin.getDrawable("divider"));
        card.add(divider).colspan(2).height(1).growX().padLeft(32).padRight(32).padBottom(8).row();
    }

    private void addLabeledRow(Table card, String labelText, Actor widget) {
        Label lbl = new Label(labelText, skin, "default");
        card.add(lbl).right().padRight(16);
        if (widget != null) {
            card.add(widget).left().row();
        }
    }

    private void addToggleRow(Table card, String labelText, boolean defaultOn) {
        Label lbl = new Label(labelText, skin, "default");
        NeonToggle toggle = new NeonToggle(defaultOn);
        Table row = new Table();
        row.add(lbl).expandX().left();
        row.add(toggle).right().size(50, 26);
        card.add(row).colspan(2).growX().padLeft(32).padRight(32).padTop(4).padBottom(4).row();
        lastToggle = toggle;
    }

    // ===== Actions =====

    private void onGenerate() {
        String algo = algoSelect.getSelected().toLowerCase();
        int height = (int) heightSlider.getValue();
        int width = (int) widthSlider.getValue();
        boolean animated = animatedToggle.isOn();
        String server = serverField.getText().trim();

        String baseUrl;
        if (server.startsWith("http://") || server.startsWith("https://")) {
            baseUrl = server.endsWith("/") ? server.substring(0, server.length() - 1) : server;
        } else if (server.contains(":")) {
            baseUrl = "http://" + server;
        } else {
            baseUrl = "http://" + server + ":5000";
        }

        statusLabel.setText("Generating...");
        statusLabel.setColor(NeonSkin.ACCENT);

        String diff = diffSelect.getSelected();
        boolean enableGhosts = ghostsToggle.isOn();

        if (raceToggle.isOn()) {
            BotAgent.BotStrategy strategy = BotAgent.BotStrategy.valueOf(botStrategySelect.getSelected());
            game.setScreen(new RaceScreen(game, algo, height, width, baseUrl, diff, strategy, !enableGhosts));
        } else if (aiAutoplayToggle.isOn()) {
            BotAgent.BotStrategy pacmanStrat = BotAgent.BotStrategy.valueOf(pacmanStrategySelect.getSelected());
            game.setScreen(new MazeScreen(game, algo, height, width, animated, baseUrl, diff, !enableGhosts, true, pacmanStrat));
        } else {
            game.setScreen(new MazeScreen(game, algo, height, width, animated, baseUrl, diff, !enableGhosts));
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
        game.setScreen(new ReplayListScreen(game, baseUrl));
    }

    // ===== Screen lifecycle =====

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(NeonSkin.BG_DARK.r, NeonSkin.BG_DARK.g, NeonSkin.BG_DARK.b, 1f);
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
