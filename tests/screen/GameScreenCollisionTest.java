package screen;

import engine.Cooldown;
import engine.GameState;
import engine.level.Level;
import entity.Collidable;
import entity.EnemyShip;
import entity.EnemyShipFormation;
import entity.Ship;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.awt.Color;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set; // Import Set for bullets if needed

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import engine.DrawManager.SpriteType;

class GameScreenCollisionTest {

    private GameScreen gameScreen;
    private GameState gameState;
    @Mock private Level mockLevel;

    private final int WIDTH = 448;
    private final int HEIGHT = 520;
    private final int FPS = 60;
    private final int INITIAL_LIVES = 3;
    
    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        MockitoAnnotations.openMocks(this);

        // Setup mock Level for constructors
        when(mockLevel.getFormationWidth()).thenReturn(1);
        when(mockLevel.getFormationHeight()).thenReturn(1);
        when(mockLevel.getBaseSpeed()).thenReturn(1);
        when(mockLevel.getShootingFrecuency()).thenReturn(1);
        when(mockLevel.getEnemyFormation()).thenReturn(null); // Keep default formation logic
        when(mockLevel.getItemDrops()).thenReturn(new ArrayList<>());
        when(mockLevel.getLevelName()).thenReturn("Test Level");

        gameState = new GameState(1, 0, INITIAL_LIVES, 0, 0, 0);
        gameScreen = new GameScreen(gameState, mockLevel, false, INITIAL_LIVES, WIDTH, HEIGHT, FPS);
        gameScreen.initialize();

        // Get the ship for positioning the enemy
        Ship ship = gameScreen.getShip();
        EnemyShip enemyToCollide = new EnemyShip(ship.getPositionX(), ship.getPositionY(), SpriteType.EnemyShipA1);

        // Use reflection to access the private collidableEntities list from GameScreen
        Field collidableEntitiesField = GameScreen.class.getDeclaredField("collidableEntities");
        collidableEntitiesField.setAccessible(true);
        List<Collidable> collidableEntities = (List<Collidable>) collidableEntitiesField.get(gameScreen);

        // Directly add the colliding enemy to the collidableEntities list
        collidableEntities.add(enemyToCollide);
    }

    @Test
    void testShipCollidesWithEnemy() {
        // Given
        int initialLives = gameState.getLivesRemaining();
        Ship ship = gameScreen.getShip();
        
        // Ensure ship is not destroyed or invincible at the start
        assertEquals(false, ship.isDestroyed(), "Ship should not be destroyed initially");
        assertEquals(false, ship.isInvincible(), "Ship should not be invincible initially");

        // When
        // manageCollisions is private, so we call update() which calls manageCollisions()
        // Or we use reflection to call manageCollisions()
        try {
            Field inputDelayField = Screen.class.getDeclaredField("inputDelay");
            inputDelayField.setAccessible(true);
            Cooldown inputDelay = (Cooldown) inputDelayField.get(gameScreen);
            // Fast-forward the input delay to enable game logic
            while (!inputDelay.checkFinished()) {
                Thread.sleep(100);
            }

            Method collisionsMethod = GameScreen.class.getDeclaredMethod("manageCollisions");
            collisionsMethod.setAccessible(true);
            collisionsMethod.invoke(gameScreen);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Then
        // 1. Check if ship lives decreased
        assertEquals(initialLives - 1, gameScreen.getGameState().getLivesRemaining(), "Ship lives should decrease by 1 after collision");
        // 2. Check if the ship is now destroyed (temporarily in original logic)
        assertEquals(true, ship.isDestroyed(), "Ship should be destroyed after collision");
    }
}