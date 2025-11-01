package engine;


import audio.SoundManager;
import engine.level.LevelManager;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import screen.*;

/**
 * Implements core game logic.
 * 
 * @author <a href="mailto:RobertoIA1987@gmail.com">Roberto Izquierdo Amo</a>
 * 
 */
public final class Core {

			/** Width of current screen. */

			private static final int WIDTH = 672;

			/** Height of current screen. */

			private static final int HEIGHT = 780;
	/** Max fps of current screen. */
	private static final int FPS = 60;

	/** Max lives. */
	private static final int MAX_LIVES = 3;
	/** Levels between extra life. */
	private static final int EXTRA_LIFE_FRECUENCY = 3;

	/** Frame to draw the screen on. */
	private static Frame frame;
	/** Screen currently shown. */
	private static Screen currentScreen;
	/** Level manager for loading level settings. */
	private static LevelManager levelManager;
	/** Application logger. */
	private static final Logger LOGGER = Logger.getLogger(Core.class
			.getSimpleName());
	/** Logger handler for printing to disk. */
	private static Handler fileHandler;
	/** Logger handler for printing to console. */
	private static ConsoleHandler consoleHandler;


	/**
	 * Test implementation.
	 * 
	 * @param args
	 *            Program args, ignored.
	 */
	public static void main(final String[] args) {
		try {
			LOGGER.setUseParentHandlers(false);

			fileHandler = new FileHandler("log");
			fileHandler.setFormatter(new MinimalFormatter());

			consoleHandler = new ConsoleHandler();
			consoleHandler.setFormatter(new MinimalFormatter());

			LOGGER.addHandler(fileHandler);
			LOGGER.addHandler(consoleHandler);
			LOGGER.setLevel(Level.ALL);

		} catch (Exception e) {
			// TODO handle exception
			e.printStackTrace();
		}

		frame = new Frame(WIDTH, HEIGHT);
		DrawManager.getInstance().setFrame(frame);
		int width = frame.getWidth();
		int height = frame.getHeight();

		levelManager = new LevelManager();
		GameState gameState = new GameState(1, 0, MAX_LIVES, MAX_LIVES, 0, 0,0);


        int returnCode = 1;
		do {
            gameState = new GameState(1, 0, MAX_LIVES,MAX_LIVES, 0, 0,gameState.getCoin());
			switch (returnCode) {
                case 1:
                    // Main menu.
                    currentScreen = new TitleScreen(width, height, FPS);
					SoundManager.stopAll();
					SoundManager.playLoop("sfx/menu_music.wav");
                    LOGGER.info("Starting " + WIDTH + "x" + HEIGHT
                            + " title screen at " + FPS + " fps.");
                    returnCode = frame.setScreen(currentScreen);
                    LOGGER.info("Closing title screen.");
                    break;
                case 2:
                    // Mode selection.
                    currentScreen = new ModeSelectionScreen(width, height, FPS);
                    LOGGER.info("Starting " + WIDTH + "x" + HEIGHT
                            + " mode selection screen at " + FPS + " fps.");
                    returnCode = frame.setScreen(currentScreen);
                    LOGGER.info("Closing mode selection screen.");
                    break;
                case 3:
                    // High scores.
                    currentScreen = new HighScoreScreen(width, height, FPS);
                    LOGGER.info("Starting " + WIDTH + "x" + HEIGHT
                            + " high score screen at " + FPS + " fps.");
                    returnCode = frame.setScreen(currentScreen);
                    LOGGER.info("Closing high score screen.");
                    break;
                case 4:
                    // Shop opened manually from main menu

                    currentScreen = new ShopScreen(gameState, width, height, FPS, false);
                    LOGGER.info("Starting shop screen (menu) with " + gameState.getCoin() + " coins.");
                    returnCode = frame.setScreen(currentScreen);
                    LOGGER.info("Closing shop screen (menu).");
                    break;
                case 6:
                    // Achievements
                    currentScreen = new AchievementScreen(width, height, FPS);
                    LOGGER.info("Starting " + WIDTH + "x" + HEIGHT
                            + " achievement screen at " + FPS + " fps.");
                    returnCode = frame.setScreen(currentScreen);
                    LOGGER.info("Closing achievement screen.");
                    break;
				case 8: // CreditScreen
					currentScreen = new CreditScreen(width, height, FPS);
					LOGGER.info("Starting " + currentScreen.getClass().getSimpleName() + " screen.");
					returnCode = frame.setScreen(currentScreen);
					break;
                case 10: // 1 Player
                case 11: // 2 Players
                    boolean isTwoPlayer = (returnCode == 11);
                    do {
                        // One extra life every few levels
                        boolean bonusLife = gameState.getLevel()
                                % EXTRA_LIFE_FRECUENCY == 0
                                && gameState.getLivesRemaining() < MAX_LIVES;

						// Music for each level
						SoundManager.stopAll();
						SoundManager.playLoop("sfx/level" + gameState.getLevel() + ".wav");

                        engine.level.Level currentLevel = levelManager.getLevel(gameState.getLevel());

                        if (currentLevel == null) {
                          break;
                        }

                        // Start a new level
                        currentScreen = new GameScreen(
                                gameState,
                                currentLevel,
                                bonusLife,
                                MAX_LIVES,
                                width,
                                height,
                                FPS,
                                isTwoPlayer
                        );

                        LOGGER.info("Starting " + WIDTH + "x" + HEIGHT
                                + " game screen at " + FPS + " fps.");
                        frame.setScreen(currentScreen);
                        LOGGER.info("Closing game screen.");
                        gameState = ((GameScreen) currentScreen).getGameState();
                        if (gameState.getLivesRemaining() > 0 || gameState.getLivesRemainingP2() > 0) {
							SoundManager.stopAll();
							SoundManager.play("sfx/levelup.wav");

							LOGGER.info("Opening shop screen with "
                                    + gameState.getCoin() + " coins.");

                            //Launch the ShopScreen (between levels)
                            currentScreen = new ShopScreen(gameState, width, height, FPS, true);

                            frame.setScreen(currentScreen);
                            LOGGER.info("Closing shop screen.");

                            gameState = new GameState(
                                    gameState.getLevel() + 1,          // Increment level
                                    gameState.getScore(),              // Keep current score
                                    gameState.getLivesRemaining(),     // Keep remaining lives
									gameState.getLivesRemainingP2(),   // Keep remaining livesP2
                                    gameState.getBulletsShot(),        // Keep bullets fired
                                    gameState.getShipsDestroyed(),     // Keep ships destroyed
                                    gameState.getCoin()                // Keep current coins
                            );
                        }
                    } while (gameState.getLivesRemaining() > 0);

					SoundManager.stopAll();
					SoundManager.play("sfx/gameover.wav");

                    LOGGER.info("Starting " + WIDTH + "x" + HEIGHT
                            + " score screen at " + FPS + " fps, with a score of "
                            + gameState.getScore() + ", "
                            + gameState.getLivesRemaining() + " lives remaining, "
                            + gameState.getBulletsShot() + " bullets shot and "
                            + gameState.getShipsDestroyed() + " ships destroyed.");

                    currentScreen = new ScoreScreen(width, height, FPS, gameState);
                    returnCode = frame.setScreen(currentScreen);
                    LOGGER.info("Closing score screen.");
                    break;
				case 100:
					currentScreen = new EasterEggScreen(width, height, FPS);
               		LOGGER.info("Starting Easter Egg screen.");
                	returnCode = frame.setScreen(currentScreen);
                	LOGGER.info("Closing Easter Egg screen.");
                	break;
                default:
                    break;
            }

        } while (returnCode != 0);

        fileHandler.flush();
        fileHandler.close();
        System.exit(0);
	}

	/**
	 * Constructor, not called.
	 */
	private Core() {

	}

	/**
	 * Controls access to the logger.
	 * 
	 * @return Application logger.
	 */
	public static Logger getLogger() {
		return LOGGER;
	}

	/**
	 * Controls access to the drawing manager.
	 * 
	 * @return Application draw manager.
	 */
	public static DrawManager getDrawManager() {
		return DrawManager.getInstance();
	}

	/**
	 * Controls access to the input manager.
	 * 
	 * @return Application input manager.
	 */
	public static InputManager getInputManager() {
		return InputManager.getInstance();
	}

	/**
	 * Controls access to the file manager.
	 * 
	 * @return Application file manager.
	 */
	public static FileManager getFileManager() {
		return FileManager.getInstance();
	}

	/**
	 * Controls creation of new cooldowns.
	 * 
	 * @param milliseconds
	 *            Duration of the cooldown.
	 * @return A new cooldown.
	 */
	public static Cooldown getCooldown(final int milliseconds) {
		return new Cooldown(milliseconds);
	}

	/**
	 * Controls creation of new cooldowns with variance.
	 * 
	 * @param milliseconds
	 *            Duration of the cooldown.
	 * @param variance
	 *            Variation in the cooldown duration.
	 * @return A new cooldown with variance.
	 */
	public static Cooldown getVariableCooldown(final int milliseconds,
			final int variance) {
		return new Cooldown(milliseconds, variance);
	}
}