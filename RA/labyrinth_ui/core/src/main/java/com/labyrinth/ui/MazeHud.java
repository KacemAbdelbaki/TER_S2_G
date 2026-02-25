package com.labyrinth.ui;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.labyrinth.maze.GenerationAnimator;

/**
 * HUD overlay displayed on top of the MazeScreen.
 * Shows algorithm info, timer, animation controls, and navigation help.
 */
public class MazeHud {

    private Stage stage;
    private Skin skin;
    private Label algoLabel;
    private Label sizeLabel;
    private Label timerLabel;
    private Label scoreLabel;
    private Label dotsLabel;
    private Label stepLabel;
    private Label helpLabel;
    private Label statusLabel;
    private TextButton backButton;
    private TextButton pauseButton;
    private TextButton restartButton;
    private TextButton skipButton;
    private Slider speedSlider;
    private Table animControls;
    private Table topBar;
    private Table bottomBar;

    private Runnable onBack;

    public MazeHud(Skin skin, Runnable onBack) {
        this.skin = skin;
        this.onBack = onBack;
        stage = new Stage(new ScreenViewport());
        buildUI();
    }

    private void buildUI() {
        // Top bar
        topBar = new Table();
        topBar.setFillParent(true);
        topBar.top().left().pad(12);

        algoLabel = new Label("", skin, "default");
        sizeLabel = new Label("", skin, "small");
        timerLabel = new Label("Time: 0.0s", skin, "default");
        scoreLabel = new Label("SCORE: 0", skin, "default");
        dotsLabel = new Label("", skin, "small");
        statusLabel = new Label("", skin, "default");

        backButton = new TextButton("< Menu", skin, "default");
        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (onBack != null) onBack.run();
            }
        });

        topBar.add(backButton).padRight(20);
        topBar.add(algoLabel).padRight(20);
        topBar.add(sizeLabel).padRight(40);
        topBar.add(dotsLabel).padRight(20);
        topBar.add(scoreLabel).padRight(20);
        topBar.add(timerLabel).expandX().right().padRight(12);
        stage.addActor(topBar);

        // Animation controls row
        animControls = new Table();
        animControls.setFillParent(true);
        animControls.top().padTop(50).left().padLeft(12);

        pauseButton = new TextButton("Pause", skin, "default");
        restartButton = new TextButton("Restart", skin, "default");
        skipButton = new TextButton("Skip >>", skin, "default");
        speedSlider = new Slider(0f, 1f, 0.05f, false, skin);
        speedSlider.setValue(0.5f);
        stepLabel = new Label("Step: 0 / 0", skin, "small");

        animControls.add(pauseButton).padRight(8);
        animControls.add(restartButton).padRight(8);
        animControls.add(skipButton).padRight(16);
        animControls.add(new Label("Speed:", skin, "small")).padRight(4);
        animControls.add(speedSlider).width(120).padRight(12);
        animControls.add(stepLabel);
        animControls.setVisible(false);
        stage.addActor(animControls);

        // Status label (center)
        Table statusTable = new Table();
        statusTable.setFillParent(true);
        statusTable.center();
        statusTable.add(statusLabel);
        stage.addActor(statusTable);

        // Bottom bar
        bottomBar = new Table();
        bottomBar.setFillParent(true);
        bottomBar.bottom().padBottom(12);

        helpLabel = new Label("WASD / Arrows to move  |  Scroll to zoom  |  ESC for menu", skin, "small");
        bottomBar.add(helpLabel);
        stage.addActor(bottomBar);
    }

    /**
     * Update HUD info each frame.
     */
    public void update(String algorithm, int mazeH, int mazeW, float elapsed,
                       int score, int dotsRemaining,
                       GenerationAnimator animator, boolean playerActive, boolean won) {
        algoLabel.setText(algorithm.toUpperCase());
        sizeLabel.setText(mazeH + " x " + mazeW);
        timerLabel.setText(String.format("Time: %.1fs", elapsed));
        scoreLabel.setText("SCORE: " + score);

        if (playerActive) {
            dotsLabel.setText("Dots: " + dotsRemaining);
        } else {
            dotsLabel.setText("");
        }

        if (won) {
            statusLabel.setText("YOU WIN! Press ESC or click Menu.");
            statusLabel.setColor(NeonSkin.ACCENT);
        } else {
            statusLabel.setText("");
        }

        // Animation controls visibility
        boolean animActive = animator != null && animator.getTotalSteps() > 0;
        animControls.setVisible(animActive && !animator.isFinished());
        if (animActive) {
            stepLabel.setText("Step: " + animator.getCurrentStep() + " / " + animator.getTotalSteps());
            pauseButton.setText(animator.isPlaying() ? "Pause" : "Play");
        }

        if (playerActive && !won) {
            helpLabel.setText("WASD / Arrows to move  |  Eat all dots to win!  |  ESC for menu");
        } else if (!playerActive && animActive) {
            helpLabel.setText("Watching generation animation...");
        }
    }

    public void showLoading() {
        statusLabel.setText("Loading maze...");
        statusLabel.setColor(NeonSkin.CYAN);
    }

    public void showError(String msg) {
        statusLabel.setText("Error: " + msg);
        statusLabel.setColor(NeonSkin.ERROR_RED);
    }

    public Stage getStage() { return stage; }
    public TextButton getPauseButton() { return pauseButton; }
    public TextButton getRestartButton() { return restartButton; }
    public TextButton getSkipButton() { return skipButton; }
    public Slider getSpeedSlider() { return speedSlider; }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    public void dispose() {
        stage.dispose();
    }
}
