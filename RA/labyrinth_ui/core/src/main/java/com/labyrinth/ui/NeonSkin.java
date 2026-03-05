package com.labyrinth.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

/**
 * Programmatic dark neon-themed skin for the Labyrinth Visualizer.
 * No external skin files needed.
 */
public class NeonSkin {

    // ----- Colour palette -----
    public static final Color BG_DARK     = new Color(0.08f, 0.08f, 0.14f, 1f);   // #141423
    public static final Color BG_MID      = new Color(0.12f, 0.12f, 0.20f, 1f);   // #1E1E33
    public static final Color BG_LIGHT    = new Color(0.18f, 0.18f, 0.28f, 1f);   // #2E2E47
    public static final Color ACCENT      = new Color(0.00f, 0.90f, 0.60f, 1f);   // #00E699 neon green
    public static final Color ACCENT_DIM  = new Color(0.00f, 0.60f, 0.40f, 1f);   // dimmer green
    public static final Color CYAN        = new Color(0.00f, 0.83f, 1.00f, 1f);   // #00D4FF
    public static final Color TEXT_WHITE  = new Color(0.92f, 0.92f, 0.96f, 1f);   // #EBEBF5
    public static final Color TEXT_GRAY   = new Color(0.60f, 0.60f, 0.68f, 1f);   // #9999AD
    public static final Color MAGENTA     = new Color(1.00f, 0.00f, 1.00f, 0.7f);
    public static final Color ERROR_RED   = new Color(1.00f, 0.30f, 0.30f, 1f);

    /**
     * Build and return a complete Skin with all widget styles configured.
     */
    public static Skin create(BitmapFont bodyFont, BitmapFont smallFont, BitmapFont titleFont) {
        Skin skin = new Skin();

        // Register fonts
        skin.add("default-font", bodyFont);
        skin.add("small-font", smallFont);
        skin.add("title-font", titleFont);

        // Register colours
        skin.add("accent", ACCENT);
        skin.add("cyan", CYAN);
        skin.add("text", TEXT_WHITE);
        skin.add("text-gray", TEXT_GRAY);

        // Create drawable textures
        Drawable bgDark     = solidDrawable(BG_DARK);
        Drawable bgMid      = solidDrawable(BG_MID);
        Drawable bgLight    = solidDrawable(BG_LIGHT);
        Drawable accentDraw = solidDrawable(ACCENT);
        Drawable accentDim  = solidDrawable(ACCENT_DIM);
        Drawable cyanDraw   = solidDrawable(CYAN);
        Drawable whiteDraw  = solidDrawable(TEXT_WHITE);
        Drawable transparent = solidDrawable(new Color(0, 0, 0, 0));

        // ----- Label -----
        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = bodyFont;
        labelStyle.fontColor = TEXT_WHITE;
        skin.add("default", labelStyle);

        Label.LabelStyle titleLabelStyle = new Label.LabelStyle();
        titleLabelStyle.font = titleFont;
        titleLabelStyle.fontColor = CYAN;
        skin.add("title", titleLabelStyle);

        Label.LabelStyle smallLabel = new Label.LabelStyle();
        smallLabel.font = smallFont;
        smallLabel.fontColor = TEXT_GRAY;
        skin.add("small", smallLabel);

        // ----- TextButton -----
        TextButton.TextButtonStyle btnStyle = new TextButton.TextButtonStyle();
        btnStyle.font = bodyFont;
        btnStyle.fontColor = TEXT_WHITE;
        btnStyle.up = bgLight;
        btnStyle.over = accentDim;
        btnStyle.down = accentDraw;
        btnStyle.downFontColor = BG_DARK;
        skin.add("default", btnStyle);

        TextButton.TextButtonStyle accentBtn = new TextButton.TextButtonStyle();
        accentBtn.font = bodyFont;
        accentBtn.fontColor = BG_DARK;
        accentBtn.up = accentDraw;
        accentBtn.over = cyanDraw;
        accentBtn.down = accentDim;
        skin.add("accent", accentBtn);

        // ----- TextField -----
        TextField.TextFieldStyle tfStyle = new TextField.TextFieldStyle();
        tfStyle.font = bodyFont;
        tfStyle.fontColor = TEXT_WHITE;
        tfStyle.background = bgMid;
        tfStyle.focusedBackground = bgLight;
        tfStyle.cursor = whiteDraw;
        tfStyle.selection = accentDim;
        skin.add("default", tfStyle);

        // ----- Slider -----
        Slider.SliderStyle sliderStyle = new Slider.SliderStyle();
        sliderStyle.background = createSliderBg(BG_LIGHT, 6);
        sliderStyle.knob = createKnob(ACCENT, 18);
        sliderStyle.knobOver = createKnob(CYAN, 20);
        sliderStyle.knobDown = createKnob(ACCENT_DIM, 20);
        skin.add("default-horizontal", sliderStyle);

        // ----- CheckBox -----
        Drawable checkOff = createCheckBox(BG_LIGHT, 24, false);
        Drawable checkOn  = createCheckBox(ACCENT, 24, true);
        CheckBox.CheckBoxStyle cbStyle = new CheckBox.CheckBoxStyle();
        cbStyle.font = bodyFont;
        cbStyle.fontColor = TEXT_WHITE;
        cbStyle.checkboxOff = checkOff;
        cbStyle.checkboxOn = checkOn;
        skin.add("default", cbStyle);

        // ----- List (for SelectBox dropdown) -----
        List.ListStyle listStyle = new List.ListStyle();
        listStyle.font = bodyFont;
        listStyle.fontColorSelected = BG_DARK;
        listStyle.fontColorUnselected = TEXT_WHITE;
        listStyle.selection = accentDraw;
        listStyle.background = bgMid;
        skin.add("default", listStyle);

        // ----- ScrollPane (for SelectBox) -----
        ScrollPane.ScrollPaneStyle scrollStyle = new ScrollPane.ScrollPaneStyle();
        scrollStyle.background = bgMid;
        skin.add("default", scrollStyle);

        // ----- SelectBox -----
        SelectBox.SelectBoxStyle sbStyle = new SelectBox.SelectBoxStyle();
        sbStyle.font = bodyFont;
        sbStyle.fontColor = TEXT_WHITE;
        sbStyle.background = bgLight;
        sbStyle.listStyle = listStyle;
        sbStyle.scrollStyle = scrollStyle;
        skin.add("default", sbStyle);

        // ----- Window -----
        Window.WindowStyle winStyle = new Window.WindowStyle();
        winStyle.titleFont = bodyFont;
        winStyle.titleFontColor = CYAN;
        winStyle.background = bgDark;
        skin.add("default", winStyle);

        return skin;
    }

    // --- Helper methods to create drawable textures ---

    private static Drawable solidDrawable(Color color) {
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(color);
        pm.fill();
        Texture tex = new Texture(pm);
        pm.dispose();
        TextureRegionDrawable d = new TextureRegionDrawable(new TextureRegion(tex));
        d.setMinWidth(0);
        d.setMinHeight(0);
        return d;
    }

    private static Drawable createSliderBg(Color color, int height) {
        Pixmap pm = new Pixmap(1, height, Pixmap.Format.RGBA8888);
        pm.setColor(color);
        pm.fill();
        Texture tex = new Texture(pm);
        pm.dispose();
        TextureRegionDrawable d = new TextureRegionDrawable(new TextureRegion(tex));
        d.setMinHeight(height);
        return d;
    }

    private static Drawable createKnob(Color color, int size) {
        Pixmap pm = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pm.setColor(color);
        pm.fillCircle(size / 2, size / 2, size / 2 - 1);
        Texture tex = new Texture(pm);
        pm.dispose();
        return new TextureRegionDrawable(new TextureRegion(tex));
    }

    private static Drawable createCheckBox(Color color, int size, boolean checked) {
        Pixmap pm = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pm.setColor(color);
        // Border
        pm.drawRectangle(0, 0, size, size);
        pm.drawRectangle(1, 1, size - 2, size - 2);
        if (checked) {
            // Checkmark (filled inner square)
            pm.fillRectangle(4, 4, size - 8, size - 8);
        }
        Texture tex = new Texture(pm);
        pm.dispose();
        return new TextureRegionDrawable(new TextureRegion(tex));
    }
}
