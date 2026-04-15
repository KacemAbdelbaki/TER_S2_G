package com.labyrinth.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.labyrinth.Main;

/**
 * Desktop launcher for the Labyrinth Visualizer.
 */
public class Lwjgl3Launcher {
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Labyrinth Visualizer");
        config.setWindowedMode(1920, 1080);
        config.setResizable(true);
        config.useVsync(true);
        config.setForegroundFPS(60);
        config.setWindowSizeLimits(800, 480, -1, -1);
        new Lwjgl3Application(new Main(), config);
    }
}
