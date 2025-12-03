package entity.pattern;

import engine.Cooldown;
import engine.Core;
import entity.*;

import java.awt.*;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Pattern coordinator for the Zeta mid-boss.
 *
 * <p>
 * ZetaBoss has TWO independent attack patterns:
 * 1. BlackHole: Continuously repeating (Active → Cooldown → Active...)
 * 2. Apocalypse: Priority interrupt that forces BlackHole to stop
 * </p>
 *
 * Pattern Priority:
 * - Apocalypse > BlackHole
 * - When Apocalypse activates, BlackHole is force-stopped
 * - After Apocalypse ends, BlackHole restarts fresh from Active state
 *
 * Movement:
 * - Boss always moves EXCEPT during Apocalypse (5 seconds total)
 * - Phase 1: Diagonal movement
 * - Phase 2: ZigZag Angry movement
 * - Phase 3: Random(Diagonal or ZigZag Angry)
 */
public class ZetaBossPattern extends BossPattern implements IBossPattern {

    /** HP ratio thresholds for phase transitions */
    private static final double PHASE1_TO_PHASE2_TRIGGER = 0.70;
    private static final double PHASE2_TO_PHASE3_TRIGGER = 0.40;

    // BlackHole parameters
    private static final int BLACKHOLE_RADIUS = 1000;
    private static final double BLACKHOLE_PULL_CONSTANT = 0.005;

    // Pattern speeds
    private static final int DIAGONAL_X_SPEED = 4;
    private static final int DIAGONAL_Y_SPEED = 3;
    private static final Color DIAGONAL_COLOR = Color.MAGENTA;

    // Screen dimensions
    private int SCREEN_WIDTH = GameConstant.SCREEN_WIDTH;
    private int SCREEN_HEIGHT = GameConstant.SCREEN_HEIGHT;

    /** Variable for Guided Missile Pattern */
    private GuidedMissilePattern guidedMissilePattern;

    /** BlackHole states */
    private enum BlackHoleState {
        ACTIVE,         // BlackHole is pulling players
        COOLDOWN,       // BlackHole is inactive
        FORCED_STOP     // BlackHole stopped by Apocalypse
    }

    /** Apocalypse states */
    private enum ApocalypseState {
        READY,          // Ready to activate (cooldown finished)
        CHARGING,       // 3 seconds charging phase
        FIRING,         // 2 seconds firing phase
        COOLDOWN        // Waiting for cooldown
    }

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

    // BlackHole state management
    private BlackHoleState blackHoleState = BlackHoleState.COOLDOWN;
    private BlackHolePattern currentBlackHole = null;
    private Cooldown blackHoleDurationTimer;
    private Cooldown blackHoleCooldownTimer;
    private int blackHoleDuration;
    private int blackHoleCooldown;

    // Apocalypse state management
    private ApocalypseState apocalypseState = ApocalypseState.COOLDOWN;
    private ApocalypseAttackPattern apocalypsePattern;
    private Cooldown apocalypseCooldownTimer;
    private int apocalypseCooldown;

    // Movement pattern
    private BossPattern movementPattern;

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

        this.guidedMissilePattern = new GuidedMissilePattern(this.boss, ships);
        // Initialize timers for phase 1
        updateTimersForPhase();

        // Start BlackHole cooldown
        this.blackHoleCooldownTimer.reset();

        // Start Apocalypse cooldown
        this.apocalypseCooldownTimer.reset();

        // Initialize movement pattern
        this.movementPattern = createMovementPattern();

        this.logger.info("ZetaBossPattern: Initialized");
    }

    /**
     * Performs one update step following priority order:
     * 1. Check Apocalypse cooldown (highest priority)
     * 2. Update active Apocalypse
     * 3. Update BlackHole cycle
     * 4. Update movement (except during Apocalypse)
     */
    public void update() {
        // Update phase based on HP
        updatePhase();

        // Priority 1: Check if Apocalypse cooldown finished
        if (apocalypseState == ApocalypseState.COOLDOWN && apocalypseCooldownTimer.checkFinished()) {
            forceStopBlackHole();
            startApocalypse();
            stopMovement();
            return;
        }

        // Priority 2: Handle active Apocalypse
        if (apocalypseState == ApocalypseState.CHARGING || apocalypseState == ApocalypseState.FIRING) {
            updateApocalypse();
            stopMovement();
            return;
        }

        // Priority 3: Handle BlackHole cycle
        updateBlackHoleCycle();

        // Priority 4: Update movement (always except during Apocalypse)
        updateMovement();

        if (guidedMissilePattern != null) {
            guidedMissilePattern.attack();
        }
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

        // Update timers if phase changed
        if (previousPhase != this.currentPhase) {
            updateTimersForPhase();
            // Recreate movement pattern for new phase
            this.movementPattern = createMovementPattern();
            logger.info("ZetaBossPattern: Phase changed to " + this.currentPhase);
        }
    }

    /**
     * Updates timers based on current phase
     */
    private void updateTimersForPhase() {
        switch (this.currentPhase) {
            case 1: // 100-70%
                this.blackHoleDuration = 5000; // 5 seconds
                this.blackHoleCooldown = 10000; // 10 seconds
                this.apocalypseCooldown = 20000; // 20 seconds
                break;
            case 2: // 70-40%
                this.blackHoleDuration = 7000; // 7 seconds
                this.blackHoleCooldown = 7000; // 7 seconds
                this.apocalypseCooldown = 15000; // 15 seconds
                break;
            case 3: // 40-0%
                this.blackHoleDuration = 9000; // 9 seconds
                this.blackHoleCooldown = 5000; // 5 seconds
                this.apocalypseCooldown = 10000; // 10 seconds
                break;
        }

        // Create new timers with updated durations
        this.blackHoleDurationTimer = new Cooldown(this.blackHoleDuration);
        this.blackHoleCooldownTimer = new Cooldown(this.blackHoleCooldown);
        this.apocalypseCooldownTimer = new Cooldown(this.apocalypseCooldown);
    }

    /**
     * Force stops BlackHole when Apocalypse starts
     */
    private void forceStopBlackHole() {
        if (blackHoleState == BlackHoleState.ACTIVE) {
            logger.info("ZetaBossPattern: BlackHole force-stopped by Apocalypse");
        }
        blackHoleState = BlackHoleState.FORCED_STOP;
        currentBlackHole = null;
    }

    /**
     * Starts Apocalypse attack
     */
    private void startApocalypse() {
        apocalypseState = ApocalypseState.CHARGING;
        apocalypsePattern.start(1);
        logger.info("ZetaBossPattern: Apocalypse activated!");
    }

    /**
     * Updates Apocalypse state
     */
    private void updateApocalypse() {
        apocalypsePattern.attack();

        // Check if Apocalypse finished
        if (!apocalypsePattern.isPatternActive()) {
            apocalypseState = ApocalypseState.COOLDOWN;
            apocalypseCooldownTimer.reset();

            // Restart BlackHole fresh from Active state
            restartBlackHoleFresh();

            // Resume movement
            this.movementPattern = createMovementPattern();

            logger.info("ZetaBossPattern: Apocalypse ended, restarting BlackHole");
        }
    }

    /**
     * Restarts BlackHole from fresh Active state after Apocalypse
     */
    private void restartBlackHoleFresh() {
        activateBlackHole();
    }

    /**
     * Updates BlackHole cycle (Active ↔ Cooldown)
     */
    private void updateBlackHoleCycle() {
        switch (blackHoleState) {
            case ACTIVE:
                // Check if duration finished
                if (blackHoleDurationTimer.checkFinished()) {
                    deactivateBlackHole();
                }
                break;

            case COOLDOWN:
                // Check if cooldown finished
                if (blackHoleCooldownTimer.checkFinished()) {
                    activateBlackHole();
                }
                break;

            case FORCED_STOP:
                // Wait for Apocalypse to finish (handled in updateApocalypse)
                break;
        }
    }

    /**
     * Activates BlackHole at a random position
     */
    private void activateBlackHole() {
        // Generate random center position for BlackHole
        int centerX = random.nextInt(SCREEN_WIDTH - 200) + 100;
        int centerY = random.nextInt(200) + 150;

        currentBlackHole = new BlackHolePattern(
                boss,
                ships,
                centerX,
                centerY,
                BLACKHOLE_RADIUS,
                BLACKHOLE_PULL_CONSTANT,
                blackHoleDuration
        );

        blackHoleState = BlackHoleState.ACTIVE;
        blackHoleDurationTimer.reset();

        logger.info("ZetaBossPattern: BlackHole activated at (" + centerX + ", " + centerY + ")");
    }

    /**
     * Deactivates BlackHole and starts cooldown
     */
    private void deactivateBlackHole() {
        blackHoleState = BlackHoleState.COOLDOWN;
        currentBlackHole = null;
        blackHoleCooldownTimer.reset();

        logger.info("ZetaBossPattern: BlackHole deactivated, cooldown started");
    }

    /**
     * Creates movement pattern based on current phase
     */
    private BossPattern createMovementPattern() {
        switch (this.currentPhase) {
            case 1: // Phase 1: DiagonalPattern
                logger.info("ZetaBossPattern: Movement - Diagonal");
                return new DiagonalPattern(boss, DIAGONAL_X_SPEED, DIAGONAL_Y_SPEED, DIAGONAL_COLOR);
            case 2: // Phase 2: ZigZagAngryPattern
                logger.info("ZetaBossPattern: Movement - ZigZag Angry");
                return new ZigZagAngryPattern(boss, SCREEN_WIDTH, SCREEN_HEIGHT);
            case 3: // Phase 3: Random between Diagonal and ZigZagAngry
                if (random.nextBoolean()) {
                    logger.info("ZetaBossPattern: Movement - Diagonal (random)");
                    return new DiagonalPattern(boss, DIAGONAL_X_SPEED, DIAGONAL_Y_SPEED, DIAGONAL_COLOR);
                } else {
                    logger.info("ZetaBossPattern: Movement - ZigZag Angry (random)");
                    return new ZigZagAngryPattern(boss, SCREEN_WIDTH, SCREEN_HEIGHT);
                }
            default:
                return new DiagonalPattern(boss, DIAGONAL_X_SPEED, DIAGONAL_Y_SPEED, DIAGONAL_COLOR);
        }
    }

    /**
     * Stops movement (during Apocalypse)
     */
    private void stopMovement() {
        // Movement is handled by not calling movementPattern.move()
    }

    /**
     * Updates movement pattern
     */
    private void updateMovement() {
        if (movementPattern != null) {
            movementPattern.move();
            movementPattern.attack();
        }
    }

    @Override
    public void attack() {
        // Attack is handled in update() for each pattern
        if (blackHoleState == BlackHoleState.ACTIVE && currentBlackHole != null) {
            currentBlackHole.attack();
        }
    }

    @Override
    public void move() {
        // Movement handled in update()
        // During Apocalypse, don't move
        if (apocalypseState == ApocalypseState.CHARGING || apocalypseState == ApocalypseState.FIRING) {
            // Boss doesn't move during Apocalypse
            return;
        }

        // Update position from movement pattern
        if (movementPattern != null) {
            this.bossPosition.x = movementPattern.getBossPosition().x;
            this.bossPosition.y = movementPattern.getBossPosition().y;
        }
    }

    @Override
    public Point getBossPosition() {
        return new Point(this.bossPosition.x, this.bossPosition.y);
    }

    @Override
    public Set<Bullet> getBullets() {
        Set<Bullet> allBullets = new java.util.HashSet<>();

        if (movementPattern != null) {
            allBullets.addAll(movementPattern.getBullets());
        }

        if (guidedMissilePattern != null) {
            allBullets.addAll(guidedMissilePattern.getBullets());
        }

        if (apocalypsePattern != null) {
            allBullets.addAll(apocalypsePattern.getBullets());
        }

        if (currentBlackHole != null) {
            allBullets.addAll(currentBlackHole.getBullets());
        }

        return allBullets;
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
     * Returns movement pattern or apocalypse pattern depending on state
     */
    public BossPattern getActivePattern() {
        if (apocalypseState == ApocalypseState.CHARGING || apocalypseState == ApocalypseState.FIRING) {
            return apocalypsePattern;
        }
        return movementPattern;
    }

    public GuidedMissilePattern getGuidedMissilePattern() {
        return this.guidedMissilePattern;
    }
}
