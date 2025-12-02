package entity;

import audio.SoundManager;
import engine.Core;
import engine.Cooldown;
import engine.DrawManager;
import entity.pattern.*;

import java.awt.*;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Zeta - Mid Boss with complex phase-based pattern cycles
 * Features 4 attack patterns with health-based phase transitions
 */
public class ZetaBoss extends MidBoss {

    /** Initial position in the x-axis. */
    private static final int INIT_POS_X = 224;
    /** Initial position in the y-axis. */
    private static final int INIT_POS_Y = 80;
    /** Width of Zeta */
    private static final int ZETA_WIDTH = 100 * 2;
    /** Height of Zeta */
    private static final int ZETA_HEIGHT = 100 * 2;
    /** Current Health of Zeta */
    private static final int ZETA_HEALTH = 60;
    /** Point of Zeta when destroyed */
    private static final int ZETA_POINT_VALUE = 800;

    // Pattern speeds
    private static final int DIAGONAL_X_SPEED = 4;
    private static final int DIAGONAL_Y_SPEED = 3;
    private static final Color DIAGONAL_COLOR = Color.MAGENTA;

    // BlackHole parameters
    private static final int BLACKHOLE_RADIUS = 300;
    private static final double BLACKHOLE_PULL_CONSTANT = 0.005;
    private static final int BLACKHOLE_DURATION = 5000; // 5 seconds default duration

    // Screen dimensions (assumed standard)
    private static final int SCREEN_WIDTH = 448;
    private static final int SCREEN_HEIGHT = 600;

    /** Pattern cycle states */
    private enum PatternCycle {
        BLACKHOLE,
        APOCALYPSE,
        MOVEMENT
    }

    /** Boss pattern instance for delegating movement logic */
    private BossPattern bossPattern;
    /** Player reference for pattern targeting */
    private Ship targetShip;
    /** Ships list for BlackHolePattern */
    private List<Ship> ships;
    /** Current boss phase (1, 2, or 3) */
    private int bossPhase = 1;
    /** Current pattern in the cycle */
    private PatternCycle currentCycle = PatternCycle.BLACKHOLE;
    /** Logger instance */
    private Logger logger;
    /** Animation cooldown */
    private Cooldown animationCooldown;
    /** Random generator for random selections */
    private Random random;

    // Pattern cooldowns
    private Cooldown blackHoleCooldown;
    private Cooldown apocalypseCooldown;

    // Current active BlackHole pattern
    private BlackHolePattern currentBlackHole = null;

    // Flags to track pattern completion
    private boolean waitingForPatternCompletion = false;

    /**
     * Constructor, establishes the boss entity's generic properties.
     *
     * @param color  Color of the boss entity.
     * @param player The player ship to target
     * @param ships  List of ships for BlackHolePattern
     */
    public ZetaBoss(Color color, Ship player, List<Ship> ships) {
        super(INIT_POS_X, INIT_POS_Y, ZETA_WIDTH, ZETA_HEIGHT, ZETA_HEALTH, ZETA_POINT_VALUE, color);
        this.targetShip = player;
        this.ships = ships;
        this.spriteType = DrawManager.SpriteType.ZetaBoss1;
        this.logger = Core.getLogger();
        this.animationCooldown = new Cooldown(200);
        this.random = new Random();

        // Initialize Apocalypse Pattern
        this.apocalypsePattern = new ApocalypseAttackPattern(this);

        // Initialize cooldowns based on phase 1
        updateCooldownsForPhase();

        this.logger.info("ZETA: Initializing Boss ZETA");

        // Start with BlackHole pattern
        startBlackHolePattern();
    }

    /**
     * Updates the entity's state for the current game frame.
     */
    @Override
    public void update() {
        // Update animation
        if (this.animationCooldown.checkFinished()) {
            this.animationCooldown.reset();
            updateSprite();
        }

        // Update current phase based on health
        updatePhase();

        // Update pattern cycle
        updatePatternCycle();

        // Execute current pattern
        if (bossPattern != null) {
            bossPattern.move();
            bossPattern.attack();

            // Update position from pattern
            this.positionX = bossPattern.getBossPosition().x;
            this.positionY = bossPattern.getBossPosition().y;
        }
    }

    /**
     * Updates the sprite based on current phase and pattern
     */
    private void updateSprite() {
        if (this.bossPhase == 1 || this.currentCycle == PatternCycle.MOVEMENT) {
            // Use movement sprites for diagonal pattern
            this.setWidth(119 * 2);
            this.setHeight(126 * 2);

            boolean isRight = isDiagonalRight();
            if (isRight) {
                this.spriteType = (this.spriteType == DrawManager.SpriteType.ZetaBossMovingRight1)
                    ? DrawManager.SpriteType.ZetaBossMovingRight2
                    : DrawManager.SpriteType.ZetaBossMovingRight1;
            } else {
                this.spriteType = (this.spriteType == DrawManager.SpriteType.ZetaBossMoving1)
                    ? DrawManager.SpriteType.ZetaBossMoving2
                    : DrawManager.SpriteType.ZetaBossMoving1;
            }
        } else {
            // Use default sprites for other patterns
            this.setWidth(100 * 2);
            this.setHeight(100 * 2);

            this.spriteType = (this.spriteType == DrawManager.SpriteType.ZetaBoss1)
                ? DrawManager.SpriteType.ZetaBoss2
                : DrawManager.SpriteType.ZetaBoss1;
        }
    }

    /**
     * Updates the current phase based on health percentage
     */
    private void updatePhase() {
        int previousPhase = this.bossPhase;
        double healthPercent = (double) this.healPoint / this.maxHp;

        if (healthPercent > 0.70) {
            this.bossPhase = 1;
        } else if (healthPercent > 0.40) {
            this.bossPhase = 2;
        } else {
            this.bossPhase = 3;
        }

        // Update cooldowns if phase changed
        if (previousPhase != this.bossPhase) {
            updateCooldownsForPhase();
            logger.info("ZETA: Phase changed to " + this.bossPhase);
        }
    }

    /**
     * Updates cooldowns based on current phase
     */
    private void updateCooldownsForPhase() {
        switch (this.bossPhase) {
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
                // Movement pattern doesn't have a fixed duration,
                // it continues until BlackHole cooldown is ready
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
                logger.info("ZETA: Cycle advancing to APOCALYPSE");
                break;
            case APOCALYPSE:
                currentCycle = PatternCycle.MOVEMENT;
                startMovementPattern();
                logger.info("ZETA: Cycle advancing to MOVEMENT");
                break;
            case MOVEMENT:
                currentCycle = PatternCycle.BLACKHOLE;
                logger.info("ZETA: Cycle advancing to BLACKHOLE");
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
            this,
            ships,
            centerX,
            centerY,
            BLACKHOLE_RADIUS,
            BLACKHOLE_PULL_CONSTANT,
            BLACKHOLE_DURATION
        );

        this.bossPattern = currentBlackHole;
        this.blackHoleCooldown.reset();

        logger.info("ZETA: BlackHole Pattern activated at (" + centerX + ", " + centerY + ")");
    }

    /**
     * Starts the Apocalypse pattern
     */
    private void startApocalypsePattern() {
        this.bossPattern = this.apocalypsePattern;
        this.apocalypsePattern.start(1);
        this.apocalypseCooldown.reset();

        logger.info("ZETA: Apocalypse Pattern activated!");
    }

    /**
     * Starts the movement pattern based on current phase
     */
    private void startMovementPattern() {
        switch (this.bossPhase) {
            case 1: // Phase 1: DiagonalPattern
                this.bossPattern = new DiagonalPattern(this, DIAGONAL_X_SPEED, DIAGONAL_Y_SPEED, DIAGONAL_COLOR);
                logger.info("ZETA: Movement Pattern - Diagonal");
                break;
            case 2: // Phase 2: ZigZagAngryPattern
                this.bossPattern = new ZigZagAngryPattern(this, SCREEN_WIDTH, SCREEN_HEIGHT);
                logger.info("ZETA: Movement Pattern - ZigZag Angry");
                break;
            case 3: // Phase 3: Random between Diagonal and ZigZagAngry
                if (random.nextBoolean()) {
                    this.bossPattern = new DiagonalPattern(this, DIAGONAL_X_SPEED, DIAGONAL_Y_SPEED, DIAGONAL_COLOR);
                    logger.info("ZETA: Movement Pattern - Diagonal (random)");
                } else {
                    this.bossPattern = new ZigZagAngryPattern(this, SCREEN_WIDTH, SCREEN_HEIGHT);
                    logger.info("ZETA: Movement Pattern - ZigZag Angry (random)");
                }
                break;
        }
    }

    /**
     * Checks if the diagonal pattern is moving right
     */
    private boolean isDiagonalRight() {
        if (bossPattern instanceof DiagonalPattern) {
            return ((DiagonalPattern) bossPattern).getIsRight();
        }
        return true;
    }

    @Override
    public void move(int distanceX, int distanceY) {
        this.positionX += distanceX;
        this.positionY += distanceY;
    }

    @Override
    public void destroy() {
        this.isDestroyed = true;
        this.spriteType = DrawManager.SpriteType.OmegaBossDeath;
        this.logger.info("ZETA: Boss ZETA destroyed!");
    }

    @Override
    public void takeDamage(int damage) {
        this.healPoint -= damage;
        SoundManager.play("sfx/ZetaBoss_hitting.wav");
        if(this.healPoint <= 0) {
            this.destroy();
        }
    }

    public BossPattern getBossPattern() {
        return this.bossPattern;
    }

    public int getBossPhase() {
        return this.bossPhase;
    }

    public void setTarget(Ship target) {
        this.targetShip = target;
        if (bossPattern != null) {
            bossPattern.setTarget(target);
        }
    }

    public BlackHolePattern getCurrentBlackHole() {
        return currentBlackHole;
    }

    @Override
    public void onCollision(Collidable other, GameModel model) {
        other.onCollideWithBoss(this, model);
    }

    @Override
    public void onHitByPlayerBullet(Bullet bullet, GameModel model) {
        model.requestBossHitByPlayerBullet(bullet, this);
    }
}
