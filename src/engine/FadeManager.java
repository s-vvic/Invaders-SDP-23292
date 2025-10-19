package engine;

import java.awt.Color;
import java.awt.Graphics;

public class FadeManager {

    private enum FadeState {
        NONE, FADE_IN, FADE_OUT
    }

    private static final int FADE_TIME = 500; // Fade duration in milliseconds

    private static FadeManager instance;

    private FadeState fadeState;
    private Cooldown fadeTimer;
    private int fadeAlpha; // 0 (transparent) to 255 (opaque)
    private boolean isFadingComplete;

    private FadeManager() {
        this.fadeState = FadeState.NONE;
        this.fadeTimer = Core.getCooldown(FADE_TIME);
        this.fadeAlpha = 0;
        this.isFadingComplete = true;
    }

    public static synchronized FadeManager getInstance() {
        if (instance == null) {
            instance = new FadeManager();
        }
        return instance;
    }

    public void fadeIn() {
        this.fadeState = FadeState.FADE_IN;
        this.fadeAlpha = 255;
        this.isFadingComplete = false;
        this.fadeTimer.reset();
    }

    public void fadeOut() {
        this.fadeState = FadeState.FADE_OUT;
        this.fadeAlpha = 0;
        this.isFadingComplete = false;
        this.fadeTimer.reset();
    }

    public void update() {
        if (this.fadeState == FadeState.NONE) {
            return;
        }

        if (!this.fadeTimer.checkFinished()) {
            float progress = (float) this.fadeTimer.timePassed() / FADE_TIME;
            if (this.fadeState == FadeState.FADE_IN) {
                this.fadeAlpha = (int) (255 * (1 - progress));
            } else { // FADE_OUT
                this.fadeAlpha = (int) (255 * progress);
            }
        } else {
            // When fade is finished
            if (this.fadeState == FadeState.FADE_OUT) {
                this.fadeAlpha = 255;
            }
            this.fadeState = FadeState.NONE;
            this.isFadingComplete = true;
        }
    }

    public void draw(Graphics g) {
        if (this.fadeAlpha > 0) {
            g.setColor(new Color(0, 0, 0, Math.min(255, this.fadeAlpha)));
            // Assuming the graphics object covers the full screen buffer
            g.fillRect(0, 0, 1000, 1000); // Use large values to ensure it covers the screen
        }
    }

    public boolean isFading() {
        return this.fadeState != FadeState.NONE;
    }

    public boolean isFadingComplete() {
        return this.isFadingComplete;
    }
}
