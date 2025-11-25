package engine;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;

import engine.DrawManager.SpriteType;

/**
 * Manages files used in the application.
 * 
 * @author <a href="mailto:RobertoIA1987@gmail.com">Roberto Izquierdo Amo</a>
 * 
 */
public final class FileManager {

	/** Singleton instance of the class. */
	private static FileManager instance;
	/** Application logger. */
	private static Logger logger;

	/**
	 * private constructor.
	 */
	private FileManager() {
		logger = Core.getLogger();
	}

	/**
	 * Returns shared instance of FileManager.
	 * 
	 * @return Shared instance of FileManager.
	 */
	protected static FileManager getInstance() {
		if (instance == null)
			instance = new FileManager();
		return instance;
	}

	/**
	 * Loads sprites from disk.
	 * 
	 * @param spriteMap
	 *                  Mapping of sprite type and empty boolean matrix that will
	 *                  contain the image.
	 * @throws IOException
	 *                     In case of loading problems.
	 */
	public void loadSprite(final Map<SpriteType, boolean[][]> spriteMap)
			throws IOException {
		InputStream inputStream = null;

		try {
			inputStream = FileManager.class.getClassLoader().getResourceAsStream("graphics");
			char c;

			// Sprite loading.
			for (Map.Entry<SpriteType, boolean[][]> sprite : spriteMap
					.entrySet()) {
				for (int i = 0; i < sprite.getValue().length; i++)
					for (int j = 0; j < sprite.getValue()[i].length; j++) {
						do
							c = (char) inputStream.read();
						while (c != '0' && c != '1');

						if (c == '1')
							sprite.getValue()[i][j] = true;
						else
							sprite.getValue()[i][j] = false;
					}
				logger.fine("Sprite " + sprite.getKey() + " loaded.");
			}
			if (inputStream != null)
				inputStream.close();
		} finally {
			if (inputStream != null)
				inputStream.close();
		}
	}

	/**
	 * Loads a font of a given size.
	 * 
	 * @param size
	 *             Point size of the font.
	 * @return New font.
	 * @throws IOException
	 *                             In case of loading problems.
	 * @throws FontFormatException
	 *                             In case of incorrect font format.
	 */
	public Font loadFont(final float size) throws IOException,
			FontFormatException {
		Font font;

		try {
			// Font loading.
			InputStream inputStream = FileManager.class.getClassLoader().getResourceAsStream("font.ttf");
			if (inputStream != null) {
				font = Font.createFont(Font.TRUETYPE_FONT, inputStream).deriveFont(size);
				inputStream.close();
			} else {
				logger.warning("Font file not found, using default font.");
				font = new Font("Monospaced", Font.PLAIN, (int) size);
			}
		} catch (Exception e) {
			logger.warning("Failed to load font, using default font.");
			font = new Font("Monospaced", Font.PLAIN, (int) size);
		}

		return font;
	}

	/**
	 * Loads achievement unlock status from file and returns it as a map.
	 *
	 * @return Map of achievement names and their unlocked status.
	 * @throws IOException
	 *                     In case of loading problems.
	 */
	public Map<String, Boolean> loadAchievements() throws IOException {
		Map<String, Boolean> unlockedStatus = new HashMap<>();
		String path = "achievements.dat";

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path), "UTF-8"))) {
			logger.info("load saved achieving file");
			String line;
			while ((line = reader.readLine()) != null) {

				String[] parts = line.split(":");
				if (parts.length == 2) {

					unlockedStatus.put(parts[0], Boolean.parseBoolean(parts[1]));
				}
			}
		} catch (FileNotFoundException e) {

			logger.info("No saved achievement file found. A new one will be created");
		}
		return unlockedStatus;
	}

	/**
	 * Saves current achievements and their unlock status to disk.
	 *
	 * @param achievements
	 *                     List of achievements to save.
	 * @throws IOException
	 *                     In case of saving problems.
	 */
	public void saveAchievements(final List<Achievement> achievements) throws IOException {
		String path = "achievements.dat";
		try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path), "UTF-8"))) {
			logger.info("Saving achievements to file");

			for (Achievement achievement : achievements) {

				writer.write(achievement.getName() + ":" + achievement.isUnlocked());
				writer.newLine();
			}
		}
	}

}
