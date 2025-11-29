package entity;

/**
 * Defines a contract for enemy formation movement strategies.
 */
public interface IMovementStrategy {

	/**
	 * Updates the movement logic
	 */
	void updateMovement();

	/**
	 * Activates the slowdown status effect on this movement strategy.
	 */
	void activateSlowdown();
	/**
	 * @return true if the movement should update every frame (Smooth),
	 * false if it should wait for the movement interval (Classic).
	 */
	boolean needsSmoothMovement();
}