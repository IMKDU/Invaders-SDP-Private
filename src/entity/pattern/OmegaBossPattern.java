package entity.pattern;

import engine.Cooldown;
import entity.GameConstant;
import entity.HasBounds;
import entity.MidBoss;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class OmegaBossPattern implements IBossPattern {

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
	private int currentPhase=1;

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
		if(boss.getMaxHealPoint()>boss.getMaxHealPoint()*3/10){
			return 2;
		}
		return 3;
	}

	private void phase1(boolean isInit){
		currentPhase=1;
		if(isInit){
			movePattern=patterns.getFirst();
			attackPattern=patterns.get(1);
			attackPattern.setCooldown(new Cooldown(attackCooldownMillis[0][0]));
		}
	}
	private void phase2(boolean isInit){
		currentPhase=2;
		if(isInit){
			movePattern=patterns.getFirst();
			attackPattern=patterns.get(2);
		}

	}
	private void phase3(boolean isInit){
		currentPhase=3;
		if(isInit){
			movePattern=patterns.getFirst();
			attackPattern = Math.random()>0.5 ? patterns.get(1) : patterns.getLast();
		}
	}

	private void resetCooldown(){
		attackCooldown.reset();
		moveCooldown.reset();
	}

	@Override
	public void attack() {
		if(attackPattern==null) return;
		attackPattern.attack();
	}

	@Override
	public void move() {
		if(movePattern==null) return;
		movePattern.move();
	}

	@Override
	public Point getBossPosition() {
		return movePattern.getBossPosition();
	}

	@Override
	public void setTarget(HasBounds target) {

	}

	@Override
	public void setCooldown(Cooldown cooldown) {

	}
}
