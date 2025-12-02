package entity;

import audio.SoundManager;
import engine.Cooldown;
import engine.DrawManager.SpriteType;
import entity.pattern.ISkill;

import java.awt.*;
import java.util.HashMap;
import java.util.Set;
import entity.skills.ChargingSkill;
import entity.skills.OriginSkill;

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
	/** Movement of the ship for each unit of time. */
	private static final int SPEED = 2;
	/** Y-offset from the ship position to the bullet spawn position */
	private static final int BULLET_SPAWN_Y_OFFSET = 36;

	/** Minimum time between shots. */
	private Cooldown shootingCooldown;
	/** Time spent inactive between hits. */
	private Cooldown destructionCooldown;
	/** Cooldown for the invincibility shield. */
	private Cooldown shieldCooldown;
	/** Checks if the ship is invincible. */
	private boolean isInvincible;
	private static final int TELEPORT_COOLDOWN_MS = 5000;
	// === [ADD] Which player: 1 = P1, 2 = P2 (default 1 for single-player compatibility) ===
    private int playerId = 1;
    private boolean isP1Ship;
    private boolean isMove;
    private boolean movingSoundPlaying = false;
	private int bombShotsRemaining = 0;
    private GameModel model;

    public void enableBomb(int count) {
        this.bombShotsRemaining = count;
    }
	// === Variable for Skil ===
	public enum SkillType {
		ORIGIN,
		CHARGE,
		DASH
	}
	private HashMap<SkillType, ISkill> skills;

    public void setPlayerId(int pid) { this.playerId = pid; }
    public int getPlayerId() { return this.playerId; }

	// === Charging Skill State ===
	// === Charging Skill Instance ===
	/** ChargingSkill instance that handles all charging logic */
	private ChargingSkill chargingSkill;

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


	public void move(String direction, int screenWidth, int screenHeight) {
		switch (direction) {
			case "RIGHT":
				if (positionX + width + SPEED <= screenWidth - 1)
					moveRight();
				break;

			case "LEFT":
				if (positionX - SPEED >= 1)
					moveLeft();
				break;

			case "UP":
				if (positionY - SPEED >= GameConstant.STAT_SEPARATION_LINE_HEIGHT)
					moveUp();
				break;

			case "DOWN":
				if (positionY + height + SPEED <= screenHeight)
					moveDown();
				break;
		}
	}

	public float getTeleportCooldownProgress() {
		if (teleportCooldown == null) return 1f;

		if (teleportCooldown.checkFinished()) return 1f;

		long now = System.currentTimeMillis();
		long passed = now - teleportCooldown.getStartTime();
		float ratio = (float) passed / teleportCooldown.getDuration();

		return Math.max(0f, Math.min(1f, ratio));
	}

	private Cooldown teleportCooldown = new Cooldown(TELEPORT_COOLDOWN_MS);
	private static final int TELEPORT_DISTANCE = 100;

	public boolean canTeleport() {
		return teleportCooldown.checkFinished();
	}

	public void teleport(String direction, int screenWidth, int screenHeight) {
		if (!canTeleport()) return;
        SoundManager.play("sfx/Teleport.wav");
		switch (direction) {
			case "RIGHT":
				this.positionX = Math.min(this.positionX + TELEPORT_DISTANCE, screenWidth - this.width - 1);
				break;

			case "LEFT":
				this.positionX = Math.max(this.positionX - TELEPORT_DISTANCE, 1);
				break;

			case "UP":
				this.positionY = Math.max(this.positionY - TELEPORT_DISTANCE, GameConstant.STAT_SEPARATION_LINE_HEIGHT);
				break;

			case "DOWN":
				this.positionY = Math.min(this.positionY + TELEPORT_DISTANCE, screenHeight - this.height - 1);
				break;
		}
		teleportCooldown.reset();

	}
	/**
	 * Shoots a bullet upwards.
	 *
	 * @param bullets
	 *            List of bullets on screen, to add the new bullet.
	 * @return Checks if the bullet was shot correctly.
	 */
	public final boolean shoot(final Set<Bullet> bullets) {
        if (controlsDisabled) return false;

        if (this.shootingCooldown.checkFinished()) {
			this.shootingCooldown.reset();

			// Get Spread Shot information from the DropItem class
			int bulletCount = ShopItem.getMultiShotBulletCount();
			int spacing = ShopItem.getMultiShotSpacing();

            int speed = ShopItem.getBulletSpeed();

			int centerX = positionX + this.width / 2;
			int centerY = positionY - BULLET_SPAWN_Y_OFFSET;

			if (bombShotsRemaining > 0) {

				Bullet b = new BombBullet(centerX, centerY, speed);
				b.setOwnerId(this.playerId);
				bullets.add(b);

				SoundManager.playPooled("sfx/laser.wav");

				bombShotsRemaining--;
				return true;
			}
            centerY = positionY;
			if (bulletCount == 1) {
				// Normal shot (when Spread Shot is not purchased)
				Bullet b = BulletPool.getBullet(centerX, centerY, speed);
				SoundManager.stop("sfx/laser.wav");
				SoundManager.playPooled("sfx/laser.wav");
				b.setOwnerId(this.playerId);  // === [ADD] Ownership flag: 1 = P1, 2 = P2, null for legacy logic ===

				bullets.add(b);
			} else {
				// Fire Spread Shot
				int startOffset = -(bulletCount / 2) * spacing;

				for (int i = 0; i < bulletCount; i++) {
					int offsetX = startOffset + (i * spacing);
					Bullet b = BulletPool.getBullet(centerX + offsetX, centerY, speed);
					b.setOwnerId(this.playerId);   // Ownership flag

					bullets.add(b);

                    // might consider putting a different sound
                    SoundManager.playPooled("sfx/laser.wav");
                }
			}
			return true;
		}
		return false;
	}

	/**
	 * Register xuser skills into skill map.
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
		// Update charging skill state
		this.chargingSkill.update();
    }
    /**
     * Register user skills into skill map.
     */

	private void registerSkills() {
		this.chargingSkill = new ChargingSkill();
		skills.put(SkillType.CHARGE, this.chargingSkill);
        skills.put(SkillType.ORIGIN, new OriginSkill());
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
        if (controlsDisabled) return;

        int shipspeed = ShopItem.getSHIPSpeedCOUNT();
		this.positionX += SPEED*(1+shipspeed/10);
        this.isMove = true;
	}

	/**
	 * Moves the ship speed units left, or until the left screen border is
	 * reached.
	 */
	public final void moveLeft() {
        if (controlsDisabled) return;

        int shipspeed = ShopItem.getSHIPSpeedCOUNT();
		this.positionX -= SPEED*(1+shipspeed/10);
        this.isMove = true;
	}

	/**
	 * Moves the ship speed units up, or until the SEPARATION_LINE_HEIGHT is
	 * reached.
	 */
	public final void moveUp() {
        if (controlsDisabled) return;

        int shipspeed = ShopItem.getSHIPSpeedCOUNT();
		this.positionY -= SPEED*(1+shipspeed/10);
        this.isMove = true;
	}

	/**
	 * Moves the ship speed units down, or until the down screen border is
	 * reached.
	 */
	public final void moveDown() {
        if (controlsDisabled) return;

        int shipspeed = ShopItem.getSHIPSpeedCOUNT();
		this.positionY += SPEED*(1+shipspeed/10);
        this.isMove = true;
	}


// === Charging Skill Methods (Delegation to ChargingSkill) ===

	/**
	 * Starts charging the skill if not on cooldown.
	 * Should be called when the player presses and holds the C key.
	 */
	public void startCharging() {
		if (this.chargingSkill != null) {
            chargingSkill.use(this);
			this.chargingSkill.startCharging();
		}
	}

	/**
	 * Stops charging the skill without firing.
	 * Should be called when the player releases the C key before fully charged.
	 */
	public void stopCharging() {
		if (this.chargingSkill != null) {
            chargingSkill.use(this);
			this.chargingSkill.stopCharging();
		}
	}

	/**
	 * Gets the current charge progress as a percentage (0.0 to 1.0).
	 * @return Charge progress percentage
	 */
	public double getChargeProgress() {
		if (this.chargingSkill != null) {
			return this.chargingSkill.getChargeProgress();
		}
		return 0.0;
	}

	/**
	 * Checks if the ship is currently charging the skill.
	 * @return True if charging
	 */
	public boolean isCharging() {
		if (this.chargingSkill != null) {
			return this.chargingSkill.isCharging();
		}
		return false;
	}

	/**
	 * Checks if the laser beam is currently active.
	 * @return True if laser is active
	 */
	public boolean isLaserActive() {
		if (this.chargingSkill != null) {
			return this.chargingSkill.isLaserActive();
		}
		return false;
	}

	/**
	 * Checks if the charging skill is ready to use (not on cooldown).
	 * @return True if skill is ready
	 */
	public boolean isChargingSkillReady() {
		if (this.chargingSkill != null) {
			return this.chargingSkill.isChargingSkillReady();
		}
		return false;
	}


	/**
	 * Gets the current cooldown progress as a percentage (0.0 to 1.0).
	 * @return Cooldown progress percentage (0.0 = ready, 1.0 = just used)
	 */
	public double getCooldownProgress() {
		if (this.chargingSkill != null) {
			return this.chargingSkill.getCooldownProgress();
		}
		return 0.0;
	}

    public void setModel(GameModel model){ this.model = model; }

    public GameModel getModel(){ return model; }


    private boolean controlsDisabled = false;

    public void disableAllControls(boolean b){
        this.controlsDisabled = b;
    }

    public boolean isControlsDisabled(){
        return this.controlsDisabled;
    }

    public ISkill getSkill(SkillType type) {
        return skills.get(type);
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
