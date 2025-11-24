package engine;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Arrays; // Added for Arrays.fill()

/**
 * Manages keyboard input for the provided screen.
 */
public final class InputManager implements KeyListener {

    /** Number of recognised keys. */
    private static final int NUM_KEYS = 256;
    /** Array with the keys marked as pressed or not. */
    private static boolean[] keys;
    /** A queue to store typed characters for text input. */
    private static Queue<Character> keyTypedQueue;
    /** Singleton instance of the class. */
    private static InputManager instance;

    /**
     * Private constructor.
     */
    private InputManager() {
        keys = new boolean[NUM_KEYS];
        keyTypedQueue = new LinkedList<Character>();
    }

    /**
     * Returns shared instance of InputManager.
     * 
     * @return Shared instance of InputManager.
     */
    protected static InputManager getInstance() {
        if (instance == null)
            instance = new InputManager();
        return instance;
    }

    /**
     * Returns true if the provided key is currently pressed.
     * 
     * @param keyCode
     *            Key number to check.
     * @return Key state.
     */
    public boolean isKeyDown(final int keyCode) {
        return keys[keyCode];
    }

    /**
     * Changes the state of the key to pressed.
     * 
     * @param key
     *            Key pressed.
     */
    @Override
    public void keyPressed(final KeyEvent key) {
        if (key.getKeyCode() >= 0 && key.getKeyCode() < NUM_KEYS)
            keys[key.getKeyCode()] = true;
    }

    /**
     * Changes the state of the key to not pressed.
     * 
     * @param key
     *            Key released.
     */
    @Override
    public void keyReleased(final KeyEvent key) {
        if (key.getKeyCode() >= 0 && key.getKeyCode() < NUM_KEYS)
            keys[key.getKeyCode()] = false;
    }

    /**
     * Buffers the typed key for text input.
     * 
     * @param key
     *            Key typed.
     */
    @Override
    public void keyTyped(final KeyEvent key) {
        keyTypedQueue.add(key.getKeyChar());
    }

    /**
     * Retrieves and removes the next character from the typed key queue.
     * 
     * @return The next typed character, or null if the queue is empty.
     */
    public Character pollTypedKey() {
        return keyTypedQueue.poll();
    }

    /**
     * Clears the queue of typed keys.
     */
    public void clearKeyQueue() {
        keyTypedQueue.clear();
    }

    /**
     * Resets the state of all keys to not pressed.
     * This should be called when transitioning between screens to prevent
     * lingering key presses from affecting new screens.
     */
    public void resetKeyState() {
        Arrays.fill(keys, false);
    }
}
