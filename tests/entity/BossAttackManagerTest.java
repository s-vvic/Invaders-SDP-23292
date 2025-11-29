package entity;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class BossAttackManagerTest {
    
    private BossAttackManager attackManager;

    @Mock
    private FinalBoss mockBoss;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        when(mockBoss.getDifficulty()).thenReturn(2);
        when(mockBoss.getMaxHp()).thenReturn(90);
        attackManager = new BossAttackManager(mockBoss);
    }

    @Test
    @DisplayName("Test When Phase 1, final boss should return bullet attack list") 
    void testPhase1Attacks() {
        when(mockBoss.getHealPoint()).thenReturn(90);

        Set<BossAttack> attacks = attackManager.processAttacks();
        assertNotNull(attacks);
    }

    @Test
    @DisplayName("Test When Phase 2, final boss also should return bullet attack list") 
    void testPhase2Attacks() {
        when(mockBoss.getHealPoint()).thenReturn(50);

        Set<BossAttack> attacks = attackManager.processAttacks();
        assertNotNull(attacks);
    }

    @Test
    @DisplayName("Test When Phase 2 start, checkAttackClear method returns true once") 
    void testPhase2ReturnTrue() {
        when(mockBoss.getHealPoint()).thenReturn(50);

        boolean firstCheck = attackManager.shouldClearAttacks();
        assertTrue(firstCheck, "First check should return true to clear attacks.");
        
        boolean secondCheck = attackManager.shouldClearAttacks();
        assertFalse(secondCheck, "Second check should return false.");
    }
}
