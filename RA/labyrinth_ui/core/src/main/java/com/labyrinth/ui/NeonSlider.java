package com.labyrinth.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

/**
 * Custom slider with thin rounded track, glowing progress fill,
 * and a circular knob with a soft glow halo.
 */
public class NeonSlider extends Slider {

    private final ShapeRenderer sr;
    private boolean hovered = false;

    private static final float TRACK_H = 4f;
    private static final float KNOB_R = 10f;
    private static final float KNOB_R_HOVER = 12f;
    private static final float GLOW_R = 18f;

    private final Color trackBg   = new Color(NeonSkin.BG_LIGHT);
    private final Color trackFill = new Color(NeonSkin.ACCENT);
    private final Color knobColor = new Color(NeonSkin.ACCENT);
    private final Color knobHover = new Color(NeonSkin.CYAN);
    private final Color glowColor = new Color(NeonSkin.GLOW_YELLOW);

    public NeonSlider(float min, float max, float step, boolean vertical, Skin skin) {
        super(min, max, step, vertical, skin);
        sr = new ShapeRenderer();

        addListener(new ClickListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                super.enter(event, x, y, pointer, fromActor);
                if (pointer == -1) hovered = true;
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                super.exit(event, x, y, pointer, toActor);
                if (pointer == -1) hovered = false;
            }
        });
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        // Do NOT call super.draw — we draw everything custom
        batch.end();

        float x = getX();
        float y = getY();
        float w = getWidth();
        float h = getHeight();
        float cy = y + h / 2f;

        float percent = getVisualPercent();
        float knobX = x + percent * w;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        sr.setProjectionMatrix(batch.getProjectionMatrix());
        sr.setTransformMatrix(batch.getTransformMatrix());
        sr.begin(ShapeRenderer.ShapeType.Filled);

        // Background track (full width)
        Color bg = new Color(trackBg);
        bg.a *= parentAlpha;
        sr.setColor(bg);
        drawPill(sr, x, cy - TRACK_H / 2f, w, TRACK_H);

        // Progress fill track (left to knob)
        if (percent > 0.005f) {
            Color fill = new Color(trackFill);
            fill.a *= parentAlpha;
            sr.setColor(fill);
            drawPill(sr, x, cy - TRACK_H / 2f, Math.max(knobX - x, TRACK_H), TRACK_H);
        }

        // Knob glow
        Color gc = new Color(glowColor);
        gc.a *= parentAlpha * (hovered || isDragging() ? 1f : 0.5f);
        sr.setColor(gc);
        sr.circle(knobX, cy, GLOW_R, 32);

        // Knob solid
        Color kc = hovered || isDragging() ? new Color(knobHover) : new Color(knobColor);
        kc.a *= parentAlpha;
        sr.setColor(kc);
        float kr = hovered || isDragging() ? KNOB_R_HOVER : KNOB_R;
        sr.circle(knobX, cy, kr, 32);

        sr.end();
        batch.begin();
    }

    private static void drawPill(ShapeRenderer sr, float x, float y, float w, float h) {
        float r = h / 2f;
        sr.rect(x + r, y, w - 2 * r, h);
        sr.circle(x + r, y + r, r, 16);
        sr.circle(x + w - r, y + r, r, 16);
    }
}
