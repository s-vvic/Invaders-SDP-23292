package screen;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.awt.Color;
import java.util.Random;
import java.util.Iterator;
import engine.Cooldown;
import engine.Core;
import engine.DrawManager.SpriteType;
import engine.CelestialBody;
import engine.StarSpeedManager;
import engine.StarOriginManager;
import engine.CelestialManager;


/**
 *  Implements the CreditScreen.
 *  screen number 8
 */
public class CreditScreen extends Screen {

    private List<Credit> creditList;


    private static final int NUM_STARS = 800;
    public static final float MAX_STAR_Z = 500.0f;
    public static final float MIN_STAR_Z = -5.0f;
    private static final int ENEMY_SPAWN_COOLDOWN = 2000;
    private static final double ENEMY_SPAWN_CHANCE = 0.05;
    private static final int SHOOTING_STAR_COOLDOWN = 3000;
    private static final double SHOOTING_STAR_SPAWN_CHANCE = 0.0;
    private Cooldown enemySpawnCooldown;
    private Cooldown shootingStarCooldown;

    private List<TitleScreen.Star> stars;
    private List<TitleScreen.BackgroundEnemy> backgroundEnemies;
    private List<TitleScreen.ShootingStar> shootingStars;

    private Random random;
    private StarSpeedManager speedManager;
    private StarOriginManager originManager;
    private CelestialManager celestialManager;

    private float scroll_Y;
    private static final int LINE_HEIGHT = 45;
    private static final int BASE_FONT_SIZE = 22;
    private static final float MAX_SCALE = 1.6f;
    private static final float SCALE_ZONE = 150.0f;
    private static final float SCROLL_SPEED = 1.5f;
    public static class Credit {
        private final int no;
        private final String teamName;
        private final String role;

        /**
         * Initializes Credit objects
         */
        public Credit(final int no, final String teamName, final String role) {
            this.no = no;
            this.teamName = teamName;
            this.role = role;
        }

        // Getter methods
        public int getNo() { return no; }
        public String getTeamName() { return teamName; }

        public String getRole() { return role; }
    }

    /**
     * constructor: Set the screen properties and load credit data.
     *
     * @param width Screen width
     * @param height Screen height
     * @param fps Frames per second
     */
    public CreditScreen(final int width, final int height, final int fps) {
        super(width, height, fps);

        this.enemySpawnCooldown = Core.getCooldown(ENEMY_SPAWN_COOLDOWN);
        this.shootingStarCooldown = Core.getCooldown(SHOOTING_STAR_COOLDOWN);
        this.enemySpawnCooldown.reset();
        this.shootingStarCooldown.reset();

        this.random = new Random();
        this.stars = new ArrayList<TitleScreen.Star>();
        List<Color> starColors = java.util.Arrays.asList(
                Color.WHITE,
                new Color(173, 216, 230),
                new Color(255, 255, 153),
                new Color(255, 182, 193),
                new Color(204, 204, 255)
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
            this.stars.add(new TitleScreen.Star(celestialBody, color));
        }
        this.backgroundEnemies = new ArrayList<TitleScreen.BackgroundEnemy>();
        this.shootingStars = new ArrayList<TitleScreen.ShootingStar>();

        this.speedManager = new StarSpeedManager();
        this.originManager = new StarOriginManager(width, height);
        this.celestialManager = new CelestialManager();

        // When the screen closes, it returns to the main menu 1.
        this.returnCode = 1;
        this.creditList = new ArrayList<>();
        loadCredits();

        this.scroll_Y = this.getHeight() + LINE_HEIGHT;
    }

    /**
     * credit information
     */
    private void loadCredits() {
        creditList.add(new Credit(0, "Instructors", "Instruct students"));
        creditList.add(new Credit(1, "Mix&Match", "Currency System"));
        creditList.add(new Credit(2, "C# Only", "Level Design"));
        creditList.add(new Credit(3, "SoundSept", "Sound Effects/BGM"));
        creditList.add(new Credit(4, "MainStream", "Main Menu"));
        creditList.add(new Credit(5, "space_bar", "Records & Achievements System"));
        creditList.add(new Credit(6, "temp", "Ship Variety"));
        creditList.add(new Credit(7, "Team8", "Gameplay HUD"));
        creditList.add(new Credit(8, "KWAK", "Item System"));
        creditList.add(new Credit(9, "ten", "Two-player Mode"));
        creditList.add(new Credit(10, "IET", "Visual effects system"));
    }

    /**
     * Starts the main loop for the screen.
     *
     * @return Code to switch to the next screen.
     */
    public final int run() {
        super.run();
        return this.returnCode;
    }

    /**
     * Every frame, we update screen elements and check for events.
     */
    @Override
    protected final void update() {
        super.update();

        originManager.updateOrigin();
        float globalSpeedMultiplier = speedManager.updateAndGetGlobalSpeedMultiplier();

        for (TitleScreen.Star star : this.stars) {
            celestialManager.update(star.getCelestialBody(), speedManager, originManager, this.getWidth(), this.getHeight(), globalSpeedMultiplier);

            star.brightness = 0.5f + (float) (Math.sin(star.brightnessOffset + System.currentTimeMillis() / 500.0) + 1.0) / 4.0f;
        }

        for (TitleScreen.BackgroundEnemy enemy : this.backgroundEnemies) {
            celestialManager.update(enemy.getCelestialBody(), speedManager, originManager, this.getWidth(), this.getHeight(), globalSpeedMultiplier);

            CelestialBody body = enemy.getCelestialBody();
            enemy.setPositionX((int)body.current_screen_x);
            enemy.setPositionY((int)body.current_screen_y);
        }

        if (this.enemySpawnCooldown.checkFinished()) {
            this.enemySpawnCooldown.reset();
            if (Math.random() < ENEMY_SPAWN_CHANCE) {
                SpriteType[] enemyTypes = { SpriteType.EnemyShipA1, SpriteType.EnemyShipB1, SpriteType.EnemyShipC1 };
                SpriteType randomEnemyType = enemyTypes[random.nextInt(enemyTypes.length)];

                float speed = (float) (Math.random() * 2.5 + 2.0);
                float z = MAX_STAR_Z;
                float spread_multiplier = 1.5f;
                float initial_screen_x_offset = (random.nextFloat() - 0.5f) * (this.getWidth() * spread_multiplier);
                float initial_screen_y_offset = (random.nextFloat() - 0.5f) * (this.getHeight() * spread_multiplier);

                CelestialBody celestialBody = new CelestialBody(z, initial_screen_x_offset, initial_screen_y_offset, speed);
                this.backgroundEnemies.add(new TitleScreen.BackgroundEnemy(celestialBody, randomEnemyType));
            }
        }

        if (this.shootingStarCooldown.checkFinished()) {
            this.shootingStarCooldown.reset();
            if (Math.random() < SHOOTING_STAR_SPAWN_CHANCE) {
                float speedX = (float) (Math.random() * 7 + 5) * (Math.random() > 0.5 ? 1 : -1);
                float speedY = (float) (Math.random() * 7 + 5) * (Math.random() > 0.5 ? 1 : -1);
                this.shootingStars.add(new TitleScreen.ShootingStar(random.nextInt(this.getWidth()), -10, speedX, speedY));
            }
        }

        Iterator<TitleScreen.ShootingStar> shootingStarIterator = this.shootingStars.iterator();
        while (shootingStarIterator.hasNext()) {
            TitleScreen.ShootingStar shootingStar = shootingStarIterator.next();
            shootingStar.x += shootingStar.speedX;
            shootingStar.y += shootingStar.speedY;
            if (shootingStar.x < -20 || shootingStar.x > this.getWidth() + 20 ||
                    shootingStar.y < -20 || shootingStar.y > this.getHeight() + 20) {
                shootingStarIterator.remove();
            }
        }
        this.scroll_Y -= SCROLL_SPEED;

        float lastCredit_Y = this.scroll_Y + (this.creditList.size() * LINE_HEIGHT);

        if (lastCredit_Y < -LINE_HEIGHT) {
            this.scroll_Y = this.getHeight() + LINE_HEIGHT;
        }
        draw();

        if (inputManager.isKeyDown(KeyEvent.VK_SPACE) && this.inputDelay.checkFinished()) {
            this.isRunning = false;
        }
    }

    private void draw() {
        drawManager.initDrawing(this);

        drawManager.drawStars(this, this.stars, 0);
        drawManager.drawShootingStars(this, this.shootingStars, 0);

        for (TitleScreen.BackgroundEnemy enemy : this.backgroundEnemies) {
            CelestialBody body = enemy.getCelestialBody();

            float FADE_IN_FRACTION = 0.2f;
            float scale_factor = (1.0f - body.z / MAX_STAR_Z);
            scale_factor = (scale_factor - FADE_IN_FRACTION) / (1.0f - FADE_IN_FRACTION);

            int screenX = (int) enemy.getPositionX();
            int screenY = (int) enemy.getPositionY();

            drawManager.drawScaledEntity(enemy, screenX, screenY, scale_factor);
        }
        drawManager.drawCreditsMenu(this);
        int screenCenterY = this.getHeight() / 2;
        int screenCenterX = this.getWidth() / 2;

        int topSafeZoneY = (this.getHeight() / 5) + 30;

        for (int i = 0; i < this.creditList.size(); i++) {
            Credit credit = this.creditList.get(i);
            String text = credit.getTeamName() + " - " + credit.getRole();

            float current_Y = this.scroll_Y + (i * LINE_HEIGHT);

            if (current_Y > topSafeZoneY && current_Y < this.getHeight() + LINE_HEIGHT) {

                float distanceToCenter = Math.abs(current_Y - screenCenterY);
                float scale = 1.0f;

                if (distanceToCenter < SCALE_ZONE) {
                    scale = 1.0f + (1.0f - (distanceToCenter / SCALE_ZONE)) * (MAX_SCALE - 1.0f);
                }
                int scaledSize = (int) (BASE_FONT_SIZE * scale);

                int textWidth = drawManager.getTextWidth(text, scaledSize);
                int x_pos = screenCenterX - (textWidth / 2);

                drawManager.drawText(text, x_pos, (int) current_Y, Color.YELLOW, scaledSize);
            }
        }

        drawManager.completeDrawing(this);
    }
}