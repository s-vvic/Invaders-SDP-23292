package screen;

import java.awt.event.KeyEvent;
import java.awt.Rectangle;

import engine.Cooldown;
import engine.Core;

/**
 * Implements the title screen.
 * 
 * @author <a href="mailto:RobertoIA1987@gmail.com">Roberto Izquierdo Amo</a>
 * 
 */
public class TitleScreen extends Screen {

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
	public TitleScreen(final int width, final int height, final int fps) {
		super(width, height, fps);

		// Defaults to play.
		this.returnCode = 2;
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

		// Mouse hover logic
		int mouseX = this.inputManager.getMouseX();
		int mouseY = this.inputManager.getMouseY();

		// Approximate bounding boxes for menu items.
		// Y positions are the baseline of the text.
		// We assume a font height of 15 and a width of 120 for hit detection.
		int menuItemWidth = 120;
		int fontHeight = 15;

		int playY = this.height / 3 * 2;
		int playX = this.width / 2 - menuItemWidth / 2;

		int highScoresY = this.height / 3 * 2 + fontHeight * 2;
		int highScoresX = this.width / 2 - menuItemWidth / 2;

		int exitY = this.height / 3 * 2 + fontHeight * 4;
		int exitX = this.width / 2 - menuItemWidth / 2;

		// Check for hover over Play
		if (mouseX >= playX && mouseX <= playX + menuItemWidth &&
			mouseY >= playY - fontHeight && mouseY <= playY) {
			this.returnCode = 2;
		}
		// Check for hover over High scores
		else if (mouseX >= highScoresX && mouseX <= highScoresX + menuItemWidth &&
			mouseY >= highScoresY - fontHeight && mouseY <= highScoresY) {
			this.returnCode = 3;
		}
		// Check for hover over Exit
		else if (mouseX >= exitX && mouseX <= exitX + menuItemWidth &&
			mouseY >= exitY - fontHeight && mouseY <= exitY) {
			this.returnCode = 0;
		}

		if (this.selectionCooldown.checkFinished()
				&& this.inputDelay.checkFinished()) {
			// Keyboard input
			if (inputManager.isKeyDown(KeyEvent.VK_UP)
					|| inputManager.isKeyDown(KeyEvent.VK_W)) {
				previousMenuItem();
				this.selectionCooldown.reset();
			}
			if (inputManager.isKeyDown(KeyEvent.VK_DOWN)
					|| inputManager.isKeyDown(KeyEvent.VK_S)) {
				nextMenuItem();
				this.selectionCooldown.reset();
			}
			if (inputManager.isKeyDown(KeyEvent.VK_SPACE)){
				this.isRunning = false;
			}

			

			// Define button areas (these are estimates, might need adjustment)
			Rectangle playArea = new Rectangle(this.width / 2 - 50, this.height / 3 * 2 - 10, 100, 30);
			Rectangle highScoresArea = new Rectangle(this.width / 2 - 75, this.height / 3 * 2 + 20, 150, 30);
			Rectangle shopArea = new Rectangle(this.width / 2 - 40, this.height / 3 * 2 + 45, 80, 30);
			Rectangle exitArea = new Rectangle(this.width / 2 - 40, this.height / 3 * 2 + 75, 80, 30);

			// Update selection based on mouse hover
			if (playArea.contains(mouseX, mouseY)) {
				this.returnCode = 2;
			} else if (highScoresArea.contains(mouseX, mouseY)) {
				this.returnCode = 3;
			} else if (shopArea.contains(mouseX, mouseY)) {
				this.returnCode = 4;
			} else if (exitArea.contains(mouseX, mouseY)) {
				this.returnCode = 0;
			}

			if (inputManager.isMouseButtonDown()) {
				if (playArea.contains(mouseX, mouseY)
						|| highScoresArea.contains(mouseX, mouseY)
						|| exitArea.contains(mouseX, mouseY)) {
					this.isRunning = false;
					this.selectionCooldown.reset();
				}
			}
		}
	}

	/**
	 * Shifts the focus to the next menu item.
	 */
	private void nextMenuItem() {
		if (this.returnCode == 4)
			this.returnCode = 0;
		else if (this.returnCode == 0)
			this.returnCode = 2;
		else
			this.returnCode++;
	}

	/**
	 * Shifts the focus to the previous menu item.
	 */
	private void previousMenuItem() {
		if (this.returnCode == 0)
			this.returnCode = 4;
		else if (this.returnCode == 2)
			this.returnCode = 0;
		else
			this.returnCode--;
	}

	/**
	 * Draws the elements associated with the screen.
	 */
	private void draw() {
		drawManager.initDrawing(this);

		drawManager.drawTitle(this);
		drawManager.drawMenu(this, this.returnCode);

		drawManager.completeDrawing(this);
	}
}
