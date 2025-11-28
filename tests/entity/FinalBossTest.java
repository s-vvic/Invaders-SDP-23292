package entity;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class FinalBossTest {

    private FinalBoss finalBoss;

    @Mock private Ship mockShip;
    @Mock private BossAttackManager mockAttackManager;
    @Mock private BossMovementManager mockMovementManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        finalBoss = new FinalBoss(0, 0, 800, 600, mockShip, 2);

        injectMock(finalBoss, "attackManager", mockAttackManager);
        injectMock(finalBoss, "movementManager", mockMovementManager);
    }

    @Test
    @DisplayName("Test when calling update, finalboss gives movementManager work to do")
    void testMovementManagerReaction() {
        finalBoss.update();
        verify(mockMovementManager).updateMovement();
    }

    @Test
    @DisplayName("Test when calling processAttacks method, finalboss gives attackManager work to do")
    void testAttackManagerReaction() {
        finalBoss.processAttacks();
        verify(mockAttackManager).processAttacks();
    }

    @Test
    @DisplayName("Test when calling shouldClearAttacks method, finalboss asks attackManager")
    void testClearCheckAttacksReaction() {
        finalBoss.shouldClearAttacks();
        verify(mockAttackManager).shouldClearAttacks();
    }
 
    /** initalize attribute force */
    private void injectMock(Object target, String fieldName, Object mock) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, mock);
        } catch (Exception e) {
            throw new RuntimeException("Mock allocate failed", e);
        }
    }
}