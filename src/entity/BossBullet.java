package entity;

import engine.DrawManager;

import java.util.concurrent.ThreadLocalRandom;


public class BossBullet extends Bullet implements Collidable {
    /** amount of horizontal change*/
    private int dx;
    /** amount of vertical change*/
    private int dy;

	private boolean markedForRemoval = false;
    private DrawManager.SpriteType[] bulletImages;

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
     * @param type
     *            bullet's sprite type
     */
    public BossBullet(int x, int y, int dx, int dy, int width, int height, String type) {
        super(x, y, 0, width, height);
        this.dx = dx;
        this.dy = dy;
        if (type.equals("SpreadShotPattern")){
            this.bulletImages = new DrawManager.SpriteType[]{DrawManager.SpriteType.PinnedBossPatternBullet, DrawManager.SpriteType.BasicBackGroundPatternBullet};
            int index = ThreadLocalRandom.current().nextInt(bulletImages.length);
            if (index == 0){
                super.width = 6;
                super.height = 10;
            }
            else if (index == 1){
                super.width = 6;
                super.height = 13;
            }
            this.spriteType = bulletImages[index];
        }
        else if (type.equals("ZigZagAngryPattern")){
            this.spriteType = DrawManager.SpriteType.ZigZagAngryPatternBullet;
        }
        else if (type.equals("PinnedBossPattern")){
            this.spriteType = DrawManager.SpriteType.PinnedBossPatternBullet;
        }
        else if (type.equals("BasicBackGroundPattern")){
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
