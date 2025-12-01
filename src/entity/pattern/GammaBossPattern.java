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
 *
 * Uses existing base patterns: {@link ZigZagPattern}, {@link TimeGapAttackPattern}, {@link DashPattern}
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

	/** Currently selected attack pattern. */
	private BossPattern attackPattern;
	/** Currently selected movement pattern. */
	private BossPattern movePattern;

	/** Cooldown for attack switching/firing. */
	private Cooldown attackCooldown;
	/** Cooldown for dash attack. */
	private Cooldown dashCooldown;

	/** Attack cooldown values for each phase. */
	private final int[][] attackCooldownMillis = {
			{8000},  // Phase 1: 8 seconds
			{7000},  // Phase 2: 7 seconds (ZigZag)
			{5000}   // Phase 3: 5 seconds (ZigZag)
	};

	/** Currently active phase (1, 2, or 3). */
	private int currentPhase = 0;

	/** Reference to the owning boss. */
	private final MidBoss boss;
	/** Reference to the player ships for targeting. */
	private List<Ship> targetShips;

	/** Current sub-pattern in the cycle (for phase 2 and 3). */
	private PatternCycleState cycleState = PatternCycleState.ATTACK;
	/** Dash counter for phase 3 (2 consecutive dashes). */
	private int consecutiveDashCount = 0;
	/** Flag to track if in dash cooldown. */
	private boolean isInDashCooldown = false;

	/** Track laser count for TimeGapAttackPattern. */
	private int lasersFired = 0;
	/** Maximum lasers for current phase. */
	private int maxLasersForPhase = 0;

	/** Track if using ZigZag or TimeGap for current attack cycle. */
	private boolean usingZigZag = false;
	/** Number of attack cycles completed (for ZigZag). */
	private int attackCyclesCompleted = 0;
	/** Required attack cycles before switching to dash. */
	private static final int REQUIRED_ATTACK_CYCLES = 2;

	/**
	 * Enum for tracking current pattern cycle state.
	 */
	private enum PatternCycleState {
		ATTACK,  // ZigZag or TimeGap attack
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
			ZigZagPattern zigzag = new ZigZagPattern(boss, screenWidth, screenHeight);
			Cooldown cooldown8s = new Cooldown(attackCooldownMillis[0][0]);
			zigzag.setCooldown(cooldown8s);

			movePattern = zigzag;
			attackPattern = zigzag;
			attackCooldown.setMilliseconds(attackCooldownMillis[0][0]);
			attackCooldown.reset();
			cycleState = PatternCycleState.ATTACK;
			Core.getLogger().info("GammaBossPattern: Phase 1 start - ZigZag (8s cooldown)");
		}
	}

	/**
	 * Phase 2 configuration: Random(ZigZag 7s OR TimeGap 4) → Dash 1x.
	 *
	 * @param isInit true if phase just started
	 */
	private void phase2(boolean isInit) {
		currentPhase = 2;
		if (isInit) {
			// Start with random attack pattern selection
			selectRandomAttackPattern(2);
			Core.getLogger().info("GammaBossPattern: Phase 2 start - Random attack → Dash 1x");
		}

		handlePhase2Cycle();
	}

	/**
	 * Phase 3 configuration: Random(ZigZag 5s OR TimeGap 8) → Dash 2x.
	 *
	 * @param isInit true if phase just started
	 */
	private void phase3(boolean isInit) {
		currentPhase = 3;
		if (isInit) {
			// Start with random attack pattern selection
			consecutiveDashCount = 0;
			selectRandomAttackPattern(3);
			Core.getLogger().info("GammaBossPattern: Phase 3 start - Random attack → Dash 2x");
		}

		handlePhase3Cycle();
	}

	/**
	 * Randomly selects between ZigZag or TimeGap attack pattern.
	 *
	 * @param phase Current phase (2 or 3)
	 */
	private void selectRandomAttackPattern(int phase) {
		// Randomly choose between ZigZag (true) or TimeGap (false)
		usingZigZag = Math.random() < 0.5;

		if (usingZigZag) {
			// Use ZigZag pattern
			ZigZagPattern zigzag = new ZigZagPattern(boss, screenWidth, screenHeight);
			int cooldownMs = attackCooldownMillis[phase - 1][0];
			Cooldown cooldown = new Cooldown(cooldownMs);
			zigzag.setCooldown(cooldown);

			movePattern = zigzag;
			attackPattern = zigzag;
			attackCooldown.setMilliseconds(cooldownMs);
			attackCooldown.reset();
			attackCyclesCompleted = 0;

			Core.getLogger().info("GammaBossPattern: Phase " + phase + " - Selected ZigZag pattern (" + cooldownMs + "ms cooldown)");
		} else {
			// Use TimeGap pattern
			int maxLasers = (phase == 2) ? 4 : 8;
			TimeGapAttackPattern timegap = new TimeGapAttackPattern(boss, targetShips, screenWidth, screenHeight);

			movePattern = timegap;
			attackPattern = timegap;
			lasersFired = 0;
			maxLasersForPhase = maxLasers;

			Core.getLogger().info("GammaBossPattern: Phase " + phase + " - Selected TimeGap pattern (" + maxLasers + " lasers)");
		}

		cycleState = PatternCycleState.ATTACK;
	}

	/**
	 * Handles Phase 2 pattern cycling.
	 * Cycle: Random(ZigZag 7s OR TimeGap 4) → Dash 1x → repeat
	 */
	private void handlePhase2Cycle() {
		if (cycleState == PatternCycleState.ATTACK) {
			// Check if attack phase is complete
			boolean attackComplete = false;

			if (usingZigZag) {
				// ZigZag: check if 2 attack cycles completed
				if (attackCooldown.checkFinished()) {
					attackCyclesCompleted++;
					attackCooldown.reset();
					if (attackCyclesCompleted >= REQUIRED_ATTACK_CYCLES) {
						attackComplete = true;
						Core.getLogger().info("GammaBossPattern: Phase 2 - ZigZag 2 cycles complete");
					}
				}
			} else {
				// TimeGap: check if enough lasers fired
				if (lasersFired >= maxLasersForPhase) {
					attackComplete = true;
					Core.getLogger().info("GammaBossPattern: Phase 2 - TimeGap 4 lasers complete");
				}
			}

			// Switch to Dash if attack complete and dash cooldown ready
			if (attackComplete && !isInDashCooldown) {
				Ship target = getRandomAliveShip();
				if (target != null) {
					DashPattern dash = new DashPattern(boss, target);
					movePattern = dash;
					attackPattern = dash;
					cycleState = PatternCycleState.DASH;
					consecutiveDashCount = 0;
					Core.getLogger().info("GammaBossPattern: Phase 2 - Starting Dash");
				}
			}
		} else if (cycleState == PatternCycleState.DASH) {
			// Check if Dash finished
			if (attackPattern instanceof DashPattern &&
					((DashPattern) attackPattern).isDashCompleted()) {
				// Start dash cooldown and select new random attack pattern
				startDashCooldown();
				selectRandomAttackPattern(2);
				Core.getLogger().info("GammaBossPattern: Phase 2 - Dash complete, starting new random attack");
			}
		}
	}

	/**
	 * Handles Phase 3 pattern cycling.
	 * Cycle: Random(ZigZag 5s OR TimeGap 8) → Dash 2x → repeat
	 */
	private void handlePhase3Cycle() {
		if (cycleState == PatternCycleState.ATTACK) {
			// Check if attack phase is complete
			boolean attackComplete = false;

			if (usingZigZag) {
				// ZigZag: check if 2 attack cycles completed
				if (attackCooldown.checkFinished()) {
					attackCyclesCompleted++;
					attackCooldown.reset();
					if (attackCyclesCompleted >= REQUIRED_ATTACK_CYCLES) {
						attackComplete = true;
						Core.getLogger().info("GammaBossPattern: Phase 3 - ZigZag 2 cycles complete");
					}
				}
			} else {
				// TimeGap: check if enough lasers fired
				if (lasersFired >= maxLasersForPhase) {
					attackComplete = true;
					Core.getLogger().info("GammaBossPattern: Phase 3 - TimeGap 8 lasers complete");
				}
			}

			// Switch to Dash if attack complete and dash cooldown ready
			if (attackComplete && !isInDashCooldown) {
				Ship target = getRandomAliveShip();
				if (target != null) {
					DashPattern dash = new DashPattern(boss, target);
					movePattern = dash;
					attackPattern = dash;
					cycleState = PatternCycleState.DASH;
					consecutiveDashCount = 0;
					Core.getLogger().info("GammaBossPattern: Phase 3 - Starting Dash 1/2");
				}
			}
		} else if (cycleState == PatternCycleState.DASH) {
			// Check if Dash finished
			if (attackPattern instanceof DashPattern &&
					((DashPattern) attackPattern).isDashCompleted()) {
				consecutiveDashCount++;

				// Check if we need another dash (2 consecutive in Phase 3)
				if (consecutiveDashCount < 2) {
					// Start second dash immediately
					Ship target = getRandomAliveShip();
					if (target != null) {
						DashPattern dash = new DashPattern(boss, target);
						movePattern = dash;
						attackPattern = dash;
						Core.getLogger().info("GammaBossPattern: Phase 3 - Starting Dash 2/2");
					}
				} else {
					// Both dashes complete, start dash cooldown and select new random attack pattern
					startDashCooldown();
					selectRandomAttackPattern(3);
					Core.getLogger().info("GammaBossPattern: Phase 3 - 2x Dash complete, starting new random attack");
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
		if (attackPattern instanceof DashPattern) {
			return ((DashPattern) attackPattern).isShowingPath();
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
		if (attackPattern instanceof DashPattern) {
			return ((DashPattern) attackPattern).getDashEndPoint(bossWidth, bossHeight);
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

		Set<Bullet> bullets = this.attackPattern.getBullets();

		// Track laser count for TimeGapAttackPattern (when in ATTACK state and using TimeGap)
		if (cycleState == PatternCycleState.ATTACK && !usingZigZag && !bullets.isEmpty()) {
			lasersFired += bullets.size();
		}

		return bullets;
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
