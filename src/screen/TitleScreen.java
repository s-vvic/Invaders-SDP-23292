package screen;

import java.awt.event.KeyEvent;
import java.awt.Color;

import engine.Cooldown;
import engine.Core;
import entity.SoundButton;
import engine.FadeManager;

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

	/** Sound button on/off object. */
	private SoundButton soundButton;

	/** Flag to manage the screen exiting state. */
	private boolean isExiting;

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
		this.isExiting = false;

		FadeManager.getInstance().fadeIn();
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
		// If the screen is currently in the process of exiting, wait for the fade to complete.
		if (this.isExiting) {
			if (FadeManager.getInstance().isFadingComplete()) {
				this.isRunning = false;
			}
			// Still waiting for fade to finish, so don't process any other logic.
			draw();
			return;
		}

		super.update();
		draw();

		// Only process input if the screen is not currently fading.
		if (!FadeManager.getInstance().isFading() && this.selectionCooldown.checkFinished()
				&& this.inputDelay.checkFinished()) {
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
				/** select menu*/
				if (this.returnCode != 5) {
					// Instead of closing the screen, start the fade out process.
					this.isExiting = true;
					FadeManager.getInstance().fadeOut();
				} else {
					this.soundButton.changeSoundState();

					if (SoundButton.getIsSoundOn()) {
						// TODO : Sound setting.
					}

					this.selectionCooldown.reset();
					if (this.soundButton.isTeamCreditScreenPossible()) {
						this.returnCode = 8;
						// Also start the fade out process here.
						this.isExiting = true;
						FadeManager.getInstance().fadeOut();
					} else {
						this.selectionCooldown.reset();
					}
				}
			}
			if (inputManager.isKeyDown(KeyEvent.VK_RIGHT)
					|| inputManager.isKeyDown(KeyEvent.VK_D)) {
				this.returnCode = 5;
				this.soundButton.setColor(Color.GREEN);
				this.selectionCooldown.reset();
			}

			if (this.returnCode == 5 && inputManager.isKeyDown(KeyEvent.VK_LEFT)
					|| inputManager.isKeyDown(KeyEvent.VK_A)) {
				this.returnCode = 4;
				this.soundButton.setColor(Color.WHITE);
				this.selectionCooldown.reset();
			}
		}
	}

	/**
	 * Shifts the focus to the next menu item.
	 */
	private void nextMenuItem() {
		if (this.returnCode == 2)
			this.returnCode = 3;
		else if (this.returnCode == 3)
			this.returnCode = 6;
		else if (this.returnCode == 6)
			this.returnCode = 4;
		else if (this.returnCode == 4)
			this.returnCode = 0;
		else if (this.returnCode == 0)
			this.returnCode = 2;
		else if (this.returnCode == 5) {
			this.soundButton.setColor(Color.WHITE);
			this.returnCode = 0;
		}
	}

	/**
	 * Shifts the focus to the previous menu item.
	 */
	private void previousMenuItem() {
		if (this.returnCode == 2)
			this.returnCode = 0;
		else if (this.returnCode == 0)
			this.returnCode = 4;
		else if (this.returnCode == 4)
			this.returnCode = 6;
		else if (this.returnCode == 6)
			this.returnCode = 3;
		else if (this.returnCode == 3)
			this.returnCode = 2;
		else if (this.returnCode == 5) {
			this.soundButton.setColor(Color.WHITE);
			this.returnCode = 6;
		}
	}

	/**
	 * Draws the elements associated with the screen.
	 */
	private void draw() {
		drawManager.initDrawing(this);

		drawManager.drawTitle(this);
		drawManager.drawMenu(this, this.returnCode);
		drawManager.drawEntity(this.soundButton, this.width * 4 / 5 - 16,
				this.height * 4 / 5 - 16);

		drawManager.completeDrawing(this);
	}
}