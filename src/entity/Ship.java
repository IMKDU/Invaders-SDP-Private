package entity;

import audio.SoundManager;
import engine.Cooldown;
import engine.DrawManager.SpriteType;
import entity.pattern.ISkill;

import java.awt.*;
import java.util.HashMap;
import java.util.Set;
import entity.pattern.skills.ChargingSkill;

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

    // === Which player: 1 = P1, 2 = P2 (default 1 for single-player compatibility) ===
	private int playerId = 1;

	// === Variable for Skil ===
	public enum SkillType {
		ORIGIN,
		CHARGE,
		DASH
	}
	private HashMap<SkillType, ISkill> skills;

	// === Charging Skill State ===
	/** Indicates if the ship is currently charging the skill */
	private boolean isCharging = false;
	/** Time when charging started (in milliseconds) */
	private long chargeStartTime = 0;
	/** Indicates if the laser beam is currently active */
	private boolean isLaserActive = false;
	/** Time when the laser was fired (in milliseconds) */
	private long laserStartTime = 0;
	/** Last time the charging skill was used (for cooldown tracking) */
	private long lastChargingSkillUse = 0;
	/** Duration required to fully charge the skill (in milliseconds) */
	private static final long CHARGE_DURATION = 5000;
	/** Duration of the laser beam after firing (in milliseconds) */
	private static final long LASER_DURATION = 1500;
	/** Cooldown duration after using the skill (in milliseconds) */
	private static final long COOLDOWN_DURATION = 0;

	/**
	 * Constructor, establishes the ship's properties.
	 *
	 * @param positionX
	 *            Initial position of the ship in the X axis.
	 * @param positionY
	 *            Initial position of the ship in the Y axis.
	 */
	public Ship(final int positionX, final int positionY, final Color color) {
		super(positionX, positionY, 13 * 2, 8 * 2, color);

		this.spriteType = SpriteType.Ship;
		this.shootingCooldown = new Cooldown(ShopItem.getShootingInterval());
		this.destructionCooldown = new Cooldown(1000);
		this.shieldCooldown = new Cooldown(0);
		this.isInvincible = false;

		this.skills = new HashMap<SkillType, ISkill>();
		registerSkills();
	}

	/**
	 * Updates status of the ship.
	 */
	public final void update() {
        if (this.isInvincible && this.shieldCooldown.checkFinished()) {
            this.isInvincible = false;
            this.setColor(Color.GREEN);
        }

        if (!this.destructionCooldown.checkFinished())
            this.spriteType = SpriteType.ShipDestroyed;
        else
            this.spriteType = SpriteType.Ship;

		// Update charging skill state
		updateChargingSkill();
	}

	/**
	 * Updates the charging skill state, handling charge completion and laser duration.
	 */
	private void updateChargingSkill() {
		long currentTime = System.currentTimeMillis();

		// Check if charging is complete (5 seconds)
		if (isCharging && (currentTime - chargeStartTime) >= CHARGE_DURATION) {
			// Automatically fire the laser when fully charged
			fireLaser();
		}

		// Check if laser duration has expired (1.5 seconds)
		if (isLaserActive && (currentTime - laserStartTime) >= LASER_DURATION) {
			isLaserActive = false;
		}
	}




	/**
	 * Switches the ship to its destroyed state.
	 */
	public final void destroy() {
        if (!this.isInvincible) {
			SoundManager.stop("sfx/impact.wav");
            SoundManager.play("sfx/impact.wav");
            this.destructionCooldown.reset();
        }
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
				SoundManager.stop("sfx/laser.wav");
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
					SoundManager.stop("sfx/laser.wav");
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
	private void registerSkills() {
		skills.put(SkillType.CHARGE, new ChargingSkill());
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
		this.setColor(Color.BLUE);
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

	/**
	 * Moves the ship speed uni ts right, or until the right screen border is
	 * reached.
	 */
	public final void moveRight() {
		int shipspeed = ShopItem.getSHIPSpeedCOUNT();
		this.positionX += SPEED*(1+shipspeed/10);
	}

	/**
	 * Moves the ship speed units left, or until the left screen border is
	 * reached.
	 */
	public final void moveLeft() {
		int shipspeed = ShopItem.getSHIPSpeedCOUNT();
		this.positionX -= SPEED*(1+shipspeed/10);
	}

	/**
	 * Moves the ship speed units up, or until the SEPARATION_LINE_HEIGHT is
	 * reached.
	 */
	public final void moveUp() {
		int shipspeed = ShopItem.getSHIPSpeedCOUNT();
		this.positionY -= SPEED*(1+shipspeed/10);
	}

	/**
	 * Moves the ship speed units down, or until the down screen border is
	 * reached.
	 */
	public final void moveDown() {
		int shipspeed = ShopItem.getSHIPSpeedCOUNT();
		this.positionY += SPEED*(1+shipspeed/10);
	}

	public void setPlayerId(int pid) { this.playerId = pid; }
	public int getPlayerId() { return this.playerId; }


	// === Charging Skill Methods ===

	/**
	 * Starts charging the skill if not on cooldown.
	 * Should be called when the player presses and holds the C key.
	 */
	public void startCharging() {
		long currentTime = System.currentTimeMillis();

		// Check if skill is on cooldown
		if (currentTime - lastChargingSkillUse < COOLDOWN_DURATION) {
			return; // Still on cooldown, can't charge
		}

		// Don't allow charging if laser is already active
		if (isLaserActive) {
			return;
		}

		// Start charging
		if (!isCharging) {
			isCharging = true;
			chargeStartTime = currentTime;
		}
	}

	/**
	 * Stops charging the skill without firing.
	 * Should be called when the player releases the C key before fully charged.
	 */
	public void stopCharging() {
		if (isCharging) {
			isCharging = false;
			chargeStartTime = 0;
		}
	}

	/**
	 * Fires the laser beam when fully charged.
	 * Called automatically when charge duration is complete.
	 */
	private void fireLaser() {
		if (!isCharging) {
			return;
		}

		// Stop charging and activate laser
		isCharging = false;
		isLaserActive = true;
		laserStartTime = System.currentTimeMillis();
		lastChargingSkillUse = laserStartTime;

		// Play sound effect (optional)
		SoundManager.stop("sfx/laser.wav");
		SoundManager.play("sfx/laser.wav");
	}

	/**
	 * Gets the current charge progress as a percentage (0.0 to 1.0).
	 * @return Charge progress percentage
	 */
	public double getChargeProgress() {
		if (!isCharging) {
			return 0.0;
		}

		long currentTime = System.currentTimeMillis();
		long elapsedTime = currentTime - chargeStartTime;
		double progress = (double) elapsedTime / CHARGE_DURATION;

		return Math.min(progress, 1.0);
	}

	/**
	 * Gets the current cooldown progress as a percentage (0.0 to 1.0).
	 * @return Cooldown progress percentage (0.0 = ready, 1.0 = just used)
	 */
	public double getCooldownProgress() {
		long currentTime = System.currentTimeMillis();
		long timeSinceUse = currentTime - lastChargingSkillUse;

		if (timeSinceUse >= COOLDOWN_DURATION) {
			return 0.0; // Skill is ready
		}

		double remaining = 1.0 - ((double) timeSinceUse / COOLDOWN_DURATION);
		return remaining;
	}

	/**
	 * Checks if the ship is currently charging the skill.
	 * @return True if charging
	 */
	public boolean isCharging() {
		return isCharging;
	}

	/**
	 * Checks if the laser beam is currently active.
	 * @return True if laser is active
	 */
	public boolean isLaserActive() {
		return isLaserActive;
	}

	/**
	 * Checks if the charging skill is ready to use (not on cooldown).
	 * @return True if skill is ready
	 */
	public boolean isChargingSkillReady() {
		long currentTime = System.currentTimeMillis();
		return (currentTime - lastChargingSkillUse) >= COOLDOWN_DURATION;
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
