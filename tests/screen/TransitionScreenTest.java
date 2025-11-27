package screen;

import static org.mockito.Mockito.*;

import java.awt.Graphics2D;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import screen.TransitionScreen.TransitionType;
import engine.DrawManager;

class TransitionScreenTest {

    @Mock
    DrawManager mockDrawManager;
    @Mock
    Graphics2D mockGraphics;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockDrawManager.getBackBufferGraphics()).thenReturn(mockGraphics);
    }

    /**
     * Scenario 1: Make sure you draw a line (star) in STARFIELD mode
     */
    @Test
    void testStarfieldDrawsLines() {

        TransitionScreen screen = new TransitionScreen(800, 600, 60, 1, TransitionType.STARFIELD);
        screen.drawManager = mockDrawManager;
        screen.update();
        screen.update();
        verify(mockGraphics, atLeastOnce()).drawLine(anyInt(), anyInt(), anyInt(), anyInt());
    }

    /**
     * Scenario 2: Make sure to fill in the square (black) when in FADE_OUT mode
     */
    @Test
    void testFadeOutDrawsRect() {
        TransitionScreen screen = new TransitionScreen(800, 600, 60, 1, TransitionType.FADE_OUT, null);
        screen.drawManager = mockDrawManager;
        screen.update();
        verify(mockGraphics, atLeastOnce()).fillRect(0, 0, 800, 600);
    }
}