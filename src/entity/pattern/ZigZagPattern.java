package entity.pattern;

import engine.core.Cooldown;
import entity.GameConstant;
import entity.base.HasBounds;

public class ZigZagPattern extends PinnedBossPattern {

	public ZigZagPattern(HasBounds boss, int screenWidth, int screenHeight) {
		super(boss, screenWidth, screenHeight);
		int shootCooldownMilli = 2000;
		shootCooldown = new Cooldown(shootCooldownMilli);
	}

	@Override
	public void move(){
		moveZigzag(4,3);
	}

	protected void moveZigzag(int zigSpeed, int vertSpeed){
		super.bossPosition.x += (this.zigDirection * zigSpeed);
		// Check and adjust position at boundaries
		if(this.bossPosition.x <= 0){
			this.bossPosition.x = 0;
			this.zigDirection = 1;  // Move right
		} else if(this.bossPosition.x >= screenWidth - boss.getWidth()) {
			this.bossPosition.x = screenWidth - boss.getWidth();
			this.zigDirection = -1;  // Move left
		}

		if(goingDown){
			this.bossPosition.y += vertSpeed;
			if (this.bossPosition.y >= screenHeight/2 - boss.getHeight()){ goingDown = false; }
		}
		else{
			this.bossPosition.y -= vertSpeed;
			if(this.bossPosition.y <= GameConstant.STAT_SEPARATION_LINE_HEIGHT) {
				goingDown = true; }
			}
		}
	}
