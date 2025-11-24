package screen;

import java.awt.Color;
import java.awt.event.KeyEvent;

/**
 * A simple screen displayed while the user is expected to interact with an
 * external web page. It prompts the user to press ESC to return.
 */
public class WebpageScreen extends Screen {

    /**
     * Constructor, establishes the properties of the screen.
     *
     * @param width
     *            Screen width.
     * @param height
     *            Screen height.
     * @param fps
     *            Frames per second, frame rate at which the game is run.
     */
    public WebpageScreen(final int width, final int height, final int fps) {
        super(width, height, fps);

        // Default return code to go back to the title screen.
        this.returnCode = 1;
    }

    /**
     * Starts the main loop for the screen.
     *
     * @return Code to switch to the next screen.
     */
    public final int run() {
        super.run();
        return this.returnCode;
    }

    /**
     * Every frame, we update screen elements and check for events.
     */
    @Override
    protected final void update() {
        super.update();
        draw();

        // Check for ESC key press to go back.
        if (inputManager.isKeyDown(KeyEvent.VK_ESCAPE) && this.inputDelay.checkFinished()) {
            this.isRunning = false;
        }
    }

    /**
     * Draws the elements associated with the screen.
     */
    private void draw() {
        // Init drawing with a black background.
        drawManager.initDrawing(this);

        // Draw the prompt message in the center of the screen.
        drawManager.drawCenteredRegularString(this, "Web page opened in browser", this.getHeight() / 2 - 20);
        drawManager.drawCenteredRegularString(this, "Press ESC to return to the main menu", this.getHeight() / 2 + 20);

        // Complete the drawing cycle.
        drawManager.completeDrawing(this);
    }
}
