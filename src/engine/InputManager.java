package engine;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

/**
 * Manages keyboard and mouse input for the provided screen.
 *
 * @author <a href="mailto:RobertoIA1987@gmail.com">Roberto Izquierdo Amo</a>
 *
 */
public final class InputManager implements KeyListener, MouseListener,
        MouseMotionListener {

    /** Number of recognised keys. */
    private static final int NUM_KEYS = 256;
    /** Array with the keys marked as pressed or not. */
    private static boolean[] keys;
    /** Singleton instance of the class. */
    private static InputManager instance;
    /** Mouse X coordinate. */
    private static int mouseX;
    /** Mouse Y coordinate. */
    private static int mouseY;
    /** Is mouse button pressed. */
    private static boolean mousePressed;

    /**
     * Private constructor.
     */
    private InputManager() {
        keys = new boolean[NUM_KEYS];
        mouseX = 0;
        mouseY = 0;
        mousePressed = false;
    }

    /**
     * Returns shared instance of InputManager.
     *
     * @return Shared instance of InputManager.
     */
    protected static InputManager getInstance() {
        if (instance == null) {
            instance = new InputManager();
        }
        return instance;
    }

    /**
     * Returns true if the provided key is currently pressed.
     *
     * @param keyCode
     * Key number to check.
     * @return Key state.
     */
    public boolean isKeyDown(final int keyCode) {
        return keys[keyCode];
    }

    /**
     * Changes the state of the key to pressed.
     *
     * @param key
     * Key pressed.
     */
    @Override
    public void keyPressed(final KeyEvent key) {
        if (key.getKeyCode() >= 0 && key.getKeyCode() < NUM_KEYS) {
            keys[key.getKeyCode()] = true;
        }
    }

    /**
     * Changes the state of the key to not pressed.
     *
     * @param key
     * Key released.
     */
    @Override
    public void keyReleased(final KeyEvent key) {
        if (key.getKeyCode() >= 0 && key.getKeyCode() < NUM_KEYS) {
            keys[key.getKeyCode()] = false;
        }
    }

    /**
     * Does nothing.
     *
     * @param key
     * Key typed.
     */
    @Override
    public void keyTyped(final KeyEvent key) {
        // Does nothing.
    }

    /**
     * Returns true if the mouse button is currently pressed.
     *
     * @return Mouse button state.
     */
    public boolean isMouseButtonDown() {
        return mousePressed;
    }

    /**
     * Returns the position of the mouse on the X axis.
     *
     * @return Position of the mouse on the X axis.
     */
    public int getMouseX() {
        return mouseX;
    }

    /**
     * Returns the position of the mouse on the Y axis.
     *
     * @return Position of the mouse on the Y axis.
     */
    public int getMouseY() {
        return mouseY;
    }

    /**
     * Captures mouse click events.
     *
     * @param e
     * Mouse event.
     */
    @Override
    public void mouseClicked(final MouseEvent e) {
        // Not used.
    }

    /**
     * Changes the state of the mouse button to pressed.
     *
     * @param e
     * Mouse event.
     */
    @Override
    public void mousePressed(final MouseEvent e) {
        mousePressed = true;
    }

    /**
     * Changes the state of the mouse button to not pressed.
     *
     * @param e
     * Mouse event.
     */
    @Override
    public void mouseReleased(final MouseEvent e) {
        mousePressed = false;
    }

    /**
     * Captures mouse enter events.
     *
     * @param e
     * Mouse event.
     */
    @Override
    public void mouseEntered(final MouseEvent e) {
        // Not used.
    }

    /**
     * Captures mouse exit events.
     *
     * @param e
     * Mouse event.
     */
    @Override
    public void mouseExited(final MouseEvent e) {
        // Not used.
    }

    /**
     * Sets the mouse position to the received value.
     *
     * @param e
     * Mouse event.
     */
    @Override
    public void mouseDragged(final MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
    }

    /**
     * Sets the mouse position to the received value.
     *
     * @param e
     * Mouse event.
     */
    @Override
    public void mouseMoved(final MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
    }
}