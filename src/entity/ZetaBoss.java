package entity;

import audio.SoundManager;
import engine.Core;
import engine.Cooldown;
import engine.DrawManager;
import entity.pattern.ZetaBossPattern;

import java.awt.*;
import java.util.List;
import java.util.logging.Logger;

/**
 * Zeta - Mid Boss with complex phase-based pattern cycles
 *
 * Features 4 attack patterns with health-based phase transitions:
 * - BlackHolePattern: Random position, phase-specific cooldowns
 * - DiagonalPattern: Random diagonal movement
 * - ApocalypseAttackPattern: 3s charging + 2s attack
 * - ZigZagAngryPattern: ZigZag with rapid fire attacks
 *
 * Phase System:
 * - Phase 1 (100-70%): BlackHole → Apocalypse → Diagonal
 * - Phase 2 (70-40%): BlackHole → Apocalypse → ZigZag Angry
 * - Phase 3 (40-0%): BlackHole → Apocalypse → Random(Diagonal or ZigZag)
 *
 * All pattern logic is delegated to {@link ZetaBossPattern}.
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

    /** Boss pattern coordinator for delegating movement and attack logic */
    private ZetaBossPattern bossPattern;
    /** Logger instance */
    private Logger logger;
    /** Animation cooldown */
    private Cooldown animationCooldown;

    /**
     * Constructor, establishes the boss entity's generic properties.
     *
     * @param color  Color of the boss entity.
     * @param player The player ship to target
     * @param ships  List of ships for BlackHolePattern
     */
    public ZetaBoss(Color color, Ship player, List<Ship> ships) {
        super(INIT_POS_X, INIT_POS_Y, ZETA_WIDTH, ZETA_HEIGHT, ZETA_HEALTH, ZETA_POINT_VALUE, color);
        this.spriteType = DrawManager.SpriteType.ZetaBoss1;
        this.logger = Core.getLogger();
        this.animationCooldown = new Cooldown(200);

        // Initialize pattern coordinator
        this.bossPattern = new ZetaBossPattern(this, ships);

        this.logger.info("ZETA: Initializing Boss ZETA");
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
    }

    /**
     * Updates the sprite based on current phase and pattern
     */
    private void updateSprite() {
        // Check if we're in a movement pattern (Diagonal or ZigZag)
        if (bossPattern != null && bossPattern.getActivePattern() instanceof entity.pattern.DiagonalPattern) {
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
     * Checks if the diagonal pattern is moving right
     */
    private boolean isDiagonalRight() {
        if (bossPattern != null && bossPattern.getActivePattern() instanceof entity.pattern.DiagonalPattern) {
            return ((entity.pattern.DiagonalPattern) bossPattern.getActivePattern()).getIsRight();
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
        SoundManager.playPooled("sfx/ZetaBoss_hitting.wav");
        if(this.healPoint <= 0) {
            this.destroy();
        }
    }

    /**
     * Get current boss pattern coordinator.
     *
     * @return Current ZetaBossPattern instance
     */
    public ZetaBossPattern getBossPattern() {
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
     * Get current BlackHole pattern if active.
     *
     * @return Current BlackHolePattern instance, or null if not active
     */
    @Override
    public entity.pattern.BlackHolePattern getCurrentBlackHole() {
        return bossPattern != null ? bossPattern.getCurrentBlackHole() : null;
    }

    /**
     * Get Apocalypse pattern instance.
     *
     * @return ApocalypseAttackPattern instance
     */
    @Override
    public entity.pattern.ApocalypseAttackPattern getApocalypsePattern() {
        return bossPattern != null ? bossPattern.getApocalypsePattern() : null;
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
