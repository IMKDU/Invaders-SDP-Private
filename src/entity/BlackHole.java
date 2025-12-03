package entity;

import engine.Cooldown;
import engine.Core;

import java.awt.*;
import java.util.List;

public class BlackHole extends Entity implements HasCircleBounds {

	private final Point centerPoint;
	private final int radius;
	private final double pullConstant;
	private final Cooldown effectCooldown;
	private boolean isDestroyed = false;
	private final List<? extends Entity> targets;

	/**
	 * Constructor, establishes the entity's generic properties.
	 *
	 * @param centerX Initial position of the entity in the X axis.
	 * @param centerY Initial position of the entity in the Y axis.
	 * @param radius Radius of the entity.
	 * @param pullConstant Pulling power of black-hole.
	 * @param effectDuration Duration of the effect.
	 * @param targets Entities which are under effect.
	 */
	public BlackHole(int centerX, int centerY, int radius, double pullConstant, int effectDuration, List<? extends Entity> targets) {
		super(0, 0, 0, 0, null);
		this.centerPoint = new Point(centerX, centerY);
		this.radius = radius;
		this.pullConstant = pullConstant;
		this.effectCooldown = new Cooldown(effectDuration);
		this.effectCooldown.reset();
		this.targets = targets;
	}

	/**
	 * Constructor, establishes the entity's generic properties.
	 *
	 * @param centerPoint Initial position of the entity in the X, Y axis.
	 * @param radius  Radius of the entity.
	 * @param pullConstant Pulling power of black-hole.
	 * @param effectDuration Duration of the effect.
	 * @param targets Entities which are under effect.
	 */
	public BlackHole(Point centerPoint, int radius, double pullConstant, int effectDuration, List<? extends Entity> targets) {
		this(centerPoint.x,centerPoint.y,radius,pullConstant,effectDuration, targets);
	}

	public void update(){
		if(effectCooldown.checkFinished()){
			isDestroyed = true;
		}
		else{
			for(Entity e : targets){
				double shipX = e.getPositionX();
				double shipY = e.getPositionY();

				double dx = this.centerPoint.x - shipX;
				double dy = this.centerPoint.y - shipY;

				double dist = Math.sqrt(dx * dx + dy * dy);
				if(dist <= radius && dist > 1){
					double force = (radius - dist) * pullConstant;

					double ux = dx/dist;
					double uy = dy/dist;

					if (!GameConstant.origin_skill_activated) {
						e.setPositionX(e.getPositionX()+(int)(ux*force));
						e.setPositionY(e.getPositionY()+(int)(uy*force));
					}
				}
			}
		}
	}
	public boolean shouldBeRemoved() {
		return isDestroyed;
	}

	@Override
	public int getPositionCX() {
		return centerPoint.x;
	}

	@Override
	public int getPositionCY() {
		return centerPoint.y;
	}

	@Override
	public int getRadius() {
		return radius;
	}
}
