package entity;

import audio.SoundManager;
import engine.Cooldown;
import engine.Core;
import engine.DrawManager;
import engine.Core;
import engine.Cooldown;
import entity.pattern.*;

import java.awt.*;
import java.util.List;
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
    private boolean ishit = false;
    private boolean isMove = false;
    private boolean isDash = false;
    private Cooldown animationCooldown;

	private SpawnMobPattern spawnPattern;

	/**
	 * Constructor, establishes the boss entity's generic properties.
	 *
	 * @param color             Color of the boss entity.
	 * @param player           The player ship to target
	 */
	public OmegaBoss(Color color, Ship player) {
		super(INIT_POS_X, INIT_POS_Y, OMEGA_WIDTH, OMEGA_HEIGHT, OMEGA_HEALTH, OMEGA_POINT_VALUE, color);
		this.targetShip = player;
		this.logger = Core.getLogger();
		this.dashCooldown = new Cooldown(DASH_COOLDOWN_MS);
        this.spriteType= DrawManager.SpriteType.OmegaBoss1;
        this.animationCooldown = new Cooldown(200);
		this.spawnPattern = new SpawnMobPattern(this,this.getHealPoint());
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
        if (this.animationCooldown.checkFinished()) {
            this.animationCooldown.reset();
            if (this.bossPhase == 2 || this.bossPhase == 3){
                this.setWidth(70 * 2);
                this.setHeight(51 * 2);
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
                if (this.spriteType == DrawManager.SpriteType.OmegaBossMoving1){
                    this.spriteType = DrawManager.SpriteType.OmegaBossMoving2;
                }
                else {
                    this.spriteType = DrawManager.SpriteType.OmegaBossMoving1;
                }
            }
            else {
                if (!isInDashCooldown){
                    this.setWidth(70 * 2);
                    this.setHeight(51 * 2);
                    if (this.isRight()){
                        if (this.spriteType == DrawManager.SpriteType.OmegaBossDash3) {
                            this.spriteType = DrawManager.SpriteType.OmegaBossDash4;
                        }
                        else {
                            this.spriteType = DrawManager.SpriteType.OmegaBossDash3;
                        }
                    }
                    else {
                        if (this.spriteType == DrawManager.SpriteType.OmegaBossDash1) {
                            this.spriteType = DrawManager.SpriteType.OmegaBossDash2;
                        }
                        else {
                            this.spriteType = DrawManager.SpriteType.OmegaBossDash1;
                        }
                    }
                }
                else {
                    this.setWidth(77 * 2);
                    this.setHeight(89 * 2);
                    if (this.spriteType == DrawManager.SpriteType.OmegaBossMoving1) {
                        this.spriteType = DrawManager.SpriteType.OmegaBossMoving2;
                    }
                    else {
                        this.spriteType = DrawManager.SpriteType.OmegaBossMoving1;
                    }

                }
            }


        }
		choosePattern();

		if (bossPattern != null) {
			bossPattern.move();
			bossPattern.attack();
			spawnPattern.update(this,this.getHealPoint());
			// Update position from pattern
			this.positionX = bossPattern.getBossPosition().x;
			this.positionY = bossPattern.getBossPosition().y;
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

		else if (this.healPoint <= this.maxHp / 2 && this.healPoint > this.maxHp / 3 && this.bossPhase == 3) {

			if (bossPattern instanceof SpreadShotPattern &&
					((SpreadShotPattern) bossPattern).isFinished()) {
				bossPattern = new DiagonalPattern(this, PATTERN_2_X_SPEED, PATTERN_2_Y_SPEED, PATTERN_2_COLOR);
				logger.info("OMEGA : SpreadShot finished → DIAGONAL pattern");

				++this.bossPhase;  // → 4
			}
		}
		else if (this.healPoint <= this.maxHp / 3 && this.bossPhase == 4) {
            ++this.bossPhase;
			// Start with dash pattern
			startDashPattern();
		}

		// Phase 3: Handle dash cooldown cycle
		if (this.bossPhase >= 5) {
			handleDashCycle();
		}
	}

	/**
	 * Handles the dash attack cycle in phase 3
	 * Alternates between dash attack and diagonal movement with 5-second cooldown
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
    private boolean isRight(){
        if (bossPattern instanceof DashPattern) {
            return ((DashPattern) bossPattern).getRightDash();
        }
        return true;
    }


	/**
	 * Start a new dash pattern
	 */
	private void startDashPattern() {
		bossPattern = new DashPattern(this, targetShip);
		isInDashCooldown = false;
		logger.info("OMEGA : Starting dash attack");
	}

	/**
	 * Start dash cooldown with diagonal movement
	 */
	private void startDashCooldown() {
		bossPattern = new DiagonalPattern(this, PATTERN_2_X_SPEED, PATTERN_2_Y_SPEED, PATTERN_2_COLOR);
		isInDashCooldown = true;
		dashCooldown.reset();
		logger.info("OMEGA : Dash cooldown started (5 seconds)");
	}

	/** move simple */
	@Override
	public void move(int distanceX, int distanceY) {
		this.positionX += distanceX;
		this.positionY += distanceY;
	}

	/** Marks the entity as destroyed and changes its sprite to an explosion. */
	@Override
	public void destroy() {
		this.isDestroyed = true;
		this.spriteType = DrawManager.SpriteType.OmegaBossDeath;
		this.logger.info("OMEGA : Boss OMEGA destroyed!");
		this.spawnPattern.clean();
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

    public boolean isShowingPath() {
        if (bossPattern instanceof DashPattern) {
            return ((DashPattern) bossPattern).isShowingPath();
        }
        return false;
    }

    /**
     * Calculate dash end point (by watching)
     * @return [x, y] array
     */
    public int[] getDashEndPoint() {
        if (bossPattern instanceof DashPattern) {
            return ((DashPattern) bossPattern).getDashEndPoint(this.width, this.height);
        }
        return new int[]{this.positionX + this.width / 2, this.positionY + this.height / 2};
    }

    /**
     * Get current boss pattern
     */
    public BossPattern getBossPattern() {
        return this.bossPattern;
    }

    /**
     * Get current boss phase
     */
    public int getBossPhase() {
        return this.bossPhase;
    }

    /**
     * Check if boss is in dash cooldown
     */
    public boolean isInDashCooldown() {
        return isInDashCooldown;
    }

    /**
     * Update target ship for pattern
     */
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

	public List<MidBossMob> getSpawnMobs() { return this.spawnPattern.getChildShips();}
}
