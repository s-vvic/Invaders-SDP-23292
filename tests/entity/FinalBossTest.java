package entity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class FinalBossTest {

    private FinalBoss finalBoss;

    @Mock
    private Ship mockShip;

    private static final int WIDTH = 100;
    private static final int HEIGHT = 80;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // create FinalBoss with mock Ship (difficulty level 2)
        finalBoss = new FinalBoss(400, 100, WIDTH, HEIGHT, mockShip, 2);
    }

    /**
     * Test shooting methods return non-null Sets (Baseline Test)
     */
    @Test
    @DisplayName("Basic shooting methods return non-null Sets")
    void testShootingMethods() {
        
        // Test each shooting method for non-null return
        
        Set<BossBullet> bullets1 = finalBoss.shoot1();
        assertNotNull(bullets1, "shoot1 method should not return null.");

        Set<BossBullet> bullets2 = finalBoss.shoot2();
        assertNotNull(bullets2, "shoot2 method should not return null.");

        Set<BossBullet> bullets3 = finalBoss.shoot3();
        assertNotNull(bullets3, "shoot3 method should not return null.");

        Set<BossBullet> bullets4 = finalBoss.shoot4();
        assertNotNull(bullets4, "shoot4 method should not return null.");
    }
    
    /**
     * Test laserShoot method returns non-null Set (Baseline Test)
     */
    @Test
    @DisplayName("Test laserShoot method returns non-null Set")
    void testLaserShootMethod() {

        // Activate laser pattern to allow shooting
        finalBoss.activateLaserPattern();
        
        Set<BossLaser> lasers = finalBoss.laserShoot();
        assertNotNull(lasers, "laserShoot() method should not return null.");
    }


    /**
     * Test movement patterns based on health phases
     */
    @Test
    @DisplayName("Phase 1 (HP > 70%) movePattern : zigzag movement")
    void testPhase1Movement() {
        // initial state (HP : 90 > 70%)
        int startX = finalBoss.getPositionX();
        int startY = finalBoss.getPositionY();

        // movePattern execution
        finalBoss.movePattern();

        // Coordinate should change due to zigzag movement logic
        assertNotEquals(startX, finalBoss.getPositionX(), "Boss should move in Phase 1 zigzag pattern.");
        assertNotEquals(startY, finalBoss.getPositionY(), "Boss should move in Phase 1 zigzag pattern.");
    }

    @Test
    @DisplayName("Phase 2 movePattern : zigzag + laser preparation")
    void testPhase2Movement() {
        // Reduce health to enter Phase 2
        // HP : 45 (50%)
        finalBoss.takeDamage(45);
        
        // movePattern execution
        finalBoss.movePattern();
        
        // check movement
        int currentX = finalBoss.getPositionX();
        finalBoss.movePattern();
        assertNotEquals(currentX, finalBoss.getPositionX(), "Boss should move in Phase 2 zigzag + laser preparation pattern.");
    }

    @Test
    @DisplayName("Phase 3 movePattern : dash movement")
    void testPhase3Movement() {
        // Given: Reduce health significantly to enter Phase 3
        // Max 90 -> 20 (about 22%)
        finalBoss.takeDamage(70); 
        
        // Dash pattern requires player position, so mock player position
        when(mockShip.getPositionX()).thenReturn(400);
        when(mockShip.getPositionY()).thenReturn(500);

        // movePattern execution
        finalBoss.movePattern();

        // Then: 대시 패턴 로직이 돌면서 위치가 변하거나 상태가 변해야 함.
        // 대시는 쿨타임과 상태(FOLLOWING, CHARGING...)가 있어 즉시 위치가 안 변할 수도 있지만
        // 에러 없이 실행되는지 확인하는 것이 중요.
        assertDoesNotThrow(() -> finalBoss.movePattern());
    }

    // =========================================================
    // 3. 페이즈 전환 로직 검증 (메서드 호출 시 상태 변화)
    // =========================================================

    @Test
    @DisplayName("Test laser pattern activation")
    void testLaserPatternActivation() {
        
        finalBoss.activateLaserPattern();

        assertDoesNotThrow(() -> finalBoss.activateLaserPattern());
    }

    @Test
    @DisplayName("Test dash pattern activation")
    void testDashPatternActivation() {

        finalBoss.activateDashPattern();
        
        assertDoesNotThrow(() -> finalBoss.dashPattern(10));
    }
}