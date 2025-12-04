package entity;

import audio.SoundManager;
import engine.Cooldown;
import engine.Core;
import engine.DrawManager;
import entity.pattern.*;
import entity.Ship;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import java.awt.*;
import java.util.Set;
import java.util.logging.Logger;

public class FinalBoss extends MidBoss {

	/** Initial position in the x-axis. */
	private static final int INIT_POS_X = GameConstant.SCREEN_WIDTH / 2 - 150;
	/** Initial position in the y-axis. */
	private static final int INIT_POS_Y = 80;
	/** Width of Noxis */
	private static final int NOXIS_WIDTH = 150 * 2;
	/** Height of Noxis */
	private static final int NOXIS_HEIGHT = 143 * 2;
//	/** Width of Noxis */
//	private static final int NOXIS_WIDTH_ANGRY = 77 * 2;
//	/** Height of Noxis */
//	private static final int NOXIS_HEIGHT_ANGRY = 89 * 2;
	/** Current Health of Noxis */
	private static final int NOXIS_HEALTH = 80;
	/** Point of Noxis when destroyed */
	private static final int NOXIS_POINT_VALUE = 1000;

    private Cooldown animationCooldown;

	private NoxisBossPattern noxisBossPattern;

	private Logger logger;

	/** basic attribute of final boss */

    public FinalBoss(List<Ship> ships){

        super(INIT_POS_X, INIT_POS_Y, NOXIS_WIDTH, NOXIS_HEIGHT, NOXIS_HEALTH, NOXIS_POINT_VALUE, null);
	    logger = Core.getLogger();
	    this.spriteType = DrawManager.SpriteType.FinalBoss1;
        this.animationCooldown = new Cooldown(500);
	    this.noxisBossPattern = new NoxisBossPattern(this, ships);
	}

    /** for vibrant moving with final boss
     * final boss spritetype is the same with special enemy and enemyshipA, because final boss spritetype have not yet implemented
     * becasue final boss is single object, moving and shooting pattern are included in update methods
     */
    @Override
    public void update(){

		chooseSprite();

		if(noxisBossPattern!=null){
			noxisBossPattern.update();

			this.positionX = noxisBossPattern.getBossPosition().x;
			this.positionY = noxisBossPattern.getBossPosition().y;
		}
    }

	private void chooseSprite(){
		if(this.animationCooldown.checkFinished()){
			this.animationCooldown.reset();

			switch (this.spriteType) {
				case FinalBoss1:
					this.spriteType = DrawManager.SpriteType.FinalBoss2;
					break;
				case FinalBoss2:
					this.spriteType = DrawManager.SpriteType.FinalBoss1;
					break;
			}
		}
	}

    /** decrease boss' healpoint */
    @Override
    public void takeDamage(int damage){
        this.healPoint -= damage;
        SoundManager.play("sfx/pikachu.wav");
        if(this.healPoint <= 0){
            this.destroy();
        }
    }

    /** move simple */
    @Override
    public void move(int distanceX, int distanceY){
        this.positionX += distanceX;
        this.positionY += distanceY;
    }

    /** flag final boss' destroy */
    @Override
    public void destroy(){
        if(!this.isDestroyed){
            this.spriteType = DrawManager.SpriteType.FinalBossDeath;
            this.isDestroyed = true;
	        this.logger.info("Noxis : Boss NOXIS destroyed!");
        }
    }

	public Set<Bullet> getBullets() { return this.noxisBossPattern.getBullets(); }

	public Set<LaserBeam> getLasers() { return this.noxisBossPattern.getLasers(); }

	public Set<BlackHole> getBlackHoles() {
		if(noxisBossPattern==null){
			return Set.of();
		}
		return this.noxisBossPattern.getBlackHoles();
	}

	/**
	 * Get the Apocalypse pattern instance
	 */
	public ApocalypseAttackPattern getApocalypsePattern() {
		return noxisBossPattern.getApocalypsePattern();
	}

	/**
	 * Get the Apocalypse pattern instance
	 */
	public DashPattern getDashPattern() {
		return noxisBossPattern.getDashPattern();
	}

	public Set<MidBossMob> getChildShips() {
		return noxisBossPattern.getChildShips();
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