package entity.bullet;

import engine.view.DrawManager.SpriteType;
import entity.Entity;
import entity.GameModel;
import entity.item.ShopItem;
import entity.base.Collidable;

/**
 * Implements a bullet that moves vertically up or down.
 * 
 * @author <a href="mailto:RobertoIA1987@gmail.com">Roberto Izquierdo Amo</a>
 * 
 */
public class Bullet extends Entity implements Collidable {
	// === [ADD] Owner flag: 1 = P1, 2 = P2, null for legacy compatibility ===
	private Integer ownerId;

    public Integer getOwnerId() { return ownerId; }
    public void setOwnerId(Integer ownerId) { this.ownerId = ownerId; }


    /**
	 * Speed of the bullet, positive or negative depending on direction -
	 * positive is down.
	 */
	private int speed;

	/** number of Penetrations */
	private int penetrationCount;
	/** Number of possible penetrations */
	private int maxPenetration;


	/**
	 * Constructor, establishes the bullet's properties.
	 * 
	 * @param positionX
	 *            Initial position of the bullet in the X axis.
	 * @param positionY
	 *            Initial position of the bullet in the Y axis.
	 * @param speed
	 *            Speed of the bullet, positive or negative depending on
	 *            direction - positive is down.
	 */
	public Bullet(final int positionX, final int positionY, final int speed, final int width, final int height) {
		super(positionX, positionY, width, height,null);
		this.speed = speed;
		this.penetrationCount = 0;
            setSprite();


	}

	/**
	 * Sets correct sprite for the bullet, based on speed.
	 */
	public void setSprite() {
		if (speed < 0){
            this.spriteType = SpriteType.Bullet;
            super.height = 10;
            super.width = 5;
        }

		else{
            this.spriteType = SpriteType.EnemyBullet;
            super.height = 15;
            super.width = 5;
        }

	}

	/**
	 * Updates the bullet's position.
	 */
	public void update() {
		this.positionY += this.speed;
	}

	/**
	 * Setter of the speed of the bullet.
	 * 
	 * @param speed
	 *            New speed of the bullet.
	 */
	public void setSpeed(final int speed) {
		this.speed = speed;
	}

	/**
	 * Getter for the speed of the bullet.
	 * 
	 * @return Speed of the bullet.
	 */
	public final int getSpeed() {
		return this.speed;
	}

	/**
	 * getter Bullet persistence status
	 *
	 * @return If true the bullet persists, If false it is deleted.
	 */
	public final boolean penetration() {
		this.penetrationCount++;

		return this.penetrationCount <= this.maxPenetration;
	}

	/**
	 * Check for penetration possibility
	 *
	 * @return True, Penetrable
	 */
	public final boolean canPenetration() {
		return this.penetrationCount < this.maxPenetration;
	}

	/**
	 * reset penetration setting
	 */
	public final void resetPenetration() {
		this.penetrationCount = 0;
		this.maxPenetration = ShopItem.getPenetrationCount();
	}

    /**
     * Manually sets the maximum penetration count of the bullet.
     * (Used when the sub-ship adjusts penetration based on its own level)
     */
    public void setMaxPenetration(int maxPenetration) {
        this.maxPenetration = maxPenetration;
    }

	/**
	 * does the bullet go off the screen
	 */
	public boolean isOffScreen(int screenWidth, int screenHeight) {
		return positionX < 0 || positionX > screenWidth ||
				positionY < 0 || positionY > screenHeight;
	}

	/**
	 * does the bullet has to be removed
	 */
	public boolean shouldBeRemoved() {
		return false;
	}


	@Override
	public void onCollision(Collidable other, GameModel model) {

		if (this.speed < 0) {
			other.onHitByPlayerBullet(this, model);
			return;
		}

		if (this.speed > 0) {
			other.onHitByEnemyBullet(this, model);
			return;
		}
	}
}