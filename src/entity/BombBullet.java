package entity;

import engine.DrawManager.SpriteType;

public class BombBullet extends Bullet {

	public BombBullet(int x, int y, int speed) {
		super(x, y, speed, 20, 53);
		this.spriteType = SpriteType.BombBullet;
	}

	@Override
	public void onCollision(Collidable other, GameModel model) {

		if (other instanceof DropItem) {
			return;
		}
        super.onCollision(other, model);
        model.requestBombAoEDamage(this);

	}
}
