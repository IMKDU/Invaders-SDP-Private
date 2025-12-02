package entity;

import java.awt.Color;
import java.util.Set;

import audio.SoundManager;
import engine.Cooldown;
import engine.DrawManager.SpriteType;

/**
 * Implements a sub-ship that follows the player and shoots.
 */
public class SubShip extends Entity {

    /** Owner ship to follow. */
    private final Ship owner;
    /** Stores whether this is the left sub-ship */
    private final boolean isLeft;
    /** Relative position offset from owner. */
    private final int offsetX;
    /** Creation time to track duration. */
    private long creationTime;
    /** Duration of the sub-ship in milliseconds (10 seconds). */
    private static final long DURATION = 10000L;
    /** Initial time between shots. */
    private static final int SHOOTING_INTERVAL = 750;
    /** Time between shots (same as basic ship). */
    private Cooldown shootingCooldown;

    private boolean isDestroyed;

    /**
     * Constructor.
     * @param owner The player ship to follow.
     * @param isLeft True if this is the left sub-ship, false for right.
     */
    public SubShip(Ship owner, boolean isLeft) {
        // Size is roughly half of the main ship (which is 13x8 * 2)
        super(0, 0, 13, 8, isLeft ? Color.CYAN : Color.ORANGE);
        this.owner = owner;
        this.isLeft = isLeft;
        if (owner.getPlayerId() == 1) {
            this.spriteType = SpriteType.ShipP1;
        } else {
            this.spriteType = SpriteType.ShipP2;
        }

        // Position offset: Left side or Right side
        int gap = 10;
        this.offsetX = isLeft ? -(this.width + gap) : (owner.getWidth() + gap);

        this.creationTime = System.currentTimeMillis();
        this.shootingCooldown = new Cooldown(SHOOTING_INTERVAL); // Basic shooting interval
        this.isDestroyed = false;

        updatePosition();
    }

    public boolean isLeft() {
        return this.isLeft;
    }

    // Reset creation time to current time (extends duration)
    public void resetCreationTime() {
        this.creationTime = System.currentTimeMillis();
    }

    // Get creation time (for finding the oldest sub-ship)
    public long getCreationTime() {
        return this.creationTime;
    }

    /**
     * Updates position relative to owner and checks duration.
     */
    public void update() {
        if (isDestroyed) return;

        // 1. Check Duration
        if (System.currentTimeMillis() - creationTime > DURATION) {
            this.destroy();
            return;
        }

        // 2. Follow Owner
        if (owner.isDestroyed()) {
            this.destroy();
        } else {
            updatePosition();
        }
    }

    public Ship getOwner() {
        return this.owner;
    }

    private void updatePosition() {
        this.positionX = owner.getPositionX() + offsetX;
        this.positionY = owner.getPositionY() + owner.getHeight() / 4; // Slightly centered Y
    }

    /**
     * Shoots bullets with half performance for Penetration and MultiShot, and full performance as owner for others.
     */
    public void shoot(final Set<Bullet> bullets) {
        if (isDestroyed) return;

        // 1. Rapid Fire - [Full application]
        // Get current upgraded cooldown from ShopItem.
        int currentInterval = ShopItem.getShootingInterval();

        // Update cooldown
        this.shootingCooldown.setMilliseconds(currentInterval);

        if (this.shootingCooldown.checkFinished()) {
            this.shootingCooldown.reset();

            // 2. Multi Shot - [Half application]
            int halfMultiShotLevel = ShopItem.getMultiShotLevel() / 2;
            int bulletCount = ShopItem.getMultiShotBulletCountForLevel(halfMultiShotLevel);
            int spacing = ShopItem.getMultiShotSpacingForLevel(halfMultiShotLevel);

            // 3. Bullet Speed - [Full application]
            // Get current upgraded speed from ShopItem.
            int speed = ShopItem.getBulletSpeed();

            // 4. Penetration - [Half application]
            int halfPenetrationLevel = ShopItem.getPenetrationLevel() / 2;
            int penetrationCount = ShopItem.getPenetrationCountForLevel(halfPenetrationLevel);

            int centerX = positionX + width / 2;
            int centerY = positionY;

            // --- Bullet firing logic (apply calculated values) ---
            int startOffset = -(bulletCount / 2) * spacing;

            for (int i = 0; i < bulletCount; i++) {
                int offsetX = startOffset + (i * spacing);
                Bullet b = BulletPool.getBullet(centerX + offsetX, centerY, speed);
                b.setOwnerId(owner.getPlayerId());
                b.setMaxPenetration(penetrationCount);
                bullets.add(b);
            }
        }
    }

    @Override
    public void onHitByEnemyBullet(Bullet bullet, GameModel model) {
        // Remove bullet
        model.requestRemoveBullet(bullet);

        // Play destruction sound
        SoundManager.play("sfx/impact.wav");

        // Destroy sub-ship (disappears immediately)
        this.destroy();
    }

    @Override
    public void onHitByBossBullet(BossBullet bullet, GameModel model) {
        // Remove bullet
        model.requestRemoveBossBullet(bullet);

        // Play destruction sound
        SoundManager.play("sfx/impact.wav");

        // // Destroy sub-ship (disappears immediately)
        this.destroy();
    }

    @Override
    public void onCollideWithEnemyShip(EnemyShip enemy, GameModel model) {
        // Play collision sound
        SoundManager.play("sfx/impact.wav");

        // Destroy sub-ship
        this.destroy();

        // (Note) Uncomment below if you want to destroy the enemy ship as well.
        // enemy.destroy();
    }

    @Override
    public void onCollideWithBoss(BossEntity boss, GameModel model) {
        // Play collision sound
        SoundManager.play("sfx/impact.wav");

        // Destroy sub-ship
        this.destroy();
    }

    public void destroy() {
        this.isDestroyed = true;
    }

    public boolean isDestroyed() {
        return isDestroyed;
    }
}