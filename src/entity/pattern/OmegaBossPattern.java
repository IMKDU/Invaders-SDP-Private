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

/**
 * Pattern coordinator for the Omega mid-boss.
 *
 * <p>
 * Selects and combines concrete {@link IBossPattern} implementations
 * (e.g. horizontal, pinned, spread shot) based on the boss's HP phase,
 * and delegates movement/attack to the active patterns.
 * </p>
 */
public class OmegaBossPattern extends BossPattern implements IBossPattern {

	/** Horizontal speed used in phase 1 movement. */
	private static final int HORIZONTAL_SPEED = 2;
	/** Pattern select threshold: Pinned(58%), Another(42%) */
	private static final double PHASE3_ATTACK_SELECT_RATIO = 0.58;
	/** HP ratio threshold: phase 1 → phase 2. */
	private static final double PHASE1_TO_PHASE2_TRIGGER = (double) 3 /5;
	/** HP ratio threshold: phase 2 → phase 3. */
	private static final double PHASE2_TO_PHASE3_TRIGGER = (double) 3 /10;

	/** List of all available sub-patterns for Omega. */
	private List<IBossPattern> patterns = new ArrayList<IBossPattern>();
	/** Currently selected attack pattern. */
	private IBossPattern attackPattern;
	/** Currently selected movement pattern. */
	private IBossPattern movePattern;

	/** Cooldown for attack switching/firing. */
	private Cooldown attackCooldown;
	/** Cooldown for movement switching. */
	private Cooldown moveCooldown;

	/** Attack cooldown values for each phase. */
	private final int[][] attackCooldownMillis = {
			{2000},
			{8000},
			{5000},
			{1500}
	};
	/** Movement cooldown value shared across phases. */
	private final int moveCooldownMillis = 8000;
	/** Currently active phase (1, 2, or 3). */
	private int currentPhase=0;

	/** Reference to the owning boss. */
	private final MidBoss boss;
	/** Reference to the player target. */
	private HasBounds player;

	/**
	 * Creates a new Omega boss pattern controller.
	 *
	 * @param boss   The owning mid-boss instance.
	 * @param player The current player target.
	 */
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

	/**
	 * Performs one update step:
	 * <ul>
	 *     <li>Determines the current phase from boss HP.</li>
	 *     <li>Initializes patterns if the phase has changed.</li>
	 *     <li>Runs phase-specific configuration logic.</li>
	 * </ul>
	 */
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

	/**
	 * Returns the current phase index based on boss HP.
	 *
	 * @return 1, 2, or 3 depending on HP thresholds.
	 */
	public int checkPhase(){
		if(boss.getHealPoint()>boss.getMaxHealPoint()*PHASE1_TO_PHASE2_TRIGGER){
			return 1;
		}
		if(boss.getHealPoint()>boss.getMaxHealPoint()*PHASE2_TO_PHASE3_TRIGGER){
			return 2;
		}
		return 3;
	}

	/**
	 * Phase 1 configuration: horizontal movement + pinned attack.
	 *
	 * @param isInit {@code true} if phase just started.
	 */
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


	/**
	 * Phase 2 configuration: pinned movement and attack.
	 *
	 * @param isInit {@code true} if phase just started.
	 */
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

	/**
	 * Phase 3 configuration: mixes pinned and spread-shot patterns.
	 *
	 * @param isInit {@code true} if phase just started.
	 */
	private void phase3(boolean isInit){
		currentPhase=3;
		boolean isPinnedAttack = Math.random()<PHASE3_ATTACK_SELECT_RATIO;
		if(isInit){
			Cooldown crazyAttackCooldown = new Cooldown(attackCooldownMillis[3][0]);
			movePattern=patterns.getFirst();
			attackPattern = patterns.get(1);
			attackCooldown.setMilliseconds(attackCooldownMillis[2][0]);
			resetCooldown();
			attackPattern.setCooldown(crazyAttackCooldown);
			Core.getLogger().info("OmegaBossPattern: phase3 start");
		}
		if(attackCooldown.checkFinished()){
			movePattern = isPinnedAttack ? patterns.getFirst() : patterns.getLast();
			attackPattern = isPinnedAttack ? patterns.get(1) : patterns.getLast();
			if(isPinnedAttack){
				Cooldown crazyAttackCooldown = new Cooldown(attackCooldownMillis[3][0]);
				attackPattern.setCooldown(crazyAttackCooldown);
				resetCooldown();
			}
		}
	}

	/**
	 * Resets both attack and movement cooldowns.
	 */
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
		if(movePattern != null){
			bossPosition.x = movePattern.getBossPosition().x;
			bossPosition.y = movePattern.getBossPosition().y;
		}
	}

	@Override
	public Set<Bullet> getBullets() {
		if (this.attackPattern == null) {
			return java.util.Collections.emptySet();
		}
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
