package entity;

import engine.Cooldown;

import java.awt.*;

public class LaserBeam extends Entity implements LaserInfo {
	private Point startPosition;
	private Point endPosition;
	private Point targetPosition;
	private Cooldown chargeCooldown;
	private Cooldown remainCooldown;
	private final int chargeCooldownMilli;
	private final int remainCooldownMilli;
	private boolean shouldBeRemoved =false;
	private boolean isActive=false;

	public LaserBeam(Point startPosition, Point targetPosition, int chargeCooldownMilli, int remainCooldownMilli) {
		super(startPosition.x, startPosition.y, 0, 0, Color.green);
		this.targetPosition=targetPosition;
		this.chargeCooldownMilli=chargeCooldownMilli;
		this.remainCooldownMilli=remainCooldownMilli;

		extendLaserPoint(startPosition, targetPosition);
	}

	/**
	 * Extend Laser from end to end of the screen.
	 */
	private void extendLaserPoint(Point startPoint, Point targetPoint) {

		double dx = targetPoint.x - startPoint.x;
		double dy = targetPoint.y - startPoint.y;

		double distance = Math.sqrt(dx * dx + dy * dy);

		if (distance == 0) {
			return;
		}

		double unitX = dx / distance;
		double unitY = dy / distance;

		// Calculate enough length
		int screenDiag = (int) Math.sqrt(
				Math.pow(GameConstant.SCREEN_WIDTH, 2) +
						Math.pow(GameConstant.SCREEN_HEIGHT, 2)
		) + 100;

		// Extend backward
		this.startPosition = new Point(
				(int) (startPoint.x - unitX * screenDiag),
				(int) (startPoint.y - unitY * screenDiag)
		);

		// Extend forward
		this.endPosition = new Point(
				(int) (startPoint.x + unitX * screenDiag),
				(int) (startPoint.y + unitY * screenDiag)
		);
	}

	public void update(){
		if(this.chargeCooldown==null){
			this.chargeCooldown = new Cooldown(chargeCooldownMilli);
			chargeCooldown.reset();
		}
		if(this.chargeCooldown.checkFinished()){
			this.color = Color.red;
			this.isActive=true;
			if(this.remainCooldown==null){
				this.remainCooldown = new Cooldown(remainCooldownMilli);
				remainCooldown.reset();
			}
		}
		if(this.remainCooldown!=null && this.remainCooldown.checkFinished()){
			shouldBeRemoved =true;
		}
	}
	public Point getTargetPosition() {
		return new Point(targetPosition);
	}

	public boolean shouldBeRemoved() {
		return shouldBeRemoved;
	}

	@Override
	public boolean isActive() { return isActive; }

	@Override
	public void onCollision(Collidable other, GameModel model) {
		other.onHitByLaserBeam(this, model);
	}

	@Override
	public void onCollideWithShip(Ship ship, GameModel model) {
		ship.onHitByLaserBeam(this, model);
	}

	@Override
	public Point getStartPosition() {
		return new Point(this.startPosition);
	}

	@Override
	public Point getEndPosition() {
		return new Point(this.endPosition);
	}
}
