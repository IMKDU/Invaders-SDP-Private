package engine;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * Manages keyboard input for the provided screen.
 * 
 * @author <a href="mailto:RobertoIA1987@gmail.com">Roberto Izquierdo Amo</a>
 * 
 */
public final class InputManager implements KeyListener {

	/** Number of recognised keys. */
	private static final int NUM_KEYS = 256;
	/** Array with the jeys marked as pressed or not. */
	private static boolean[] keys;
	/** Singleton instance of the class. */
	private static InputManager instance;
	private boolean leftShiftDown;
	private boolean rightShiftDown;

	/**
	 * Private constructor.
	 */
	private InputManager() {
		keys = new boolean[NUM_KEYS];
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
		int code = key.getKeyCode();
		if (code >= 0 && code < NUM_KEYS)
			keys[code] = true;

		if (code == KeyEvent.VK_SHIFT) {
			int loc = key.getKeyLocation();
			if (loc == KeyEvent.KEY_LOCATION_LEFT) {
				leftShiftDown = true;
			} else if (loc == KeyEvent.KEY_LOCATION_RIGHT) {
				rightShiftDown = true;
			}
		}
	}

	/**
	 * Changes the state of the key to not pressed.
	 * 
	 * @param key
	 *            Key released.
	 */
	@Override
	public void keyReleased(final KeyEvent key) {
		int code = key.getKeyCode();
		if (code >= 0 && code < NUM_KEYS)
			keys[code] = false;

		// when right, left shifs released
		if (code == KeyEvent.VK_SHIFT) {
			int loc = key.getKeyLocation();
			if (loc == KeyEvent.KEY_LOCATION_LEFT) {
				leftShiftDown = false;
			} else if (loc == KeyEvent.KEY_LOCATION_RIGHT) {
				rightShiftDown = false;
			}
		}
	}

	/**
	 * Does nothing.
	 * 
	 * @param key
	 *            Key typed.
	 */
	@Override
	public void keyTyped(final KeyEvent key) {

	}

	public boolean isP1KeyDown(int keyCode) {
		return isKeyDown(keyCode);
	}

	public boolean isP2KeyDown(int keyCode) {
		return isKeyDown(keyCode);
	}

	public boolean isLeftShiftDown() {
		return leftShiftDown;
	}

	public boolean isRightShiftDown() {
		return rightShiftDown;
	}

	public boolean isP2SlashDown() {
		return keys[KeyEvent.VK_SLASH];
	}
}