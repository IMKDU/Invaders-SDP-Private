package entity.bullet;

import java.util.HashSet;
import java.util.Set;

/**
 * Implements a pool of recyclable bullets.
 *
 * @author <a href="mailto:RobertoIA1987@gmail.com">Roberto Izquierdo Amo</a>
 *
 */
public final class BulletPool {

	/** Set of already created bullets. */
	private static Set<Bullet> pool = new HashSet<Bullet>();
	private static Set<BombBullet> bombPool = new HashSet<>();
	/**
	 * Constructor, not called.
	 */
	private BulletPool() {

	}

	/**
	 * Returns a bullet from the pool if one is available, a new one if there
	 * isn't.
	 *
	 * @param positionX
	 *            Requested position of the bullet in the X axis.
	 * @param positionY
	 *            Requested position of the bullet in the Y axis.
	 * @param speed
	 *            Requested speed of the bullet, positive or negative depending
	 *                  on direction - positive is down.
	 * @return Requested bullet.
	 */
	public static Bullet getBullet(final int positionX,
								   final int positionY, final int speed) {
		Bullet bullet;
		if (!pool.isEmpty()) {
			bullet = pool.iterator().next();
			pool.remove(bullet);
			bullet.setPositionX(positionX - bullet.getWidth() / 2);
			bullet.setPositionY(positionY);
			bullet.setSpeed(speed);
			bullet.resetPenetration();
			bullet.setSprite();
		} else {
			bullet = new Bullet(positionX, positionY, speed, 5 * 2 , 10 * 2);
			bullet.setPositionX(positionX - bullet.getWidth() / 2);
		}
		return bullet;
	}

	public static BombBullet getBombBullet(final int x, final int y, final int speed) {
		BombBullet b;

		if (!bombPool.isEmpty()) {
			b = bombPool.iterator().next();
			bombPool.remove(b);
			b.reset(x, y, speed);
		} else {
			b = new BombBullet(x, y, speed,20 * 2, 53 *2);
		}

		return b;
	}

	/**
	 * Adds one or more bullets to the list of available ones.
	 *
	 * @param bullet
	 *            Bullets to recycle.
	 */
	public static void recycle(final Set<Bullet> bullet) {
		pool.addAll(bullet);
	}

	public static void recycleBomb(final BombBullet b) {
		bombPool.add(b);
	}

}
