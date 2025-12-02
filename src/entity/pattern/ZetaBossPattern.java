package entity.pattern;

import engine.Cooldown;
import engine.Core;
import entity.Bullet;
import entity.HasBounds;
import entity.MidBoss;
import entity.Ship;

import java.awt.*;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Pattern coordinator for the Zeta mid-boss.
 *
 * <p>
 * Manages three distinct phases based on HP percentage:
 * - Phase 1 (100-70%): BlackHole → Apocalypse → Diagonal movement
 * - Phase 2 (70-40%): BlackHole → Apocalypse → ZigZag Angry
 * - Phase 3 (40-0%): BlackHole → Apocalypse → Random(Diagonal or ZigZag)
 * </p>
 *
 * Pattern Cooldowns by Phase:
 * - BlackHole: 10s (Phase 1), 7s (Phase 2), 5s (Phase 3)
 * - Apocalypse: 13s (Phase 1), 10s (Phase 2), 7s (Phase 3)
 */
public class ZetaBossPattern extends BossPattern implements IBossPattern {

    /** HP ratio thresholds for phase transitions */
    private static final double PHASE1_TO_PHASE2_TRIGGER = 0.70;
    private static final double PHASE2_TO_PHASE3_TRIGGER = 0.40;

    // BlackHole parameters
    private static final int BLACKHOLE_RADIUS = 300;
    private static final double BLACKHOLE_PULL_CONSTANT = 0.005;
    private static final int BLACKHOLE_DURATION = 5000; // 5 seconds

    // Pattern speeds
    private static final int DIAGONAL_X_SPEED = 4;
    private static final int DIAGONAL_Y_SPEED = 3;
    private static final Color DIAGONAL_COLOR = Color.MAGENTA;

    // Screen dimensions
    private static final int SCREEN_WIDTH = 448;
    private static final int SCREEN_HEIGHT = 600;

    /** Pattern cycle states */
    private enum PatternCycle {
        BLACKHOLE,
        APOCALYPSE,
        MOVEMENT
    }

    /** Currently active pattern cycle */
    private PatternCycle currentCycle = PatternCycle.BLACKHOLE;
    /** Current boss phase (1, 2, or 3) */
    private int currentPhase = 1;

    /** Reference to the owning boss */
    private final MidBoss boss;
    /** Reference to player ships for BlackHolePattern */
    private final List<Ship> ships;
    /** Logger instance */
    private final Logger logger;
    /** Random generator for random selections */
    private final Random random;

    /** Currently active pattern */
    private BossPattern activePattern;
    /** Apocalypse pattern instance */
    private ApocalypseAttackPattern apocalypsePattern;
    /** Current active BlackHole pattern */
    private BlackHolePattern currentBlackHole = null;

    /** Pattern cooldowns */
    private Cooldown blackHoleCooldown;
    private Cooldown apocalypseCooldown;

    /** Flag to track pattern completion */
    private boolean waitingForPatternCompletion = false;

    /**
     * Creates a new Zeta boss pattern controller.
     *
     * @param boss Boss instance
     * @param ships List of player ships for BlackHolePattern
     */
    public ZetaBossPattern(MidBoss boss, List<Ship> ships) {
        super(new Point(boss.getPositionX(), boss.getPositionY()));
        this.boss = boss;
        this.ships = ships;
        this.logger = Core.getLogger();
        this.random = new Random();

        // Initialize Apocalypse Pattern
        this.apocalypsePattern = new ApocalypseAttackPattern(boss);

        // Initialize cooldowns for phase 1
        updateCooldownsForPhase();

        // Start with BlackHole pattern
        startBlackHolePattern();

        this.logger.info("ZetaBossPattern: Initialized");
    }

    /**
     * Performs one update step:
     * - Determines current phase from boss HP
     * - Manages pattern cycle transitions
     */
    public void update() {
        // Update current phase based on health
        updatePhase();

        // Update pattern cycle
        updatePatternCycle();
    }

    /**
     * Updates the current phase based on health percentage
     */
    private void updatePhase() {
        int previousPhase = this.currentPhase;
        double healthPercent = (double) boss.getHealPoint() / boss.getMaxHealPoint();

        if (healthPercent > PHASE1_TO_PHASE2_TRIGGER) {
            this.currentPhase = 1;
        } else if (healthPercent > PHASE2_TO_PHASE3_TRIGGER) {
            this.currentPhase = 2;
        } else {
            this.currentPhase = 3;
        }

        // Update cooldowns if phase changed
        if (previousPhase != this.currentPhase) {
            updateCooldownsForPhase();
            logger.info("ZetaBossPattern: Phase changed to " + this.currentPhase);
        }
    }

    /**
     * Updates cooldowns based on current phase
     */
    private void updateCooldownsForPhase() {
        switch (this.currentPhase) {
            case 1: // 100-70%
                this.blackHoleCooldown = new Cooldown(10000); // 10 seconds
                this.apocalypseCooldown = new Cooldown(13000); // 13 seconds
                break;
            case 2: // 70-40%
                this.blackHoleCooldown = new Cooldown(7000); // 7 seconds
                this.apocalypseCooldown = new Cooldown(10000); // 10 seconds
                break;
            case 3: // 40-0%
                this.blackHoleCooldown = new Cooldown(5000); // 5 seconds
                this.apocalypseCooldown = new Cooldown(7000); // 7 seconds
                break;
        }
    }

    /**
     * Updates the pattern cycle: BlackHole → Apocalypse → Movement → repeat
     */
    private void updatePatternCycle() {
        // If waiting for pattern completion, check if it's done
        if (waitingForPatternCompletion) {
            if (isCurrentPatternComplete()) {
                waitingForPatternCompletion = false;
                advanceToNextCycle();
            }
            return;
        }

        // Check if we should start the next pattern in the cycle
        switch (currentCycle) {
            case BLACKHOLE:
                if (blackHoleCooldown.checkFinished()) {
                    startBlackHolePattern();
                    waitingForPatternCompletion = true;
                }
                break;
            case APOCALYPSE:
                if (apocalypseCooldown.checkFinished()) {
                    startApocalypsePattern();
                    waitingForPatternCompletion = true;
                }
                break;
            case MOVEMENT:
                // Movement pattern continues until BlackHole cooldown is ready
                if (blackHoleCooldown.checkFinished()) {
                    advanceToNextCycle();
                }
                break;
        }
    }

    /**
     * Checks if the current pattern has completed
     */
    private boolean isCurrentPatternComplete() {
        switch (currentCycle) {
            case BLACKHOLE:
                return currentBlackHole != null && currentBlackHole.isFinished();
            case APOCALYPSE:
                return !apocalypsePattern.isPatternActive();
            case MOVEMENT:
                return false; // Movement pattern runs until interrupted
            default:
                return true;
        }
    }

    /**
     * Advances to the next pattern in the cycle
     */
    private void advanceToNextCycle() {
        switch (currentCycle) {
            case BLACKHOLE:
                currentCycle = PatternCycle.APOCALYPSE;
                logger.info("ZetaBossPattern: Cycle advancing to APOCALYPSE");
                break;
            case APOCALYPSE:
                currentCycle = PatternCycle.MOVEMENT;
                startMovementPattern();
                logger.info("ZetaBossPattern: Cycle advancing to MOVEMENT");
                break;
            case MOVEMENT:
                currentCycle = PatternCycle.BLACKHOLE;
                logger.info("ZetaBossPattern: Cycle advancing to BLACKHOLE");
                break;
        }
    }

    /**
     * Starts the BlackHole pattern at a random position
     */
    private void startBlackHolePattern() {
        // Generate random center position for BlackHole
        int centerX = random.nextInt(SCREEN_WIDTH - 200) + 100; // Keep away from edges
        int centerY = random.nextInt(200) + 150; // Upper half of screen

        currentBlackHole = new BlackHolePattern(
            boss,
            ships,
            centerX,
            centerY,
            BLACKHOLE_RADIUS,
            BLACKHOLE_PULL_CONSTANT,
            BLACKHOLE_DURATION
        );

        this.activePattern = currentBlackHole;
        this.blackHoleCooldown.reset();

        logger.info("ZetaBossPattern: BlackHole Pattern activated at (" + centerX + ", " + centerY + ")");
    }

    /**
     * Starts the Apocalypse pattern
     */
    private void startApocalypsePattern() {
        this.activePattern = this.apocalypsePattern;
        this.apocalypsePattern.start(1);
        this.apocalypseCooldown.reset();

        logger.info("ZetaBossPattern: Apocalypse Pattern activated!");
    }

    /**
     * Starts the movement pattern based on current phase
     */
    private void startMovementPattern() {
        switch (this.currentPhase) {
            case 1: // Phase 1: DiagonalPattern
                this.activePattern = new DiagonalPattern(boss, DIAGONAL_X_SPEED, DIAGONAL_Y_SPEED, DIAGONAL_COLOR);
                logger.info("ZetaBossPattern: Movement Pattern - Diagonal");
                break;
            case 2: // Phase 2: ZigZagAngryPattern
                this.activePattern = new ZigZagAngryPattern(boss, SCREEN_WIDTH, SCREEN_HEIGHT);
                logger.info("ZetaBossPattern: Movement Pattern - ZigZag Angry");
                break;
            case 3: // Phase 3: Random between Diagonal and ZigZagAngry
                if (random.nextBoolean()) {
                    this.activePattern = new DiagonalPattern(boss, DIAGONAL_X_SPEED, DIAGONAL_Y_SPEED, DIAGONAL_COLOR);
                    logger.info("ZetaBossPattern: Movement Pattern - Diagonal (random)");
                } else {
                    this.activePattern = new ZigZagAngryPattern(boss, SCREEN_WIDTH, SCREEN_HEIGHT);
                    logger.info("ZetaBossPattern: Movement Pattern - ZigZag Angry (random)");
                }
                break;
        }
    }

    @Override
    public void attack() {
        if (activePattern != null) {
            activePattern.attack();
        }
    }

    @Override
    public void move() {
        if (activePattern != null) {
            activePattern.move();
            // Update boss position from active pattern
            this.bossPosition.x = activePattern.getBossPosition().x;
            this.bossPosition.y = activePattern.getBossPosition().y;
        }
    }

    @Override
    public Point getBossPosition() {
        return new Point(this.bossPosition.x, this.bossPosition.y);
    }

    @Override
    public Set<Bullet> getBullets() {
        if (activePattern != null) {
            return activePattern.getBullets();
        }
        return this.bullets;
    }

    @Override
    public void setTarget(HasBounds target) {
        // Not used for ZetaBoss patterns
    }

    /**
     * Get the current active phase (1-3)
     */
    public int getCurrentPhase() {
        return this.currentPhase;
    }

    /**
     * Get the current BlackHole pattern if active
     */
    public BlackHolePattern getCurrentBlackHole() {
        return currentBlackHole;
    }

    /**
     * Get the Apocalypse pattern instance
     */
    public ApocalypseAttackPattern getApocalypsePattern() {
        return apocalypsePattern;
    }

    /**
     * Get the currently active pattern for rendering
     */
    public BossPattern getActivePattern() {
        return activePattern;
    }
}
