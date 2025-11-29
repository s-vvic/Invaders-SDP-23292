package entity;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class BossMovementManagerTest {
    private BossMovementManager movementManager;

    @Mock private FinalBoss mockBoss;
    @Mock private Ship mockShip;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockBoss.getDifficulty()).thenReturn(2);

        movementManager = new BossMovementManager(mockBoss, mockShip);
    }

    @Test
    @DisplayName("Test when phase1, zigzag movement is executed")
    void testPhase1Movement() {
        when(mockBoss.getHealPoint()).thenReturn(90);
        when(mockBoss.getMaxHp()).thenReturn(90);
        
        movementManager.updateMovement();

        verify(mockBoss, atLeastOnce()).setPositionX(anyInt());
    }

    @Test
    @DisplayName("Test when phase3, dash movement is executed")
    void testDashActivation() {
        when(mockBoss.getHealPoint()).thenReturn(20);
        when(mockBoss.getMaxHp()).thenReturn(90);
        when(mockBoss.isLaserActivate()).thenReturn(false);

        movementManager.updateMovement();
        movementManager.updateMovement();
        verify(mockBoss, atLeastOnce()).move(anyInt(), anyInt());
    }
}
