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
import entity.FinalBoss;
import entity.Ship;
import entity.BossAttack;

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
        injectMock(gameScreen, "bossAttacks", new HashSet<BossAttack>());

        when(mockFinalBoss.getDifficulty()).thenReturn(2);
        when(mockFinalBoss.getMaxHp()).thenReturn(90);

        when(mockFinalBoss.processAttacks()).thenReturn(new HashSet<>());
        when(mockFinalBoss.shouldClearAttacks()).thenReturn(false);
    }

    @Test
    @DisplayName("GameScreen delegates attack processing to FinalBoss")
    void testAttackDelegation() {
        when(mockFinalBoss.getHealPoint()).thenReturn(90);

        gameScreen.finalbossManage();

        verify(mockFinalBoss, atLeastOnce()).processAttacks();
    }

    @Test
    @DisplayName("GameScreen checks if bullets should be cleared")
    void testClearCheckDelegation() {
        when(mockFinalBoss.shouldClearAttacks()).thenReturn(true);

        gameScreen.finalbossManage();
        
        verify(mockFinalBoss).shouldClearAttacks();
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