package entity.pattern;

import engine.Cooldown;
import entity.BossBullet;
import entity.HasBounds;

/**
 * Custom ZigZag pattern for GammaBoss with configurable attack cooldown.
 * Extends ZigZagPattern to support different cooldown durations per phase.
 */
public class GammaBossZigZagPattern extends ZigZagPattern {

	private final int customCooldownMilli;

	/**
	 * Constructor with custom cooldown configuration.
	 *
	 * @param boss Boss entity using this pattern
	 * @param screenWidth Screen width for boundary checking
	 * @param screenHeight Screen height for boundary checking
	 * @param attackCooldownMilli Custom attack cooldown in milliseconds
	 */
	public GammaBossZigZagPattern(HasBounds boss, int screenWidth, int screenHeight, int attackCooldownMilli) {
		super(boss, screenWidth, screenHeight);
		this.customCooldownMilli = attackCooldownMilli;
		this.shootCooldown = new Cooldown(attackCooldownMilli);
		this.boss = boss;
	}

	/**
	 * Executes radial spread attack pattern.
	 * Fires bullets in multiple directions from boss center.
	 */
	@Override
	public void attack() {
		// Execute background pattern attack
		backGroundPattern.attack();

		// Check if attack cooldown has finished
		if (this.shootCooldown.checkFinished()) {
			this.shootCooldown.reset();

			// Fire radial bullets in 5 directions: center, left 1-2, right 1-2
			int[] directions = {0, 1, -1, 2, -2};
			for (int direction : directions) {
				BossBullet bullet = new BossBullet(
					this.boss.getPositionX() + this.boss.getWidth() / 2,
					this.boss.getPositionY() + this.boss.getHeight(),
					direction,
					4,  // vertical speed
					6,  // bullet width
					10, // bullet height
					"GammaBoss"
				);
				bullets.add(bullet);
			}
		}
	}
}
