package com.labyrinth;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.labyrinth.screens.MenuScreen;
import com.labyrinth.ui.NeonSkin;

/**
 * Main entry point extending Game for screen management.
 */
public class Main extends Game {
    public SpriteBatch batch;
    public BitmapFont titleFont;
    public BitmapFont bodyFont;
    public BitmapFont smallFont;
    public Skin skin;

    @Override
    public void create() {
        batch = new SpriteBatch();

        // Generate fonts from built-in default (FreeType used if a TTF is bundled)
        titleFont = generateFont(48, true);
        bodyFont = generateFont(22, false);
        smallFont = generateFont(16, false);

        // Build the neon skin
        skin = NeonSkin.create(bodyFont, smallFont, titleFont);

        setScreen(new MenuScreen(this));
    }

    private BitmapFont generateFont(int size, boolean bold) {
        try {
            String path = bold ? "DejaVuSans-Bold.ttf" : "DejaVuSans.ttf";
            FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal(path));
            FreeTypeFontParameter param = new FreeTypeFontParameter();
            param.size = size;
            param.color = com.badlogic.gdx.graphics.Color.WHITE;
            param.borderWidth = 0;
            param.shadowOffsetX = 0;
            param.shadowOffsetY = 0;
            param.genMipMaps = true;
            param.minFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.Linear;
            param.magFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.Linear;
            BitmapFont font = gen.generateFont(param);
            gen.dispose();
            return font;
        } catch (Exception e) {
            // Fallback to default bitmap font (will be blurry at large sizes)
            BitmapFont font = new BitmapFont();
            font.getData().setScale(size / 15f);
            return font;
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        // Fonts are owned by the Skin — disposing them here would cause
        // a "Pixmap already disposed" crash when Skin.dispose() runs.
        skin.dispose();
    }
}
