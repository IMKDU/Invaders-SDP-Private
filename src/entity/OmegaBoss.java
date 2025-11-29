package entity;

import audio.SoundManager;
import engine.Cooldown;
import engine.Core;
import engine.DrawManager;
import entity.pattern.*;

import java.awt.*;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Omega - Middle Boss
 */
public class OmegaBoss extends MidBoss {

	/** Initial position in the x-axis. */
	private static final int INIT_POS_X = 224;
	/** Initial position in the y-axis. */
	private static final int INIT_POS_Y = 80;
	/** Width of Omega */
    private static final int OMEGA_WIDTH = 70 * 2;
    /** Height of Omega */
    private static final int OMEGA_HEIGHT = 51 * 2;
	/** Current Health of Omega */
	private static final int OMEGA_HEALTH = 45;
	/** Point of Omega when destroyed */
	private static final int OMEGA_POINT_VALUE = 500;
	/** Speed of x in pattern 1 */
	private static final int PATTERN_1_X_SPEED = 1;
	/** Speed of x in pattern 2 */
	private static final int PATTERN_2_X_SPEED = 4;
	/** Speed of y in pattern 2 */
	private static final int PATTERN_2_Y_SPEED = 3;
	/** Color of pattern 2 */
	private static final Color PATTERN_2_COLOR = Color.MAGENTA;

	/** Boss pattern instance for delegating movement logic */
	private BossPattern bossPattern;
	/** Player reference for pattern targeting */
	private Ship targetShip;
	/** Current boss phase */
	private int bossPhase = 1;
	/** Logger instance */
	private Logger logger;
    private boolean ishit = false;
    private Cooldown animationCooldown;

	/**
	 * Constructor, establishes the boss entity's generic properties.
	 *
	 * @param player           The player ship to target
	 */
	public OmegaBoss(Ship player) {
		super(INIT_POS_X, INIT_POS_Y, OMEGA_WIDTH, OMEGA_HEIGHT, OMEGA_HEALTH, OMEGA_POINT_VALUE, null);
		this.targetShip = player;
		this.logger = Core.getLogger();
        this.spriteType= DrawManager.SpriteType.OmegaBoss1;
        this.animationCooldown = new Cooldown(200);
		this.logger.info("OMEGA : Initializing Boss OMEGA");
		this.logger.info("OMEGA : move using the default pattern");
        SoundManager.play("sfx/OmegaBossAppearance.wav");
		choosePattern();
	}

	/**
	 * Updates the entity's state for the current game frame.
	 * This method is called on every tick of the game loop and is responsible for
	 * executing the boss's movement patterns.
	 */
	@Override
	public void update() {
		chooseSprite();
		choosePattern();

		if (bossPattern != null) {
			bossPattern.move();
			bossPattern.attack();
			// Update position from pattern
			this.positionX = bossPattern.getBossPosition().x;
			this.positionY = bossPattern.getBossPosition().y;
		}
	}

	private void chooseSprite(){
		if (this.animationCooldown.checkFinished()) {
			this.animationCooldown.reset();
			if (this.bossPhase == 2 || this.bossPhase == 3){
				this.setWidth(OMEGA_WIDTH);
				this.setHeight(OMEGA_HEIGHT);
				if (this.ishit){
					this.spriteType = DrawManager.SpriteType.OmegaBossHitting;
					this.ishit = false;
				}
				else {
					if (this.spriteType == DrawManager.SpriteType.OmegaBoss1){
						this.spriteType = DrawManager.SpriteType.OmegaBoss2;
					}
					else {
						this.spriteType = DrawManager.SpriteType.OmegaBoss1;
					}
				}
			}
			else if (this.bossPhase == 4) {
				this.setWidth(77 * 2);
				this.setHeight(89 * 2);
				if (this.spriteType == DrawManager.SpriteType.OmegaBossMoving1) {
					this.spriteType = DrawManager.SpriteType.OmegaBossMoving2;
				} else {
					this.spriteType = DrawManager.SpriteType.OmegaBossMoving1;
				}
			}
		}
	}

	/**
	 * Chooses the appropriate pattern based on boss health
	 * Pattern 1: Simple horizontal movement (HP > 50%)
	 * Pattern 2: Diagonal movement (50% >= HP > 33%)
	 * Pattern 3: Dash attack with cooldown (HP <= 33%)
	 */
	private void choosePattern() {
		if (this.healPoint > this.maxHp / 2 && this.bossPhase == 1) {
			++this.bossPhase;
			bossPattern = new HorizontalPattern(this, PATTERN_1_X_SPEED);
			logger.info("OMEGA : move using horizontal pattern");
		}
		// PHASE 2 → SpreadShotPattern
		else if (this.healPoint <= this.maxHp / 2 && this.healPoint > this.maxHp / 3 && this.bossPhase == 2) {
            bossPattern = new SpreadShotPattern(this, targetShip);
			logger.info("OMEGA : Using SPREAD SHOT pattern");
			this.bossPhase = 3;
			return;
		}

		else if (this.bossPhase == 3) {

			if (bossPattern instanceof SpreadShotPattern &&
					((SpreadShotPattern) bossPattern).isFinished()) {
				bossPattern = new DiagonalPattern(this, PATTERN_2_X_SPEED, PATTERN_2_Y_SPEED, PATTERN_2_COLOR);
				logger.info("OMEGA : SpreadShot finished → DIAGONAL pattern");

				++this.bossPhase;  // → 4
			}
		}
	}

	/** move simple */
	@Override
	public void move(int distanceX, int distanceY) {
		this.positionX += distanceX;
		this.positionY += distanceY;
	}

	/** Marks the entity as destroyed and changes its sprite to

	private SpawnMobPattern spawnPattern; an explosion. */
	@Override
	public void destroy() {
		this.isDestroyed = true;
		this.spriteType = DrawManager.SpriteType.OmegaBossDeath;
		this.logger.info("OMEGA : Boss OMEGA destroyed!");
	}

	/**
	 * Reduces health and destroys the entity if it drops to zero or below.
	 *
	 * @param damage The amount of damage to inflict.
	 */
	@Override
	public void takeDamage(int damage) {
		this.healPoint -= damage;
        SoundManager.play("sfx/OmegaBoss_hitting.wav");
        ishit =true;
	}

	public Set<Bullet> getBullets() {
		return this.bossPattern.getBullets();
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
