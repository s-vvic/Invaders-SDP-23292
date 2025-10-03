package screen;

import java.awt.event.KeyEvent;
import java.awt.Rectangle;
import java.awt.Color;

import engine.Cooldown;
import engine.Core;
import entity.SoundButton;

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

	private SoundButton soundButton;

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
		this.soundButton = new SoundButton(0, 0);
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
				if (this.returnCode != 5) {
					this.isRunning = false;
				} else {
					soundButton.changeSoundState();
					// TODO : Sound setting.

					this.selectionCooldown.reset();
				}
			}
			if (inputManager.isKeyDown(KeyEvent.VK_RIGHT)
					|| inputManager.isKeyDown(KeyEvent.VK_D)) {
				this.returnCode = 5;
				soundButton.setColor(Color.GREEN);
				this.selectionCooldown.reset();
			}
			if (inputManager.isKeyDown(KeyEvent.VK_LEFT)
					|| inputManager.isKeyDown(KeyEvent.VK_A)) {
				previousMenuItem();
				soundButton.setColor(Color.WHITE);
				this.selectionCooldown.reset();
			}

			// Mouse input
			int mouseX = inputManager.getMouseX();
			int mouseY = inputManager.getMouseY();

			// Define button areas (these are estimates, might need adjustment)
			Rectangle playArea = new Rectangle(this.width / 2 - 50, this.height / 3 * 2 - 15, 100, 30);
			Rectangle highScoresArea = new Rectangle(this.width / 2 - 75, this.height / 3 * 2 + 25, 150, 30);
			Rectangle exitArea = new Rectangle(this.width / 2 - 40, this.height / 3 * 2 + 65, 80, 30);
			Rectangle soundArea = new Rectangle(this.width * 4 / 5 - 16, this.height * 4 / 5 + 16, 32, 32);

			// Update selection based on mouse hover
			if (playArea.contains(mouseX, mouseY)) {
				this.returnCode = 2;
			} else if (highScoresArea.contains(mouseX, mouseY)) {
				this.returnCode = 3;
			} else if (exitArea.contains(mouseX, mouseY)) {
				this.returnCode = 0;
			} else if (soundArea.contains(mouseX, mouseY)) {
				this.returnCode = 5;
				soundButton.setColor(Color.GREEN);
			} 

			if (this.returnCode != 5)
				soundButton.setColor(Color.WHITE);

			if (inputManager.isMouseButtonDown()) {
				if (playArea.contains(mouseX, mouseY)
						|| highScoresArea.contains(mouseX, mouseY)
						|| exitArea.contains(mouseX, mouseY)) {
					this.isRunning = false;
					this.selectionCooldown.reset();
				} else if (soundArea.contains(mouseX, mouseY)) {
					soundButton.changeSoundState();
					// TODO : Sound setting.

					this.selectionCooldown.reset();
				}
			}
		}
	}

	/**
	 * Shifts the focus to the next menu item.
	 */
	private void nextMenuItem() {
		if (this.returnCode == 3)
			this.returnCode = 0;
		else if (this.returnCode == 0)
			this.returnCode = 2;
		else if (this.returnCode == 5) {
			soundButton.setColor(Color.WHITE);
			this.returnCode = 0;
		}
		else
			this.returnCode++;
	}

	/**
	 * Shifts the focus to the previous menu item.
	 */
	private void previousMenuItem() {
		if (this.returnCode == 0)
			this.returnCode = 3;
		else if (this.returnCode == 2)
			this.returnCode = 0;
		else if (this.returnCode == 5) {
			soundButton.setColor(Color.WHITE);
			this.returnCode = 4;
		}
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
		drawManager.drawEntity(soundButton, this.width * 4 / 5 - 16,
				this.height * 4 / 5 - 16);

		drawManager.completeDrawing(this);
	}
}
