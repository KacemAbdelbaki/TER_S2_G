package com.labyrinth.ui;

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
 * Modern rounded drawables, gradients, and glow effects — no external files.
 */
public class NeonSkin {

    // ----- Pac-Man colour palette -----
    public static final Color BG_DARK     = new Color(0.05f, 0.05f, 0.15f, 1f);  // deep navy (maze bg)
    public static final Color BG_MID      = new Color(0.08f, 0.08f, 0.22f, 1f);
    public static final Color BG_LIGHT    = new Color(0.14f, 0.14f, 0.30f, 1f);
    public static final Color ACCENT      = new Color(1.00f, 0.84f, 0.00f, 1f);  // Pac-Man yellow
    public static final Color ACCENT_DIM  = new Color(0.80f, 0.67f, 0.00f, 1f);  // darker gold
    public static final Color CYAN        = new Color(0.00f, 0.83f, 1.00f, 1f);  // ghost cyan (Inky)
    public static final Color TEXT_WHITE  = new Color(0.95f, 0.95f, 0.98f, 1f);
    public static final Color TEXT_GRAY   = new Color(0.60f, 0.60f, 0.68f, 1f);
    public static final Color MAGENTA     = new Color(1.00f, 0.40f, 0.70f, 1f);  // ghost pink (Pinky)
    public static final Color ERROR_RED   = new Color(1.00f, 0.20f, 0.20f, 1f);  // ghost red (Blinky)
    public static final Color ORANGE      = new Color(1.00f, 0.60f, 0.20f, 1f);  // ghost orange (Clyde)

    // Extended glow / glassmorphism colours
    public static final Color GLOW_CYAN   = new Color(0.00f, 0.83f, 1.00f, 0.30f);
    public static final Color GLOW_YELLOW = new Color(1.00f, 0.84f, 0.00f, 0.30f);
    public static final Color PANEL_BG    = new Color(0.07f, 0.07f, 0.20f, 0.92f);
    public static final Color PANEL_BORDER= new Color(0.30f, 0.30f, 0.80f, 0.40f); // blue maze-wall tint

    // Shared drawable dimensions for buttons
    private static final int BTN_W = 320, BTN_H = 52, BTN_R = 26;
    // Pill selector dimensions
    private static final int SEL_W = 240, SEL_H = 40, SEL_R = 20;

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

        // Legacy solid drawables (used by internal widget styles)
        Drawable whiteDraw  = solidDrawable(TEXT_WHITE);

        // ----- Label -----
        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = bodyFont;
        labelStyle.fontColor = TEXT_WHITE;
        skin.add("default", labelStyle);

        Label.LabelStyle titleLabelStyle = new Label.LabelStyle();
        titleLabelStyle.font = titleFont;
        titleLabelStyle.fontColor = ACCENT; // Pac-Man yellow title
        skin.add("title", titleLabelStyle);

        Label.LabelStyle smallLabel = new Label.LabelStyle();
        smallLabel.font = smallFont;
        smallLabel.fontColor = TEXT_GRAY;
        skin.add("small", smallLabel);

        // Section header style
        Label.LabelStyle sectionLabel = new Label.LabelStyle();
        sectionLabel.font = smallFont;
        sectionLabel.fontColor = ACCENT; // yellow section headers
        skin.add("section", sectionLabel);

        // ----- TextButton "default" — outline style (yellow border) -----
        TextButton.TextButtonStyle btnStyle = new TextButton.TextButtonStyle();
        btnStyle.font = bodyFont;
        btnStyle.fontColor = TEXT_WHITE;
        btnStyle.up   = roundedRectBorderDrawable(ACCENT_DIM, new Color(0, 0, 0, 0.15f), BTN_W, BTN_H, BTN_R, 2);
        btnStyle.over = roundedRectBorderDrawable(ACCENT, new Color(ACCENT.r, ACCENT.g, ACCENT.b, 0.15f), BTN_W, BTN_H, BTN_R, 2);
        btnStyle.down = roundedRectDrawable(ACCENT_DIM, BTN_W, BTN_H, BTN_R);
        btnStyle.overFontColor = ACCENT;
        btnStyle.downFontColor = BG_DARK;
        skin.add("default", btnStyle);

        // ----- TextButton "accent" — filled Pac-Man yellow gradient -----
        TextButton.TextButtonStyle accentBtn = new TextButton.TextButtonStyle();
        accentBtn.font = bodyFont;
        accentBtn.fontColor = BG_DARK;
        accentBtn.up   = gradientDrawable(ACCENT, ACCENT_DIM, BTN_W, BTN_H, BTN_R);
        accentBtn.over = gradientDrawable(new Color(1f, 0.92f, 0.20f, 1f), ACCENT, BTN_W, BTN_H, BTN_R);
        accentBtn.down = roundedRectDrawable(ACCENT_DIM, BTN_W, BTN_H, BTN_R);
        accentBtn.overFontColor = BG_DARK;
        skin.add("accent", accentBtn);

        // ----- TextField — rounded background -----
        Drawable tfBg      = roundedRectDrawable(BG_MID, 260, 38, 12);
        Drawable tfFocusBg = roundedRectBorderDrawable(ACCENT, BG_LIGHT, 260, 38, 12, 1);
        TextField.TextFieldStyle tfStyle = new TextField.TextFieldStyle();
        tfStyle.font = bodyFont;
        tfStyle.fontColor = TEXT_WHITE;
        tfStyle.background = tfBg;
        tfStyle.focusedBackground = tfFocusBg;
        tfStyle.cursor = whiteDraw;
        tfStyle.selection = solidDrawable(ACCENT_DIM);
        skin.add("default", tfStyle);

        // ----- Slider — thin track, circular knob -----
        Slider.SliderStyle sliderStyle = new Slider.SliderStyle();
        sliderStyle.background = createRoundedSliderTrack(BG_LIGHT, 200, 4, 2);
        sliderStyle.knob      = createKnobGlow(ACCENT, 22, GLOW_YELLOW, 30);
        sliderStyle.knobOver  = createKnobGlow(new Color(1f, 0.92f, 0.2f, 1f), 24, GLOW_YELLOW, 34);
        sliderStyle.knobDown  = createKnobGlow(ACCENT_DIM, 24, GLOW_YELLOW, 34);
        skin.add("default-horizontal", sliderStyle);

        // ----- CheckBox (kept for backward compat, but NeonToggle replaces in MenuScreen) -----
        Drawable checkOff = createCheckBox(BG_LIGHT, 24, false);
        Drawable checkOn  = createCheckBox(ACCENT, 24, true);
        CheckBox.CheckBoxStyle cbStyle = new CheckBox.CheckBoxStyle();
        cbStyle.font = bodyFont;
        cbStyle.fontColor = TEXT_WHITE;
        cbStyle.checkboxOff = checkOff;
        cbStyle.checkboxOn = checkOn;
        skin.add("default", cbStyle);

        // ----- List (for SelectBox dropdown) — rounded background -----
        List.ListStyle listStyle = new List.ListStyle();
        listStyle.font = bodyFont;
        listStyle.fontColorSelected = BG_DARK;
        listStyle.fontColorUnselected = TEXT_WHITE;
        listStyle.selection = roundedRectDrawable(ACCENT, SEL_W, 36, 8);
        listStyle.background = roundedRectDrawable(BG_MID, SEL_W, 160, 12);
        skin.add("default", listStyle);

        // ----- ScrollPane (for SelectBox) -----
        ScrollPane.ScrollPaneStyle scrollStyle = new ScrollPane.ScrollPaneStyle();
        scrollStyle.background = roundedRectDrawable(BG_MID, SEL_W, 160, 12);
        skin.add("default", scrollStyle);

        // ----- SelectBox — pill-shaped with glowing border -----
        SelectBox.SelectBoxStyle sbStyle = new SelectBox.SelectBoxStyle();
        sbStyle.font = bodyFont;
        sbStyle.fontColor = TEXT_WHITE;
        sbStyle.background = roundedRectBorderDrawable(PANEL_BORDER, BG_LIGHT, SEL_W, SEL_H, SEL_R, 1);
        sbStyle.backgroundOver = roundedRectBorderDrawable(ACCENT, new Color(ACCENT.r, ACCENT.g, ACCENT.b, 0.10f), SEL_W, SEL_H, SEL_R, 2);
        sbStyle.listStyle = listStyle;
        sbStyle.scrollStyle = scrollStyle;
        skin.add("default", sbStyle);

        // ----- Window — glassmorphism card -----
        Window.WindowStyle winStyle = new Window.WindowStyle();
        winStyle.titleFont = bodyFont;
        winStyle.titleFontColor = ACCENT;
        winStyle.background = roundedRectBorderDrawable(PANEL_BORDER, PANEL_BG, 700, 900, 24, 1);
        skin.add("default", winStyle);

        // Register special drawables into skin for direct lookup (typed as Drawable)
        skin.add("panel-bg", roundedRectBorderDrawable(PANEL_BORDER, PANEL_BG, 700, 900, 24, 1), Drawable.class);
        skin.add("panel-shadow", glowDrawable(new Color(0, 0, 0, 0.35f), 700, 900, 24, 16), Drawable.class);
        skin.add("divider", solidDrawable(new Color(ACCENT.r, ACCENT.g, ACCENT.b, 0.3f)), Drawable.class);

        return skin;
    }

    // ===== Drawable factory methods =====

    /** Solid 1×1 colour drawable. */
    public static Drawable solidDrawable(Color color) {
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

    /** Filled rounded rectangle. */
    public static Drawable roundedRectDrawable(Color fill, int w, int h, int r) {
        Pixmap pm = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        pm.setColor(fill);
        drawRoundedRect(pm, 0, 0, w, h, r, true);
        Texture tex = new Texture(pm);
        pm.dispose();
        TextureRegionDrawable d = new TextureRegionDrawable(new TextureRegion(tex));
        d.setMinWidth(w);
        d.setMinHeight(h);
        d.setLeftWidth(r);
        d.setRightWidth(r);
        d.setTopHeight(6);
        d.setBottomHeight(6);
        return d;
    }

    /** Rounded rectangle with a border and separate fill. */
    public static Drawable roundedRectBorderDrawable(Color border, Color fill, int w, int h, int r, int bw) {
        Pixmap pm = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        // outer border
        pm.setColor(border);
        drawRoundedRect(pm, 0, 0, w, h, r, true);
        // inner fill
        pm.setColor(fill);
        drawRoundedRect(pm, bw, bw, w - bw * 2, h - bw * 2, Math.max(r - bw, 0), true);
        Texture tex = new Texture(pm);
        pm.dispose();
        TextureRegionDrawable d = new TextureRegionDrawable(new TextureRegion(tex));
        d.setMinWidth(w);
        d.setMinHeight(h);
        d.setLeftWidth(r);
        d.setRightWidth(r);
        d.setTopHeight(6);
        d.setBottomHeight(6);
        return d;
    }

    /** Vertical gradient inside a rounded rectangle. */
    public static Drawable gradientDrawable(Color top, Color bottom, int w, int h, int r) {
        Pixmap pm = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        // Draw scanline-by-scanline gradient, then mask with rounded rect
        for (int y = 0; y < h; y++) {
            float t = (float) y / (h - 1);
            float cr = top.r + (bottom.r - top.r) * t;
            float cg = top.g + (bottom.g - top.g) * t;
            float cb = top.b + (bottom.b - top.b) * t;
            float ca = top.a + (bottom.a - top.a) * t;
            pm.setColor(cr, cg, cb, ca);
            pm.drawLine(0, y, w - 1, y);
        }
        // mask: clear pixels outside the rounded rect
        Pixmap mask = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        mask.setColor(Color.WHITE);
        drawRoundedRect(mask, 0, 0, w, h, r, true);
        pm.setBlending(Pixmap.Blending.None);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if ((mask.getPixel(x, y) & 0x000000FF) == 0) {
                    pm.drawPixel(x, y, 0);
                }
            }
        }
        pm.setBlending(Pixmap.Blending.SourceOver);
        mask.dispose();
        Texture tex = new Texture(pm);
        pm.dispose();
        TextureRegionDrawable d = new TextureRegionDrawable(new TextureRegion(tex));
        d.setMinWidth(w);
        d.setMinHeight(h);
        d.setLeftWidth(r);
        d.setRightWidth(r);
        d.setTopHeight(6);
        d.setBottomHeight(6);
        return d;
    }

    /** Outer glow halo — expanded semi-transparent rounded rect. */
    public static Drawable glowDrawable(Color glow, int w, int h, int r, int glowSize) {
        int gw = w + glowSize * 2;
        int gh = h + glowSize * 2;
        Pixmap pm = new Pixmap(gw, gh, Pixmap.Format.RGBA8888);
        // Draw several expanding layers with decreasing alpha
        int layers = glowSize;
        for (int i = layers; i >= 1; i--) {
            float alpha = glow.a * (1f - (float) i / layers) * 0.6f;
            pm.setColor(glow.r, glow.g, glow.b, alpha);
            int offset = glowSize - i;
            drawRoundedRect(pm, offset, offset, gw - offset * 2, gh - offset * 2, r + i, true);
        }
        Texture tex = new Texture(pm);
        pm.dispose();
        TextureRegionDrawable d = new TextureRegionDrawable(new TextureRegion(tex));
        d.setMinWidth(gw);
        d.setMinHeight(gh);
        d.setLeftWidth(glowSize + r);
        d.setRightWidth(glowSize + r);
        d.setTopHeight(glowSize);
        d.setBottomHeight(glowSize);
        return d;
    }

    /** Thin rounded slider track. */
    private static Drawable createRoundedSliderTrack(Color color, int w, int h, int r) {
        Pixmap pm = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        pm.setColor(color);
        drawRoundedRect(pm, 0, 0, w, h, r, true);
        Texture tex = new Texture(pm);
        pm.dispose();
        TextureRegionDrawable d = new TextureRegionDrawable(new TextureRegion(tex));
        d.setMinHeight(h);
        d.setMinWidth(0);
        return d;
    }

    /** Circular knob with outer glow halo. */
    public static Drawable createKnobGlow(Color knobColor, int knobSize, Color glowColor, int totalSize) {
        Pixmap pm = new Pixmap(totalSize, totalSize, Pixmap.Format.RGBA8888);
        int cx = totalSize / 2, cy = totalSize / 2;
        // glow halo
        int glowR = totalSize / 2 - 1;
        pm.setColor(glowColor);
        pm.fillCircle(cx, cy, glowR);
        // solid knob
        pm.setColor(knobColor);
        pm.fillCircle(cx, cy, knobSize / 2 - 1);
        Texture tex = new Texture(pm);
        pm.dispose();
        return new TextureRegionDrawable(new TextureRegion(tex));
    }

    /** Legacy checkbox drawable. */
    private static Drawable createCheckBox(Color color, int size, boolean checked) {
        Pixmap pm = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pm.setColor(color);
        pm.drawRectangle(0, 0, size, size);
        pm.drawRectangle(1, 1, size - 2, size - 2);
        if (checked) {
            pm.fillRectangle(4, 4, size - 8, size - 8);
        }
        Texture tex = new Texture(pm);
        pm.dispose();
        return new TextureRegionDrawable(new TextureRegion(tex));
    }

    // ===== Pixmap rounded-rect primitive =====

    /**
     * Draw a filled rounded rectangle onto the given pixmap.
     * Uses circle-fill for corners and rect-fill for the body.
     */
    private static void drawRoundedRect(Pixmap pm, int x, int y, int w, int h, int r, boolean filled) {
        if (r < 1) {
            if (filled) pm.fillRectangle(x, y, w, h);
            else pm.drawRectangle(x, y, w, h);
            return;
        }
        r = Math.min(r, Math.min(w / 2, h / 2));
        if (filled) {
            // central rect
            pm.fillRectangle(x + r, y, w - 2 * r, h);
            // left and right strips
            pm.fillRectangle(x, y + r, r, h - 2 * r);
            pm.fillRectangle(x + w - r, y + r, r, h - 2 * r);
            // four corner circles
            pm.fillCircle(x + r, y + r, r);
            pm.fillCircle(x + w - r - 1, y + r, r);
            pm.fillCircle(x + r, y + h - r - 1, r);
            pm.fillCircle(x + w - r - 1, y + h - r - 1, r);
        } else {
            pm.drawLine(x + r, y, x + w - r, y);
            pm.drawLine(x + r, y + h - 1, x + w - r, y + h - 1);
            pm.drawLine(x, y + r, x, y + h - r);
            pm.drawLine(x + w - 1, y + r, x + w - 1, y + h - r);
        }
    }
}
