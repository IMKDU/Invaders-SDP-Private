package entity;

import engine.Cooldown;
import java.awt.*;

public class LaserBeam extends Entity {
	private Point targetPosition;
	private Cooldown chargeCooldown;
	private Cooldown remainCooldown;
	private final int chargeCooldownMilli;
	private final int remainCooldownMilli;
	private boolean shouldBeRemoved =false;

	public LaserBeam(Point startPosition, Point targetPosition, int chargeCooldownMilli, int remainCooldownMilli) {
		super(startPosition.x, startPosition.y, 0, 0, null);
		this.targetPosition=targetPosition;
		this.chargeCooldownMilli=chargeCooldownMilli;
		this.remainCooldownMilli=remainCooldownMilli;
	}

	public void update(){
		if(this.chargeCooldown==null){
			this.chargeCooldown = new Cooldown(chargeCooldownMilli);
			chargeCooldown.reset();
		}
		if(this.chargeCooldown.checkFinished()){
			color = Color.red;
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
		return targetPosition;
	}

	public boolean shouldBeRemoved() {
		return shouldBeRemoved;
	}

	@Override
	public void onCollision(Collidable other, GameModel model) {
		other.onHitByLaserBeam(this, model);
	}

	@Override
	public void onCollideWithShip(Ship ship, GameModel model) {
		ship.onHitByLaserBeam(this, model);
	}
}
