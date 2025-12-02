package entity;

import java.awt.Color;
import engine.DrawManager.SpriteType;

public class BombBullet extends Bullet {

	public BombBullet(int x, int y, int speed, Color color) {
		super(x, y, speed, color);
		this.spriteType = SpriteType.Bullet;
	}

	@Override
	public void onCollision(Collidable other, GameModel model) {

		if (other instanceof DropItem) {
			return;
		}
		model.requestBombAoEDamage(this);

	}
}
