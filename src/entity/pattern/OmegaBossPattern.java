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

public class OmegaBossPattern extends BossPattern implements IBossPattern {

	private static final int HORIZONTAL_SPEED = 2;

	private List<IBossPattern> patterns = new ArrayList<IBossPattern>();
	private IBossPattern attackPattern;
	private IBossPattern movePattern;

	private Cooldown attackCooldown;
	private Cooldown moveCooldown;

	private final int[][] attackCooldownMillis = {
			{8000},
			{8000},
			{5000}
	};
	private final int moveCooldownMillis = 8000;
	private int currentPhase=0;

	private final MidBoss boss;
	private HasBounds player;

	public OmegaBossPattern(MidBoss boss, HasBounds player) {
		super(new Point(boss.getPositionX(),boss.getPositionY()));
		this.boss = boss;
		this.player = player;
		attackCooldown = new Cooldown(attackCooldownMillis[0][0]);
		moveCooldown = new Cooldown(moveCooldownMillis);

		patterns.add(new HorizontalPattern(boss, HORIZONTAL_SPEED));
		patterns.add(new PinnedBossPattern(boss, GameConstant.SCREEN_WIDTH, GameConstant.SCREEN_HEIGHT));
		patterns.add(new SpreadShotPattern(boss, player));
		initPatternConfiguration();
	}

	private void initPatternConfiguration(){
		patterns.get(1).validateBackgroundPattern(false);
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
		if(boss.getHealPoint()>boss.getMaxHealPoint()*3/5){
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
			resetCooldown();
			attackPattern.setCooldown(attackCooldown);
			Core.getLogger().info("OmegaBossPattern: phase1 start");
		}
	}
	private void phase2(boolean isInit){
		currentPhase=2;
		if(isInit){
			movePattern=patterns.getLast();
			attackPattern=patterns.getLast();
			attackCooldown.setMilliseconds(attackCooldownMillis[1][0]);
			resetCooldown();
			attackPattern.setCooldown(attackCooldown);
			Core.getLogger().info("OmegaBossPattern: phase2 start");
		}
	}
	private void phase3(boolean isInit){
		currentPhase=3;
		boolean isPinnedAttack;
		if(isInit){
			movePattern=patterns.getFirst();
			attackPattern = patterns.get(1);
			attackCooldown.setMilliseconds(attackCooldownMillis[2][0]);
			resetCooldown();
			attackPattern.setCooldown(attackCooldown);
			Core.getLogger().info("OmegaBossPattern: phase3 start");
		}
		if(attackCooldown.checkFinished()){
			isPinnedAttack = Math.random()<0.65;
			movePattern = isPinnedAttack ? patterns.getFirst() : patterns.getLast();
			attackPattern = isPinnedAttack ? patterns.get(1) : patterns.getLast();
			if(isPinnedAttack) resetCooldown();
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
		if(movePattern != null){
			bossPosition.x = movePattern.getBossPosition().x;
			bossPosition.y = movePattern.getBossPosition().y;
		}
	}

	public Set<Bullet> getBullets() {
		return this.attackPattern.getBullets();
	}

	@Override
	public void setTarget(HasBounds target) {
		attackPattern.setTarget(target);
	}

	@Override
	public void setCooldown(Cooldown cooldown) {
		attackPattern.setCooldown(cooldown);
		movePattern.setCooldown(cooldown);
	}

	@Override
	public void validateBackgroundPattern(boolean condition) {
		attackPattern.validateBackgroundPattern(condition);
	}
}
