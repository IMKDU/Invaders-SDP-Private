package entity;

import engine.DrawManager.SpriteType;

public class BombBullet extends Bullet {

	public BombBullet(int x, int y, int speed, int width, int height) {
		super(x, y, speed, width, height,false);
		this.spriteType = SpriteType.BombBullet;
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
