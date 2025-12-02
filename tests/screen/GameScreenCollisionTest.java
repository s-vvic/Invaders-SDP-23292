package screen;

import engine.Cooldown;
import engine.GameState;
import engine.level.Level;
import entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.awt.Color;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import engine.DrawManager.SpriteType;

class GameScreenCollisionTest {

    private GameState gameState;
    @Mock private Level mockLevel;

    private static final int WIDTH = 448;
    private static final int HEIGHT = 520;
    private static final int FPS = 60;
    private static final int INITIAL_LIVES = 3;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        when(mockLevel.getFormationWidth()).thenReturn(1);
        when(mockLevel.getFormationHeight()).thenReturn(1);
        when(mockLevel.getBaseSpeed()).thenReturn(1);
        when(mockLevel.getShootingFrecuency()).thenReturn(1);
        when(mockLevel.getEnemyFormation()).thenReturn(null);
        when(mockLevel.getItemDrops()).thenReturn(new ArrayList<>());
        when(mockLevel.getLevelName()).thenReturn("Test Level");

        gameState = new GameState(1, 0, INITIAL_LIVES, 0, 0, 0);
    }

    @Test
    @DisplayName("Collision with Regular Enemy A")
    void testCollisionWithEnemyA() throws Exception {
        EnemyShip enemy = new EnemyShip(0, 0, SpriteType.EnemyShipA1);
        performCollisionTest(enemy);
    }

    @Test
    @DisplayName("Collision with Regular Enemy B")
    void testCollisionWithEnemyB() throws Exception {
        EnemyShip enemy = new EnemyShip(0, 0, SpriteType.EnemyShipB1);
        performCollisionTest(enemy);
    }

    @Test
    @DisplayName("Collision with Regular Enemy C")
    void testCollisionWithEnemyC() throws Exception {
        EnemyShip enemy = new EnemyShip(0, 0, SpriteType.EnemyShipC1);
        performCollisionTest(enemy);
    }

    @Test
    @DisplayName("Collision with Special Enemy")
    void testCollisionWithSpecialEnemy() throws Exception {
        EnemyShip enemy = new EnemyShip(Color.PINK, EnemyShip.Direction.RIGHT, 1);
        performCollisionTest(enemy);
    }

    @Test
    @DisplayName("Collision with Chaser")
    void testCollisionWithChaser() throws Exception {
        Chaser enemy = new Chaser(0, 0, null, 1);
        performCollisionTest(enemy);
    }

    @Test
    @DisplayName("Collision with OmegaBoss")
    void testCollisionWithOmegaBoss() throws Exception {
        OmegaBoss enemy = new OmegaBoss(Color.ORANGE, 500);
        performCollisionTest(enemy);
    }

    @Test
    @DisplayName("Collision with FinalBoss")
    void testCollisionWithFinalBoss() throws Exception {
        FinalBoss enemy = new FinalBoss(0, 0, WIDTH, HEIGHT, null, 1);
        performCollisionTest(enemy);
    }

    /**
     * Helper method to perform the actual collision test logic for a given enemy.
     * @param enemyToCollide The enemy to test collision with.
     */
    private void performCollisionTest(Collidable enemyToCollide) throws Exception {
        // Given: A fresh GameScreen for each enemy type to ensure test isolation
        GameScreen gameScreen = new GameScreen(gameState, mockLevel, false, INITIAL_LIVES, WIDTH, HEIGHT, FPS);
        gameScreen.initialize();

        Ship ship = gameScreen.getShip();
        int initialLives = gameState.getLivesRemaining();

        // Use reflection to get and clear the list of collidable entities
        Field collidableEntitiesField = GameScreen.class.getDeclaredField("collidableEntities");
        collidableEntitiesField.setAccessible(true);
        List<Collidable> collidableEntities = (List<Collidable>) collidableEntitiesField.get(gameScreen);

        collidableEntities.clear();

        // Position the enemy at the same location as the ship for a guaranteed collision
        if (enemyToCollide instanceof Entity) {
            Entity enemyEntity = (Entity) enemyToCollide;
            enemyEntity.setPositionX(ship.getPositionX());
            enemyEntity.setPositionY(ship.getPositionY());
        }

        // Handle specific enemy setup if needed
        if (enemyToCollide instanceof Chaser) {
            // Target isn't needed for collision, but good practice to be aware of it.
        } else if (enemyToCollide instanceof OmegaBoss) {
            ((OmegaBoss) enemyToCollide).attach(gameScreen);
        }

        collidableEntities.add(enemyToCollide);

        // Ensure ship is in a vulnerable state before the test
        assertEquals(false, ship.isDestroyed(), "Ship should not be destroyed initially");
        assertEquals(false, ship.isInvincible(), "Ship should not be invincible initially");
        assertEquals(false, ship.isShipTemporarilyDestroyed(), "Ship should not be temporarily destroyed initially");

        // When: Fast-forward the input delay and invoke the collision logic
        Field inputDelayField = Screen.class.getDeclaredField("inputDelay");
        inputDelayField.setAccessible(true);
        Cooldown inputDelay = (Cooldown) inputDelayField.get(gameScreen);
        Field nextMillisField = Cooldown.class.getDeclaredField("time");
        nextMillisField.setAccessible(true);
        nextMillisField.set(inputDelay, 0L);

        Method collisionsMethod = GameScreen.class.getDeclaredMethod("manageCollisions");
        collisionsMethod.setAccessible(true);
        collisionsMethod.invoke(gameScreen);

        // Then: Verify the consequences of the collision
        assertEquals(initialLives - 1, gameScreen.getGameState().getLivesRemaining(),
                "Ship lives should decrease by 1 after collision with " + enemyToCollide.getClass().getSimpleName());

        assertTrue(ship.isShipTemporarilyDestroyed(),
                "Ship should be temporarily destroyed after collision with " + enemyToCollide.getClass().getSimpleName());
    }
}