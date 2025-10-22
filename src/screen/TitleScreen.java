package screen;

import java.awt.event.KeyEvent;
import java.awt.Color;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

import engine.Cooldown;
import engine.Core;
import engine.DrawManager;
import engine.DrawManager.SpriteType;
import entity.Entity;
import entity.SoundButton;

import audio.SoundManager;
import engine.StarSpeedManager;


/**
 * Implements the title screen.
 * 
 * @author <a href="mailto:RobertoIA1987@gmail.com">Roberto Izquierdo Amo</a>
 * 
 */
public class TitleScreen extends Screen {

	/**
	 * A simple class to represent a star for the animated background.
	 * Stores the non-rotating base coordinates and speed.
	 */
	public static class Star {
		public float x; // Current screen X
		public float y; // Current screen Y
		public float z; // Depth (0 = close, MAX_Z = far)
		public float initial_angle; // Direction from center
		public float speed; // This is now the base speed for z_speed calculation
		public float brightness;
        public float brightnessOffset;
		public Color color;
        public List<java.awt.geom.Point2D.Float> trail; // List to store recent positions for trail
        public int trail_length; // Dynamic trail length based on speed

		public Star(float x, float y, float z, float initial_angle, float speed, Color color, int trail_length) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.initial_angle = initial_angle;
			this.speed = speed; // Keep base speed for potential future use or scaling
			this.brightness = 0;
			this.brightnessOffset = (float) (Math.random() * Math.PI * 2);
			this.color = color;
            this.trail = new ArrayList<>(); // Initialize the trail list
            this.trail_length = trail_length;
		}

        public Color getColor() {
            return color;
        }
	}

	/**
	 * A simple class to represent a shooting star.
	 */
	public static class ShootingStar {
		public float x;
		public float y;
		public float speedX;
		public float speedY;

		public ShootingStar(float x, float y, float speedX, float speedY) {
			this.x = x;
			this.y = y;
			this.speedX = speedX;
			this.speedY = speedY;
		}
	}

	/**
	 * A simple class to represent a background enemy.
	 */
	private static class BackgroundEnemy extends Entity {
		private int speed;

		public BackgroundEnemy(int positionX, int positionY, int speed, SpriteType spriteType) {
			super(positionX, positionY, 12 * 2, 8 * 2, Color.WHITE);
			this.speed = speed;
			this.spriteType = spriteType;
		}

		public int getSpeed() {
			return speed;
		}
	}

	/** Milliseconds between changes in user selection. */
	private static final int SELECTION_TIME = 200;
	/** Number of stars in the background. */
	private static final int NUM_STARS = 1200;
	/** Speed of the rotation animation. */
    private static final float ROTATION_SPEED = 4.0f;
	/** Maximum Z-depth for stars. */
	public static final float MAX_STAR_Z = 500.0f;
	/** Minimum Z-depth for stars (when they reset). */
	private static final float MIN_STAR_Z = -90.0f;
	/** X-coordinate of the starfield origin (center of screen). */
	private static int STAR_ORIGIN_X;
	/** Minimum scale factor for stars, even when far away, to ensure they are always visible and spread out. */
	private static final float MIN_SPREAD_SCALE = 0.01f;
	/** Multiplier to convert star speed to trail length. */
	private static final float TRAIL_SPEED_MULTIPLIER = 2.0f;
	/** Maximum trail length to prevent excessively long trails for very fast stars. */
	private static final int MAX_TRAIL_LENGTH = 10;
	/** Y-coordinate of the starfield origin (center of screen). */
	private static int STAR_ORIGIN_Y;
	/** Milliseconds between enemy spawns. */
	private static final int ENEMY_SPAWN_COOLDOWN = 2000;
	/** Probability of an enemy spawning. */
	private static final double ENEMY_SPAWN_CHANCE = 0.3;
	/** Milliseconds between shooting star spawns. */
    private static final int SHOOTING_STAR_COOLDOWN = 3000;
    /** Probability of a shooting star spawning. */
    private static final double SHOOTING_STAR_SPAWN_CHANCE = 0.0;

	/** Time between changes in user selection. */
	private Cooldown selectionCooldown;
	/** Cooldown for enemy spawning. */
	private Cooldown enemySpawnCooldown;
	/** Cooldown for shooting star spawning. */
    private Cooldown shootingStarCooldown;

	/** List of stars for the background animation. */
	private List<Star> stars;
	/** List of background enemies. */
	private List<Entity> backgroundEnemies;
	/** List of shooting stars. */
    private List<ShootingStar> shootingStars;

	/** Sound button on/off object. */
	private SoundButton soundButton;

    private boolean musicStarted = false;

	/** Current rotation angle of the starfield. */
    private float currentAngle;
    /** Target rotation angle of the starfield. */
    private float targetAngle;

		/** Random number generator. */
	        private Random random;
	        /** Manages global star speed cycles. */
	        private StarSpeedManager speedManager;
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
		this.enemySpawnCooldown = Core.getCooldown(ENEMY_SPAWN_COOLDOWN);
		this.shootingStarCooldown = Core.getCooldown(SHOOTING_STAR_COOLDOWN);
		this.selectionCooldown.reset();
		this.enemySpawnCooldown.reset();
		this.shootingStarCooldown.reset();

		this.random = new Random();
		// Initialize starfield origin
		STAR_ORIGIN_X = width / 2;
		STAR_ORIGIN_Y = height / 2;

		this.stars = new ArrayList<Star>();
		List<Color> starColors = java.util.Arrays.asList(
			Color.WHITE,
			new Color(173, 216, 230), // Light Blue
			new Color(255, 255, 153), // Light Yellow
			new Color(255, 182, 193), // Light Pink
			new Color(204, 204, 255)  // Lavender
		);
		for (int i = 0; i < NUM_STARS; i++) {
			float speed = (float) (Math.random() * 2.5 + 2.0); // Base speed for z_speed calculation
			Color color = starColors.get(random.nextInt(starColors.size()));

			// Distribute z values evenly across the range to avoid gaps
			                float z = MAX_STAR_Z - (i * (MAX_STAR_Z - MIN_STAR_Z) / NUM_STARS);
			                if (z < MIN_STAR_Z) z = MIN_STAR_Z; // Ensure it doesn't go below MIN_STAR_Z due to float precision
			
			                float initial_angle = random.nextFloat() * (float) (Math.PI * 2); // Random direction
			
			                // Calculate trail length based on speed
			                int trail_length = (int) (speed * TRAIL_SPEED_MULTIPLIER);
			                if (trail_length < 1) trail_length = 1; // Minimum trail length
			                if (trail_length > MAX_TRAIL_LENGTH) trail_length = MAX_TRAIL_LENGTH; // Maximum trail length
			
			    			this.stars.add(new Star(STAR_ORIGIN_X, STAR_ORIGIN_Y, z, initial_angle, speed, color, trail_length)); // Pass new properties		}
		this.backgroundEnemies = new ArrayList<Entity>();
		this.shootingStars = new ArrayList<ShootingStar>();

		// Initialize rotation angles
		this.currentAngle = 0;
		this.targetAngle = 0;
		this.speedManager = new StarSpeedManager();
	}
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

		// Smoothly animate the rotation angle
        if (currentAngle < targetAngle) {
            currentAngle = Math.min(currentAngle + ROTATION_SPEED, targetAngle);
        } else if (currentAngle > targetAngle) {
            currentAngle = Math.max(currentAngle - ROTATION_SPEED, targetAngle);
        }

		// Animate stars in their non-rotating space
		for (Star star : this.stars) {
			                            float globalSpeedMultiplier = speedManager.updateAndGetGlobalSpeedMultiplier();
			                			// Calculate dynamic approach speed based on base speed, proximity, and global speed multiplier
			                			float current_approach_speed = star.speed * (1.0f - star.z / MAX_STAR_Z) * 2.0f * globalSpeedMultiplier; // Scale by proximity, boost by 2.0f
			                			if (current_approach_speed < 0.1f) current_approach_speed = 0.1f; // Ensure minimum speed
			                			star.z -= current_approach_speed; // Move star closer			
			            // If star passes the viewer, reset it to far away, creating a seamless loop
			            if (star.z <= MIN_STAR_Z) {
			                // Calculate overshoot amount and wrap around MAX_STAR_Z
			                star.z = MAX_STAR_Z - (MIN_STAR_Z - star.z);
			                star.x = STAR_ORIGIN_X;
			                star.y = STAR_ORIGIN_Y;
			                // No z_speed to re-randomize, only initial_angle
			                star.initial_angle = random.nextFloat() * (float) (Math.PI * 2); // Re-randomize direction
			            }
			// Calculate current screen position based on depth and initial angle
			float scale = MIN_SPREAD_SCALE + (MAX_STAR_Z - star.z) / MAX_STAR_Z * (1.0f - MIN_SPREAD_SCALE); // Scale factor for perspective, ensuring minimum spread
			star.x = STAR_ORIGIN_X + (float) (Math.cos(star.initial_angle) * scale * this.getWidth() / 2);
			star.y = STAR_ORIGIN_Y + (float) (Math.sin(star.initial_angle) * scale * this.getHeight() / 2);

            // Update trail history
            star.trail.add(new java.awt.geom.Point2D.Float(star.x, star.y));
            if (star.trail.size() > star.trail_length) { // Use dynamic trail_length
                star.trail.remove(0); // Remove oldest position
            }

			// Update brightness for twinkling effect
			star.brightness = 0.5f + (float) (Math.sin(star.brightnessOffset + System.currentTimeMillis() / 500.0) + 1.0) / 4.0f;
		}

		// Spawn and move background enemies
		if (this.enemySpawnCooldown.checkFinished()) {
			this.enemySpawnCooldown.reset();
			if (Math.random() < ENEMY_SPAWN_CHANCE) {
				SpriteType[] enemyTypes = { SpriteType.EnemyShipA1, SpriteType.EnemyShipB1, SpriteType.EnemyShipC1 };
				SpriteType randomEnemyType = enemyTypes[random.nextInt(enemyTypes.length)];
				int randomX = (int) (Math.random() * this.getWidth());
				int speed = random.nextInt(2) + 1;
				this.backgroundEnemies.add(new BackgroundEnemy(randomX, -20, speed, randomEnemyType));
			}
		}

		java.util.Iterator<Entity> enemyIterator = this.backgroundEnemies.iterator();
		while (enemyIterator.hasNext()) {
			BackgroundEnemy enemy = (BackgroundEnemy) enemyIterator.next();
			enemy.setPositionY(enemy.getPositionY() + enemy.getSpeed());
			if (enemy.getPositionY() > this.getHeight()) {
				enemyIterator.remove();
			}
		}

		// Spawn and move shooting stars
        if (this.shootingStarCooldown.checkFinished()) {
            this.shootingStarCooldown.reset();
            if (Math.random() < SHOOTING_STAR_SPAWN_CHANCE) {
                float speedX = (float) (Math.random() * 7 + 5) * (Math.random() > 0.5 ? 1 : -1);
                float speedY = (float) (Math.random() * 7 + 5) * (Math.random() > 0.5 ? 1 : -1);
                this.shootingStars.add(new ShootingStar(random.nextInt(this.getWidth()), -10, speedX, speedY));
            }
        }

		java.util.Iterator<ShootingStar> shootingStarIterator = this.shootingStars.iterator();
        while (shootingStarIterator.hasNext()) {
            ShootingStar shootingStar = shootingStarIterator.next();
            shootingStar.x += shootingStar.speedX;
            shootingStar.y += shootingStar.speedY;
            if (shootingStar.x < -20 || shootingStar.x > this.getWidth() + 20 ||
                shootingStar.y < -20 || shootingStar.y > this.getHeight() + 20) {
                shootingStarIterator.remove();
            }
        }

		// Handle sound button color
		if (this.returnCode == 5) {
            float pulse = (float) ((Math.sin(System.currentTimeMillis() / 200.0) + 1.0) / 2.0);
            Color pulseColor = new Color(0, 0.5f + pulse * 0.5f, 0);
            this.soundButton.setColor(pulseColor);
        } else {
            this.soundButton.setColor(Color.WHITE);
        }

		draw();
		if (this.selectionCooldown.checkFinished()
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
				if (this.returnCode != 5) {
					this.isRunning = false;
				} else {
					this.soundButton.changeSoundState();

					if (SoundButton.getIsSoundOn()) {
						SoundManager.uncutAllSound();
					} else {
						SoundManager.cutAllSound();
					}

					if (this.soundButton.isTeamCreditScreenPossible()) {
						this.returnCode = 8;
						this.isRunning = false;
					} else {
						this.selectionCooldown.reset();
					}
				}
			}
			if (inputManager.isKeyDown(KeyEvent.VK_RIGHT)
					|| inputManager.isKeyDown(KeyEvent.VK_D)) {
				this.returnCode = 5;
				this.targetAngle += 90;
				this.selectionCooldown.reset();
			}
			if (this.returnCode == 5 && inputManager.isKeyDown(KeyEvent.VK_LEFT)
					|| inputManager.isKeyDown(KeyEvent.VK_A)) {
				this.returnCode = 4;
				this.targetAngle -= 90;
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
			this.returnCode = 0;
		}
		this.targetAngle += 90;
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
			this.returnCode = 6;
		}
		this.targetAngle -= 90;
	}

	/**
	 * Draws the elements associated with the screen.
	 */
	private void draw() {
		drawManager.initDrawing(this);

		// Draw stars with rotation
		drawManager.drawStars(this, this.stars, this.currentAngle);

		// Draw shooting stars with rotation
        drawManager.drawShootingStars(this, this.shootingStars, this.currentAngle);

		// Draw background enemies with rotation
		final double angleRad = Math.toRadians(this.currentAngle);
        final double cosAngle = Math.cos(angleRad);
        final double sinAngle = Math.sin(angleRad);
        final int centerX = this.getWidth() / 2;
        final int centerY = this.getHeight() / 2;

		for (Entity enemy : this.backgroundEnemies) {
			float relX = enemy.getPositionX() - centerX;
            float relY = enemy.getPositionY() - centerY;

            double rotatedX = relX * cosAngle - relY * sinAngle;
            double rotatedY = relX * sinAngle + relY * cosAngle;

            int screenX = (int) (rotatedX + centerX);
            int screenY = (int) (rotatedY + centerY);

			drawManager.drawEntity(enemy, screenX, screenY);
		}

		drawManager.drawTitle(this);
		drawManager.drawMenu(this, this.returnCode);
		drawManager.drawEntity(this.soundButton, this.width * 4 / 5 - 16,
				this.height * 4 / 5 - 16);

		drawManager.completeDrawing(this);
	}

	/**
	 * Getter for the sound state.
	 * @return isSoundOn of the sound button.
	 */
	public boolean getIsSoundOn() {
		return SoundButton.getIsSoundOn();
	}
}
