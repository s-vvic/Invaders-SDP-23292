package screen;

import static org.mockito.Mockito.*;
import java.util.HashSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import engine.GameState;
import engine.level.Level;
import entity.BossBullet;
import entity.BossLaser;
import entity.FinalBoss;
import entity.Ship;

/**
 * Tests for finalbossManage method in GameScreen class.
 */
class GameScreenBossTest {

    private GameScreen gameScreen;

    @Mock private GameState mockGameState;
    @Mock private Level mockLevel;
    @Mock private Ship mockShip;
    @Mock private FinalBoss mockFinalBoss;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        when(mockGameState.getLevel()).thenReturn(6); // Level with Final Boss of difficulty 2
        when(mockGameState.getScore()).thenReturn(0);
        when(mockGameState.getLivesRemaining()).thenReturn(3);
        
        gameScreen = new GameScreen(mockGameState, mockLevel, false, 3, 800, 600, 60);

        injectMock(gameScreen, "finalBoss", mockFinalBoss);

        injectMock(gameScreen, "ship", mockShip);

        injectMock(gameScreen, "bossBullets", new HashSet<BossBullet>());

        injectMock(gameScreen, "bossLasers", new HashSet<BossLaser>());

        when(mockFinalBoss.getDifficulty()).thenReturn(2);
        when(mockFinalBoss.getMaxHp()).thenReturn(90);
        when(mockFinalBoss.shoot1()).thenReturn(new HashSet<>());
        when(mockFinalBoss.shoot2()).thenReturn(new HashSet<>());
        when(mockFinalBoss.shoot3()).thenReturn(new HashSet<>());
        when(mockFinalBoss.shoot4()).thenReturn(new HashSet<>());
        when(mockFinalBoss.laserShoot()).thenReturn(new HashSet<>());
    }

    @Test
    @DisplayName("Boss should shoot3 in phase 1")
    void testFinalBossShootPhase1() {
        when(mockFinalBoss.getHealPoint()).thenReturn(90); // Phase 1 HP

        gameScreen.finalbossManage();

        verify(mockFinalBoss).shoot3();
    }

    /**
     * Boss should start laser pattern in phase 2
     */
    @Test
    @DisplayName("Boss should start laser pattern in phase 2")
    void testFinalBossShootPhase2() {
        when(mockFinalBoss.getHealPoint()).thenReturn(45); // Phase 2 HP
        gameScreen.finalbossManage();
        gameScreen.finalbossManage();
        
        verify(mockFinalBoss).laserShoot();
        verify(mockFinalBoss).shoot1();
        verify(mockFinalBoss).shoot2();
    }

    /**
     * Boss should shoot4 in phase 3
     */
    @Test
    @DisplayName("Boss should shoot4 in phase 3")
    void testFinalBossShootPhase3() {
        when(mockFinalBoss.getHealPoint()).thenReturn(25); // Phase 3 HP
        gameScreen.finalbossManage();

        verify(mockFinalBoss).shoot4();
    }

    /**
     * Helper method to inject mock into private field
     * @param target
     * @param fieldName
     * @param mock
     */
    private void injectMock(Object target, String fieldName, Object mock) {
    try {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
        
        field.setAccessible(true); 
        
        field.set(target, mock);   
        
    } catch (Exception e) {
        throw new RuntimeException("Mock insert failed: " + fieldName, e);
    }
}
}