package entity.bullet;

import engine.view.DrawManager.SpriteType;
import entity.item.DropItem;
import entity.ship.EnemyShip;
import entity.GameModel;
import entity.base.BossEntity;
import entity.base.Collidable;

public class BombBullet extends Bullet {

	public BombBullet(int x, int y, int speed, int width, int height) {
		super(x, y, speed, width, height);
		this.spriteType = SpriteType.BombBullet;
    }
    @Override
    public void setSprite() {
        // do nothing
    }

	public void reset(int x, int y, int speed) {
		this.positionX = x;
		this.positionY = y;
		this.setSpeed(speed);
		this.spriteType = SpriteType.BombBullet;
		resetPenetration();
	}

	/** Called when BombBullet truly hits a valid enemy/boss. */
	public void explode(GameModel model) {
		model.requestBombAoEDamage(this);
	}

	/** Completely block all entity-based collision callbacks */
	@Override
	public void onCollision(Collidable other, GameModel model) {

		if (other instanceof DropItem) {
			return;
		}
        if (!(other instanceof EnemyShip) && !(other instanceof BossEntity)) {
            return;
        }
        super.onCollision(other, model);
        model.requestBombAoEDamage(this);

	}
}
