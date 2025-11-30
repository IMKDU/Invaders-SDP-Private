package entity;

import java.awt.Color;
import engine.DrawManager.SpriteType;

public class BombBullet extends Bullet {

	public BombBullet(int x, int y, int speed) {
		super(x, y, speed, 17, 40);
		this.spriteType = SpriteType.BombBullet;
	}

	@Override
	public void onCollision(Collidable other, GameModel model) {

		if (other instanceof DropItem) {
			return;
		}
		model.requestBombAoEDamage(this);

	}
}
