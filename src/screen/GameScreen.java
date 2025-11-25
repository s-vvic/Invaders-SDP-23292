package screen;

import java.awt.Color;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import engine.Cooldown;
import engine.Core;
import engine.GameState;
import engine.GameTimer;
import engine.AchievementManager;
import engine.InputManager;
import engine.ItemHUDManager;
import engine.AuthManager;
import engine.ApiClient;
import entity.*;
import engine.level.Level;
import engine.level.LevelManager;
import engine.level.LevelEnemyFormation;


/**
 * Implements the game screen, where the action happens.
 *
 * @author <a href="mailto:RobertoIA1987@gmail.com">Roberto Izquierdo Amo</a>
 *
 */
public class GameScreen extends Screen {
    private boolean isPaused = false;
    private boolean escPressedLastFrame = false;

    /**
     * Milliseconds until the screen accepts user input.
     */
    private static final int INPUT_DELAY = 6000;
    /**
     * Bonus score for each life remaining at the end of the level.
     */
    private static final int LIFE_SCORE = 100;
    /**
     * Minimum time between bonus ship's appearances.
     */
    private static final int BONUS_SHIP_INTERVAL = 20000;
    /**
     * Maximum variance in the time between bonus ship's appearances.
     */
    private static final int BONUS_SHIP_VARIANCE = 10000;
    /**
     * Time until bonus ship explosion disappears.
     */
    private static final int BONUS_SHIP_EXPLOSION = 500;
    /**
     * Time until bonus ship explosion disappears.
     */
    private static final int BOSS_EXPLOSION = 600;
    /**
     * Time from finishing the level to screen change.
     */
    private static final int SCREEN_CHANGE_INTERVAL = 1500;
    /**
     * Height of the interface separation line.
     */
    private static final int SEPARATION_LINE_HEIGHT = 68;
    /**
     * Height of the items separation line (above items).
     */

    private static final int ITEMS_SEPARATION_LINE_HEIGHT = 600;

    private static final int TAKE_LASER_DAMAGE_TIME = 3000;


    /**
     * Current level data (direct from Level system).
     */
    private Level currentLevel;
    /**
     * Current difficulty level number.
     */
    private int level;
    /**
     * Formation of enemy ships.
     */
    private List<EnemyShipFormation> enemyFormations;
    /**
     * Formation of special enemy ships.
     */
    private EnemyShipSpecialFormation enemyShipSpecialFormation;
    /**
     * Player's ship.
     */
    private Ship ship;
    /**
     * team drawing may implement
     */
    private FinalBoss finalBoss;
    /**
     * Time until Boss explosion disappears.
     */
    private Cooldown bossExplosionCooldown;
    /**
     * Time until the player can take damage again.
     */
    private Cooldown takeLaserDamageCooldown;
    /**
     * Time from finishing the level to screen change.
     */

    private EnemyShipChaserFormation chaserFormation;

    private Cooldown screenFinishedCooldown;
    /**
     * OmegaBoss
     */
    private MidBoss omegaBoss;
    /**
     * Set of all bullets fired by on-screen ships.
     */
    private Set<Bullet> bullets;
    /**
     * Set of all dropItems dropped by on screen ships.
     */
    private Set<DropItem> dropItems;
    /**
     * Current score.
     */
    private int score;
    /**
     * current level parameter
     */
    public Level currentlevel;
    /**
     * Player lives left.
     */
    private int lives;
    /**
     * Total bullets shot by the player.
     */
    private int bulletsShot;
    /**
     * Total ships destroyed by the player.
     */
    private int shipsDestroyed;
    /**
     * Moment the game starts.
     */
    private long gameStartTime;
    /**
     * Checks if the level is finished.
     */
    private boolean levelFinished;
    /**
     * Checks if a bonus life is received.
     */
    private boolean bonusLife;
    /**
     * Maximum number of lives.
     */
    private int maxLives;
    /**
     * Current coin.
     */
    private int coin;

    private void addPoints(final int points) {
        this.score += points;
    }

    /**
     * bossBullets carry bullets which Boss fires
     */
    private Set<BossBullet> bossBullets;
    /**
     * bossLasers carry lasers which Boss fires
     */
    private Set<BossLaser> bossLasers;
    /**
     * Is the bullet on the screen erased
     */
    private boolean is_cleared = false;
    /**
     * Timer to track elapsed time.
     */
    private GameTimer gameTimer;
    /**
     * Elapsed time since the game started.
     */
    private long elapsedTime;
    // Achievement popup
    private String achievementText;
    private Cooldown achievementPopupCooldown;

    private enum StagePhase {wave, boss_wave}

    ;
    private StagePhase currentPhase;
    /**
     * Health change popup.
     */
    private String healthPopupText;
    private Cooldown healthPopupCooldown;

    /**
     * Constructor, establishes the properties of the screen.
     *
     * @param gameState Current game state.	 * @param level
     *                  Current level settings.
     * @param bonusLife Checks if a bonus life is awarded this level.
     * @param maxLives  Maximum number of lives.
     * @param width     Screen width.
     * @param height    Screen height.
     * @param fps       Frames per second, frame rate at which the game is run.
     */
    public GameScreen(final GameState gameState,
                      final Level level, final boolean bonusLife, final int maxLives,
                      final int width, final int height, final int fps) {
        super(width, height, fps);

        this.currentLevel = level;
        this.bonusLife = bonusLife;
        this.currentlevel = level;
        this.maxLives = maxLives;
        this.level = gameState.getLevel();
        this.score = gameState.getScore();
        this.coin = gameState.getCoin();
        this.lives = gameState.getLivesRemaining();
        if (this.bonusLife)
            this.lives++;
        this.bulletsShot = gameState.getBulletsShot();
        this.shipsDestroyed = gameState.getShipsDestroyed();
    }

    /**
     * Initializes basic screen properties, and adds necessary elements.
     */
    public final void initialize() {
        super.initialize();
        /** Initialize the bullet Boss fired */
        this.bossBullets = new HashSet<>();
        this.enemyFormations = new ArrayList<>();
        this.bossLasers = new HashSet<>();

        String formationType = "A"; // 1. 기본값을 "A"로 먼저 설정합니다.
        LevelEnemyFormation formationInfo = this.currentLevel.getEnemyFormation();

        if (formationInfo != null) {
            String typeFromFile = formationInfo.getFormationType();

            if (typeFromFile != null) {
                formationType = typeFromFile;
            }
        }
        switch (formationType) {
            case "B":
                this.logger.info("Spawning Formation Type B (2 groups)");
                EnemyShipFormation formation1 = new EnemyShipFormation(this.currentLevel, 0, 0, EnemyShipFormation.Direction.DOWN_RIGHT);
                formation1.attach(this);
                formation1.applyEnemyColorByLevel(this.currentLevel);
                this.enemyFormations.add(formation1);


                EnemyShipFormation formation2 = new EnemyShipFormation(this.currentLevel, (2 * this.width / 3), 0, EnemyShipFormation.Direction.DOWN_LEFT);
                formation2.attach(this);
                formation2.applyEnemyColorByLevel(this.currentLevel);
                this.enemyFormations.add(formation2);
                break;

            case "A":
            default:
                this.logger.info("Spawning Formation Type A (1 group)");
                EnemyShipFormation formation = new EnemyShipFormation(this.currentLevel, 0, 0, EnemyShipFormation.Direction.DOWN_RIGHT);
                formation.attach(this);
                formation.applyEnemyColorByLevel(this.currentLevel);
                this.enemyFormations.add(formation);
                break;
        }


        this.ship = new Ship(this.width / 2, ITEMS_SEPARATION_LINE_HEIGHT - 75, Color.green);

        // special enemy initial
        enemyShipSpecialFormation = new EnemyShipSpecialFormation(this.currentLevel,
                Core.getVariableCooldown(BONUS_SHIP_INTERVAL, BONUS_SHIP_VARIANCE),
                Core.getCooldown(BONUS_SHIP_EXPLOSION));
        enemyShipSpecialFormation.attach(this);
        this.bossExplosionCooldown = Core
                .getCooldown(BOSS_EXPLOSION);
        this.takeLaserDamageCooldown = Core
                .getCooldown(TAKE_LASER_DAMAGE_TIME);
        this.screenFinishedCooldown = Core.getCooldown(SCREEN_CHANGE_INTERVAL);
        this.bullets = new HashSet<Bullet>();
        this.dropItems = new HashSet<DropItem>();

        this.chaserFormation = new EnemyShipChaserFormation(this.currentLevel, this.width, this.ship);
        this.chaserFormation.attach(this);

        // Special input delay / countdown.
        this.gameStartTime = System.currentTimeMillis();
        this.inputDelay = Core.getCooldown(INPUT_DELAY);
        this.inputDelay.reset();


        this.gameTimer = new GameTimer();
        this.elapsedTime = 0;
        this.finalBoss = null;
        this.omegaBoss = null;
        this.currentPhase = StagePhase.wave;
    }

    /**
     * Starts the action.
     *
     * @return Next screen code.
     */
    public final int run() {
        super.run();

        this.score += LIFE_SCORE * (this.lives - 1);
        this.logger.info("Screen cleared with a score of " + this.score);

        return this.returnCode;
    }

    /**
     * Updates the elements on screen and checks for events.
     */
    @Override
    protected final void update() {

        // =====================================================
        // ★ Pause 상태에서 Q 눌러 returnCode = 1 이 되면
        //   게임 로직이 절대로 더 실행되면 안됨
        //   → run() 종료 → Core.java로 returnCode=1 전달 → TitleScreen 이동
        // =====================================================
        if (this.returnCode == 1) {
            this.isRunning = false;   // run() 루프 종료
            return;                   // update 중단
        }

        super.update();

        // =====================================================
        // ESC / Pause / Q 처리 (이 안에서 returnCode=1이 될 수 있음)
        // =====================================================
        handleInput();

        // =====================================================
        // ★ Pause 화면이면 게임 업데이트 중단 + Pause UI만 렌더링
        // =====================================================
        if (isPaused) {
            drawPausePopup();
            return;
        }

        // =====================================================
        // 게임 시작 전 카운트다운
        // =====================================================
        if (this.inputDelay.checkFinished() && !this.levelFinished) {

            // 타이머 시작
            if (!this.gameTimer.isRunning()) {
                this.gameTimer.start();
            }

            // =================================================
            // ★ 실제 게임 진행 로직 (적 이동, 총알 업데이트, 보스 패턴 등)
            // =================================================
            updateGameLogic();
        }

        // =====================================================
        // 경과 시간 업데이트
        // =====================================================
        if (this.gameTimer.isRunning()) {
            this.elapsedTime = this.gameTimer.getElapsedTime();
            AchievementManager.getInstance()
                    .onTimeElapsedSeconds((int)(elapsedTime / 1000));
        }

        // =====================================================
        // 총알 / 아이템 정리 + 충돌 처리
        // =====================================================
        cleanItems();
        manageCollisions();
        ItemHUDManager.getInstance().update(inputManager.getMouseX(), inputManager.getMouseY());
        cleanBullets();

        // =====================================================
        // 그리기
        // =====================================================
        draw();

        // =====================================================
        // 레벨 종료 / 게임오버 처리
        // (여기서 returnCode가 변경될 수 있음)
        // =====================================================
        checkGameStatus();
    }



    /**
     * Draws the elements associated with the screen.
     */
    private void draw() {
        drawManager.initDrawing(this);

        if (this.lives > 0) {
            drawManager.drawEntity(this.ship, this.ship.getPositionX(),
                    this.ship.getPositionY());
        }

        // special enemy draw
        enemyShipSpecialFormation.draw();

        /** draw final boss at the field */
        /** draw final boss bullets */
        if (this.finalBoss != null && !this.finalBoss.isDestroyed()) {
            for (BossBullet bossBullet : bossBullets) {
                drawManager.drawEntity(bossBullet, bossBullet.getPositionX(), bossBullet.getPositionY());
            }

            for (BossLaser bossLaser : bossLasers) {
                drawManager.drawEntity(bossLaser, bossLaser.getPositionX(), bossLaser.getPositionY());
            }

            drawManager.drawEntity(finalBoss, finalBoss.getPositionX(), finalBoss.getPositionY());
        }

        for (EnemyShipFormation formation : this.enemyFormations) {
            formation.draw();
        }
        chaserFormation.draw();

        if (this.omegaBoss != null) {
            this.omegaBoss.draw(drawManager);
        }

        for (Bullet bullet : this.bullets)
            drawManager.drawEntity(bullet, bullet.getPositionX(),
                    bullet.getPositionY());

        for (DropItem dropItem : this.dropItems)
            drawManager.drawEntity(dropItem, dropItem.getPositionX(), dropItem.getPositionY());

        // Interface.
        drawManager.drawScore(this, this.score);
        drawManager.drawCoin(this, this.coin);
        drawManager.drawLives(this, this.lives);
        drawManager.drawTime(this, this.elapsedTime);
        drawManager.drawItemsHUD(this);
        drawManager.drawLevel(this, this.currentLevel.getLevelName());
        drawManager.drawHorizontalLine(this, SEPARATION_LINE_HEIGHT - 1);
        drawManager.drawHorizontalLine(this, ITEMS_SEPARATION_LINE_HEIGHT);

        if (this.achievementText != null && !this.achievementPopupCooldown.checkFinished()) {
            drawManager.drawAchievementPopup(this, this.achievementText);
        } else {
            this.achievementText = null; // clear once expired
        }

        // Health notification popup
        if (this.healthPopupText != null && !this.healthPopupCooldown.checkFinished()) {
            drawManager.drawHealthPopup(this, this.healthPopupText);
        } else {
            this.healthPopupText = null;
        }

        // Countdown to game start.
        if (!this.inputDelay.checkFinished()) {
            int countdown = (int) ((INPUT_DELAY
                    - (System.currentTimeMillis()
                    - this.gameStartTime)) / 1000);
            drawManager.drawCountDown(this, this.level, countdown,
                    this.bonusLife);
            drawManager.drawHorizontalLine(this, this.height / 2 - this.height
                    / 12);
            drawManager.drawHorizontalLine(this, this.height / 2 + this.height
                    / 12);
        }

        drawManager.drawSystemMessages(this);
        drawManager.completeDrawing(this);
    }


    /**
     * Cleans bullets that go off screen.
     */
    private void cleanBullets() {
        Set<Bullet> recyclable = new HashSet<Bullet>();
        for (Bullet bullet : this.bullets) {
            bullet.update();
            if (bullet.getPositionY() < SEPARATION_LINE_HEIGHT
                    || bullet.getPositionY() > this.height)
                recyclable.add(bullet);
        }
        this.bullets.removeAll(recyclable);
        BulletPool.recycle(recyclable);
    }

    /**
     * Cleans Items that go off screen.
     */

    private void cleanItems() {
        Set<DropItem> recyclable = new HashSet<DropItem>();
        for (DropItem dropItem : this.dropItems) {
            dropItem.update();
            if (dropItem.getPositionY() < SEPARATION_LINE_HEIGHT
                    || dropItem.getPositionY() > this.height)
                recyclable.add(dropItem);
        }
        this.dropItems.removeAll(recyclable);
        ItemPool.recycle(recyclable);
    }

    /**
     * Manages collisions between bullets and ships.
     */
    private void manageBulletShipCollisions() {
        Set<Bullet> recyclable = new HashSet<Bullet>();
        for (Bullet bullet : this.bullets) {
            if (bullet.getSpeed() > 0) {
                handleEnemyBulletCollision(bullet, recyclable);
            } else {
                handlePlayerBulletCollision(bullet, recyclable);
            }
        }
        this.bullets.removeAll(recyclable);
        BulletPool.recycle(recyclable);
    }

    /**
     * Handles collisions between enemy bullets and player ships.
     *
     * @param bullet The enemy bullet to check.
     * @param recyclable A set to add the bullet to if it should be recycled.
     */
    private void handleEnemyBulletCollision(Bullet bullet, Set<Bullet> recyclable) {
        if (this.lives > 0 && checkCollision(bullet, this.ship) && !this.levelFinished) {
            recyclable.add(bullet);
            if (!this.ship.isInvincible() && !GameState.isInvincible()) {
                if (!this.ship.isDestroyed()) {
                    this.ship.destroy();
                    this.lives--;
                    showHealthPopup("-1 Health");
                    this.logger.info("Hit on player ship, " + this.lives + " lives remaining.");
                }
            }
        }
    }

    /**
     * Handles collisions between player bullets and enemy ships.
     * This method coordinates collision checks with different enemy types.
     *
     * @param bullet     The player bullet to check.
     * @param recyclable A set to add the bullet to if it should be recycled.
     */
    private void handlePlayerBulletCollision(Bullet bullet, Set<Bullet> recyclable) {
        boolean consumed = checkCollisionWithNormalEnemies(bullet, recyclable);
        if (consumed) {
            return;
        }
        checkCollisionWithSpecialEnemiesAndBosses(bullet, recyclable);
    }

    /**
     * Checks and handles collisions between a player bullet and special enemies or bosses.
     * Processes damage, awards points, and handles destruction for special enemies, Omega Boss, and Final Boss.
     *
     * @param bullet     The player bullet.
     * @param recyclable A set to add the bullet to if it is consumed.
     */
    private void checkCollisionWithSpecialEnemiesAndBosses(Bullet bullet, Set<Bullet> recyclable) {
        // special enemy bullet event
        for (EnemyShip enemyShipSpecial : this.enemyShipSpecialFormation) {
            if (enemyShipSpecial != null && !enemyShipSpecial.isDestroyed()
                    && checkCollision(bullet, enemyShipSpecial)) {
                int pts = enemyShipSpecial.getPointValue();
                addPoints(pts);
                this.coin += (pts / 10);
                this.shipsDestroyed++;
                this.enemyShipSpecialFormation.destroy(enemyShipSpecial);
                recyclable.add(bullet);
            }
        }
        if (this.omegaBoss != null
                && !this.omegaBoss.isDestroyed()
                && checkCollision(bullet, this.omegaBoss)) {
            this.omegaBoss.takeDamage(2);
            if (this.omegaBoss.getHealPoint() <= 0) {
                this.shipsDestroyed++;
                int pts = this.omegaBoss.getPointValue();
                addPoints(pts);
                this.coin += (pts / 10);
                this.omegaBoss.destroy();
                AchievementManager.getInstance().unlockAchievement("Boss Slayer");
                this.bossExplosionCooldown.reset();
            }
            recyclable.add(bullet);
        }

        /** when final boss collide with bullet */
        if (this.finalBoss != null && !this.finalBoss.isDestroyed() && checkCollision(bullet, this.finalBoss)) {
            this.finalBoss.takeDamage(1);
            if (this.finalBoss.getHealPoint() <= 0) {
                int pts = this.finalBoss.getPointValue();
                addPoints(pts);
                this.coin += (pts / 10);
                this.finalBoss.destroy();
                AchievementManager.getInstance().unlockAchievement("Boss Slayer");
            }
            recyclable.add(bullet);
        }
        for (Chaser currentChaser : this.chaserFormation) {
            if (!currentChaser.isDestroyed() && checkCollision(bullet, currentChaser)) {

                currentChaser.takeDamage(1);
                if (currentChaser.isDestroyed()) {
                    int pts = currentChaser.getPointValue();
                    addPoints(pts);
                    this.coin += (pts / 10);
                    this.shipsDestroyed++;
                }
                if (!bullet.penetration()) {
                    recyclable.add(bullet);
                    break;
                }
            }
        }
    }

    /**
     * Checks and handles collisions between a player bullet and normal enemy ships.
     * <p>
     * Awards points, destroys the enemy, and handles item drops upon collision.
     *
     * @param bullet     The player bullet.
     * @param recyclable A set to add the bullet to if it is consumed.
     * @return True if the bullet was consumed (hit a non-penetratable target), false otherwise.
     */
    private boolean checkCollisionWithNormalEnemies(Bullet bullet, Set<Bullet> recyclable) {
        for (EnemyShipFormation formation : this.enemyFormations) {
            for (EnemyShip enemyShip : formation) {
                if (!enemyShip.isDestroyed() && checkCollision(bullet, enemyShip)) {
                    int pts = enemyShip.getPointValue();
                    addPoints(pts);
                    this.coin += (pts / 10);
                    this.shipsDestroyed++;
                    handleItemDrop(enemyShip);
                    formation.destroy(enemyShip);
                    AchievementManager.getInstance().onEnemyDefeated();
                    if (!bullet.penetration()) {
                        recyclable.add(bullet);
                        return true; // Bullet was consumed
                    }
                }
            }
        }
        return false; // Bullet was not consumed
    }

    /**
     * Handles the logic for dropping an item when an enemy is destroyed.
     * <p>
     * Calculates drop chances and creates a new DropItem if successful.
     *
     * @param enemyShip The enemy ship that was destroyed.
     */
    private void handleItemDrop(EnemyShip enemyShip) {
        String enemyType = enemyShip.getEnemyType();
        if (enemyType != null && this.currentLevel.getItemDrops() != null) {
            List<engine.level.ItemDrop> potentialDrops = new ArrayList<>();
            for (engine.level.ItemDrop itemDrop : this.currentLevel.getItemDrops()) {
                if (enemyType.equals(itemDrop.getEnemyType())) {
                    potentialDrops.add(itemDrop);
                }
            }

            List<engine.level.ItemDrop> successfulDrops = new ArrayList<>();
            for (engine.level.ItemDrop itemDrop : potentialDrops) {
                if (Math.random() < itemDrop.getDropChance()) {
                    successfulDrops.add(itemDrop);
                }
            }

            if (!successfulDrops.isEmpty()) {
                engine.level.ItemDrop selectedDrop = successfulDrops.get((int) (Math.random() * successfulDrops.size()));
                DropItem.ItemType droppedType = DropItem.ItemType.fromString(selectedDrop.getItemId());
                if (droppedType != null) {
                    final int ITEM_DROP_SPEED = 3;
                    DropItem newDropItem = ItemPool.getItem(
                            enemyShip.getPositionX() + enemyShip.getWidth() / 2,
                            enemyShip.getPositionY() + enemyShip.getHeight() / 2,
                            ITEM_DROP_SPEED,
                            droppedType
                    );
                    this.dropItems.add(newDropItem);
                    this.logger.info("An item (" + droppedType + ") dropped");
                }
            }
        }
    }

    /**
     * Manages all collision detection within the game.
     * Calls specific methods to handle collisions between bullets and ships,
     * ships and enemies, and players and dropped items.
     */
    private void manageCollisions() {
        manageBulletShipCollisions();
        manageShipEnemyCollisions();
        manageItemCollisions();
    }

    /**
     * Checks the current game status for win or lose conditions.
     * Updates game state and prepares for screen change if the level is finished
     * or all player lives are depleted.
     */
    private void checkGameStatus() {
        if ((this.lives == 0) && !this.levelFinished) {
            this.levelFinished = true;
            this.screenFinishedCooldown.reset();
            if (this.gameTimer.isRunning()) {
                this.gameTimer.stop();
            }
        }
        if (this.levelFinished && this.screenFinishedCooldown.checkFinished()) {
            boolean isGameOver = this.lives == 0;
            boolean isFinalLevelCleared = !isGameOver && this.level == new LevelManager().getNumberOfLevels();

            if (isGameOver) { // Game Over condition
                draw(); // Draw the final frame before capturing.
                Core.lastScreenCapture = drawManager.getBackBuffer();
                this.returnCode = 99;
            } else { // Level cleared condition
                // Unlock level-specific achievements
                if (this.level == 1) {
                    AchievementManager.getInstance().unlockAchievement("Beginner");
                } else if (this.level == 3) {
                    AchievementManager.getInstance().unlockAchievement("Intermediate");
                }

                if (this.currentlevel.getCompletionBonus() != null) {
                    this.coin += this.currentlevel.getCompletionBonus().getCurrency();
                    this.logger.info("Awarded " + this.currentlevel.getCompletionBonus().getCurrency() + " coins for level completion.");
                }

                String achievement = this.currentlevel.getAchievementTrigger();
                if (achievement != null && !achievement.isEmpty()) {
                    AchievementManager.getInstance().unlockAchievement(achievement);
                    this.logger.info("Unlocked achievement: " + achievement);
                }
            }

            // Submit score to backend if logged in
            AuthManager authManager = AuthManager.getInstance();
            if (authManager.isLoggedIn()) {
                if (isGameOver || isFinalLevelCleared) {
                    // Unlock "Conqueror" achievement if the final level is cleared
                    if (isFinalLevelCleared) {
                        AchievementManager.getInstance().unlockAchievement("Conqueror");
                    }

                    try {
                        ApiClient.getInstance().saveScore(this.score);
                        this.logger.info("Score " + this.score + " submitted to backend for user " + authManager.getUserId());
                    } catch (Exception e) { // saveScore is async, but catching potential sync exceptions
                        this.logger.severe("Error submitting score to backend: " + e.getMessage());
                    }
                } else {
                    this.logger.info("Level " + this.level + " cleared. Score will be saved at the end of the game.");
                }
            } else {
                this.logger.info("User not logged in, score not submitted to backend.");
            }
            this.isRunning = false;
        }
    }

    /**
     * Manages collisions between player ship and enemy ships.
     * <p>
     * Player loses a life immediately upon collision with any enemy.
     */
    private void manageShipEnemyCollisions() {
        // =====  collision check =====
        if (!this.levelFinished && this.lives > 0 && !this.ship.isDestroyed()
                && !this.ship.isInvincible() && !GameState.isInvincible()) {
            // Check collision with normal enemy ships
            for (EnemyShipFormation formation : this.enemyFormations) {
                for (EnemyShip enemyShip : formation) {
                    if (!enemyShip.isDestroyed() && checkCollision(this.ship, enemyShip)) {
                        formation.destroy(enemyShip);
                        this.ship.destroy();
                        this.lives--;
                        showHealthPopup("-1 Life (Collision!)");
                        this.logger.info("Ship collided with enemy! " + this.lives
                                + " lives remaining.");
                        return;
                    }
                }
            }

            // Check collision with special enemy formation (red/blue ships)
            for (EnemyShip enemyShipSpecial : this.enemyShipSpecialFormation) {
                if (enemyShipSpecial != null && !enemyShipSpecial.isDestroyed()
                        && checkCollision(this.ship, enemyShipSpecial)) {
                    enemyShipSpecial.destroy();
                    this.ship.destroy();
                    this.lives--;
                    showHealthPopup("-1 Life (Collision!)");
                    this.logger.info("Ship collided with special enemy formation! "
                            + this.lives + " lives remaining.");
                    return;
                }
            }

            for (Chaser currentChaser : this.chaserFormation) {
                if (!currentChaser.isDestroyed() && checkCollision(this.ship, currentChaser)) {
                    currentChaser.destroy();
                    this.ship.destroy();
                    this.lives--;
                    showHealthPopup("-1 Life (Collision!)");
                    this.logger.info("Ship collided with Chaser! " + this.lives + " lives remaining.");
                    return;
                }
            }

            // Check collision with omega boss (mid boss - yellow/pink ship)
            if (this.omegaBoss != null && !this.omegaBoss.isDestroyed()
                    && checkCollision(this.ship, this.omegaBoss)) {
                this.ship.destroy();
                this.lives--;
                showHealthPopup("-1 Life (Boss Collision!)");
                this.logger.info("Ship collided with omega boss! " + this.lives
                        + " lives remaining.");
                return;
            }

            // Check collision with final boss
            if (this.finalBoss != null && !this.finalBoss.isDestroyed()
                    && checkCollision(this.ship, this.finalBoss)) {
                this.ship.destroy();
                this.lives--;
                showHealthPopup("-1 Life (Boss Collision!)");
                this.logger.info("Ship collided with final boss! " + this.lives
                        + " lives remaining.");
                return;
            }
        }
    }

    /**
     * Manages collisions between player ship and dropped items.
     * Applies item effects when player collects them.
     */
    private void manageItemCollisions() {
        Set<DropItem> acquiredDropItems = new HashSet<DropItem>();
        if (!this.levelFinished && (this.lives > 0 && !this.ship.isDestroyed())) {
            for (DropItem dropItem : this.dropItems) {
                if (this.lives > 0 && !this.ship.isDestroyed() && checkCollision(this.ship, dropItem)) {
                    this.logger.info("Player acquired dropItem: " + dropItem.getItemType());
                    // Add item to HUD display
                    ItemHUDManager.getInstance().addActiveItem(dropItem.getItemType());
                    ItemHUDManager.getInstance().triggerFlash(dropItem.getItemType());
                    switch (dropItem.getItemType()) {
                        case Heal:
                            gainLife();
                            break;
                        case Shield:
                            ship.activateInvincibility(5000); // 5 seconds of invincibility
                            break;
                        case Stop:
                            DropItem.applyTimeFreezeItem(3000);
                            break;
                        case Push:
                            for (EnemyShipFormation formation : this.enemyFormations) {
                                DropItem.PushbackItem(formation, 20);
                            }
                            break;
                        case Explode:
                            int destroyedEnemy = 0;
                            for (EnemyShipFormation formation : this.enemyFormations) {
                                destroyedEnemy += formation.destroyAll();
                            }
                            int pts = destroyedEnemy * 5;
                            addPoints(pts);
                            break;
                        case Slow:
                            for (EnemyShipFormation formation : this.enemyFormations) {
                                formation.activateSlowdown();
                            }
                            this.logger.info("Enemy formation slowed down!");
                            break;
                        default:
                            // For other dropItem types. Free to add!
                            break;
                    }
                    acquiredDropItems.add(dropItem);
                }
            }
            this.dropItems.removeAll(acquiredDropItems);
            ItemPool.recycle(acquiredDropItems);
        }
    }


    /**
     * Checks if two entities are colliding.
     *
     * @param a First entity, the bullet.
     * @param b Second entity, the ship.
     * @return Result of the collision test.
     */
    private boolean checkCollision(final Entity a, final Entity b) {
        // Calculate center point of the entities in both axis.
        int centerAX = a.getPositionX() + a.getWidth() / 2;
        int centerAY = a.getPositionY() + a.getHeight() / 2;
        int centerBX = b.getPositionX() + b.getWidth() / 2;
        int centerBY = b.getPositionY() + b.getHeight() / 2;
        // Calculate maximum distance without collision.
        int maxDistanceX = a.getWidth() / 2 + b.getWidth() / 2;
        int maxDistanceY = a.getHeight() / 2 + b.getHeight() / 2;
        // Calculates distance.
        int distanceX = Math.abs(centerAX - centerBX);
        int distanceY = Math.abs(centerAY - centerBY);

        return distanceX < maxDistanceX && distanceY < maxDistanceY;
    }

    /**
     * Shows an achievement popup message on the HUD.
     *
     * @param message Text to display in the popup.
     */
    public void showAchievement(String message) {
        this.achievementText = message;
        this.achievementPopupCooldown = Core.getCooldown(2500); // Show for 2.5 seconds
        this.achievementPopupCooldown.reset();
    }

    /**
     * Displays a notification popup when the player gains or loses health
     *
     * @param message Text to display in the popup
     */
    public void showHealthPopup(String message) {
        this.healthPopupText = message;
        this.healthPopupCooldown = Core.getCooldown(500);
        this.healthPopupCooldown.reset();
    }

    /**
     * Returns a GameState object representing the status of the game.
     *
     * @return Current game state.
     */
    public final GameState getGameState() {
        if (this.coin > 2000) {
            AchievementManager.getInstance().unlockAchievement("Mr. Greedy");
        }
        return new GameState(this.level, this.score, this.lives,
                this.bulletsShot, this.shipsDestroyed, this.coin);
    }

    /**
     * Adds one life to the player.
     */
    public final void gainLife() {
        if (this.lives < this.maxLives) {
            this.lives++;
        }
    }

    /**
     * @return The player's ship instance.
     */
    public Ship getShip() {
        return this.ship;
    }

    private void bossReveal() {
        String bossName = this.currentlevel.getBossId();

        if (bossName == null || bossName.isEmpty()) {
            this.logger.info("No boss for this level. Proceeding to finish.");
            return;
        }

        this.logger.info("Spawning boss: " + bossName);
        switch (bossName) {
            case "finalBoss1":
                this.finalBoss = new FinalBoss(this.width / 2 - 75, 80, this.width, this.height, this.ship, 1);
                this.logger.info("Final Boss has spawned!");
                break;
            case "finalBoss2":
                this.finalBoss = new FinalBoss(this.width / 2 - 75, 80, this.width, this.height, this.ship, 2);
                this.logger.info("Final Boss has spawned!");
                break;
            case "omegaBoss":
            case "omegaAndFinal":
                this.omegaBoss = new OmegaBoss(Color.ORANGE, ITEMS_SEPARATION_LINE_HEIGHT);
                omegaBoss.attach(this);
                this.logger.info("Omega Boss has spawned!");
                break;
            default:
                this.logger.warning("Unknown bossId: " + bossName);
                break;
        }
        this.is_cleared = false;
    }

    /**
     * Manage Final Boss's shooting
     */
    public void finalbossManage() {
        if (this.finalBoss != null && !this.finalBoss.isDestroyed()) {
            this.finalBoss.update();
            /** called the boss shoot logic */
            if (this.finalBoss.getHealPoint() > this.finalBoss.getMaxHp() * FinalBoss.PHASE_2_HP_THRESHOLD) {
                if (this.finalBoss.getDifficulty() == 1) {
                    bossBullets.addAll(this.finalBoss.shoot1());
                    bossBullets.addAll(this.finalBoss.shoot2());
                } else {
                    bossBullets.addAll(this.finalBoss.shoot3());
                }
            } else if (this.finalBoss.getHealPoint() > this.finalBoss.getMaxHp() * FinalBoss.PHASE_3_HP_THRESHOLD) {
                /**  clear bullets if shoot3() was called */
                if (this.finalBoss.getDifficulty() != 1 && !is_cleared) {
                    bossBullets.clear();
                    is_cleared = true;
                } else {
                    bossBullets.addAll(this.finalBoss.shoot1());
                    bossBullets.addAll(this.finalBoss.shoot2());
                    bossLasers.addAll(this.finalBoss.laserShoot());
                }
            } else { // dash pattern
                if (this.finalBoss.getDifficulty() != 1) {
                    bossBullets.addAll(this.finalBoss.shoot4());
                }
                bossBullets.addAll(this.finalBoss.shoot2());
            }

            /** bullets to erase */
            Set<BossBullet> bulletsToRemove = new HashSet<>();

            for (BossBullet bossBullet : bossBullets) {
                bossBullet.update();
                /** If the bullet goes off the screen */
                if (bossBullet.isOffScreen(width, height)) {
                    /** bulletsToRemove carry bullet */
                    bulletsToRemove.add(bossBullet);
                }
                /** If the bullet collides with ship */
                else if (this.lives > 0 && this.checkCollision(bossBullet, this.ship) && !GameState.isInvincible()) {
                    if (!this.ship.isDestroyed()) {
                        this.ship.destroy();
                        this.lives--;
                        this.logger.info("Hit on player ship, " + this.lives + " lives remaining.");
                    }
                    bulletsToRemove.add(bossBullet);
                }
            }

            /** lasers to erase */
            Set<BossLaser> lasersToRemove = new HashSet<>();

            for (BossLaser bossLaser : bossLasers) {
                bossLaser.update();
                /** If the laser goes off the screen */
                if (bossLaser.isRemoved()) {
                    /** lasersToRemove carry laser */
                    lasersToRemove.add(bossLaser);
                }
                /** If the laser collides with ship */
                else if (this.lives > 0 && this.checkCollision(bossLaser, this.ship) && !GameState.isInvincible()
                        && takeLaserDamageCooldown.checkFinished()) {

                    takeLaserDamageCooldown.reset();

                    if (!this.ship.isDestroyed()) {
                        this.ship.destroy();
                        this.lives--;
                        this.logger.info("Hit on player ship, " + this.lives + " lives remaining.");
                    }
                }
            }
            /** all bullets are removed */
            bossBullets.removeAll(bulletsToRemove);
            bossLasers.removeAll(lasersToRemove);
        }
        if (this.finalBoss != null && this.finalBoss.isDestroyed()) {
            this.levelFinished = true;
            this.screenFinishedCooldown.reset();
        }
    }

    /**
     * Updates the main game logic, including enemy movements, boss patterns,
     * and player ship states based on the current game phase.
     */
    private void updateGameLogic() {
        switch (this.currentPhase) {
            case wave:
                if (!DropItem.isTimeFreezeActive()) {
                    for (EnemyShipFormation formation : this.enemyFormations) {
                        formation.update();
                        formation.shoot(this.bullets);
                    }
                }


                boolean allEmpty = true;
                for (EnemyShipFormation formation : this.enemyFormations) {
                    if (!formation.isEmpty()) {
                        allEmpty = false;
                        break;
                    }
                }


                if (allEmpty) {
                    this.currentPhase = StagePhase.boss_wave;
                }
                break;

            case boss_wave:
                if (this.finalBoss == null && this.omegaBoss == null) {
                    bossReveal();
                    for (EnemyShipFormation formation : this.enemyFormations) {
                        formation.clear();
                    }
                }
                if (this.finalBoss != null) {
                    finalbossManage();
                } else if (this.omegaBoss != null) {
                    this.omegaBoss.update();
                    if (this.omegaBoss.isDestroyed()) {
                        if ("omegaAndFinal".equals(this.currentlevel.getBossId())) {
                            this.omegaBoss = null;
                            this.finalBoss = new FinalBoss(this.width / 2 - 50, 50, this.width, this.height, this.ship, 3);
                            this.logger.info("Final Boss has spawned!");
                        } else {
                            this.levelFinished = true;
                            this.screenFinishedCooldown.reset();
                        }
                    }
                } else {
                    if (!this.levelFinished) {
                        this.levelFinished = true;
                        this.screenFinishedCooldown.reset();
                    }
                }
                break;
        }
        this.ship.update();
        // special enemy update
        this.enemyShipSpecialFormation.update();
        this.chaserFormation.update(this.ship);
    }

    /**
     * Handles player input for both player 1 and player 2.
     * Processes movement and shooting based on keyboard input.
     */
    /**
     * Handles player input for both player 1 and player 2.
     * Processes movement and shooting based on keyboard input.
     */
    /**
     * Handles player input for both player 1 and player 2.
     * Processes movement and shooting based on keyboard input.
     */
    /**
     * Handles player input for pause, quit, movement, and shooting.
     */
    private void handleInput() {

        // ===== ESC 입력으로 일시정지 토글 =====
        boolean escPressed = inputManager.isKeyDown(java.awt.event.KeyEvent.VK_ESCAPE);

        if (escPressed && !escPressedLastFrame) {
            isPaused = !isPaused;
        }

        escPressedLastFrame = escPressed;

        // ============================
        //  일시정지 상태(Pause UI)
        // ============================
        if (isPaused) {

            // Q 눌러서 TitleScreen으로 이동
            boolean qPressed = inputManager.isKeyDown(java.awt.event.KeyEvent.VK_Q);

            if (qPressed) {
                this.returnCode = 1;   // ★ Core.java TitleScreen
                this.isRunning = false;
            }

            return;  // Pause 상태에서 아래 입력은 무시
        }

        // ============================
        //  게임 플레이 입력 처리
        // ============================
        if (this.lives > 0 && !this.ship.isDestroyed()) {

            boolean p1Right = inputManager.isKeyDown(java.awt.event.KeyEvent.VK_D);
            boolean p1Left  = inputManager.isKeyDown(java.awt.event.KeyEvent.VK_A);
            boolean p1Up    = inputManager.isKeyDown(java.awt.event.KeyEvent.VK_W);
            boolean p1Down  = inputManager.isKeyDown(java.awt.event.KeyEvent.VK_S);
            boolean p1Fire  = inputManager.isKeyDown(java.awt.event.KeyEvent.VK_SPACE);

            boolean isRightBorder = this.ship.getPositionX()
                    + this.ship.getWidth() + this.ship.getSpeed() > this.width - 1;

            boolean isLeftBorder = this.ship.getPositionX()
                    - this.ship.getSpeed() < 1;

            boolean isUpBorder = this.ship.getPositionY()
                    - this.ship.getSpeed() < SEPARATION_LINE_HEIGHT;

            boolean isDownBorder = this.ship.getPositionY()
                    + this.ship.getHeight() + this.ship.getSpeed() > ITEMS_SEPARATION_LINE_HEIGHT;

            if (p1Right && !isRightBorder) this.ship.moveRight();
            if (p1Left  && !isLeftBorder ) this.ship.moveLeft();
            if (p1Up    && !isUpBorder   ) this.ship.moveUp();
            if (p1Down  && !isDownBorder ) this.ship.moveDown();

            if (p1Fire) {
                if (this.ship.shoot(this.bullets)) {
                    this.bulletsShot++;
                    AchievementManager.getInstance().onShotFired();
                }
            }
        }
    }




    private void drawPausePopup() {
        drawManager.initDrawing(this);

        drawManager.drawFilledRectangle(
                this.width / 2 - 200,
                this.height / 2 - 150,
                400,
                300,
                new Color(0, 0, 0, 150)
        );

        drawManager.drawCenteredText(this, "PAUSED", this.width / 2, this.height / 2 - 60, 32, Color.WHITE);
        drawManager.drawCenteredText(this, "Press ESC to Resume", this.width / 2, this.height / 2, 24, Color.WHITE);
        drawManager.drawCenteredText(this, "Press Q to Quit to Menu", this.width / 2, this.height / 2 + 50, 24, Color.WHITE);

        drawManager.completeDrawing(this);
    }
}