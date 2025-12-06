package entity.bullet;

import engine.view.DrawManager;
import entity.GameModel;
import entity.ship.Ship;
import entity.base.Collidable;
import entity.pattern.BossPattern;

import java.util.concurrent.ThreadLocalRandom;


public class BossBullet extends Bullet implements Collidable {
    /** amount of horizontal change*/
    private int dx;
    /** amount of vertical change*/
    private int dy;

	private boolean markedForRemoval = false;
    private DrawManager.SpriteType[] bulletImages;
    private final int PINNED_WIDTH  = 6;
    private final int PINNED_HEIGHT = 10;

    private final int BACKGROUND_WIDTH  = 6;
    private final int BACKGROUND_HEIGHT = 13;
    private final int ENEMYBULLET_WIDTH = 5;
    private final int ENEMYBULLET_HEIGHT = 15;

	/** bossBullets carry bullets that the boss will shoot */
    /**
     * Constructor, establishes boss bullets.
     *
     * @param x
     *            current x-coordinate
     * @param y
     *            current y-coordinate
     * @param dx
     *            amount of horizontal change
     * @param dy
     *            amount of vertical change
     * @param width
     *            bullet's width
     * @param height
     *            bullet's height
     * @param patternBulletType
     *            bullet's sprite type
     */
    public BossBullet(int x, int y, int dx, int dy, int width, int height, BossPattern.PatternBulletType patternBulletType) {
        super(x, y, 0, width * 2, height * 2);
        this.dx = dx;
        this.dy = dy;
        applyPattern(patternBulletType);

    }
    @Override
    public void setSprite() {
        // do nothing
    }

    private void applyPattern(BossPattern.PatternBulletType type){
        if (type == BossPattern.PatternBulletType.SPREAD_SHOT){
            this.bulletImages = new DrawManager.SpriteType[]{DrawManager.SpriteType.PinnedBossPatternBullet, DrawManager.SpriteType.BasicBackGroundPatternBullet,DrawManager.SpriteType.EnemyBullet};
            int index = ThreadLocalRandom.current().nextInt(bulletImages.length);
            if (index == 0){
                this.width = PINNED_WIDTH;
                this.height = PINNED_HEIGHT;
            }
            else if (index == 1){
                this.width = BACKGROUND_WIDTH;
                this.height = BACKGROUND_HEIGHT;
            }
            else {
                this.width = ENEMYBULLET_WIDTH;
                this.height = ENEMYBULLET_HEIGHT;
            }
            this.spriteType = bulletImages[index];
        }
        else if (type == BossPattern.PatternBulletType.ZIGZAG_ANGRY){
            this.spriteType = DrawManager.SpriteType.ZigZagAngryPatternBullet;
        }
        else if (type == BossPattern.PatternBulletType.PINNED){
            this.spriteType = DrawManager.SpriteType.PinnedBossPatternBullet;
        }
        else if (type == BossPattern.PatternBulletType.BACKGROUND){
            this.spriteType = DrawManager.SpriteType.BasicBackGroundPatternBullet;
        }
    }

    /**
     * move a bullet
     */
	@Override
    public void update() {
        this.positionX += this.dx;
        this.positionY += this.dy;
    }

	public void markForRemoval() {
		this.markedForRemoval = true;
	}

	/**
	 * Handles collision behavior for boss bullets.
	 * Boss bullets damage the player when they collide.
	 */
	@Override
	public void onCollision(Collidable other, GameModel model) {
		other.onHitByBossBullet(this, model);
	}

	@Override
	public void onHitByPlayerBullet(Bullet bullet, GameModel model) {
	}

	@Override
	public void onCollideWithShip(Ship ship, GameModel model) {
		ship.onHitByBossBullet(this, model);
	}

	@Override
	public boolean shouldBeRemoved() {
		return markedForRemoval;
	}
}
