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
    private Label livesLabel;
    private Label dotsLabel;
    private Label stepLabel;
    private Label helpLabel;
    private Label difficultyLabel;
    private Label statusLabel;
    private TextButton backButton;
    private TextButton pauseButton;
    private TextButton restartButton;
    private TextButton skipButton;
    private Slider speedSlider;
    private Table animControls;
    private Table topBar;
    private Table bottomBar;
    private Table ratingTable;
    private Label ratingResultLabel;
    private boolean ratingVisible = false;
    private boolean ratingSubmitted = false;

    private Runnable onBack;
    private RatingListener ratingListener;

    /** Callback for when the user clicks a star rating. */
    public interface RatingListener {
        void onRate(int rating);
    }

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
        livesLabel = new Label("LIVES: 3", skin, "default");
        dotsLabel = new Label("", skin, "small");
        difficultyLabel = new Label("AI: CLASSIC", skin, "default");
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
        topBar.add(livesLabel).padRight(20);
        topBar.add(difficultyLabel).padRight(20);
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

        helpLabel = new Label("WASD/Arrows: Move | Scroll: Zoom | G: Toggle Ghost AI (A*/Classic) | ESC: Menu", skin, "small");
        bottomBar.add(helpLabel);
        stage.addActor(bottomBar);

        // Rating panel (hidden until win)
        ratingTable = new Table();
        ratingTable.setFillParent(true);
        ratingTable.center().padTop(60);

        ratingTable.add(new Label("Rate this maze:", skin, "default")).colspan(6).padBottom(10).row();

        for (int star = 0; star <= 5; star++) {
            final int rating = star;
            String label = star == 0 ? "0" : "";
            for (int s = 0; s < star; s++) label += "\u2605";
            TextButton starBtn = new TextButton(label.isEmpty() ? "0" : label, skin, "default");
            starBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (!ratingSubmitted && ratingListener != null) {
                        ratingListener.onRate(rating);
                    }
                }
            });
            ratingTable.add(starBtn).pad(4).minWidth(50);
        }
        ratingTable.row();
        ratingResultLabel = new Label("", skin, "small");
        ratingTable.add(ratingResultLabel).colspan(6).padTop(8);
        ratingTable.setVisible(false);
        stage.addActor(ratingTable);
    }

    /**
     * Update HUD info each frame.
     */
    public void update(String algorithm, int mazeH, int mazeW, float elapsed,
                       int score, int dotsRemaining, int lives,
                       GenerationAnimator animator, boolean playerActive, boolean won,
                       boolean gameOver) {
        algoLabel.setText(algorithm.toUpperCase());
        sizeLabel.setText(mazeH + " x " + mazeW);
        timerLabel.setText(String.format("Time: %.1fs", elapsed));
        scoreLabel.setText("SCORE: " + score);
        livesLabel.setText("LIVES: " + lives);

        if (playerActive) {
            dotsLabel.setText("Dots: " + dotsRemaining);
        } else {
            dotsLabel.setText("");
        }

        if (won) {
            statusLabel.setText("YOU WIN! Rate the maze below, then ESC or click Menu.");
            statusLabel.setColor(NeonSkin.ACCENT);
            if (!ratingVisible) {
                ratingTable.setVisible(true);
                ratingVisible = true;
            }
        } else if (gameOver) {
            statusLabel.setText("GAME OVER! Score: " + score + "  |  Rate the maze below, then ESC or Menu.");
            statusLabel.setColor(NeonSkin.ERROR_RED);
            if (!ratingVisible) {
                ratingTable.setVisible(true);
                ratingVisible = true;
            }
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

        if (playerActive && !won && !gameOver) {
            helpLabel.setText("WASD / Arrows to move  |  Eat all dots to win!  |  ESC for menu");
        } else if (!playerActive && animActive) {
            helpLabel.setText("Watching generation animation...");
        }
    }
    public void setDifficulty(String diffStr) {
        difficultyLabel.setText("AI: " + diffStr);
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

    public void setRatingListener(RatingListener listener) { this.ratingListener = listener; }

    /** Show a result message after rating is submitted. */
    public void showRatingResult(float avg, int votes) {
        ratingSubmitted = true;
        ratingResultLabel.setText(String.format("Rating: %.2f  (%d votes)", avg, votes));
        ratingResultLabel.setColor(NeonSkin.ACCENT);
    }

    /** Show a rating error message. */
    public void showRatingError(String msg) {
        ratingResultLabel.setText("Rating error: " + msg);
        ratingResultLabel.setColor(NeonSkin.ERROR_RED);
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    public void dispose() {
        stage.dispose();
    }
}
