package entity;

import audio.SoundManager;
import engine.Cooldown;
import engine.Core;
import engine.DrawManager;
import entity.pattern.BackgroundExplosionPattern;
import entity.pattern.GammaBossPattern;

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
 *
 * All pattern logic is delegated to {@link GammaBossPattern}.
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

    /** Screen dimensions */
    private final int screenWidth;
    private final int screenHeight;

    /** Boss pattern coordinator for delegating movement and attack logic */
    private GammaBossPattern bossPattern;
    /** Background explosion pattern for environmental hazards */
    private BackgroundExplosionPattern explosionPattern;
    /** Player ships reference for pattern targeting */
    private List<Ship> targetShips;
    /** Logger instance */
    private Logger logger;

    /** Animation cooldown for sprite changes */
    private Cooldown animationCooldown;
    /** Flag to track if boss was hit (for visual feedback) */
    private boolean ishit = false;

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
        this.spriteType = DrawManager.SpriteType.OmegaBoss1; // Using OmegaBoss sprite temporarily
        this.animationCooldown = new Cooldown(200);

        // Initialize pattern coordinator
        this.bossPattern = new GammaBossPattern(this, ships, screenWidth, screenHeight);

        // Initialize background explosion pattern
        this.explosionPattern = new BackgroundExplosionPattern();

        this.logger.info("GAMMA: Initializing Boss GAMMA");
        SoundManager.play("sfx/OmegaBossAppearance.wav");
    }

    /**
     * Updates the entity's state for the current game frame.
     * Handles animation, pattern updates, and position synchronization.
     */
    @Override
    public void update() {
        // Handle sprite animation
        if (this.animationCooldown.checkFinished()) {
            this.animationCooldown.reset();
            updateSprite();
        }

        // Update pattern coordinator
        if (bossPattern != null) {
            bossPattern.update();
            bossPattern.move();
            bossPattern.attack();

            // Update position from pattern
            this.positionX = bossPattern.getBossPosition().x;
            this.positionY = bossPattern.getBossPosition().y;
        }

        // Update background explosion pattern
        if (explosionPattern != null) {
            explosionPattern.attack();
        }
    }

    /**
     * Update sprite based on current phase.
     * Cycles between OmegaBoss1 and OmegaBoss2 for animation.
     * Shows hitting sprite when boss is hit.
     */
    private void updateSprite() {
        if (this.ishit) {
            this.spriteType = DrawManager.SpriteType.OmegaBossHitting;
            this.ishit = false;
        } else {
            if (this.spriteType == DrawManager.SpriteType.OmegaBoss1) {
                this.spriteType = DrawManager.SpriteType.OmegaBoss2;
            } else {
                this.spriteType = DrawManager.SpriteType.OmegaBoss1;
            }
        }
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
        ishit = true;

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
        return bossPattern != null && bossPattern.isShowingPath();
    }

    /**
     * Calculate dash end point for visualization.
     *
     * @return [x, y] array of dash end point
     */
    public int[] getDashEndPoint() {
        if (bossPattern != null) {
            return bossPattern.getDashEndPoint(this.width, this.height);
        }
        return new int[]{this.positionX + this.width / 2, this.positionY + this.height / 2};
    }

    /**
     * Get current boss pattern.
     *
     * @return Current GammaBossPattern instance
     */
    public GammaBossPattern getBossPattern() {
        return this.bossPattern;
    }

    /**
     * Get current boss phase.
     *
     * @return Current phase (1-3)
     */
    public int getBossPhase() {
        return bossPattern != null ? bossPattern.getCurrentPhase() : 1;
    }

    /**
     * Check if boss is in dash cooldown.
     *
     * @return true if in dash cooldown
     */
    public boolean isInDashCooldown() {
        return bossPattern != null && bossPattern.isInDashCooldown();
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

    /**
     * Get the current background explosion entity.
     *
     * @return Current Explosion entity, or null if none active
     */
    public Explosion getBoom() {
        return explosionPattern != null ? explosionPattern.getBoom() : null;
    }

    @Override
    public void onHitByPlayerBullet(Bullet bullet, GameModel model) {
        model.requestBossHitByPlayerBullet(bullet, this);
    }
}