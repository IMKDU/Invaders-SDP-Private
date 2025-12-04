package entity;

import engine.DrawManager.SpriteType;

public class BombBullet extends Bullet {

	public BombBullet(int x, int y, int speed, int width, int height) {
		super(x, y, speed, width, height,false);
		this.spriteType = SpriteType.BombBullet;}

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
