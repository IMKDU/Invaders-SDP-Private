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
 *
 * <p>
 * Controls animation, sprite transitions, damage handling, and delegates
 * movement/attack logic to {@link OmegaBossPattern}.
 * </p>
 */
public class OmegaBoss extends MidBoss {

	/** Initial position in the x-axis. */
	private static final int INIT_POS_X = 224;
	/** Initial position in the y-axis. */
	private static final int INIT_POS_Y = 160;
	/** Width of Omega */
    private static final int OMEGA_WIDTH = 70 * 2;
    /** Height of Omega */
    private static final int OMEGA_HEIGHT = 51 * 2;
	/** Width of Omega */
	private static final int OMEGA_WIDTH_ANGRY = 77 * 2;
	/** Height of Omega */
	private static final int OMEGA_HEIGHT_ANGRY = 89 * 2;
	/** Current Health of Omega */
	private static final int OMEGA_HEALTH = 45;
	/** Point of Omega when destroyed */
	private static final int OMEGA_POINT_VALUE = 500;

	/** Pattern manager for Omega's movement and attack logic. */
	private OmegaBossPattern omegaBossPattern;
	/** Logger instance */
	private Logger logger;
	/** Flag indicating if boss was recently hit (for flashing animation). */
    private boolean ishit = false;
	/** Cooldown for sprite animation switching. */
    private Cooldown animationCooldown;

	/**
	 * Constructor, establishes the boss entity's generic properties.
	 *
	 * @param player           The player ship to target
	 */
	public OmegaBoss(Ship player) {
		super(INIT_POS_X, INIT_POS_Y, OMEGA_WIDTH, OMEGA_HEIGHT, OMEGA_HEALTH, OMEGA_POINT_VALUE, null);
		this.logger = Core.getLogger();
        this.spriteType= DrawManager.SpriteType.OmegaBoss1;
        this.animationCooldown = new Cooldown(200);
		this.omegaBossPattern = new OmegaBossPattern(this, player);
		this.logger.info("OMEGA : Initializing Boss OMEGA");
		this.logger.info("OMEGA : move using the default pattern");
        SoundManager.play("sfx/OmegaBossAppearance.wav");
	}

	/**
	 * Updates the entity's state for the current game frame.
	 * <p>
	 * This method:
	 * <ol>
	 *     <li>Chooses the correct sprite based on animation state and phase.</li>
	 *     <li>Updates the pattern logic ({@link OmegaBossPattern#update()}).</li>
	 *     <li>Executes pattern movement and attacks.</li>
	 *     <li>Synchronizes boss coordinates with the pattern.</li>
	 * </ol>
	 * </p>
	 */
	@Override
	public void update() {
		chooseSprite();
		omegaBossPattern.update();
		omegaBossPattern.move();
		omegaBossPattern.attack();
		this.positionX = omegaBossPattern.getBossPosition().x;
		this.positionY = omegaBossPattern.getBossPosition().y;
	}

	/**
	 * Selects the current sprite based on:
	 * <ul>
	 *     <li>Phase (normal vs angry)</li>
	 *     <li>Hit animation</li>
	 *     <li>Idle animation cycle every 200 ms</li>
	 * </ul>
	 *
	 * <p>
	 * Also resizes the boss sprite when entering the angry phase.
	 * </p>
	 */
	private void chooseSprite(){
		if (this.animationCooldown.checkFinished()) {
			this.animationCooldown.reset();
			if (omegaBossPattern.checkPhase() == 1 || omegaBossPattern.checkPhase() == 2){
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
			else if (omegaBossPattern.checkPhase() == 3) {
				this.setWidth(OMEGA_WIDTH_ANGRY);
				this.setHeight(OMEGA_HEIGHT_ANGRY);
				if (this.spriteType == DrawManager.SpriteType.OmegaBossMoving1) {
					this.spriteType = DrawManager.SpriteType.OmegaBossMoving2;
				} else {
					this.spriteType = DrawManager.SpriteType.OmegaBossMoving1;
				}
			}
		}
	}

	/**
	 * Simple position-based movement. Mostly used by pattern logic.
	 *
	 * @param distanceX Movement offset on x-axis.
	 * @param distanceY Movement offset on y-axis.
	 */
	@Override
	public void move(int distanceX, int distanceY) {
		this.positionX += distanceX;
		this.positionY += distanceY;
	}

	/** Marks the entity as destroyed and changes its sprite to an explosion. */
	@Override
	public void destroy() {
		if(!this.isDestroyed){
			this.isDestroyed = true;
			this.spriteType = DrawManager.SpriteType.OmegaBossDeath;
			this.logger.info("OMEGA : Boss OMEGA destroyed!");
		}
	}

	@Override
	public void takeDamage(int damage) {
		this.healPoint -= damage;
        SoundManager.play("sfx/OmegaBoss_hitting.wav");
        ishit =true;
	}

	/**
	 * Returns all bullets fired by the boss pattern.
	 *
	 * @return Set of active bullets created by {@link OmegaBossPattern}.
	 */
	public Set<Bullet> getBullets() {
		return this.omegaBossPattern.getBullets();
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
