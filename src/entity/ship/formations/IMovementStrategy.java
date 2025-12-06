package entity.ship.formations;

/**
 * Defines a contract for enemy formation movement strategies.
 */
public interface IMovementStrategy {

	/**
	 * Updates the movement logic
	 */
	void updateMovement();

	/**
	 * @return true if the movement should update every frame (Smooth),
	 * false if it should wait for the movement interval (Classic).
	 */
	boolean needsSmoothMovement();
}