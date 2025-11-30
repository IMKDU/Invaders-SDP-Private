package entity;

import audio.SoundManager;
import engine.Cooldown;
import engine.Core;
import engine.DrawManager;
import entity.pattern.*;

import java.awt.*;
import java.util.List;
import java.util.logging.Logger;

/**
 * GammaBoss - Advanced Middle Boss
 *
 * Features three distinct phases based on health percentage:
 * Phase 1 (100-60%): ZigZag pattern with 8-second attack cooldown
 * Phase 2 (60-30%): ZigZag (5s) → TimeGap (4 lasers) → Dash → repeat
 * Phase 3 (30-0%): ZigZag (5s) → TimeGap (8 lasers) → 2x Dash → repeat
 */
public class GammaBoss extends MidBoss {

	/** Initial position in the x-axis */
	private static final int INIT_POS_X = 224;
	/** Initial position in the y-axis */
	private static final int INIT_POS_Y = 80;
	/** Width of GammaBoss */
	private static final int GAMMA_WIDTH = 70 * 2;
	/** Height of GammaBoss */
	private static final int GAMMA_HEIGHT = 51 * 2;
	/** Health points */
	private static final int GAMMA_HEALTH = 65;
	/** Point value when destroyed */
	private static final int GAMMA_POINT_VALUE = 700;

	/** Dash cooldown duration in milliseconds (15 seconds) */
	private static final int DASH_COOLDOWN_MS = 15000;

	/** Screen dimensions */
	private final int screenWidth;
	private final int screenHeight;

	/** Boss pattern instance for delegating movement and attack logic */
	private BossPattern bossPattern;
	/** Player ships reference for pattern targeting */
	private List<Ship> targetShips;
	/** Current boss phase (1-3 based on HP) */
	private int bossPhase = 1;
	/** Logger instance */
	private Logger logger;

	/** Cooldown timer for dash attack */
	private Cooldown dashCooldown;
	/** Flag to track if currently in dash cooldown */
	private boolean isInDashCooldown = false;

	/** Animation cooldown for sprite changes */
	private Cooldown animationCooldown;

	/** Pattern cycle tracking for phase transitions */
	private int patternCycleCount = 0;
	/** Current pattern type in cycle */
	private PatternType currentPatternType = PatternType.ZIGZAG;
	/** Dash counter for phase 3 (2 consecutive dashes) */
	private int consecutiveDashCount = 0;

	/**
	 * Enum for tracking current pattern type in the pattern cycle.
	 */
	private enum PatternType {
		ZIGZAG,
		TIMEGAP,
		DASH
	}

	/**
	 * Constructor, establishes the GammaBoss entity's properties.
	 *
	 * @param color Color of the boss entity
	 * @param ships List of player ships to target
	 * @param screenWidth Screen width for boundary checking
	 * @param screenHeight Screen height for boundary checking
	 */
	public GammaBoss(Color color, List<Ship> ships, int screenWidth, int screenHeight) {
		super(INIT_POS_X, INIT_POS_Y, GAMMA_WIDTH, GAMMA_HEIGHT, GAMMA_HEALTH, GAMMA_POINT_VALUE, color);
		this.targetShips = ships;
		this.screenWidth = screenWidth;
		this.screenHeight = screenHeight;
		this.logger = Core.getLogger();
		this.dashCooldown = new Cooldown(DASH_COOLDOWN_MS);
		this.spriteType = DrawManager.SpriteType.OmegaBoss1; // Using OmegaBoss sprite temporarily
		this.animationCooldown = new Cooldown(200);

		this.logger.info("GAMMA: Initializing Boss GAMMA");
		this.logger.info("GAMMA: Starting with Phase 1 - ZigZag pattern (8s cooldown)");

		// Start with initial pattern
		choosePattern();
	}

	/**
	 * Updates the entity's state for the current game frame.
	 * Handles animation, pattern selection, and position updates.
	 */
	@Override
	public void update() {
		// Handle sprite animation
		if (this.animationCooldown.checkFinished()) {
			this.animationCooldown.reset();
			updateSprite();
		}

		// Update pattern based on health
		choosePattern();

		// Execute current pattern
		if (bossPattern != null) {
			bossPattern.move();
			bossPattern.attack();

			// Update position from pattern
			this.positionX = bossPattern.getBossPosition().x;
			this.positionY = bossPattern.getBossPosition().y;
		}

		// Update dash cooldown
		if (isInDashCooldown && dashCooldown.checkFinished()) {
			isInDashCooldown = false;
			logger.info("GAMMA: Dash cooldown finished");
		}
	}

	/**
	 * Update sprite based on current phase.
	 */
	private void updateSprite() {
		// Cycle between OmegaBoss1 and OmegaBoss2 for animation
		if (this.spriteType == DrawManager.SpriteType.OmegaBoss1) {
			this.spriteType = DrawManager.SpriteType.OmegaBoss2;
		} else {
			this.spriteType = DrawManager.SpriteType.OmegaBoss1;
		}
	}

	/**
	 * Chooses the appropriate pattern based on boss health and phase.
	 *
	 * Phase 1 (100-60%): ZigZag with 8s cooldown
	 * Phase 2 (60-30%): ZigZag (5s) → TimeGap (4 lasers) → Dash → repeat
	 * Phase 3 (30-0%): ZigZag (5s) → TimeGap (8 lasers) → 2x Dash → repeat
	 */
	private void choosePattern() {
		double hpPercentage = (double) this.healPoint / this.maxHp;

		// Phase 1: 100% - 60% HP
		if (hpPercentage > 0.6 && bossPhase == 1) {
			if (bossPattern == null) {
				// Initialize Phase 1 pattern
				bossPattern = new GammaBossZigZagPattern(this, screenWidth, screenHeight, 8000);
				currentPatternType = PatternType.ZIGZAG;
				logger.info("GAMMA: Phase 1 - ZigZag pattern (8s cooldown)");
			}
			return;
		}

		// Transition to Phase 2: 60% - 30% HP
		if (hpPercentage <= 0.6 && hpPercentage > 0.3 && bossPhase == 1) {
			bossPhase = 2;
			// Reset to new pattern cycle
			bossPattern = new GammaBossZigZagPattern(this, screenWidth, screenHeight, 5000);
			currentPatternType = PatternType.ZIGZAG;
			patternCycleCount = 0;
			logger.info("GAMMA: Entering Phase 2 - ZigZag (5s) → TimeGap (4 lasers) → Dash cycle");
			return;
		}

		// Transition to Phase 3: 30% - 0% HP
		if (hpPercentage <= 0.3 && bossPhase == 2) {
			bossPhase = 3;
			// Reset to new pattern cycle
			bossPattern = new GammaBossZigZagPattern(this, screenWidth, screenHeight, 5000);
			currentPatternType = PatternType.ZIGZAG;
			patternCycleCount = 0;
			consecutiveDashCount = 0;
			logger.info("GAMMA: Entering Phase 3 - ZigZag (5s) → TimeGap (8 lasers) → 2x Dash cycle");
			return;
		}

		// Handle Phase 2 pattern cycling
		if (bossPhase == 2) {
			handlePhase2Cycle();
		}

		// Handle Phase 3 pattern cycling
		if (bossPhase == 3) {
			handlePhase3Cycle();
		}
	}

	/**
	 * Handles Phase 2 pattern cycling.
	 * Cycle: ZigZag → TimeGap (4 lasers) → Dash → repeat
	 */
	private void handlePhase2Cycle() {
		if (currentPatternType == PatternType.ZIGZAG) {
			// After 2 pattern cycles, switch to TimeGap
			patternCycleCount++;
			if (patternCycleCount >= 2) {
				bossPattern = new GammaBossTimeGapPattern(this, targetShips, screenWidth, screenHeight, 4);
				currentPatternType = PatternType.TIMEGAP;
				patternCycleCount = 0;
				logger.info("GAMMA: Phase 2 - Switching to TimeGap pattern (4 lasers)");
			}
		} else if (currentPatternType == PatternType.TIMEGAP) {
			// Check if TimeGap pattern finished
			if (bossPattern instanceof GammaBossTimeGapPattern &&
					((GammaBossTimeGapPattern) bossPattern).isFinished()) {
				// Switch to Dash if cooldown ready
				if (!isInDashCooldown) {
					startDashPattern();
					currentPatternType = PatternType.DASH;
				} else {
					// Wait for cooldown, stay in TimeGap
					logger.info("GAMMA: Waiting for dash cooldown...");
				}
			}
		} else if (currentPatternType == PatternType.DASH) {
			// Check if Dash pattern finished
			if (bossPattern instanceof GammaBossDashPattern &&
					((GammaBossDashPattern) bossPattern).isDashCompleted()) {
				// Start cooldown and return to ZigZag
				startDashCooldown();
				bossPattern = new GammaBossZigZagPattern(this, screenWidth, screenHeight, 5000);
				currentPatternType = PatternType.ZIGZAG;
				logger.info("GAMMA: Phase 2 - Dash complete, returning to ZigZag");
			}
		}
	}

	/**
	 * Handles Phase 3 pattern cycling.
	 * Cycle: ZigZag → TimeGap (8 lasers) → 2x Dash → repeat
	 */
	private void handlePhase3Cycle() {
		if (currentPatternType == PatternType.ZIGZAG) {
			// After 2 pattern cycles, switch to TimeGap
			patternCycleCount++;
			if (patternCycleCount >= 2) {
				bossPattern = new GammaBossTimeGapPattern(this, targetShips, screenWidth, screenHeight, 8);
				currentPatternType = PatternType.TIMEGAP;
				patternCycleCount = 0;
				consecutiveDashCount = 0;
				logger.info("GAMMA: Phase 3 - Switching to TimeGap pattern (8 lasers)");
			}
		} else if (currentPatternType == PatternType.TIMEGAP) {
			// Check if TimeGap pattern finished
			if (bossPattern instanceof GammaBossTimeGapPattern &&
					((GammaBossTimeGapPattern) bossPattern).isFinished()) {
				// Switch to Dash if cooldown ready
				if (!isInDashCooldown) {
					startDashPattern();
					currentPatternType = PatternType.DASH;
				} else {
					// Wait for cooldown
					logger.info("GAMMA: Waiting for dash cooldown...");
				}
			}
		} else if (currentPatternType == PatternType.DASH) {
			// Check if Dash pattern finished
			if (bossPattern instanceof GammaBossDashPattern &&
					((GammaBossDashPattern) bossPattern).isDashCompleted()) {
				consecutiveDashCount++;

				// Check if we need another dash (2 consecutive in Phase 3)
				if (consecutiveDashCount < 2 && !isInDashCooldown) {
					// Start second dash immediately
					startDashPattern();
					logger.info("GAMMA: Phase 3 - Starting second consecutive dash");
				} else {
					// Both dashes complete, return to ZigZag
					startDashCooldown();
					bossPattern = new GammaBossZigZagPattern(this, screenWidth, screenHeight, 5000);
					currentPatternType = PatternType.ZIGZAG;
					consecutiveDashCount = 0;
					logger.info("GAMMA: Phase 3 - 2x Dash complete, returning to ZigZag");
				}
			}
		}
	}

	/**
	 * Start a new dash pattern.
	 */
	private void startDashPattern() {
		Ship target = getRandomAliveShip();
		if (target != null) {
			bossPattern = new GammaBossDashPattern(this, target, screenWidth, screenHeight);
			logger.info("GAMMA: Starting dash attack");
		}
	}

	/**
	 * Start dash cooldown.
	 */
	private void startDashCooldown() {
		isInDashCooldown = true;
		dashCooldown.reset();
		logger.info("GAMMA: Dash cooldown started (15 seconds)");
	}

	/**
	 * Get a random alive ship from the target list.
	 *
	 * @return Random alive ship, or null if none available
	 */
	private Ship getRandomAliveShip() {
		List<Ship> aliveShips = new java.util.ArrayList<>();
		for (Ship ship : targetShips) {
			if (ship != null && !ship.isDestroyed()) {
				aliveShips.add(ship);
			}
		}
		if (aliveShips.isEmpty()) return null;
		return aliveShips.get((int)(Math.random() * aliveShips.size()));
	}

	/** Simple movement method */
	@Override
	public void move(int distanceX, int distanceY) {
		this.positionX += distanceX;
		this.positionY += distanceY;
	}

	/** Marks the entity as destroyed */
	@Override
	public void destroy() {
		if (!this.isDestroyed) {
			this.isDestroyed = true;
			this.spriteType = DrawManager.SpriteType.OmegaBossDeath;
			this.logger.info("GAMMA: Boss GAMMA destroyed!");
		}
	}

	/**
	 * Reduces health and destroys the entity if it drops to zero or below.
	 *
	 * @param damage The amount of damage to inflict
	 */
	@Override
	public void takeDamage(int damage) {
		this.healPoint -= damage;
		SoundManager.play("sfx/OmegaBoss_hitting.wav");

		if (this.healPoint <= 0) {
			this.destroy();
		}
	}

	/**
	 * Check if boss is showing dash path.
	 *
	 * @return true if showing dash path
	 */
	public boolean isShowingPath() {
		if (bossPattern instanceof GammaBossDashPattern) {
			return ((GammaBossDashPattern) bossPattern).isShowingPath();
		}
		return false;
	}

	/**
	 * Calculate dash end point for visualization.
	 *
	 * @return [x, y] array of dash end point
	 */
	public int[] getDashEndPoint() {
		if (bossPattern instanceof GammaBossDashPattern) {
			return ((GammaBossDashPattern) bossPattern).getDashEndPoint(this.width, this.height);
		}
		return new int[]{this.positionX + this.width / 2, this.positionY + this.height / 2};
	}

	/**
	 * Get current boss pattern.
	 *
	 * @return Current BossPattern instance
	 */
	public BossPattern getBossPattern() {
		return this.bossPattern;
	}

	/**
	 * Get current boss phase.
	 *
	 * @return Current phase (1-3)
	 */
	public int getBossPhase() {
		return this.bossPhase;
	}

	/**
	 * Check if boss is in dash cooldown.
	 *
	 * @return true if in dash cooldown
	 */
	public boolean isInDashCooldown() {
		return isInDashCooldown;
	}

	@Override
	public void onCollision(Collidable other, GameModel model) {
		other.onCollideWithBoss(this, model);
	}

	/**
	 * Update target ships for pattern.
	 *
	 * @param ships List of player ships
	 */
	public void setTargetShips(List<Ship> ships) {
		this.targetShips = ships;
	}

	@Override
	public void onHitByPlayerBullet(Bullet bullet, GameModel model) {
		model.requestBossHitByPlayerBullet(bullet, this);
	}
}
