package entity.pattern;

import engine.Cooldown;
import engine.Core;
import entity.Bullet;
import entity.GameConstant;
import entity.HasBounds;
import entity.MidBoss;
import entity.Ship;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Pattern coordinator for the Gamma mid-boss.
 *
 * <p>
 * Manages three distinct phases based on HP percentage:
 * - Phase 1 (100-60%): ZigZag with 8-second attack cooldown
 * - Phase 2 (60-30%): ZigZag (5s) → TimeGap (4 lasers) → Dash → repeat
 * - Phase 3 (30-0%): ZigZag (5s) → TimeGap (8 lasers) → 2x Dash → repeat
 * </p>
 */
public class GammaBossPattern extends BossPattern implements IBossPattern {

	/** HP ratio threshold: phase 1 → phase 2 (60%). */
	private static final double PHASE1_TO_PHASE2_TRIGGER = 0.6;
	/** HP ratio threshold: phase 2 → phase 3 (30%). */
	private static final double PHASE2_TO_PHASE3_TRIGGER = 0.3;

	/** Dash cooldown duration in milliseconds (15 seconds). */
	private static final int DASH_COOLDOWN_MS = 15000;

	/** Screen dimensions. */
	private final int screenWidth;
	private final int screenHeight;

	/** List of all available sub-patterns for Gamma. */
	private List<IBossPattern> patterns = new ArrayList<>();
	/** Currently selected attack pattern. */
	private IBossPattern attackPattern;
	/** Currently selected movement pattern. */
	private IBossPattern movePattern;

	/** Cooldown for attack switching/firing. */
	private Cooldown attackCooldown;
	/** Cooldown for dash attack. */
	private Cooldown dashCooldown;

	/** Attack cooldown values for each phase. */
	private final int[][] attackCooldownMillis = {
			{8000},  // Phase 1: 8 seconds
			{5000},  // Phase 2: 5 seconds
			{5000}   // Phase 3: 5 seconds
	};

	/** Currently active phase (1, 2, or 3). */
	private int currentPhase = 0;

	/** Reference to the owning boss. */
	private final MidBoss boss;
	/** Reference to the player ships for targeting. */
	private List<Ship> targetShips;

	/** Pattern cycle tracking. */
	private int patternCycleCount = 0;
	/** Current sub-pattern in the cycle (for phase 2 and 3). */
	private PatternCycleState cycleState = PatternCycleState.ZIGZAG;
	/** Dash counter for phase 3 (2 consecutive dashes). */
	private int consecutiveDashCount = 0;
	/** Flag to track if in dash cooldown. */
	private boolean isInDashCooldown = false;

	/** Pattern indices in the patterns list. */
	private static final int ZIGZAG_8S_INDEX = 0;
	private static final int ZIGZAG_5S_INDEX = 1;
	private static final int TIMEGAP_4_INDEX = 2;
	private static final int TIMEGAP_8_INDEX = 3;
	private static final int DASH_INDEX = 4;

	/**
	 * Enum for tracking current pattern cycle state.
	 */
	private enum PatternCycleState {
		ZIGZAG,
		TIMEGAP,
		DASH
	}

	/**
	 * Creates a new Gamma boss pattern controller.
	 *
	 * @param boss Boss instance
	 * @param ships List of player ships for targeting
	 * @param screenWidth Screen width for boundary checking
	 * @param screenHeight Screen height for boundary checking
	 */
	public GammaBossPattern(MidBoss boss, List<Ship> ships, int screenWidth, int screenHeight) {
		super(new Point(boss.getPositionX(), boss.getPositionY()));
		this.boss = boss;
		this.targetShips = ships;
		this.screenWidth = screenWidth;
		this.screenHeight = screenHeight;
		this.attackCooldown = new Cooldown(attackCooldownMillis[0][0]);
		this.dashCooldown = new Cooldown(DASH_COOLDOWN_MS);

		initializePatterns();
	}

	/**
	 * Initialize all sub-patterns used by GammaBoss.
	 */
	private void initializePatterns() {
		// Pattern 0: ZigZag with 8-second cooldown (Phase 1)
		GammaBossZigZagPattern zigzag8s = new GammaBossZigZagPattern(boss, screenWidth, screenHeight, 8000);
		patterns.add(zigzag8s);

		// Pattern 1: ZigZag with 5-second cooldown (Phase 2 & 3)
		GammaBossZigZagPattern zigzag5s = new GammaBossZigZagPattern(boss, screenWidth, screenHeight, 5000);
		patterns.add(zigzag5s);

		// Pattern 2: TimeGap with 4 lasers (Phase 2)
		GammaBossTimeGapPattern timegap4 = new GammaBossTimeGapPattern(boss, targetShips, screenWidth, screenHeight, 4);
		patterns.add(timegap4);

		// Pattern 3: TimeGap with 8 lasers (Phase 3)
		GammaBossTimeGapPattern timegap8 = new GammaBossTimeGapPattern(boss, targetShips, screenWidth, screenHeight, 8);
		patterns.add(timegap8);

		// Pattern 4: Dash with retreat
		GammaBossDashPattern dash = new GammaBossDashPattern(boss, getRandomAliveShip(), screenWidth, screenHeight);
		patterns.add(dash);

		// Disable background patterns for ZigZag patterns to avoid double shooting
		zigzag8s.validateBackgroundPattern(false);
		zigzag5s.validateBackgroundPattern(false);
	}

	/**
	 * Performs one update step:
	 * - Determines current phase from boss HP
	 * - Initializes patterns if phase changed
	 * - Runs phase-specific pattern cycle logic
	 */
	public void update() {
		int trigger = checkPhase();
		boolean isInit = trigger != currentPhase;

		if (trigger == 1) {
			phase1(isInit);
		} else if (trigger == 2) {
			phase2(isInit);
		} else if (trigger == 3) {
			phase3(isInit);
		}

		// Update dash cooldown
		if (isInDashCooldown && dashCooldown.checkFinished()) {
			isInDashCooldown = false;
			Core.getLogger().info("GammaBossPattern: Dash cooldown finished");
		}
	}

	/**
	 * Returns the current phase index based on boss HP.
	 *
	 * @return 1, 2, or 3 depending on HP thresholds
	 */
	public int checkPhase() {
		double hpRatio = (double) boss.getHealPoint() / boss.getMaxHealPoint();

		if (hpRatio > PHASE1_TO_PHASE2_TRIGGER) {
			return 1;
		}
		if (hpRatio > PHASE2_TO_PHASE3_TRIGGER) {
			return 2;
		}
		return 3;
	}

	/**
	 * Phase 1 configuration: ZigZag pattern with 8-second cooldown.
	 *
	 * @param isInit true if phase just started
	 */
	private void phase1(boolean isInit) {
		currentPhase = 1;
		if (isInit) {
			movePattern = patterns.get(ZIGZAG_8S_INDEX);
			attackPattern = patterns.get(ZIGZAG_8S_INDEX);
			attackCooldown.setMilliseconds(attackCooldownMillis[0][0]);
			attackCooldown.reset();
			attackPattern.setCooldown(attackCooldown);
			cycleState = PatternCycleState.ZIGZAG;
			patternCycleCount = 0;
			Core.getLogger().info("GammaBossPattern: Phase 1 start - ZigZag (8s cooldown)");
		}
	}

	/**
	 * Phase 2 configuration: ZigZag → TimeGap (4 lasers) → Dash cycle.
	 *
	 * @param isInit true if phase just started
	 */
	private void phase2(boolean isInit) {
		currentPhase = 2;
		if (isInit) {
			movePattern = patterns.get(ZIGZAG_5S_INDEX);
			attackPattern = patterns.get(ZIGZAG_5S_INDEX);
			attackCooldown.setMilliseconds(attackCooldownMillis[1][0]);
			attackCooldown.reset();
			attackPattern.setCooldown(attackCooldown);
			cycleState = PatternCycleState.ZIGZAG;
			patternCycleCount = 0;
			Core.getLogger().info("GammaBossPattern: Phase 2 start - ZigZag → TimeGap (4) → Dash cycle");
		}

		handlePhase2Cycle();
	}

	/**
	 * Phase 3 configuration: ZigZag → TimeGap (8 lasers) → 2x Dash cycle.
	 *
	 * @param isInit true if phase just started
	 */
	private void phase3(boolean isInit) {
		currentPhase = 3;
		if (isInit) {
			movePattern = patterns.get(ZIGZAG_5S_INDEX);
			attackPattern = patterns.get(ZIGZAG_5S_INDEX);
			attackCooldown.setMilliseconds(attackCooldownMillis[2][0]);
			attackCooldown.reset();
			attackPattern.setCooldown(attackCooldown);
			cycleState = PatternCycleState.ZIGZAG;
			patternCycleCount = 0;
			consecutiveDashCount = 0;
			Core.getLogger().info("GammaBossPattern: Phase 3 start - ZigZag → TimeGap (8) → 2x Dash cycle");
		}

		handlePhase3Cycle();
	}

	/**
	 * Handles Phase 2 pattern cycling.
	 * Cycle: ZigZag → TimeGap (4 lasers) → Dash → repeat
	 */
	private void handlePhase2Cycle() {
		if (cycleState == PatternCycleState.ZIGZAG) {
			// After 2 attack cycles, switch to TimeGap
			if (attackCooldown.checkFinished()) {
				patternCycleCount++;
				if (patternCycleCount >= 2) {
					// Reset TimeGap pattern
					patterns.set(TIMEGAP_4_INDEX,
						new GammaBossTimeGapPattern(boss, targetShips, screenWidth, screenHeight, 4));

					movePattern = patterns.get(TIMEGAP_4_INDEX);
					attackPattern = patterns.get(TIMEGAP_4_INDEX);
					cycleState = PatternCycleState.TIMEGAP;
					patternCycleCount = 0;
					Core.getLogger().info("GammaBossPattern: Phase 2 - Switching to TimeGap (4 lasers)");
				}
			}
		} else if (cycleState == PatternCycleState.TIMEGAP) {
			// Check if TimeGap finished
			if (attackPattern instanceof GammaBossTimeGapPattern &&
					((GammaBossTimeGapPattern) attackPattern).isFinished()) {
				// Switch to Dash if cooldown ready
				if (!isInDashCooldown) {
					// Create new dash pattern with current target
					patterns.set(DASH_INDEX,
						new GammaBossDashPattern(boss, getRandomAliveShip(), screenWidth, screenHeight));

					movePattern = patterns.get(DASH_INDEX);
					attackPattern = patterns.get(DASH_INDEX);
					cycleState = PatternCycleState.DASH;
					Core.getLogger().info("GammaBossPattern: Phase 2 - Starting Dash");
				}
			}
		} else if (cycleState == PatternCycleState.DASH) {
			// Check if Dash finished
			if (attackPattern instanceof GammaBossDashPattern &&
					((GammaBossDashPattern) attackPattern).isDashCompleted()) {
				// Start dash cooldown and return to ZigZag
				startDashCooldown();
				movePattern = patterns.get(ZIGZAG_5S_INDEX);
				attackPattern = patterns.get(ZIGZAG_5S_INDEX);
				attackCooldown.reset();
				cycleState = PatternCycleState.ZIGZAG;
				patternCycleCount = 0;
				Core.getLogger().info("GammaBossPattern: Phase 2 - Returning to ZigZag");
			}
		}
	}

	/**
	 * Handles Phase 3 pattern cycling.
	 * Cycle: ZigZag → TimeGap (8 lasers) → 2x Dash → repeat
	 */
	private void handlePhase3Cycle() {
		if (cycleState == PatternCycleState.ZIGZAG) {
			// After 2 attack cycles, switch to TimeGap
			if (attackCooldown.checkFinished()) {
				patternCycleCount++;
				if (patternCycleCount >= 2) {
					// Reset TimeGap pattern
					patterns.set(TIMEGAP_8_INDEX,
						new GammaBossTimeGapPattern(boss, targetShips, screenWidth, screenHeight, 8));

					movePattern = patterns.get(TIMEGAP_8_INDEX);
					attackPattern = patterns.get(TIMEGAP_8_INDEX);
					cycleState = PatternCycleState.TIMEGAP;
					patternCycleCount = 0;
					consecutiveDashCount = 0;
					Core.getLogger().info("GammaBossPattern: Phase 3 - Switching to TimeGap (8 lasers)");
				}
			}
		} else if (cycleState == PatternCycleState.TIMEGAP) {
			// Check if TimeGap finished
			if (attackPattern instanceof GammaBossTimeGapPattern &&
					((GammaBossTimeGapPattern) attackPattern).isFinished()) {
				// Switch to Dash if cooldown ready
				if (!isInDashCooldown) {
					// Create new dash pattern with current target
					patterns.set(DASH_INDEX,
						new GammaBossDashPattern(boss, getRandomAliveShip(), screenWidth, screenHeight));

					movePattern = patterns.get(DASH_INDEX);
					attackPattern = patterns.get(DASH_INDEX);
					cycleState = PatternCycleState.DASH;
					Core.getLogger().info("GammaBossPattern: Phase 3 - Starting Dash " + (consecutiveDashCount + 1));
				}
			}
		} else if (cycleState == PatternCycleState.DASH) {
			// Check if Dash finished
			if (attackPattern instanceof GammaBossDashPattern &&
					((GammaBossDashPattern) attackPattern).isDashCompleted()) {
				consecutiveDashCount++;

				// Check if we need another dash (2 consecutive in Phase 3)
				if (consecutiveDashCount < 2 && !isInDashCooldown) {
					// Create new dash pattern for second dash
					patterns.set(DASH_INDEX,
						new GammaBossDashPattern(boss, getRandomAliveShip(), screenWidth, screenHeight));

					movePattern = patterns.get(DASH_INDEX);
					attackPattern = patterns.get(DASH_INDEX);
					Core.getLogger().info("GammaBossPattern: Phase 3 - Starting second consecutive Dash");
				} else {
					// Both dashes complete, return to ZigZag
					startDashCooldown();
					movePattern = patterns.get(ZIGZAG_5S_INDEX);
					attackPattern = patterns.get(ZIGZAG_5S_INDEX);
					attackCooldown.reset();
					cycleState = PatternCycleState.ZIGZAG;
					patternCycleCount = 0;
					consecutiveDashCount = 0;
					Core.getLogger().info("GammaBossPattern: Phase 3 - 2x Dash complete, returning to ZigZag");
				}
			}
		}
	}

	/**
	 * Start dash cooldown.
	 */
	private void startDashCooldown() {
		isInDashCooldown = true;
		dashCooldown.reset();
		Core.getLogger().info("GammaBossPattern: Dash cooldown started (15 seconds)");
	}

	/**
	 * Get a random alive ship from the target list.
	 *
	 * @return Random alive ship, or null if none available
	 */
	private Ship getRandomAliveShip() {
		List<Ship> aliveShips = new ArrayList<>();
		for (Ship ship : targetShips) {
			if (ship != null && !ship.isDestroyed()) {
				aliveShips.add(ship);
			}
		}
		if (aliveShips.isEmpty()) return null;
		return aliveShips.get((int)(Math.random() * aliveShips.size()));
	}

	/**
	 * Check if showing dash path.
	 *
	 * @return true if currently showing dash path
	 */
	public boolean isShowingPath() {
		if (attackPattern instanceof GammaBossDashPattern) {
			return ((GammaBossDashPattern) attackPattern).isShowingPath();
		}
		return false;
	}

	/**
	 * Get dash end point for visualization.
	 *
	 * @param bossWidth Boss width
	 * @param bossHeight Boss height
	 * @return Dash end point coordinates
	 */
	public int[] getDashEndPoint(int bossWidth, int bossHeight) {
		if (attackPattern instanceof GammaBossDashPattern) {
			return ((GammaBossDashPattern) attackPattern).getDashEndPoint(bossWidth, bossHeight);
		}
		return new int[]{bossPosition.x + bossWidth / 2, bossPosition.y + bossHeight / 2};
	}

	@Override
	public void attack() {
		if (attackPattern == null) return;
		attackPattern.attack();
	}

	@Override
	public void move() {
		if (movePattern == null) return;
		movePattern.move();
		if (movePattern != null) {
			bossPosition.x = movePattern.getBossPosition().x;
			bossPosition.y = movePattern.getBossPosition().y;
		}
	}

	@Override
	public Set<Bullet> getBullets() {
		if (this.attackPattern == null) {
			return java.util.Collections.emptySet();
		}
		return this.attackPattern.getBullets();
	}

	@Override
	public void setTarget(HasBounds target) {
		if (attackPattern != null) {
			attackPattern.setTarget(target);
		}
	}

	@Override
	public void setCooldown(Cooldown cooldown) {
		if (attackPattern != null) {
			attackPattern.setCooldown(cooldown);
		}
		if (movePattern != null) {
			movePattern.setCooldown(cooldown);
		}
	}

	@Override
	public void validateBackgroundPattern(boolean condition) {
		if (attackPattern != null) {
			attackPattern.validateBackgroundPattern(condition);
		}
	}

	/**
	 * Get current phase.
	 *
	 * @return Current phase number (1-3)
	 */
	public int getCurrentPhase() {
		return currentPhase;
	}

	/**
	 * Check if in dash cooldown.
	 *
	 * @return true if in dash cooldown
	 */
	public boolean isInDashCooldown() {
		return isInDashCooldown;
	}
}
