package engine;

import java.awt.event.KeyEvent;

/**
 * Handles the logic for detecting a secret command sequence.
 */
public class SecretCommandHandler {

    private int state = 0;
    private final int[] commandSequence = {
        KeyEvent.VK_UP,
        KeyEvent.VK_UP,
        KeyEvent.VK_DOWN,
        KeyEvent.VK_DOWN,
        KeyEvent.VK_LEFT,
        KeyEvent.VK_RIGHT,
        KeyEvent.VK_LEFT,
        KeyEvent.VK_RIGHT
    };

    /**
     * Processes a key press and updates the command state.
     * @param keyCode The KeyEvent code of the key that was pressed.
     */
    public void handleKey(int keyCode) {
        // Check if the pressed key is the next one in the sequence
        if (state < commandSequence.length && keyCode == commandSequence[state]) {
            state++;
        } else {
            // If the wrong key is pressed, reset the sequence,
            // but allow the first key of the sequence to start a new attempt.
            if (keyCode == commandSequence[0]) {
                state = 1;
            } else {
                state = 0;
            }
        }
    }

    /**
     * Checks if the full command sequence has been entered.
     * @return true if the command is complete, false otherwise.
     */
    public boolean isCommandComplete() {
        return state == commandSequence.length;
    }

    /**
     * Resets the command sequence detector.
     */
    public void reset() {
        this.state = 0;
    }
}
