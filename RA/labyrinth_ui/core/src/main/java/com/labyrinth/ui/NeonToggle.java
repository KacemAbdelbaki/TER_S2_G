package com.labyrinth.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

/**
 * Modern toggle switch replacing CheckBox.
 * Pill-shaped track with a sliding circular handle; ON = neon glow, OFF = dim.
 */
public class NeonToggle extends Actor {

    private static final float TRACK_W = 50f;
    private static final float TRACK_H = 26f;
    private static final float HANDLE_R = 10f;
    private static final float ANIM_DURATION = 0.2f;

    private boolean on;
    private float animProgress; // 0 = OFF, 1 = ON

    private final ShapeRenderer sr;

    // Colours
    private final Color trackOn  = new Color(NeonSkin.ACCENT);
    private final Color trackOff = new Color(NeonSkin.BG_LIGHT);
    private final Color handleOn = new Color(NeonSkin.TEXT_WHITE);
    private final Color handleOff = new Color(NeonSkin.TEXT_GRAY);
    private final Color glowOn   = new Color(NeonSkin.GLOW_YELLOW);

    public NeonToggle(boolean initialState) {
        this.on = initialState;
        this.animProgress = initialState ? 1f : 0f;
        setSize(TRACK_W, TRACK_H);
        sr = new ShapeRenderer();

        addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                toggle();
            }
        });
    }

    public void toggle() {
        on = !on;
        fire(new ChangeListener.ChangeEvent());
    }

    public boolean isOn() { return on; }

    public void setOn(boolean state) {
        if (this.on != state) {
            this.on = state;
            fire(new ChangeListener.ChangeEvent());
        }
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        float target = on ? 1f : 0f;
        if (animProgress != target) {
            float speed = 1f / ANIM_DURATION;
            if (animProgress < target) {
                animProgress = Math.min(animProgress + speed * delta, target);
            } else {
                animProgress = Math.max(animProgress - speed * delta, target);
            }
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        // Flush the sprite batch so we can use ShapeRenderer
        batch.end();

        float smoothT = Interpolation.smooth.apply(animProgress);
        float x = getX();
        float y = getY();
        float w = getWidth();
        float h = getHeight();
        float r = h / 2f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        sr.setProjectionMatrix(batch.getProjectionMatrix());
        sr.setTransformMatrix(batch.getTransformMatrix());

        // Draw glow behind track when ON
        if (smoothT > 0.01f) {
            sr.begin(ShapeRenderer.ShapeType.Filled);
            Color gc = new Color(glowOn.r, glowOn.g, glowOn.b, glowOn.a * smoothT * parentAlpha);
            sr.setColor(gc);
            float glowPad = 4f;
            drawPill(sr, x - glowPad, y - glowPad, w + glowPad * 2, h + glowPad * 2);
            sr.end();
        }

        // Track
        sr.begin(ShapeRenderer.ShapeType.Filled);
        Color tc = lerpColor(trackOff, trackOn, smoothT);
        tc.a *= parentAlpha;
        sr.setColor(tc);
        drawPill(sr, x, y, w, h);
        sr.end();

        // Handle
        float handleX = x + r + (w - 2 * r) * smoothT;
        float handleY = y + h / 2f;
        Color hc = lerpColor(handleOff, handleOn, smoothT);
        hc.a *= parentAlpha;

        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(hc);
        drawCircle(sr, handleX, handleY, HANDLE_R);
        sr.end();

        // Resume sprite batch
        batch.begin();
    }

    private static void drawPill(ShapeRenderer sr, float x, float y, float w, float h) {
        float r = h / 2f;
        sr.rect(x + r, y, w - 2 * r, h);
        drawCircle(sr, x + r, y + r, r);
        drawCircle(sr, x + w - r, y + r, r);
    }

    private static void drawCircle(ShapeRenderer sr, float cx, float cy, float r) {
        sr.circle(cx, cy, r, 32);
    }

    private static Color lerpColor(Color a, Color b, float t) {
        return new Color(
            a.r + (b.r - a.r) * t,
            a.g + (b.g - a.g) * t,
            a.b + (b.b - a.b) * t,
            a.a + (b.a - a.a) * t
        );
    }
}
