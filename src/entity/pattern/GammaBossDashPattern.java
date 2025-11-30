package entity.pattern;

import engine.Core;
import entity.GameConstant;
import entity.HasBounds;

import java.awt.*;
import java.util.logging.Logger;

/**
 * Custom Dash pattern for GammaBoss with retreat phase.
 * Pattern sequence:
 * 1. Retreat for 1 second (move to top-center)
 * 2. Show dash path for 2 seconds
 * 3. Execute dash attack
 */
public class GammaBossDashPattern extends BossPattern {

	protected Logger logger;
	private boolean isRetreating = true;
	private boolean isShowingPath = false;
	private boolean isDashing = false;
	private boolean dashCompleted = false;

	private HasBounds target;
	private HasBounds boss;
	private double dashDirectionX;
	private double dashDirectionY;

	private long retreatStartTime;
	private long pathShowStartTime;

	private static final long RETREAT_DURATION = 1000; // 1 second retreat
	private static final long PATH_SHOW_DURATION = 2000; // 2 seconds path display
	private static final int DASH_SPEED = 10;
	private static final int RETREAT_SPEED = 5;

	private final int screenWidth;
	private final int screenHeight;

	/**
	 * Constructor for GammaBoss dash pattern.
	 *
	 * @param boss Boss entity using this pattern
	 * @param target Target entity (player ship)
	 * @param screenWidth Screen width for boundary checking
	 * @param screenHeight Screen height for boundary checking
	 */
	public GammaBossDashPattern(HasBounds boss, HasBounds target, int screenWidth, int screenHeight) {
		super(new Point(boss.getPositionX(), boss.getPositionY()));
		this.boss = boss;
		this.target = target;
		this.screenWidth = screenWidth;
		this.screenHeight = screenHeight;
		this.logger = Core.getLogger();

		// Start retreat phase
		this.retreatStartTime = System.currentTimeMillis();
		this.logger.info("GAMMA: Dash pattern initiated - retreating");
	}

	/**
	 * Execute movement logic based on current phase.
	 */
	@Override
	public void move() {
		// Phase 1: Retreat to top-center
		if (isRetreating) {
			long elapsedTime = System.currentTimeMillis() - retreatStartTime;

			if (elapsedTime >= RETREAT_DURATION) {
				// Retreat finished, start showing path
				isRetreating = false;
				isShowingPath = true;
				pathShowStartTime = System.currentTimeMillis();
				logger.info("GAMMA: Retreat complete, showing dash path");
			} else {
				// Move towards top-center
				retreatToTopCenter();
			}
			return;
		}

		// Phase 2: Show dash path
		if (isShowingPath) {
			long elapsedTime = System.currentTimeMillis() - pathShowStartTime;

			if (elapsedTime >= PATH_SHOW_DURATION) {
				// Calculate dash direction
				int dx = target.getPositionX() - this.bossPosition.x;
				int dy = target.getPositionY() - this.bossPosition.y;
				double distance = Math.sqrt(dx * dx + dy * dy);

				if (distance < 1.0) {
					logger.warning("GAMMA: Target too close, aborting dash");
					dashCompleted = true;
					return;
				}

				// Calculate normalized direction
				dashDirectionX = dx / distance;
				dashDirectionY = dy / distance;

				// Start dashing
				isShowingPath = false;
				isDashing = true;
				logger.info("GAMMA: Dashing! Direction=(" + dashDirectionX + ", " + dashDirectionY + ")");
			}
			return;
		}

		// Phase 3: Execute dash
		if (isDashing) {
			executeDash();
		}
	}

	/**
	 * Retreat to top-center position.
	 */
	private void retreatToTopCenter() {
		// Calculate target position (top-center)
		int targetCenterX = screenWidth / 2;
		int targetCenterY = screenHeight / 5;

		int targetX = targetCenterX - boss.getWidth() / 2;
		int targetY = targetCenterY - boss.getHeight() / 2;

		// Calculate movement delta
		int dx = targetX - bossPosition.x;
		int dy = targetY - bossPosition.y;
		double distance = Math.sqrt(dx * dx + dy * dy);

		if (distance > 1.0) {
			// Move towards target position
			bossPosition.x += (int)((dx / distance) * RETREAT_SPEED);
			bossPosition.y += (int)((dy / distance) * RETREAT_SPEED);
		}
	}

	/**
	 * Execute dash movement towards target.
	 */
	private void executeDash() {
		// Calculate new position
		int newX = boss.getPositionX() + (int)(dashDirectionX * DASH_SPEED);
		int newY = boss.getPositionY() + (int)(dashDirectionY * DASH_SPEED);

		// Check boundaries
		boolean hitBoundary = false;

		if (newX <= 0) {
			newX = 0;
			hitBoundary = true;
		} else if (newX + boss.getWidth() >= GameConstant.SCREEN_WIDTH) {
			newX = GameConstant.SCREEN_WIDTH - boss.getWidth();
			hitBoundary = true;
		}

		if (newY <= GameConstant.STAT_SEPARATION_LINE_HEIGHT) {
			newY = GameConstant.STAT_SEPARATION_LINE_HEIGHT;
			hitBoundary = true;
		} else if (newY + boss.getHeight() >= GameConstant.ITEMS_SEPARATION_LINE_HEIGHT) {
			newY = GameConstant.ITEMS_SEPARATION_LINE_HEIGHT - boss.getHeight();
			hitBoundary = true;
		}

		// Update position
		this.bossPosition.x = newX;
		this.bossPosition.y = newY;

		// End dash when hitting boundary
		if (hitBoundary) {
			logger.info("GAMMA: Dash completed, hit boundary");
			isDashing = false;
			dashCompleted = true;
		}
	}

	@Override
	public void attack() {
		// Dash pattern is movement-only, no attack
	}

	@Override
	public void setTarget(HasBounds target) {
		this.target = target;
	}

	/**
	 * Check if showing dash path.
	 *
	 * @return true if in path display phase
	 */
	public boolean isShowingPath() {
		return isShowingPath;
	}

	/**
	 * Check if currently dashing.
	 *
	 * @return true if in dash execution phase
	 */
	public boolean isDashing() {
		return isDashing;
	}

	/**
	 * Check if dash pattern is completed.
	 *
	 * @return true if dash is finished
	 */
	public boolean isDashCompleted() {
		return dashCompleted;
	}

	/**
	 * Check if currently retreating.
	 *
	 * @return true if in retreat phase
	 */
	public boolean isRetreating() {
		return isRetreating;
	}

	/**
	 * Calculate dash end point for visualization.
	 *
	 * @param bossWidth Boss width
	 * @param bossHeight Boss height
	 * @return Array with [x, y] coordinates
	 */
	public int[] getDashEndPoint(int bossWidth, int bossHeight) {
		if (isShowingPath) {
			// Return target position when showing path
			return new int[]{
				target.getPositionX() + target.getWidth() / 2,
				target.getPositionY() + target.getHeight() / 2
			};
		}

		// Calculate boundary position when dashing
		int endX = bossPosition.x;
		int endY = bossPosition.y;

		if (dashDirectionX > 0) {
			endX = GameConstant.SCREEN_WIDTH - bossWidth;
		} else if (dashDirectionX < 0) {
			endX = 0;
		}

		if (dashDirectionY > 0) {
			endY = GameConstant.ITEMS_SEPARATION_LINE_HEIGHT - bossHeight;
		} else if (dashDirectionY < 0) {
			endY = GameConstant.STAT_SEPARATION_LINE_HEIGHT;
		}

		return new int[]{endX + bossWidth / 2, endY + bossHeight / 2};
	}
}
