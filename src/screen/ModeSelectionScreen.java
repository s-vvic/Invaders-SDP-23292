package screen;

import java.awt.event.KeyEvent;

import engine.Cooldown;
import engine.Core;

public class ModeSelectionScreen extends Screen {

    /** Milliseconds between changes in user selection. */
    private static final int SELECTION_TIME = 200;

    /** Time between changes in user selection. */
    private Cooldown selectionCooldown;

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
    public ModeSelectionScreen(final int width, final int height, final int fps) {
        super(width, height, fps);

        this.returnCode = 10;
        this.selectionCooldown = Core.getCooldown(SELECTION_TIME);
        this.selectionCooldown.reset();
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
        if (this.selectionCooldown.checkFinished()
                && (this.inputManager.isKeyDown(KeyEvent.VK_UP) || this.inputManager.isKeyDown(KeyEvent.VK_W))) {
            previousMenuItem();
            this.selectionCooldown.reset();
        }
        if (this.selectionCooldown.checkFinished()
                && (this.inputManager.isKeyDown(KeyEvent.VK_DOWN) || this.inputManager.isKeyDown(KeyEvent.VK_S))) {
            nextMenuItem();
            this.selectionCooldown.reset();
        }
        if (this.selectionCooldown.checkFinished()
                && this.inputManager.isKeyDown(KeyEvent.VK_SPACE)) {
            // Set a new returnCode to trigger the transition screen
            if (this.returnCode == 10) { // 1 Player
                this.returnCode = 13;
            } else if (this.returnCode == 11) { // 2 Players
                this.returnCode = 14;
            }
            this.isRunning = false;
        }
    }

    /**
     * Shifts the focus to the next menu item.
     */
    private void nextMenuItem() {
        if (this.returnCode == 11)
            this.returnCode = 10;
        else
            this.returnCode++;
    }

    /**
     * Shifts the focus to the previous menu item.
     */
    private void previousMenuItem() {
        if (this.returnCode == 10)
            this.returnCode = 11;
        else
            this.returnCode--;
    }

    /**
     * Draws the elements associated with the screen.
     */
    private void draw() {
        drawManager.initDrawing(this);

        drawManager.drawTitle(this);
        drawManager.drawModeSelection(this, this.returnCode);

        drawManager.completeDrawing(this);
    }
}
