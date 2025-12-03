package entity;

import engine.DrawManager.SpriteType;

public class BombBullet extends Bullet {

	public BombBullet(int x, int y, int speed) {
		super(x, y, speed, 17, 40);
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
	}
}
