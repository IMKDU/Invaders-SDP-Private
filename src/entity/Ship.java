package entity;

import audio.SoundManager;
import engine.Cooldown;
import engine.DrawManager.SpriteType;
import entity.pattern.ISkill;

import java.awt.*;
import java.util.HashMap;
import java.util.Set;

/**
 * Implements a ship, to be controlled by the player.
 *
 * @author <a href="mailto:RobertoIA1987@gmail.com">Roberto Izquierdo Amo</a>
 *
 */
public class Ship extends Entity implements Collidable {

	// === Constants ===
	/** Time between shots. */
	private static final int SHOOTING_INTERVAL = 750;
	/** Speed of the bullets shot by the ship. */
	private static final int BULLET_SPEED = -6;
	/** Movement of the ship for each unit of time. */
	private static final int SPEED = 2;

	/** Minimum time between shots. */
	private Cooldown shootingCooldown;
	/** Time spent inactive between hits. */
	private Cooldown destructionCooldown;
	/** Cooldown for the invincibility shield. */
	private Cooldown shieldCooldown;
	/** Checks if the ship is invincible. */
	private boolean isInvincible;
    // === [ADD] Which player: 1 = P1, 2 = P2 (default 1 for single-player compatibility) ===
    private int playerId = 1;
    private boolean isP1Ship;
    private boolean isMove;
    private boolean movingSoundPlaying = false;

	// === Variable for Skil ===
	public enum SkillType {
		ORIGIN,
		CHARGE,
		DASH
	}
	private HashMap<SkillType, ISkill> skills;

    public void setPlayerId(int pid) { this.playerId = pid; }
    public int getPlayerId() { return this.playerId; }

	/**
	 * Constructor, establishes the ship's properties.
	 *
	 * @param positionX
	 *            Initial position of the ship in the X axis.
	 * @param positionY
	 *            Initial position of the ship in the Y axis.
	 */
	public Ship(final int positionX, final int positionY,final Color color,final boolean isP1Ship) {
        super(positionX, positionY, 25 * 2, 31 * 2, color);
        if (isP1Ship){
            this.spriteType = SpriteType.ShipP1;
            this.isP1Ship = true;
        }
        else {
            this.spriteType = SpriteType.ShipP2;
            this.isP1Ship = false;
        }

		this.shootingCooldown = new Cooldown(ShopItem.getShootingInterval());
		this.destructionCooldown = new Cooldown(1000);
		this.shieldCooldown = new Cooldown(0);
		this.isInvincible = false;

		this.skills = new HashMap<SkillType, ISkill>();
		registerSkills();
	}


	/**
	 * Shoots a bullet upwards.
	 *
	 * @param bullets
	 *            List of bullets on screen, to add the new bullet.
	 * @return Checks if the bullet was shot correctly.
	 */
	public final boolean shoot(final Set<Bullet> bullets) {
		if (this.shootingCooldown.checkFinished()) {
			this.shootingCooldown.reset();

			// Get Spread Shot information from the DropItem class
			int bulletCount = ShopItem.getMultiShotBulletCount();
			int spacing = ShopItem.getMultiShotSpacing();

			int centerX = positionX + this.width / 2;
			int centerY = positionY;
			if (bulletCount == 1) {
				// Normal shot (when Spread Shot is not purchased)
				Bullet b = BulletPool.getBullet(centerX, centerY, BULLET_SPEED);
                SoundManager.play("sfx/laser.wav");
                b.setOwnerId(this.playerId);  // === [ADD] Ownership flag: 1 = P1, 2 = P2, null for legacy logic ===

				bullets.add(b);
			} else {
				// Fire Spread Shot
				int startOffset = -(bulletCount / 2) * spacing;

				for (int i = 0; i < bulletCount; i++) {
					int offsetX = startOffset + (i * spacing);
					Bullet b = BulletPool.getBullet(centerX + offsetX, centerY, BULLET_SPEED);
					b.setOwnerId(this.playerId);   // Ownership flag

					bullets.add(b);

                    // might consider putting a different sound
                    SoundManager.play("sfx/laser.wav");
                }
			}
			return true;
		}
		return false;
	}

	/**
	 * Register user skills into skill map.
	 */
    public final void update() {
        if (this.isInvincible && this.shieldCooldown.checkFinished()) {
            this.isInvincible = false;
        }
        if (!this.destructionCooldown.checkFinished()) {
            double ratio = this.destructionCooldown.getRemaining() / (double) this.destructionCooldown.getTotal();
            SpriteType explosion1 = this.isP1Ship ? SpriteType.ShipP1Explosion1 : SpriteType.ShipP2Explosion1;
            SpriteType explosion2 = this.isP1Ship ? SpriteType.ShipP1Explosion2 : SpriteType.ShipP2Explosion2;
            SpriteType explosion3 = this.isP1Ship ? SpriteType.ShipP1Explosion3 : SpriteType.ShipP2Explosion3;

            if (ratio > 0.6) {
                this.spriteType = explosion1;
            } else if (ratio > 0.3) {
                this.spriteType = explosion2;
            } else {
                this.spriteType = explosion3;
            }
        } else {
            SpriteType moveSprite = this.isP1Ship ? SpriteType.ShipP1Move : SpriteType.ShipP2Move;
            SpriteType idleSprite = this.isP1Ship ? SpriteType.ShipP1 : SpriteType.ShipP2;

            if (this.isMove) {
                this.spriteType = moveSprite;
                if (!movingSoundPlaying) {
                    SoundManager.playSingleLoop("sfx/ShipMoving.wav");
                    movingSoundPlaying = true;
                }
                this.isMove = false;
            } else {
                this.spriteType = idleSprite;
                if (movingSoundPlaying) {
                    SoundManager.stopSingleLoop("sfx/ShipMoving.wav");
                    movingSoundPlaying = false;
                }
            }
        }
    }
    /**
     * Register user skills into skill map.
     */
    private void registerSkills() {

    }
	/**
	 * Called in GameScreen.java to use skills for each situation.
	 */
	public void useSkill(final SkillType skillType) {
		ISkill skill = skills.get(skillType);
		if (skill != null) {
			skill.use(this);
		}
	}

    /**
     * Activates the ship's invincibility shield for a given duration.
     *
     * @param duration
     *            Duration of the invincibility in milliseconds.
     */
    public final void activateInvincibility(final int duration) {
        this.isInvincible = true;
        this.shieldCooldown.setMilliseconds(duration);
        this.shieldCooldown.reset();
    }
    public final void destroy() {
        if (!this.isInvincible) {
            SoundManager.stopSingleLoop("sfx/ShipMoving.wav");
            SoundManager.play("sfx/destroy.wav");
            this.destructionCooldown.reset();
        }
    }

	/**
	 * Checks if the ship is destroyed.
	 *
	 * @return True if the ship is currently destroyed.
	 */
	public final boolean isDestroyed() {
		return !this.destructionCooldown.checkFinished();
	}

	/**
	 * Getter for the ship's speed.
	 *
	 * @return Speed of the ship.
	 */
	public final int getSpeed() {
		return SPEED;
	}

	/**
	 * Getter for the ship's invincibility state.
	 *
	 * @return True if the ship is currently invincible.
	 */
	public final boolean isInvincible() {
		return this.isInvincible || this.isDestroyed();
	}


    public float getInvincibilityRatio() {
        if (!isInvincible || shieldCooldown.getTotal() == 0) return 0f;
        float ratio = (float) shieldCooldown.getRemaining() / shieldCooldown.getTotal();
        return ratio;
    }
	/**
	 * Moves the ship speed uni ts right, or until the right screen border is
	 * reached.
	 */
	public final void moveRight() {
		int shipspeed = ShopItem.getSHIPSpeedCOUNT();
		this.positionX += SPEED*(1+shipspeed/10);
        this.isMove = true;
	}

	/**
	 * Moves the ship speed units left, or until the left screen border is
	 * reached.
	 */
	public final void moveLeft() {
		int shipspeed = ShopItem.getSHIPSpeedCOUNT();
		this.positionX -= SPEED*(1+shipspeed/10);
        this.isMove = true;
	}

	/**
	 * Moves the ship speed units up, or until the SEPARATION_LINE_HEIGHT is
	 * reached.
	 */
	public final void moveUp() {
		int shipspeed = ShopItem.getSHIPSpeedCOUNT();
		this.positionY -= SPEED*(1+shipspeed/10);
        this.isMove = true;
	}

	/**
	 * Moves the ship speed units down, or until the down screen border is
	 * reached.
	 */
	public final void moveDown() {
		int shipspeed = ShopItem.getSHIPSpeedCOUNT();
		this.positionY += SPEED*(1+shipspeed/10);
        this.isMove = true;
	}

	@Override
	public void onCollision(Collidable other, GameModel model) {
		if (model.isLevelFinished()) return;
		other.onCollideWithShip(this, model);
	}

	@Override
	public void onHitByEnemyBullet(Bullet bullet, GameModel model) {
		if (!this.isInvincible()) {
			model.requestShipDamage(this, 1);
		}
		model.requestRemoveBullet(bullet);
	}

	@Override
	public void onHitByBossBullet(BossBullet b, GameModel model) {
		if (!this.isInvincible()) {
			model.requestShipDamage(this, 1);
		}
		model.requestRemoveBossBullet(b);
	}

	@Override
	public void onCollideWithEnemyShip(EnemyShip enemy, GameModel model) {
		model.requestPlayerCrash(this, enemy);
	}

	@Override
	public void onCollideWithBoss(BossEntity boss, GameModel model) {
		model.requestPlayerCrash(this, (Entity) boss);
	}

	@Override
	public void onCollideWithDropItem(DropItem item, GameModel model) {
		model.requestApplyItem(this, item);
	}
}
