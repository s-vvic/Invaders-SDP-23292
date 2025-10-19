package screen;

import java.awt.event.KeyEvent;

/**
 * Implements the Easter Egg screen.
 * A simple screen that exits when the space key is pressed.
 */
public class EasterEggScreen extends Screen {

    /**
     * Constructor, establishes the properties of the screen.
     *
     * @param width    Screen width.
     * @param height   Screen height.
     * @param fps      Frames per second.
     */
    public EasterEggScreen(final int width, final int height, final int fps) {
        super(width, height, fps);

        // This screen returns to the main menu (TitleScreen) when it closes.
        this.returnCode = 1;
    }

    /**
     * Starts the action.
     *
     * @return Next screen code.
     */
    public final int run() {
        super.run();
        return this.returnCode;
    }

    /**
     * Updates the elements on screen and checks for events.
     */
    protected final void update() {
        super.update();

        draw();
        if (this.inputDelay.checkFinished() && inputManager.isKeyDown(KeyEvent.VK_SPACE)) {
            this.isRunning = false;
        }
    }

    /**
     * Draws the elements associated with the screen.
     */
    private void draw() {
        drawManager.initDrawing(this);

        // You can ask the drawManager to draw a custom message or image here.
        // For example:
        drawManager.drawTitle(this); // Temporarily reusing the title drawing.
        drawManager.completeDrawing(this);
    }
}