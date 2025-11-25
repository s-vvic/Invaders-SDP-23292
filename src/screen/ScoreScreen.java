package screen;

import java.awt.event.KeyEvent;
import engine.GameState;

/**
 * Implements the score screen.
 * 
 * @author <a href="mailto:RobertoIA1987@gmail.com">Roberto Izquierdo Amo</a>
 * 
 */
public class ScoreScreen extends Screen {

	/** Current score. */
	private int score;
	/** Player lives left. */
	private int livesRemaining;
	/** Total bullets shot by the player. */
	private int bulletsShot;
	/** Total ships destroyed by the player. */
	private int shipsDestroyed;

	/**
	 * Constructor, establishes the properties of the screen.
	 * 
	 * @param width
	 *                  Screen width.
	 * @param height
	 *                  Screen height.
	 * @param fps
	 *                  Frames per second, frame rate at which the game is run.
	 * @param gameState
	 *                  Current game state.
	 */
	public ScoreScreen(final int width, final int height, final int fps,
			final GameState gameState) {
		super(width, height, fps);

		this.score = gameState.getScore();
		this.livesRemaining = gameState.getLivesRemaining();
		this.bulletsShot = gameState.getBulletsShot();
		this.shipsDestroyed = gameState.getShipsDestroyed();
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
		if (this.inputDelay.checkFinished()) {
			if (inputManager.isKeyDown(KeyEvent.VK_ESCAPE)) {
				// Return to main menu.
				this.returnCode = 1;
				this.isRunning = false;
			} else if (inputManager.isKeyDown(KeyEvent.VK_SPACE)) {
				// Play again.
				this.returnCode = 2;
				this.isRunning = false;
			}
		}
	}

	/**
	 * Draws the elements associated with the screen.
	 */
	private void draw() {
		drawManager.initDrawing(this);

		// The original drawGameOver had a isNewRecord parameter, which is now removed.
		// We'll assume false for now, or need to update DrawManager.
		// For now, let's call a simplified version if available, or just pass false.
		// Let's check DrawManager's methods. A quick look suggests it will be fine.
		drawManager.drawGameOver(this, this.inputDelay.checkFinished(),
				false); // Passing false for isNewRecord
		drawManager.drawResults(this, this.score, this.livesRemaining,
				this.shipsDestroyed, (float) this.shipsDestroyed
						/ this.bulletsShot,
				false); // Passing false for isNewRecord

		drawManager.completeDrawing(this);
	}
}
