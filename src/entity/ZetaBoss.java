package entity;

import engine.Core;
import engine.Cooldown;
import engine.DrawManager;
import entity.pattern.ApocalypseAttackPattern;
import entity.pattern.BossPattern;
import entity.pattern.DashPattern;
import entity.pattern.DiagonalPattern;
import entity.pattern.HorizontalPattern;

import java.awt.*;
import java.util.logging.Logger;

/**
 * Zeta - Mid Boss (Enhanced version of Omega)
 * Inherits all patterns from OmegaBoss and includes the Apocalypse pattern.
 */
public class ZetaBoss extends MidBoss {

    /** Initial position in the x-axis. */
    private static final int INIT_POS_X = 224;
    /** Initial position in the y-axis. */
    private static final int INIT_POS_Y = 80;
    /** Width of Zeta */
    private static final int ZETA_WIDTH = 64;
    /** Height of Zeta */
    private static final int ZETA_HEIGHT = 28;
    /** Current Health of Zeta */
    private static final int ZETA_HEALTH = 60; // Slightly higher HP than Omega
    /** Point of Zeta when destroyed */
    private static final int ZETA_POINT_VALUE = 800;
    /** Speed of x in pattern 1 */
    private static final int PATTERN_1_X_SPEED = 1;
    /** Speed of x in pattern 2 */
    private static final int PATTERN_2_X_SPEED = 4;
    /** Speed of y in pattern 2 */
    private static final int PATTERN_2_Y_SPEED = 3;
    /** Color of pattern 2 */
    private static final Color PATTERN_2_COLOR = Color.MAGENTA;

    private boolean hasUsedApocalypse = false;

    /** Dash cooldown duration in milliseconds (5 seconds) */
    private static final int DASH_COOLDOWN_MS = 5000;

    /** Boss pattern instance for delegating movement logic */
    private BossPattern bossPattern;
    /** Player reference for pattern targeting */
    private Ship targetShip;
    /** Current boss phase */
    private int bossPhase = 1;
    /** Logger instance */
    private Logger logger;
    /** Cooldown timer for dash attack */
    private Cooldown dashCooldown;
    /** Flag to track if currently in dash cooldown */
    private boolean isInDashCooldown = false;

    /**
     * Constructor, establishes the boss entity's generic properties.
     *
     * @param color             Color of the boss entity.
     * @param player           The player ship to target
     */
    public ZetaBoss(Color color, Ship player) {
        super(INIT_POS_X, INIT_POS_Y, ZETA_WIDTH, ZETA_HEIGHT, ZETA_HEALTH, ZETA_POINT_VALUE, color);
        this.targetShip = player;
        // Reusing OmegaBoss sprites as placeholders
        this.spriteType = DrawManager.SpriteType.OmegaBoss1;
        this.logger = Core.getLogger();
        this.dashCooldown = new Cooldown(DASH_COOLDOWN_MS);

        // Initialize Apocalypse Pattern
        this.apocalypsePattern = new ApocalypseAttackPattern(this);

        this.logger.info("ZETA : Initializing Boss ZETA");

        choosePattern();
    }

    /**
     * Updates the entity's state for the current game frame.
     */
    @Override
    public void update() {
        choosePattern();

        if (bossPattern != null) {
            bossPattern.move();
            bossPattern.attack();

            // Update position from pattern
            this.positionX = bossPattern.getBossPosition().x;
            this.positionY = bossPattern.getBossPosition().y;
        }
    }

    /**
     * Chooses the appropriate pattern based on boss health
     */
    private void choosePattern() {

        // Do not execute the normal pattern logic below while the Apocalypse Pattern is active
        if (this.apocalypsePattern.isPatternActive()) {
            return;
        }
        // Trigger if HP is 50% or less, hasn't been used yet
        if (!this.hasUsedApocalypse && this.healPoint <= this.maxHp / 2) {
            if (this.bossPattern != this.apocalypsePattern) {
                this.bossPattern = this.apocalypsePattern; // Switch pattern
                this.apocalypsePattern.start(1); // Trigger start
                this.hasUsedApocalypse = true;
                logger.info("ZETA : Apocalypse Pattern Activated!");
                return;
            }
        }

        // Execute normal pattern logic if Apocalypse Pattern is finished or inactive

        // Pattern 1: Horizontal (HP > 50%)
        if (this.healPoint > this.maxHp / 2) {
            if (this.bossPhase < 2) {
                this.bossPhase = 2;
                bossPattern = new HorizontalPattern(this, PATTERN_1_X_SPEED);
                logger.info("ZETA : move using horizontal pattern");
            }
        }
        // Pattern 2: Diagonal (33% < HP <= 50%)
        else if (this.healPoint > this.maxHp / 3) {
            // Return to diagonal pattern immediately after Apocalypse
            if (this.bossPhase < 3 || this.bossPattern == this.apocalypsePattern) {
                this.bossPhase = 3;
                bossPattern = new DiagonalPattern(this, PATTERN_2_X_SPEED, PATTERN_2_Y_SPEED, PATTERN_2_COLOR);
                logger.info("ZETA : move using diagonal pattern");
            }
        }
        // Pattern 3: Dash (HP <= 33%)
        else {
            if (this.bossPhase < 4) {
                this.bossPhase = 4;
                startDashPattern();
            }
            handleDashCycle();
        }
    }

    /**
     * Handles the dash attack cycle in phase 3
     */
    private void handleDashCycle() {
        // Check if dash is completed
        if (bossPattern instanceof DashPattern) {
            DashPattern dashPattern = (DashPattern) bossPattern;
            if (dashPattern.isDashCompleted()) {
                startDashCooldown();
            }
        }
        // Check if cooldown is finished and ready for next dash
        else if (isInDashCooldown && dashCooldown.checkFinished()) {
            startDashPattern();
        }
    }

    private void startDashPattern() {
        bossPattern = new DashPattern(this, targetShip);
        isInDashCooldown = false;
        logger.info("ZETA : Starting dash attack");
    }

    private void startDashCooldown() {
        bossPattern = new DiagonalPattern(this, PATTERN_2_X_SPEED, PATTERN_2_Y_SPEED, PATTERN_2_COLOR);
        isInDashCooldown = true;
        dashCooldown.reset();
        logger.info("ZETA : Dash cooldown started (5 seconds)");
    }

    @Override
    public void move(int distanceX, int distanceY) {
        this.positionX += distanceX;
        this.positionY += distanceY;
    }

    @Override
    public void destroy() {
        this.isDestroyed = true;
        this.spriteType = DrawManager.SpriteType.OmegaBossDeath; // Reuse Omega death sprite
        this.logger.info("ZETA : Boss ZETA destroyed!");
    }

    @Override
    public void takeDamage(int damage) {
        this.healPoint -= damage;
    }

    public boolean isShowingPath() {
        if (bossPattern instanceof DashPattern) {
            return ((DashPattern) bossPattern).isShowingPath();
        }
        return false;
    }

    public int[] getDashEndPoint() {
        if (bossPattern instanceof DashPattern) {
            return ((DashPattern) bossPattern).getDashEndPoint(this.width, this.height);
        }
        return new int[]{this.positionX + this.width / 2, this.positionY + this.height / 2};
    }

    public BossPattern getBossPattern() {
        return this.bossPattern;
    }

    public int getBossPhase() {
        return this.bossPhase;
    }

    public boolean isInDashCooldown() {
        return isInDashCooldown;
    }

    public void setTarget(Ship target) {
        this.targetShip = target;
        if (bossPattern != null) {
            bossPattern.setTarget(target);
        }
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