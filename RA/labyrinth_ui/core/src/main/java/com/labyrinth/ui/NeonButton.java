package com.labyrinth.ui;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

/**
 * Animated button wrapper that adds hover scale-pulse and click squeeze.
 * Drop-in replacement for TextButton in layout tables.
 */
public class NeonButton extends Table {

    private final TextButton inner;
    private boolean hovered = false;
    private float hoverProgress = 0f; // 0→1
    private float clickAnim = 0f;     // >0 when click squeeze is active
    private float pulseTime = 0f;     // continuous timer for glow oscillation

    private static final float HOVER_SPEED = 5f;   // seconds⁻¹
    private static final float CLICK_DURATION = 0.15f;
    private static final float HOVER_SCALE = 1.05f;
    private static final float CLICK_SCALE = 0.95f;

    public NeonButton(String text, String styleName, Skin skin) {
        inner = new TextButton(text, skin, styleName);
        add(inner).grow();

        setTransform(true);

        inner.addListener(new ClickListener() {
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

            @Override
            public void clicked(InputEvent event, float x, float y) {
                clickAnim = CLICK_DURATION;
            }
        });
    }

    /** Forward a ChangeListener to the inner button. */
    public void addChangeListener(ChangeListener listener) {
        inner.addListener(listener);
    }

    public TextButton getInner() {
        return inner;
    }

    @Override
    public void act(float delta) {
        super.act(delta);

        // Hover interpolation
        float hoverTarget = hovered ? 1f : 0f;
        if (hoverProgress < hoverTarget) {
            hoverProgress = Math.min(hoverProgress + HOVER_SPEED * delta, 1f);
        } else if (hoverProgress > hoverTarget) {
            hoverProgress = Math.max(hoverProgress - HOVER_SPEED * delta, 0f);
        }

        // Click animation countdown
        if (clickAnim > 0) {
            clickAnim = Math.max(clickAnim - delta, 0);
        }

        // Pulse timer
        if (hovered) {
            pulseTime += delta;
        } else {
            pulseTime = 0;
        }

        // Apply scale
        float baseScale = 1f + (HOVER_SCALE - 1f) * Interpolation.smooth.apply(hoverProgress);
        if (clickAnim > 0) {
            float clickT = clickAnim / CLICK_DURATION;
            baseScale *= MathUtils.lerp(1f, CLICK_SCALE, clickT);
        }
        // Subtle pulse on hover
        if (hoverProgress > 0.1f) {
            float pulse = 1f + 0.012f * MathUtils.sin(pulseTime * 5f) * hoverProgress;
            baseScale *= pulse;
        }

        setScale(baseScale);
        setOrigin(getWidth() / 2f, getHeight() / 2f);
    }
}
