package screen;


import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.awt.Desktop;
import java.awt.Color;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;


import engine.Cooldown;
import engine.Core;
import engine.DrawManager;
import engine.DrawManager.SpriteType;
import engine.CelestialBody;
import entity.Entity;
import entity.SoundButton;
import engine.Nebula;

import audio.SoundManager;
import engine.StarSpeedManager;
import engine.StarOriginManager;
import engine.CelestialManager;
import engine.NebulaSettings;
import engine.AuthManager;


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
		private CelestialBody celestialBody;
		public float brightness;
        public float brightnessOffset;
		public Color color;

		public Star(CelestialBody celestialBody, Color color) {
			this.celestialBody = celestialBody;
			this.color = color;
			this.brightness = 0;
			this.brightnessOffset = (float) (Math.random() * Math.PI * 2);
		}

        public Color getColor() {
            return color;
        }

		public CelestialBody getCelestialBody() {
			return celestialBody;
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
	public static class BackgroundEnemy extends Entity {
		private CelestialBody celestialBody;

		public BackgroundEnemy(CelestialBody celestialBody, SpriteType spriteType) {
			// Initial position (X, Y) is irrelevant as it will be calculated each frame.
			super(0, 0, 12 * 2, 8 * 2, Color.WHITE);
			this.celestialBody = celestialBody;
			this.spriteType = spriteType;
		}

		public CelestialBody getCelestialBody() {
			return celestialBody;
		}
	}

	/** Milliseconds between changes in user selection. */
	private static final int SELECTION_TIME = 200;
	/** Number of stars in the background. */
	private static final int NUM_STARS = 800;
	/** Speed of the rotation animation. */
    private static final float ROTATION_SPEED = 4.0f;
	/** Maximum Z-depth for stars. */
	public static final float MAX_STAR_Z = 500.0f;
	/** Minimum Z-depth for stars (when they reset). */
	public static final float MIN_STAR_Z = -5.0f;
	/** X-coordinate of the starfield origin (center of screen). */
	private static int STAR_ORIGIN_X;
	/** Minimum scale factor for stars, even when far away, to ensure they are always visible and spread out. */
	private static final float MIN_SPREAD_SCALE = 0.1f;
	/** Multiplier to convert star speed to trail length. */
	private static final float TRAIL_SPEED_MULTIPLIER = 2.0f;
	/** Maximum trail length to prevent excessively long trails for very fast stars. */
	public static final int MAX_TRAIL_LENGTH = 10;
	/** Y-coordinate of the starfield origin (center of screen). */
	private static int STAR_ORIGIN_Y;
	/** Milliseconds between enemy spawns. */
	private static final int ENEMY_SPAWN_COOLDOWN = 2000;
	/** Probability of an enemy spawning. */
	private static final double ENEMY_SPAWN_CHANCE = 0.05;
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
    	/** List of nebulas for the background. */
    	private List<Nebula> nebulas;
	/** Sound button on/off object. */
	private SoundButton soundButton;
	private int commandState = 0;

    private boolean musicStarted = false;

	/** Current rotation angle of the starfield. */
    private float currentAngle;
    /** Target rotation angle of the starfield. */
    private float targetAngle;

		/** Random number generator. */
	        private Random random;
	        	/** Manages global star speed cycles. */
	                private StarSpeedManager speedManager;
	                /** Manages the oscillating origin of the starfield. */
	                private StarOriginManager originManager;	/** Manages the movement of all celestial bodies. */
	                private CelestialManager celestialManager;	/**
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
			float speed = (float) (Math.random() * 2.5 + 2.0);
			Color color = starColors.get(random.nextInt(starColors.size()));
			float z = MAX_STAR_Z - (i * (MAX_STAR_Z - MIN_STAR_Z) / NUM_STARS);
			if (z < MIN_STAR_Z) z = MIN_STAR_Z;
			
			float spread_multiplier = 1.5f;
			float initial_screen_x_offset = (random.nextFloat() - 0.5f) * (this.getWidth() * spread_multiplier);
			float initial_screen_y_offset = (random.nextFloat() - 0.5f) * (this.getHeight() * spread_multiplier);
			
			CelestialBody celestialBody = new CelestialBody(z, initial_screen_x_offset, initial_screen_y_offset, speed);
			this.stars.add(new Star(celestialBody, color));
		}
		this.backgroundEnemies = new ArrayList<Entity>();
		this.shootingStars = new ArrayList<ShootingStar>();

		this.nebulas = new ArrayList<Nebula>();
		/*
		this.nebulas = new ArrayList<Nebula>();
		// Create some nebulas using settings from NebulaSettings class
		for (int i = 0; i < NebulaSettings.NUM_NEBULAS; i++) {
			float x = random.nextFloat() * this.getWidth();
			float y = random.nextFloat() * this.getHeight();
			float z = MAX_STAR_Z + random.nextFloat() * MAX_STAR_Z; // Place them behind the stars
			float size = NebulaSettings.MIN_NEBULA_SIZE + random.nextFloat() * (NebulaSettings.MAX_NEBULA_SIZE - NebulaSettings.MIN_NEBULA_SIZE);
			Color color = NebulaSettings.NEBULA_COLORS[random.nextInt(NebulaSettings.NEBULA_COLORS.length)];
			float speed = NebulaSettings.MIN_NEBULA_SPEED + random.nextFloat() * (NebulaSettings.MAX_NEBULA_SPEED - NebulaSettings.MIN_NEBULA_SPEED);
			this.nebulas.add(new Nebula(x, y, z, size, color, speed));
		}
		*/

		// Initialize rotation angles
		this.currentAngle = 0;
		this.targetAngle = 0;
		this.speedManager = new StarSpeedManager();
		this.originManager = new StarOriginManager(width, height);
		this.celestialManager = new CelestialManager();
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

		// Update managers and get global values once per frame
		originManager.updateOrigin();
		float globalSpeedMultiplier = speedManager.updateAndGetGlobalSpeedMultiplier();

		// Animate stars
		for (Star star : this.stars) {
			celestialManager.update(star.getCelestialBody(), speedManager, originManager, this.getWidth(), this.getHeight(), globalSpeedMultiplier);
			
			// Update brightness for twinkling effect (this is star-specific)
			star.brightness = 0.5f + (float) (Math.sin(star.brightnessOffset + System.currentTimeMillis() / 500.0) + 1.0) / 4.0f;
		}

		// Animate background enemies
		for (Entity entity : this.backgroundEnemies) {
			BackgroundEnemy enemy = (BackgroundEnemy) entity;
			celestialManager.update(enemy.getCelestialBody(), speedManager, originManager, this.getWidth(), this.getHeight(), globalSpeedMultiplier);
			
			// Update enemy entity position for the renderer
			CelestialBody body = enemy.getCelestialBody();
			enemy.setPositionX((int)body.current_screen_x);
			enemy.setPositionY((int)body.current_screen_y);
		}

		/* // Animate nebulas
		for (Nebula nebula : this.nebulas) {
			nebula.z -= nebula.speed;
			// Reset nebula if it gets too close
			if (nebula.z < -200) { // A bit arbitrary, lets them pass the screen
				nebula.z = MAX_STAR_Z + random.nextFloat() * MAX_STAR_Z;
				nebula.x = random.nextFloat() * this.getWidth();
				nebula.y = random.nextFloat() * this.getHeight();
			}
		}
		*/

		// Spawn and move background enemies
		if (this.enemySpawnCooldown.checkFinished()) {
			this.enemySpawnCooldown.reset();
			if (Math.random() < ENEMY_SPAWN_CHANCE) {
				SpriteType[] enemyTypes = { SpriteType.EnemyShipA1, SpriteType.EnemyShipB1, SpriteType.EnemyShipC1 };
				SpriteType randomEnemyType = enemyTypes[random.nextInt(enemyTypes.length)];
				
				// Spawn enemies like stars
				float speed = (float) (Math.random() * 2.5 + 2.0);
				float z = MAX_STAR_Z;
				float spread_multiplier = 1.5f;
				float initial_screen_x_offset = (random.nextFloat() - 0.5f) * (this.getWidth() * spread_multiplier);
				float initial_screen_y_offset = (random.nextFloat() - 0.5f) * (this.getHeight() * spread_multiplier);

				CelestialBody celestialBody = new CelestialBody(z, initial_screen_x_offset, initial_screen_y_offset, speed);
				this.backgroundEnemies.add(new BackgroundEnemy(celestialBody, randomEnemyType));
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
            
            
                                    if (inputManager.isKeyDown(KeyEvent.VK_L)) {
                                        this.returnCode = 9; // New return code for LoginScreen
                                        this.isRunning = false;
                                        this.selectionCooldown.reset();
                                    } else if (inputManager.isKeyDown(KeyEvent.VK_O) && AuthManager.getInstance().isLoggedIn()) {
                                        AuthManager.getInstance().logout();
                                        this.selectionCooldown.reset();
                                    }
                        
                        			if (inputManager.isKeyDown(KeyEvent.VK_UP)
                        					|| inputManager.isKeyDown(KeyEvent.VK_W)) {                if (this.commandState == 0 || this.commandState == 1) {
                    this.commandState++;
                } else {
                    
                    this.commandState = 0;
                }
                previousMenuItem();
                this.selectionCooldown.reset();
            }
            
            else if (inputManager.isKeyDown(KeyEvent.VK_DOWN)
                    || inputManager.isKeyDown(KeyEvent.VK_S)) {
               
                if (this.commandState == 2 || this.commandState == 3) {
                    this.commandState++;
                } else {
                    this.commandState = 0;
                }
                nextMenuItem();
                this.selectionCooldown.reset();
            }
          
            else if (inputManager.isKeyDown(KeyEvent.VK_LEFT)
                    || inputManager.isKeyDown(KeyEvent.VK_A)) {
              
                if (this.commandState == 4 || this.commandState == 6) {
                    this.commandState++;
                } else {
                    this.commandState = 0;
                }
                if (this.returnCode == 5) { 
                    this.returnCode = 4;
                    this.soundButton.setColor(Color.WHITE);
                }
                this.selectionCooldown.reset();
            }
    
            else if (inputManager.isKeyDown(KeyEvent.VK_RIGHT)
                    || inputManager.isKeyDown(KeyEvent.VK_D)) {
               
                if (this.commandState == 5) {
                    this.commandState++;
                } 
              
                else if (this.commandState == 7) {
                    this.returnCode = 100;
                    this.isRunning = false;
                } else {
                    this.commandState = 0;
                }

                if (this.isRunning) { 
                    this.returnCode = 5; 
                    this.soundButton.setColor(Color.GREEN);
                    this.selectionCooldown.reset();
                }
            }
           
            else if (inputManager.isKeyDown(KeyEvent.VK_SPACE)) {
               
                this.commandState = 0;
                
                if (this.returnCode == 7) { // Web Dashboard
                    try {
                        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                            Desktop.getDesktop().browse(new URI("http://localhost:7070"));
                        }
                    } catch (IOException | URISyntaxException e) {
                        e.printStackTrace();
                    }
                    this.selectionCooldown.reset();
                } else if (this.returnCode != 5) {
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
			this.returnCode = 7; // Shop -> Web Dashboard
		else if (this.returnCode == 7)
			this.returnCode = 0; // Web Dashboard -> Exit
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
			this.returnCode = 7; // Exit -> Web Dashboard
		else if (this.returnCode == 7)
			this.returnCode = 4; // Web Dashboard -> Shop
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

		// Draw nebulas first, so they are in the background
		// drawManager.drawNebulas(this, this.nebulas);

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

		for (Entity entity : this.backgroundEnemies) {
			BackgroundEnemy enemy = (BackgroundEnemy) entity;
			CelestialBody body = enemy.getCelestialBody();

			// Calculate scale factor, with an initial fade-in period.
			float FADE_IN_FRACTION = 0.2f; // First 20% of the path is invisible
			float scale_factor = (1.0f - body.z / TitleScreen.MAX_STAR_Z);
			scale_factor = (scale_factor - FADE_IN_FRACTION) / (1.0f - FADE_IN_FRACTION);

			// Get un-rotated position from the entity (which was set in update)
			float relX = entity.getPositionX() - centerX;
            float relY = entity.getPositionY() - centerY;

            double rotatedX = relX * cosAngle - relY * sinAngle;
            double rotatedY = relX * sinAngle + relY * cosAngle;

            int screenX = (int) (rotatedX + centerX);
            int screenY = (int) (rotatedY + centerY);

			// Call the new scaled drawing method
			drawManager.drawScaledEntity(enemy, screenX, screenY, scale_factor);
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
