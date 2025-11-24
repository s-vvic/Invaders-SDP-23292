package engine;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import entity.Entity;
import entity.FinalBoss;
import entity.Ship;
import screen.CreditScreen;
import screen.EasterEggScreen;
import screen.Screen;
import screen.TitleScreen;
import screen.TitleScreen.Star;
import screen.TitleScreen.ShootingStar;

/**
 * Manages screen drawing.
 *
 * @author <a href="mailto:RobertoIA1987@gmail.com">Roberto Izquierdo Amo</a>
 *
 */
public final class DrawManager {

	/**
	 * Private inner class for managing system messages with a timeout.
	 */
	private static class SystemMessage {
		/** The message content. */
		private String message;
		/** The timestamp when the message should expire. */
		private long expiryTime;

		/**
		 * @param message The text of the message.
		 * @param duration The duration in milliseconds for the message to be displayed.
		 */
		SystemMessage(String message, long duration) {
			this.message = message;
			this.expiryTime = System.currentTimeMillis() + duration;
		}
	}

	/** Singleton instance of the class. */
	private static DrawManager instance;
	/** Current frame. */
	private static Frame frame;
	/** FileManager instance. */
	private static FileManager fileManager;
	/** Application logger. */
	private static final Logger logger = Core.getLogger();
	/** Graphics context. */
	private static Graphics graphics;
	/** Buffer Graphics. */
	private static Graphics backBufferGraphics;
	/** Buffer image. */
	private static BufferedImage backBuffer;
	/** Normal sized font. */
	private static Font fontRegular;
	/** Normal sized font properties. */
	private static FontMetrics fontRegularMetrics;
	/** Big sized font. */
	private static Font fontBig;
	/** Big sized font properties. */
	private static FontMetrics fontBigMetrics;
	/** Small sized font for credits. */
	private static Font fontSmall;
	/** Small sized font properties. */
	private static FontMetrics fontSmallMetrics;

	/** A thread-safe list to hold system messages. */
	private static final List<SystemMessage> systemMessages = Collections.synchronizedList(new ArrayList<>());
	/** Default duration for a system message to be on screen. */
	private static final long MESSAGE_DURATION = 4000; // 4 seconds.


	/** Sprite types mapped to their images. */
	private static Map<SpriteType, boolean[][]> spriteMap;

	private float rainbowHue = 0.0f;
	/**
	 * Draws a string at a specific x, y, color, and size.
	 *
	 * @param text The string to draw.
	 * @param x X coordinate.
	 * @param y Y coordinate.
	 * @param c The color to use.
	 * @param size The font size.
	 */
	public void drawText(final String text, final int x, final int y, final Color c, final int size) {
		// 기본 폰트(fontRegular)를 기반으로 원하는 크기의 새 폰트를 생성합니다.
		Font newFont = fontRegular.deriveFont((float)size);

		backBufferGraphics.setFont(newFont);
		backBufferGraphics.setColor(c);
		backBufferGraphics.drawString(text, x, y);
	}

	/**
	 * Gets the pixel width of a string for a specific font size.
	 *
	 * @param text The string to measure.
	 * @param size The font size.
	 * @return The width of the string in pixels.
	 */
	public int getTextWidth(final String text, final int size) {
		// 측정할 폰트를 생성합니다.
		Font newFont = fontRegular.deriveFont((float)size);
		// 해당 폰트의 측정 도구(FontMetrics)를 가져옵니다.
		FontMetrics metrics = backBufferGraphics.getFontMetrics(newFont);
		// 문자열의 너비를 반환합니다.
		return metrics.stringWidth(text);
	}

	/** Sprite types. */
	public static enum SpriteType {
		Ship, ShipDestroyed, Bullet, EnemyBullet, EnemyShipA1, EnemyShipA2,
		EnemyShipB1, EnemyShipB2, EnemyShipC1, EnemyShipC2, EnemyShipSpecial,
		FinalBoss1, FinalBoss2,FinalBossBullet,FinalBossDeath,OmegaBoss1, OmegaBoss2,OmegaBossDeath, Chaser, Explosion, SoundOn, SoundOff, Item_MultiShot,
		Item_Atkspeed, Item_Penetrate, Item_Explode, Item_Slow, Item_Stop,
		Item_Push, Item_Shield, Item_Heal, FinalBossPowerUp1, FinalBossPowerUp2,
		FinalBossPowerUp3, FinalBossPowerUp4, BossLaser1, BossLaser2, BossLaser3,
	}

	/**
	 * Private constructor.
	 */
	private DrawManager() {
		fileManager = Core.getFileManager();
		logger.info("Started loading resources.");

		try {
			spriteMap = new LinkedHashMap<SpriteType, boolean[][]>();

			spriteMap.put(SpriteType.Ship, new boolean[13][8]);
			spriteMap.put(SpriteType.ShipDestroyed, new boolean[13][8]);
			spriteMap.put(SpriteType.Bullet, new boolean[3][5]);
			spriteMap.put(SpriteType.EnemyBullet, new boolean[3][5]);
			spriteMap.put(SpriteType.EnemyShipA1, new boolean[12][8]);
			spriteMap.put(SpriteType.EnemyShipA2, new boolean[12][8]);
			spriteMap.put(SpriteType.EnemyShipB1, new boolean[12][8]);
			spriteMap.put(SpriteType.EnemyShipB2, new boolean[12][8]);
			spriteMap.put(SpriteType.EnemyShipC1, new boolean[12][8]);
			spriteMap.put(SpriteType.EnemyShipC2, new boolean[12][8]);
			spriteMap.put(SpriteType.EnemyShipSpecial, new boolean[16][7]);
			spriteMap.put(SpriteType.Explosion, new boolean[13][7]);
			spriteMap.put(SpriteType.SoundOn, new boolean[15][15]);
			spriteMap.put(SpriteType.SoundOff, new boolean[15][15]);
			spriteMap.put(SpriteType.Item_Explode, new boolean[5][5]);
			spriteMap.put(SpriteType.Item_Slow, new boolean[5][5]);
			spriteMap.put(SpriteType.Item_Stop, new boolean[5][5]);
			spriteMap.put(SpriteType.Item_Push, new boolean[5][5]);
			spriteMap.put(SpriteType.Item_Shield, new boolean[5][5]);
			spriteMap.put(SpriteType.Item_Heal, new boolean[5][5]);
			spriteMap.put(SpriteType.FinalBoss1, new boolean[50][40]);
			spriteMap.put(SpriteType.FinalBoss2, new boolean[50][40]);
			spriteMap.put(SpriteType.FinalBossBullet,new boolean[3][5]);
			spriteMap.put(SpriteType.FinalBossDeath, new boolean[50][40]);
			spriteMap.put(SpriteType.OmegaBoss1, new boolean[32][14]);
			spriteMap.put(SpriteType.OmegaBoss2, new boolean[32][14]);
			spriteMap.put(SpriteType.OmegaBossDeath, new boolean[16][16]);
			spriteMap.put(SpriteType.Chaser, new boolean[10][10]);
			spriteMap.put(SpriteType.FinalBossPowerUp1, new boolean[80][70]);
			spriteMap.put(SpriteType.FinalBossPowerUp2, new boolean[80][70]);
			spriteMap.put(SpriteType.FinalBossPowerUp3, new boolean[80][70]);
			spriteMap.put(SpriteType.FinalBossPowerUp4, new boolean[80][70]);
			spriteMap.put(SpriteType.BossLaser1, new boolean[50][40]);
			spriteMap.put(SpriteType.BossLaser2, new boolean[50][40]);
			spriteMap.put(SpriteType.BossLaser3, new boolean[50][40]);
			fileManager.loadSprite(spriteMap);
			logger.info("Finished loading the sprites.");

			fontRegular = fileManager.loadFont(14f);
			fontBig = fileManager.loadFont(24f);
			fontSmall = fileManager.loadFont(13f);
			logger.info("Finished loading the fonts.");

		} catch (IOException e) {
			logger.warning("Loading failed.");
		} catch (FontFormatException e) {
			logger.warning("Font formatting failed.");
		}
	}

	/**
	 * Returns shared instance of DrawManager.
	 */
	public static DrawManager getInstance() {
		if (instance == null)
			instance = new DrawManager();
		return instance;
	}

	/**
	 * Adds a new system message to be displayed on screen.
	 * @param message The message to display.
	 */
	public static void addSystemMessage(final String message) {
		synchronized (systemMessages) {
			systemMessages.add(new SystemMessage(message, MESSAGE_DURATION));
		}
	}

	/**
	 * Draws all active system messages on the screen and removes expired ones.
	 * @param screen The screen to draw on.
	 */
	public void drawSystemMessages(final Screen screen) {
		synchronized (systemMessages) {
			long currentTime = System.currentTimeMillis();
			// Remove expired messages
			systemMessages.removeIf(msg -> msg.expiryTime < currentTime);

			int yPos = 45; // Start below the top UI elements.
			for (SystemMessage msg : systemMessages) {
				backBufferGraphics.setFont(fontRegular);
				int stringWidth = fontRegularMetrics.stringWidth(msg.message);
				int xPos = (screen.getWidth() - stringWidth) / 2;

				// Draw a semi-transparent background for readability
				backBufferGraphics.setColor(new Color(0, 0, 0, 150));
				backBufferGraphics.fillRoundRect(xPos - 10, yPos - fontRegularMetrics.getAscent(), stringWidth + 20, fontRegularMetrics.getHeight() + 4, 10, 10);

				// Draw the message text
				backBufferGraphics.setColor(Color.ORANGE);
				backBufferGraphics.drawString(msg.message, xPos, yPos + 5);

				yPos += fontRegularMetrics.getHeight() + 10;
			}
		}
	}

	    /**
	     * Returns the graphics context of the back buffer.
	     * 
	     * @return The graphics context.
	     */
	    public Graphics getBackBufferGraphics() {
	        return backBufferGraphics;
	    }
	
	    /**
	     * Returns the back buffer image.
	     * 
	     * @return The back buffer image.
	     */
	    public BufferedImage getBackBuffer() {
	        return backBuffer;
	    }
	/**
	 * Sets the frame to draw the image on.
	 */
	public void setFrame(final Frame currentFrame) {
		frame = currentFrame;
	}

	/**
	 * First part of the drawing process.
	 */
	public void initDrawing(final Screen screen) {
		backBuffer = new BufferedImage(screen.getWidth(), screen.getHeight(),
				BufferedImage.TYPE_INT_RGB);

		graphics = frame.getGraphics();
		backBufferGraphics = backBuffer.getGraphics();

		backBufferGraphics.setColor(Color.BLACK);
		backBufferGraphics.fillRect(0, 0, screen.getWidth(), screen.getHeight());

		fontRegularMetrics = backBufferGraphics.getFontMetrics(fontRegular);
		fontBigMetrics = backBufferGraphics.getFontMetrics(fontBig);
		fontSmallMetrics = backBufferGraphics.getFontMetrics(fontSmall);
	}


	/**
	 * Draws the completed drawing on screen.
	 */
	public void completeDrawing(final Screen screen) {
		graphics.drawImage(backBuffer, frame.getInsets().left,
				frame.getInsets().top, frame);
	}

	/**
	 * Draws an entity.
	 */
	public void drawEntity(final Entity entity, final int positionX, final int positionY) {
		boolean[][] image = spriteMap.get(entity.getSpriteType());
		backBufferGraphics.setColor(entity.getColor());
		for (int i = 0; i < image.length; i++)
			for (int j = 0; j < image[i].length; j++)
				if (image[i][j])
					backBufferGraphics.drawRect(positionX + i * 2, positionY + j * 2, 1, 1);

        if (entity instanceof FinalBoss) {
            backBufferGraphics.setColor(Color.RED);
            backBufferGraphics.drawRect(entity.getPositionX(), entity.getPositionY(), entity.getWidth(), entity.getHeight());
        }
	}

	/**
	 * Draws a scaled entity.
	 */
	public void drawScaledEntity(final Entity entity, final int positionX, final int positionY, float scale) {
		boolean[][] image = spriteMap.get(entity.getSpriteType());
		backBufferGraphics.setColor(entity.getColor());

		if (scale <= 0) return; // Don't draw if invisible

		// The original size of a "pixel" is 2.
		float pixelSize = 2.0f * scale;
		if (pixelSize < 1.0f) pixelSize = 1.0f; // Draw at least 1x1 pixels.

		for (int i = 0; i < image.length; i++) {
			for (int j = 0; j < image[i].length; j++) {
				if (image[i][j]) {
					backBufferGraphics.drawRect(
						positionX + (int)(i * pixelSize), 
						positionY + (int)(j * pixelSize), 
						(int)Math.ceil(pixelSize), 
						(int)Math.ceil(pixelSize));
				}
			}
		}
	}

	/**
	 * Draws current score on screen.
	 */
	public void drawScore(final Screen screen, final int score) {
		backBufferGraphics.setFont(fontRegular);
		backBufferGraphics.setColor(Color.WHITE);
		String scoreString = String.format("%04d", score);
		backBufferGraphics.drawString(scoreString, screen.getWidth() - 120, 38);
	}

    /**
     * Draws the elapsed time on screen.
     */
    public void drawTime(final Screen screen, final long milliseconds) {
        backBufferGraphics.setFont(fontRegular);
        backBufferGraphics.setColor(Color.GRAY);
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        seconds %= 60;
        String timeString = String.format("Time: %02d:%02d", minutes, seconds);
		int x = 15;
		        int y = screen.getHeight() - 50;		backBufferGraphics.drawString(timeString, x, y);
    }

    /**
     * Draws current coin on screen.
     */
    public void drawCoin(final Screen screen, final int coin) {
        backBufferGraphics.setFont(fontRegular);
        backBufferGraphics.setColor(Color.WHITE);
        String coinString = String.format("%03d$", coin);
        int x = screen.getWidth() / 2 - fontRegularMetrics.stringWidth(coinString) / 2;
        int y = screen.getHeight() - 75;
        backBufferGraphics.drawString(coinString, x, y);
    }

	/**
	 * Draws number of remaining lives on screen.
	 */
	public void drawLives(final Screen screen, final int lives) {
		backBufferGraphics.setFont(fontRegular);
		backBufferGraphics.setColor(Color.WHITE);
		backBufferGraphics.drawString("Lives:", 23, 38);
		Ship dummyShip = null;
		if(GameState.isInvincible()){
			rainbowHue += 0.01f;
    		if (rainbowHue > 1.0f) {
        		rainbowHue -= 1.0f;
    		}
    		Color dynamicRainbowColor = Color.getHSBColor(rainbowHue, 1.0f, 1.0f);
			dummyShip = new Ship(0, 0, dynamicRainbowColor);
		}
		else{
			dummyShip = new Ship(0, 0, Color.green);
		}
		
		for (int i = 0; i < lives; i++)
			drawEntity(dummyShip, 80 + 53 * i, 15);
	}

	/**
	 * Draws the items HUD.
	 */
	public void drawItemsHUD(final Screen screen) {
		ItemHUDManager itemHUD = ItemHUDManager.getInstance();
		itemHUD.initialize(screen);
		itemHUD.drawItems(screen, backBufferGraphics);
	}

    /**
     * Draws the current level on the bottom-left of the screen.
     */
    public void drawLevel(final Screen screen, final String levelName) {
        final int paddingX = 30;
        final int paddingY = 75;
        backBufferGraphics.setFont(fontRegular);
        backBufferGraphics.setColor(Color.WHITE);
        int yPos = screen.getHeight() - paddingY;
        backBufferGraphics.drawString(levelName, paddingX, yPos);
    }

    /**
     * Draws an achievement pop-up message on the screen.
     */
    public void drawAchievementPopup(final Screen screen, final String text) {
        int popupWidth = 375;
        int popupHeight = 75;
        int x = screen.getWidth() / 2 - popupWidth / 2;
        int y = 120;
        backBufferGraphics.setColor(new Color(0, 0, 0, 200));
        backBufferGraphics.fillRoundRect(x, y, popupWidth, popupHeight, 15, 15);
        backBufferGraphics.setColor(Color.YELLOW);
        backBufferGraphics.drawRoundRect(x, y, popupWidth, popupHeight, 15, 15);
        backBufferGraphics.setFont(fontRegular);
        backBufferGraphics.setColor(Color.WHITE);
        drawCenteredRegularString(screen, text, y + popupHeight / 2 + 5);
    }

    /**
     * Draws a notification popup for changes in health.
     */
    public void drawHealthPopup(final Screen screen, final String text) {
        int popupWidth = 375;
        int popupHeight = 60;
        int x = screen.getWidth() / 2 - popupWidth / 2;
        int y = 105;
        backBufferGraphics.setColor(new Color(0, 0, 0, 200));
        backBufferGraphics.fillRoundRect(x, y, popupWidth, popupHeight, 15, 15);
        Color textColor;
        if (text.startsWith("+")) {
            textColor = new Color(50, 255, 50);
        } else {
            textColor = new Color(255, 50, 50);
        }
        backBufferGraphics.setColor(textColor);
        drawCenteredBigString(screen, text, y + popupHeight / 2 + 5);
    }

	/**
	 * Draws a thick line from side to side of the screen.
	 */
	public void drawHorizontalLine(final Screen screen, final int positionY) {
		backBufferGraphics.setColor(Color.GREEN);
		backBufferGraphics.drawLine(0, positionY, screen.getWidth(), positionY);
		backBufferGraphics.drawLine(0, positionY + 1, screen.getWidth(), positionY + 1);
	}

	/**
	 * Draws game title.
	 */
	public void drawTitle(final Screen screen) {
		String titleString = "Invaders";
		String instructionsString = "select with w+s / arrows, confirm with space";
		
		backBufferGraphics.setColor(Color.GRAY);
		drawCenteredRegularString(screen, instructionsString, screen.getHeight() / 2);

		backBufferGraphics.setColor(Color.GREEN);
		drawCenteredBigString(screen, titleString, screen.getHeight() / 3);

		// Draw login status
		AuthManager authManager = AuthManager.getInstance();
		backBufferGraphics.setFont(fontRegular);
		if (authManager.isLoggedIn()) {
			backBufferGraphics.setColor(Color.WHITE);
			String welcomeString = "Welcome, " + authManager.getUsername() + "!";
			backBufferGraphics.drawString(welcomeString, 20, 30);
			String logoutString = "[O]ut";
			backBufferGraphics.drawString(logoutString, 20, 50);
		} else {
			backBufferGraphics.setColor(Color.WHITE);
			String loginString = "[L]ogin";
			backBufferGraphics.drawString(loginString, 20, 30);
		}
	}

	/**
	 * Draws main menu.
	 */
	public void drawMenu(final Screen screen, final int option) {
		String playString = "Play";
        String highScoresString = "High scores";
        String achievementsString = "Achievements";
        String shopString = "Shop";
        String webDashboardString = "Web Dashboard";
        String exitString = "Exit";

		// Pulsing color for selected item
		float pulse = (float) ((Math.sin(System.currentTimeMillis() / 200.0) + 1.0) / 2.0);
		Color pulseColor = new Color(0, 0.5f + pulse * 0.5f, 0);

        if (option == 2) backBufferGraphics.setColor(pulseColor);
        else backBufferGraphics.setColor(Color.WHITE);
        drawCenteredRegularString(screen, playString, screen.getHeight() / 3 * 2);

        if (option == 3) backBufferGraphics.setColor(pulseColor);
        else backBufferGraphics.setColor(Color.WHITE);
        drawCenteredRegularString(screen, highScoresString, screen.getHeight() / 3 * 2 + fontRegularMetrics.getHeight() * 1);

        if (option == 6) backBufferGraphics.setColor(pulseColor);
        else backBufferGraphics.setColor(Color.WHITE);
        drawCenteredRegularString(screen, achievementsString, screen.getHeight() / 3 * 2 + fontRegularMetrics.getHeight() * 2);

        if (option == 4) backBufferGraphics.setColor(pulseColor);
        else backBufferGraphics.setColor(Color.WHITE);
        drawCenteredRegularString(screen, shopString, screen.getHeight() / 3 * 2 + fontRegularMetrics.getHeight() * 3);

        if (option == 7) backBufferGraphics.setColor(pulseColor);
        else backBufferGraphics.setColor(Color.WHITE);
        drawCenteredRegularString(screen, webDashboardString, screen.getHeight() / 3 * 2 + fontRegularMetrics.getHeight() * 4);

        if (option == 0) backBufferGraphics.setColor(pulseColor);
        else backBufferGraphics.setColor(Color.WHITE);
        drawCenteredRegularString(screen, exitString, screen.getHeight() / 3 * 2 + fontRegularMetrics.getHeight() * 5);
	}

	/**
	 * Draws game results.
	 */
	public void drawResults(final Screen screen, final int score, final int livesRemaining, final int shipsDestroyed, final float accuracy, final boolean isNewRecord) {
		String scoreString = String.format("score %04d", score);
		String livesRemainingString = "lives remaining " + livesRemaining;
		String shipsDestroyedString = "enemies destroyed " + shipsDestroyed;
		String accuracyString = String.format("accuracy %.2f%%", accuracy * 100);
		int height = isNewRecord ? 4 : 2;
		backBufferGraphics.setColor(Color.WHITE);
		drawCenteredRegularString(screen, scoreString, screen.getHeight() / height);
		drawCenteredRegularString(screen, livesRemainingString, screen.getHeight() / height + fontRegularMetrics.getHeight() * 2);
		drawCenteredRegularString(screen, shipsDestroyedString, screen.getHeight() / height + fontRegularMetrics.getHeight() * 4);
		drawCenteredRegularString(screen, accuracyString, screen.getHeight() / height + fontRegularMetrics.getHeight() * 6);
	}

	/**
	 * Draws interactive characters for name input.
	 */
	public void drawNameInput(final Screen screen, final char[] name, final int nameCharSelected) {
		String newRecordString = "New Record!";
		String introduceNameString = "Introduce name:";
		backBufferGraphics.setColor(Color.GREEN);
		drawCenteredRegularString(screen, newRecordString, screen.getHeight() / 4 + fontRegularMetrics.getHeight() * 10);
		backBufferGraphics.setColor(Color.WHITE);
		drawCenteredRegularString(screen, introduceNameString, screen.getHeight() / 4 + fontRegularMetrics.getHeight() * 12);

		int positionX = screen.getWidth() / 2 - (fontRegularMetrics.getWidths()[name[0]] + fontRegularMetrics.getWidths()[name[1]] + fontRegularMetrics.getWidths()[name[2]] + fontRegularMetrics.getWidths()[' ']) / 2;

		for (int i = 0; i < 3; i++) {
			if (i == nameCharSelected) backBufferGraphics.setColor(Color.GREEN);
			else backBufferGraphics.setColor(Color.WHITE);
			positionX += fontRegularMetrics.getWidths()[name[i]] / 2;
			positionX = i == 0 ? positionX : positionX + (fontRegularMetrics.getWidths()[name[i - 1]] + fontRegularMetrics.getWidths()[' ']) / 2;
			backBufferGraphics.drawString(Character.toString(name[i]), positionX, screen.getHeight() / 4 + fontRegularMetrics.getHeight() * 14);
		}
	}

	/**
	 * Draws basic content of game over screen.
	 */
	public void drawGameOver(final Screen screen, final boolean acceptsInput, final boolean isNewRecord) {
		String gameOverString = "Game Over";
		String continueOrExitString = "Press Space to play again, Escape to exit";
		int height = isNewRecord ? 4 : 2;
		backBufferGraphics.setColor(Color.GREEN);
		drawCenteredBigString(screen, gameOverString, screen.getHeight() / height - fontBigMetrics.getHeight() * 2);
		if (acceptsInput) backBufferGraphics.setColor(Color.GREEN);
		else backBufferGraphics.setColor(Color.GRAY);
		drawCenteredRegularString(screen, continueOrExitString, screen.getHeight() / 2 + fontRegularMetrics.getHeight() * 10);
	}

	/**
	 * Draws high score screen title and instructions.
	 */
	public void drawHighScoreMenu(final Screen screen) {
		String highScoreString = "High Scores";
		String instructionsString = "Press Space to return";
		backBufferGraphics.setColor(Color.GREEN);
		drawCenteredBigString(screen, highScoreString, screen.getHeight() / 8);
		backBufferGraphics.setColor(Color.GRAY);
		drawCenteredRegularString(screen, instructionsString, screen.getHeight() / 5);
	}

	/**
	 * Draws high scores.
	 */
    public void drawHighScores(final Screen screen, final List<Score> highScores) {
        backBufferGraphics.setColor(Color.WHITE);
        int i = 0;
        String scoreString = "";
        for (Score score : highScores) {
            scoreString = String.format("%s        %04d", score.getName(), score.getScore());
            drawCenteredRegularString(screen, scoreString, screen.getHeight() / 4 + fontRegularMetrics.getHeight() * (i + 1) * 2);
            i++;
        }
    }

    public void drawAchievements(final Screen screen, final List<Achievement> achievements) {
        backBufferGraphics.setColor(Color.GREEN);
        drawCenteredBigString(screen, "Achievements", screen.getHeight() / 8);
        int i = 0;
        for (Achievement achievement : achievements) {
            if (achievement.isUnlocked()) {
                backBufferGraphics.setColor(Color.GREEN);
            } else {
                backBufferGraphics.setColor(Color.WHITE);
            }
            drawCenteredRegularString(screen, achievement.getName() + " - " + achievement.getDescription(), screen.getHeight() / 5 + fontRegularMetrics.getHeight() * (i + 1) * 2);
            i++;
        }
        backBufferGraphics.setColor(Color.GRAY);
        drawCenteredRegularString(screen, "Press ESC to return", screen.getHeight() - 50);
    }

	/**
	 * Draws the credits screen title and instructions.
	 */
	public void drawCreditsMenu(final Screen screen) {
		String creditsString = "Credits";
		String instructionsString = "Press Space to return";
		backBufferGraphics.setColor(Color.GREEN);
		drawCenteredBigString(screen, creditsString, screen.getHeight() / 8);
		backBufferGraphics.setColor(Color.GRAY);
		drawCenteredRegularString(screen, instructionsString, screen.getHeight() / 5);
	}

	/**
	 * Draws the list of credits on the screen.
	 */
	public void drawCredits(final Screen screen, final List<CreditScreen.Credit> creditList) {
		backBufferGraphics.setFont(fontSmall);
		int yPosition = screen.getHeight() / 4;
		final int xPosition = screen.getWidth() / 10;
		final int lineSpacing = fontSmallMetrics.getHeight() + 3;
		final int teamSpacing = lineSpacing + 6;
		for (CreditScreen.Credit credit : creditList) {
			backBufferGraphics.setColor(Color.GREEN);
			String teamInfo = String.format("%s - %s", credit.getTeamName(), credit.getRole());
			backBufferGraphics.drawString(teamInfo, xPosition, yPosition);
			yPosition += lineSpacing;
			yPosition += teamSpacing;
		}
	}

	/**
	 * Draws a centered string on regular font.
	 */
	public void drawCenteredRegularString(final Screen screen, final String string, final int height) {
		backBufferGraphics.setFont(fontRegular);
		backBufferGraphics.drawString(string, screen.getWidth() / 2 - fontRegularMetrics.stringWidth(string) / 2, height);
	}

	/**
	 * Draws a centered string with the currently set font.
	 */
	public void drawCenteredString(final Screen screen, final String string, final int height) {
		FontMetrics metrics = backBufferGraphics.getFontMetrics(); // Use current font's metrics
		backBufferGraphics.drawString(string, screen.getWidth() / 2 - metrics.stringWidth(string) / 2, height);
	}

	/**
	 * Draws a centered string on big font.
	 */
	    public void drawCenteredBigString(final Screen screen, final String string, final int height) {
	        backBufferGraphics.setFont(fontBig);
	        backBufferGraphics.drawString(string, screen.getWidth() / 2 - fontBigMetrics.stringWidth(string) / 2, height);
	    }
	
		/**
		 * Draws a centered string on big font onto a given image.
		 */
		public void drawCenteredBigStringOnImage(BufferedImage image, final String string, final int height) {
			if (image == null) return;
			Graphics2D g = image.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.setFont(fontBig);
			FontMetrics metrics = g.getFontMetrics(fontBig);
			g.setColor(Color.GREEN);
			g.drawString(string, image.getWidth() / 2 - metrics.stringWidth(string) / 2, height);
			g.dispose();
		}
	/**
	 * Countdown to game start.
	 */
	public void drawCountDown(final Screen screen, final int level, final int number, final boolean bonusLife) {
		int rectWidth = screen.getWidth();
		int rectHeight = screen.getHeight() / 6;
		backBufferGraphics.setColor(Color.BLACK);
		backBufferGraphics.fillRect(0, screen.getHeight() / 2 - rectHeight / 2, rectWidth, rectHeight);
		backBufferGraphics.setColor(Color.GREEN);
		if (number >= 4) {
			if (!bonusLife) {
				drawCenteredBigString(screen, "Level " + level, screen.getHeight() / 2 + fontBigMetrics.getHeight() / 3);
			} else {
				drawCenteredBigString(screen, "Level " + level + " - Bonus life!", screen.getHeight() / 2 + fontBigMetrics.getHeight() / 3);
			}
		} else if (number != 0) {
			drawCenteredBigString(screen, Integer.toString(number), screen.getHeight() / 2 + fontBigMetrics.getHeight() / 3);
		} else {
			drawCenteredBigString(screen, "GO!", screen.getHeight() / 2 + fontBigMetrics.getHeight() / 3);
		}
	}

	/**
	 * Draws the complete shop screen with all items and levels.
	 */
	public void drawShopScreen(final Screen screen, final int coinBalance, final int selectedItem, final int selectionMode, final int selectedLevel, final int totalItems, final String[] itemNames, final String[] itemDescriptions, final int[][] itemPrices, final int[] maxLevels, final screen.ShopScreen shopScreen) {
		// Draw title
		backBufferGraphics.setColor(Color.GREEN);
		drawCenteredBigString(screen, "SHOP", screen.getHeight() / 8);
		// Draw coin balance
		backBufferGraphics.setColor(Color.YELLOW);
		String balanceString = String.format("Your Balance: %d coins", coinBalance);
		drawCenteredRegularString(screen, balanceString, 120);
		// Draw instructions based on mode
		backBufferGraphics.setColor(Color.GRAY);
		String instructions = "";
		if (selectionMode == 0) {
			instructions = "W/S: Navigate | SPACE: Select | ESC: Exit";
		} else {
			instructions = "A/D: Change Level | SPACE: Buy | ESC: Back";
		}
		drawCenteredRegularString(screen, instructions, 145);

		int headerHeight = 165;
		int footerHeight = 50;
		int availableHeight = screen.getHeight() - headerHeight - footerHeight;

		int currentY = 170;
		int baseSpacing = 58;
		int expandedExtraSpace = 55;

		boolean hasExpandedItem = (selectionMode == 1);

		int totalRequiredHeight = totalItems * baseSpacing;
		if (hasExpandedItem) {
			totalRequiredHeight += expandedExtraSpace;
		}

		int adjustedSpacing = baseSpacing;
		if (totalRequiredHeight > availableHeight) {
			int overflow = totalRequiredHeight - availableHeight;
			adjustedSpacing = baseSpacing - (overflow / totalItems);
			if (adjustedSpacing < 48) {
				adjustedSpacing = 48;
			}
		}

		for (int i = 0; i < totalItems; i++) {
			boolean isSelected = (i == selectedItem) && (selectionMode == 0);
			boolean isLevelSelection = (i == selectedItem && selectionMode == 1);
			int currentLevel = shopScreen.getItemCurrentLevel(i);
			drawShopItem(screen, itemNames[i], itemDescriptions[i], itemPrices[i], maxLevels[i], currentLevel, currentY, isSelected, coinBalance, isLevelSelection, selectedLevel);
			if (isLevelSelection) {
				currentY += adjustedSpacing + expandedExtraSpace;
			} else {
				currentY += adjustedSpacing;
			}
		}

		int exitY = screen.getHeight() - 30;
		if (selectedItem == totalItems && selectionMode == 0) {
			backBufferGraphics.setColor(Color.GREEN);
		} else {
			backBufferGraphics.setColor(Color.WHITE);
		}

		if (shopScreen.betweenLevels) {
			drawCenteredRegularString(screen, "< Back to Game >", exitY);
		} else {
			drawCenteredRegularString(screen, "< Back to Main Menu >", exitY);
		}
	}

	/**
	 * Draws a single shop item with level indicators.
	 */
	public void drawShopItem(final Screen screen, final String itemName, final String description, final int[] prices, final int maxLevel, final int currentLevel, final int yPosition, final boolean isSelected, final int playerCoins, final boolean isLevelSelection, final int selectedLevel) {
		if (isSelected || isLevelSelection) {
			backBufferGraphics.setColor(Color.GREEN);
		} else {
			backBufferGraphics.setColor(Color.WHITE);
		}
		String levelInfo = currentLevel > 0 ? " [Lv." + currentLevel + "/" + maxLevel + "]" : " [Not Owned]";
		backBufferGraphics.setFont(fontRegular);
		backBufferGraphics.drawString(itemName + levelInfo, 30, yPosition);

		if (isSelected || isLevelSelection) {
			backBufferGraphics.setColor(Color.GRAY);
			backBufferGraphics.drawString(description, 30, yPosition + 15);
		}

		if (isLevelSelection) {
			int levelStartX = 30;
			int levelY = yPosition + 35;
			int maxWidth = screen.getWidth() - 60;
			int currX = levelStartX;
			int currY = levelY;
			int spaceBetween = 18;

			for (int lvl = 1; lvl <= maxLevel; lvl++) {
				int price = prices[lvl - 1];
				boolean canAfford = playerCoins >= price;
				boolean isOwned = currentLevel >= lvl;
				boolean isThisLevel = (lvl == selectedLevel);

				if (isOwned) {
					backBufferGraphics.setColor(Color.DARK_GRAY);
				} else if (isThisLevel) {
					backBufferGraphics.setColor(Color.GREEN);
				} else if (canAfford) {
					backBufferGraphics.setColor(Color.WHITE);
				} else {
					backBufferGraphics.setColor(Color.RED);
				}
				String levelText = "Lv." + lvl + (isOwned ? " [OWNED]" : " (" + price + "$)");
				int textWidth = fontRegularMetrics.stringWidth(levelText);

				if (currX + textWidth > levelStartX + maxWidth) {
					currX = levelStartX;
					currY += fontRegularMetrics.getHeight() + 3;
				}
				backBufferGraphics.drawString(levelText, currX, currY);
				currX += textWidth + spaceBetween;
			}
		}
	}

	/**
	 * Draws purchase feedback message.
	 */
	public void drawShopFeedback(final Screen screen, final String message) {
		int popupWidth = 300;
		int popupHeight = 50;
		int x = screen.getWidth() / 2 - popupWidth / 2;
		int y = 70;

		backBufferGraphics.setColor(new Color(0, 0, 0, 200));
		backBufferGraphics.fillRoundRect(x, y, popupWidth, popupHeight, 15, 15);

		if (message.contains("Purchased")) {
			backBufferGraphics.setColor(Color.GREEN);
		} else if (message.contains("Not enough") || message.contains("failed")) {
			backBufferGraphics.setColor(Color.RED);
		} else {
			backBufferGraphics.setColor(Color.YELLOW);
		}
		backBufferGraphics.drawRoundRect(x, y, popupWidth, popupHeight, 15, 15);

		backBufferGraphics.setFont(fontRegular);
		drawCenteredRegularString(screen, message, y + popupHeight / 2 + 5);
	}

	/**
	 * Draws the starfield background.
	 * 
	 * @param screen
	 *            Screen to draw on.
	 * @param stars
	 *            List of stars to draw.
	 * @param angle
	 *            Current rotation angle.
	 */
	public void drawStars(final Screen screen, final List<Star> stars, final float angle) {
		for (Star star : stars) {
			CelestialBody body = star.getCelestialBody();

			// Calculate size based on depth (z), with an initial fade-in period.
			float FADE_IN_FRACTION = 0.2f;
			float scale_factor = (1.0f - body.z / TitleScreen.MAX_STAR_Z);
			scale_factor = (scale_factor - FADE_IN_FRACTION) / (1.0f - FADE_IN_FRACTION);

			if (scale_factor <= 0) continue; // Don't draw if invisible or not yet visible

			// Respecting the user's previous change to a max size of 5.
			int size = (int) (scale_factor * 5.0f) + 1;
			if (size < 1) size = 1;
			if (size > 5) size = 5;

			// Use star's brightness to set its color for twinkling effect
			float b = star.brightness;
			if (b < 0) b = 0;
			if (b > 1) b = 1;

			Color baseColor = star.getColor();
			int r = (int)(baseColor.getRed() * b);
			int g = (int)(baseColor.getGreen() * b);
			int blue = (int)(baseColor.getBlue() * b);
			
            // Draw trail
            if (body.trail != null) {
                for (int i = 0; i < body.trail.size(); i++) {
                    java.awt.geom.Point2D.Float trailPoint = body.trail.get(i);
                    float trailBrightness = b * ( (float) (i + 1) / body.trail.size() ); // Fade out trail
                    if (trailBrightness < 0) trailBrightness = 0;
                    if (trailBrightness > 1) trailBrightness = 1;

                    // Scale trail color by brightness
                    int tr = (int)(baseColor.getRed() * trailBrightness);
                    int tg = (int)(baseColor.getGreen() * trailBrightness);
                    int tblue = (int)(baseColor.getBlue() * trailBrightness);
                    backBufferGraphics.setColor(new Color(tr, tg, tblue));

                    // Trail size can also fade or be smaller
                    int trailSize = (int) (size * ( (float) (i + 1) / body.trail.size() ));
                    if (trailSize < 1) trailSize = 1;
                    backBufferGraphics.fillRect((int) trailPoint.x, (int) trailPoint.y, trailSize, trailSize);
                }
            }

			// Draw the main star
			backBufferGraphics.setColor(new Color(r, g, blue));
			backBufferGraphics.fillRect((int) body.current_screen_x, (int) body.current_screen_y, size, size);
		}
	}	public void drawShootingStars(final Screen screen, final List<ShootingStar> shootingStars, final float angle) {
		final int centerX = screen.getWidth() / 2;
		final int centerY = screen.getHeight() / 2;
		final double angleRad = Math.toRadians(angle);
		final double cosAngle = Math.cos(angleRad);
		final double sinAngle = Math.sin(angleRad);

		for (ShootingStar star : shootingStars) {
			// Calculate the rotated position for the head of the star
			float relX = star.x - centerX;
			float relY = star.y - centerY;
			double rotatedX = relX * cosAngle - relY * sinAngle;
			double rotatedY = relX * sinAngle + relY * cosAngle;
			int screenX = (int) (rotatedX + centerX);
			int screenY = (int) (rotatedY + centerY);

			// Draw the tail (8 segments, as per user's intention)
			for (int i = 1; i <= 8; i++) {
				// Calculate previous positions for the tail
				float prevRelX = (star.x - star.speedX * i * 0.1f) - centerX;
				float prevRelY = (star.y - star.speedY * i * 0.1f) - centerY;

				double prevRotatedX = prevRelX * cosAngle - prevRelY * sinAngle;
				double prevRotatedY = prevRelX * sinAngle + prevRelY * cosAngle;

				int prevScreenX = (int) (prevRotatedX + centerX);
				int prevScreenY = (int) (prevRotatedY + centerY);

				// Fade the tail out (as per user's intention)
				float brightness = 1.0f - (i * 0.1f);
				if (brightness < 0) brightness = 0; // Safety check
				backBufferGraphics.setColor(new Color(brightness, brightness, brightness));
				backBufferGraphics.fillRect(prevScreenX, prevScreenY, 2, 2);
			}

			// Draw the head of the star
			backBufferGraphics.setColor(Color.WHITE);
			backBufferGraphics.fillRect(screenX, screenY, 3, 3);
		}
	}
	public void drawEasterEgg(final Screen screen) {
    
    	EasterEggScreen eeScreen = (EasterEggScreen) screen;
    	String[] menuOptions = eeScreen.getMenuOptions();
    	int selectedOption = eeScreen.getSelectedOption();
		boolean[] optionStates = eeScreen.getOptionStates();

 
    	String titleString = "CHEAT MOD!!";
    
    	rainbowHue += 0.04f;
    	if (rainbowHue > 1.0f) {
        	rainbowHue -= 1.0f;
    	}
    	Color dynamicRainbowColor = Color.getHSBColor(rainbowHue, 1.0f, 1.0f);
    	backBufferGraphics.setColor(dynamicRainbowColor);
    	drawCenteredBigString(screen, titleString, screen.getHeight() / 4);

    	Color defaultColor = Color.WHITE;
    	Color highlightColor = Color.YELLOW;

    	int menuStartY = screen.getHeight() / 3 + 40;
   	    int menuSpacing = 40;

    	for (int i = 0; i < menuOptions.length; i++) {

        	String menuText = menuOptions[i];

        	String stateText = optionStates[i] ? "ON" : "OFF"; 
    
        	String fullMenuText = menuText + ": " + stateText;

        	Color textColor;

        	if (i == selectedOption) {
            	textColor = highlightColor;
            	fullMenuText = "> " + fullMenuText + " <"; 
        	} else {
            	textColor = defaultColor;
        	}
        	backBufferGraphics.setColor(textColor);
        	drawCenteredRegularString(screen, fullMenuText, menuStartY + (i * menuSpacing));

    	}

    	String instructionsString = "UP/DOWN: Select, SPACE: Apply, ESC: Exit";
    	backBufferGraphics.setColor(Color.GRAY);
    	drawCenteredRegularString(screen, instructionsString, screen.getHeight() - 80);
	}
    public void drawFilledRectangle(int x, int y, int width, int height, Color color) {
        backBufferGraphics.setColor(color);
        backBufferGraphics.fillRect(x, y, width, height);
    }
    public void drawCenteredText(final Screen screen, final String text,
                                 final int centerX, final int centerY,
                                 final int fontSize, final Color color) {

        Font oldFont = backBufferGraphics.getFont();
        Font newFont = fontRegular.deriveFont((float)fontSize);

        backBufferGraphics.setFont(newFont);
        FontMetrics metrics = backBufferGraphics.getFontMetrics();

        int x = centerX - metrics.stringWidth(text) / 2;
        int y = centerY + metrics.getAscent() / 2;

        backBufferGraphics.setColor(color);
        backBufferGraphics.drawString(text, x, y);

        backBufferGraphics.setFont(oldFont);
    }

}