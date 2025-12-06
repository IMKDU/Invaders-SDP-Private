package entity.pattern;

import engine.core.Cooldown;
import engine.core.Core;
import entity.*;
import entity.base.HasBounds;
import entity.bullet.BossBullet;

import java.awt.*;
import java.util.logging.Logger;

public class SpreadShotPattern extends BossPattern {

	private final Logger logger = Core.getLogger();

	private final HasBounds boss;
	private HasBounds target;

	private final int widthBoundary;

	private boolean movingToCenter = true;
	private int currentTargetX = 0;
	private static final int SPREAD_STEP = 12;
	private static final int SHOOT_COOLDOWN_MILLI = 5000;

	private boolean isFinished = false;

	public SpreadShotPattern(HasBounds boss, HasBounds target) {
		super(new Point(boss.getPositionX(), boss.getPositionY()));
		this.boss = boss;
		this.target = target;
		this.widthBoundary = GameConstant.SCREEN_WIDTH;
	}

	@Override
	public void move() {
		if (shootCooldown!=null && shootCooldown.checkFinished()){
			Core.getLogger().info("SpreadShotPattern: spread-shot is ready.");
			isFinished = false;
			movingToCenter = true;
			currentTargetX = 0;
			shootCooldown.reset();
		}
		if (isFinished) return;

		if (movingToCenter) {
			moveToCenter();
		}
	}

	@Override
	public void attack() {
		if (shootCooldown==null){
			this.shootCooldown = new Cooldown(SHOOT_COOLDOWN_MILLI);
		}
		if (movingToCenter) return;
		performSpreadShot();
	}

	@Override
	public void setTarget(HasBounds target) {
		this.target = target;
	}

	private static final int MOVE_SPEED = 6;

	private void moveToCenter() {
		int bossWidth = boss.getWidth();

		int targetX = (widthBoundary - bossWidth) / 2;

		if (boss.getPositionX() < targetX) bossPosition.x = boss.getPositionX() + MOVE_SPEED;
		else if (boss.getPositionX() > targetX) bossPosition.x = boss.getPositionX() - MOVE_SPEED;

		bossPosition.y = boss.getPositionY();

		if (Math.abs(bossPosition.x - targetX) <= MOVE_SPEED) {
			bossPosition.x = targetX;
			movingToCenter = false;
			currentTargetX = 0;
			logger.info("SpreadShotPattern : Center reached. Ready for SPREAD-SHOT.");
		}
	}

	private void performSpreadShot() {
		if (target == null || isFinished) {
			isFinished = true;
			return;
		}

		int safeSpace = target.getWidth();
		int rightLimit = widthBoundary - safeSpace;

		if (currentTargetX >= rightLimit) {
			logger.info("SpreadShotPattern : Spread shot completed.");
			isFinished = true;
			return;
		}

		int gunX = bossPosition.x + boss.getWidth() / 2;
		int gunY = bossPosition.y + boss.getHeight();

		int targetX = currentTargetX;
		int targetY = GameConstant.SCREEN_HEIGHT;

		double dx = targetX - gunX;
		double dy = targetY - gunY;
		double len = Math.sqrt(dx * dx + dy * dy);
		if (len == 0) len = 1;

		double speed = 6.0;
		int vx = (int) Math.round(dx / len * speed);
		int vy = (int) Math.round(dy / len * speed);

		if (vx == 0 && vy == 0) vy = 1;

		BossBullet bullet = new BossBullet(
				gunX,
				gunY,
				vx,
				vy,
				6,
				12,
				PatternBulletType.SPREAD_SHOT
		);

		this.bullets.add(bullet);

		currentTargetX += SPREAD_STEP;
	}

	@Override
	public Point getBossPosition() {
		return this.bossPosition;
	}

	public boolean isFinished() {
		return isFinished;
	}
}
