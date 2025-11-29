package entity.pattern;

import engine.Cooldown;
import engine.Core;
import entity.Bullet;
import entity.GameConstant;
import entity.HasBounds;
import entity.MidBoss;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class OmegaBossPattern {

	private static final int HORIZONTAL_SPEED = 2;

	private List<IBossPattern> patterns = new ArrayList<IBossPattern>();
	private IBossPattern attackPattern;
	private IBossPattern movePattern;

	private Cooldown attackCooldown;
	private Cooldown moveCooldown;

	private final int[][] attackCooldownMillis = {
			{10000},
			{10000},
			{5000}
	};
	private int currentPhase=0;

	private final MidBoss boss;
	private HasBounds player;

	public OmegaBossPattern(MidBoss boss, HasBounds player) {
		this.boss = boss;
		this.player = player;

		patterns.add(new HorizontalPattern(boss, HORIZONTAL_SPEED));
		patterns.add(new PinnedBossPattern(boss, GameConstant.SCREEN_WIDTH, GameConstant.SCREEN_HEIGHT));
		patterns.add(new SpreadShotPattern(boss, player));
	}

	public void update(){
		int trigger = checkPhase();
		boolean isInit = trigger != currentPhase;
		if(trigger == 1){
			phase1(isInit);
		}
		else if(trigger == 2){
			phase2(isInit);
		}
		else if(trigger == 3){
			phase3(isInit);
		}
	}

	public int checkPhase(){
		if(boss.getHealPoint()>boss.getMaxHealPoint()*2/5){
			return 1;
		}
		if(boss.getHealPoint()>boss.getMaxHealPoint()*3/10){
			return 2;
		}
		return 3;
	}

	private void phase1(boolean isInit){
		currentPhase=1;
		if(isInit){
			movePattern=patterns.getFirst();
			attackPattern=patterns.get(1);
			attackPattern.validateBackgroundPattern(false);
			attackPattern.setCooldown(new Cooldown(attackCooldownMillis[0][0]));
			Core.getLogger().info("OmegaBossPattern: phase1 start");
		}
	}
	private void phase2(boolean isInit){
		currentPhase=2;
		if(isInit){
			movePattern=patterns.getFirst();
			attackPattern=patterns.get(2);
			attackPattern.setCooldown(new Cooldown(attackCooldownMillis[1][0]));
			Core.getLogger().info("OmegaBossPattern: phase2 start");
		}

	}
	private void phase3(boolean isInit){
		boolean isPinnedAttack = Math.random()>0.5 ;
		currentPhase=3;
		if(isInit){
			movePattern=patterns.getFirst();
			attackPattern = isPinnedAttack ? patterns.get(1) : patterns.getLast();
			attackPattern.setCooldown(new Cooldown(attackCooldownMillis[2][0]));
			Core.getLogger().info("OmegaBossPattern: phase3 start");
		}
		if(!isPinnedAttack){
			movePattern=patterns.getLast();
		}
	}

	private void resetCooldown(){
		attackCooldown.reset();
		moveCooldown.reset();
	}

	public void attack() {
		if(attackPattern==null) return;
		attackPattern.attack();
	}

	public void move() {
		if(movePattern==null) return;
		movePattern.move();
	}

	public Point getBossPosition() {
		return movePattern.getBossPosition();
	}

	public Set<Bullet> getBullets() {
		return this.attackPattern.getBullets();
	}
}
