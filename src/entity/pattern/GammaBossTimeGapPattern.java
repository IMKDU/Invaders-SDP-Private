package entity.pattern;

import engine.Cooldown;
import entity.Bullet;
import entity.HasBounds;
import entity.LaserBullet;
import entity.Ship;

import java.awt.*;
import java.util.List;

/**
 * Custom TimeGap attack pattern for GammaBoss.
 * Fires a configurable number of lasers with specific timing:
 * - Marker display: 1 second
 * - Laser interval: 0.5 seconds
 * - Each laser fires 1.5 seconds after marker display
 */
public class GammaBossTimeGapPattern extends BossPattern {

	private static final int SHOOT_COOLDOWN_MILLI = 500;  // 0.5 second interval between lasers
	private static final int CHARGE_COOLDOWN_MILLI = 1500; // 1.5 second charge time
	private static final int REMAIN_COOLDOWN_MILLI = 1000; // 1 second laser remains
	private static final int MOVING_SMOOTH_RATIO = 10;

	private Cooldown shootCooldown;
	private HasBounds boss;
	private List<Ship> ships;
	private final int screenWidth;
	private final int screenHeight;

	private final int maxLaserCount;
	private int currentLaserCount = 0;
	private boolean isFinished = false;

	/**
	 * Constructor for GammaBoss TimeGap pattern.
	 *
	 * @param boss Boss entity using this pattern
	 * @param ships List of player ships for targeting
	 * @param screenWidth Screen width for laser positioning
	 * @param screenHeight Screen height for laser positioning
	 * @param laserCount Number of lasers to fire (4 or 8)
	 */
	public GammaBossTimeGapPattern(HasBounds boss, List<Ship> ships, int screenWidth, int screenHeight, int laserCount) {
		super(new Point(boss.getPositionX(), boss.getPositionY()));
		this.boss = boss;
		this.ships = ships;
		this.screenWidth = screenWidth;
		this.screenHeight = screenHeight;
		this.maxLaserCount = laserCount;
		this.shootCooldown = new Cooldown(SHOOT_COOLDOWN_MILLI);
		this.shootCooldown.reset();
	}

	/**
	 * Execute laser attack pattern.
	 * Fires lasers at random alive ships from random screen positions.
	 */
	@Override
	public void attack() {
		// Check if pattern is finished
		if (isFinished || currentLaserCount >= maxLaserCount) {
			isFinished = true;
			return;
		}

		// Check if cooldown finished to fire next laser
		if (shootCooldown.checkFinished()) {
			shootCooldown.reset();

			// Choose random alive ship as target
			Ship target = chooseRandomAliveShip();
			if (target == null) {
				isFinished = true;
				return;
			}

			// Calculate target position (center of ship)
			Point targetPosition = new Point(
				target.getPositionX() + target.getWidth() / 2,
				target.getPositionY() + target.getHeight() / 2
			);

			// Create laser from random position
			int randomX = (int) (Math.random() * screenWidth);
			int randomY = (int) (Math.random() * screenHeight);
			Point initBulletPosition = new Point(randomX, randomY);

			// Create and add laser bullet
			Bullet laser = new LaserBullet(
				initBulletPosition,
				targetPosition,
				CHARGE_COOLDOWN_MILLI,
				REMAIN_COOLDOWN_MILLI
			);
			this.bullets.add(laser);

			currentLaserCount++;
		}
	}

	/**
	 * Move boss to center-top position smoothly.
	 */
	@Override
	public void move() {
		// Calculate target position (center-top of screen)
		int targetCenterX = screenWidth / 2;
		int targetCenterY = screenHeight / 4;

		int targetX = targetCenterX - boss.getWidth() / 2;
		int targetY = targetCenterY - boss.getHeight() / 2;

		// Calculate movement delta
		int dx = targetX - bossPosition.x;
		int dy = targetY - bossPosition.y;

		// Move smoothly towards target
		bossPosition.x += dx / MOVING_SMOOTH_RATIO;
		bossPosition.y += dy / MOVING_SMOOTH_RATIO;

		// Snap to target when very close
		if (Math.abs(targetX - bossPosition.x) <= 1) bossPosition.x = targetX;
		if (Math.abs(targetY - bossPosition.y) <= 1) bossPosition.y = targetY;
	}

	/**
	 * Choose random alive ship from the list.
	 *
	 * @return Random alive ship, or null if none available
	 */
	private Ship chooseRandomAliveShip() {
		List<Ship> aliveShips = new java.util.ArrayList<>();

		for (Ship ship : ships) {
			if (ship != null && !ship.isDestroyed()) {
				aliveShips.add(ship);
			}
		}

		if (aliveShips.isEmpty()) return null;

		int idx = (int)(Math.random() * aliveShips.size());
		return aliveShips.get(idx);
	}

	/**
	 * Check if pattern has finished firing all lasers.
	 *
	 * @return true if pattern is finished
	 */
	public boolean isFinished() {
		return isFinished;
	}

	/**
	 * Get current laser count.
	 *
	 * @return Number of lasers fired so far
	 */
	public int getCurrentLaserCount() {
		return currentLaserCount;
	}

	@Override
	public void setTarget(HasBounds target) {
		// Target is handled via ships list
	}
}
