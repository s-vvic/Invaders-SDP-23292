package screen;

import java.awt.event.KeyEvent;
import java.util.List;

import engine.Achievement;
import engine.AchievementManager;
import engine.Core;
import engine.FadeManager;

/**
 * Implements the achievement screen, which displays the player's achievements.
 */
public class AchievementScreen extends Screen {

    private boolean isExiting;

    /**
     * Constructor for the AchievementScreen.
     *
     * @param width  Screen width.
     * @param height Screen height.
     * @param fps    Frames per second.
     */
    public AchievementScreen(int width, int height, int fps) {
        super(width, height, fps);
        this.returnCode = 1; // Default return code
        this.isExiting = false;

        FadeManager.getInstance().fadeIn();
    }

    /**
     * Initializes the screen elements.
     */
    @Override
    public void initialize() {
        super.initialize();
    }

    /**
     * Runs the screen's main loop.
     *
     * @return The screen's return code.
     */
    @Override
    public int run() {
        super.run();
        return this.returnCode;
    }

    /**
     * Updates the screen's state.
     */
    @Override
    protected void update() {
        if (this.isExiting) {
            if (FadeManager.getInstance().isFadingComplete()) {
                this.isRunning = false;
            }
            draw();
            return;
        }

        super.update();
        draw();

        if (!FadeManager.getInstance().isFading() && inputManager.isKeyDown(KeyEvent.VK_ESCAPE)) {
            this.isExiting = true;
            FadeManager.getInstance().fadeOut();
        }
    }

    /**
     * Draws the achievements on the screen.
     */
    private void draw() {
        drawManager.initDrawing(this);
        List<Achievement> achievements = AchievementManager.getInstance().getAchievements();
        drawManager.drawAchievements(this, achievements);
        drawManager.completeDrawing(this);
    }
}
