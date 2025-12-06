package entity.pattern;

import engine.core.Cooldown;
import entity.bullet.Bullet;
import entity.base.HasBounds;
import entity.bullet.LaserBeam;

import java.awt.*;
import java.util.Set;

/**
 * Common contract for boss movement and attack patterns.
 */
public interface IBossPattern {

	/**
	 * Performs this pattern's attack behavior for the current frame.
	 */
	void attack();

	/**
	 * Updates this pattern's movement for the current frame.
	 */
	void move();

	/**
	 * Returns the current boss position controlled by this pattern.
	 *
	 * @return Current boss position.
	 */
	Point getBossPosition();

	/**
	 * Returns all bullets managed or spawned by this pattern.
	 *
	 * @return Set of active bullets.
	 */
	Set<Bullet> getBullets();

	/**
	 * Returns all Lasers managed or spawned by this pattern.
	 *
	 * @return Set of active bullets.
	 */
	Set<LaserBeam> getLasers();

	/**
	 * Returns all black-holes managed or spawned by this pattern.
	 *
	 * @return Set of active blackHoles.
	 */
	Set<BlackHole> getBlackHoles();


	/**
	 * Sets the current target (usually the player) for this pattern.
	 *
	 * @param target Target entity to track.
	 */
	void setTarget(HasBounds target);

	/**
	 * Sets a cooldown used by this pattern (e.g. for firing or dashing).
	 *
	 * @param cooldown Cooldown instance to use.
	 */
	void setCooldown(Cooldown cooldown);

	/**
	 * Updates this pattern based on an external background condition.
	 *
	 * @param condition Background state flag.
	 */
	void validateBackgroundPattern(boolean condition);
}
