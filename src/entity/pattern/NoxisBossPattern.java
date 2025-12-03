package entity.pattern;

import engine.Cooldown;
import engine.Core;
import entity.*;

import java.awt.*;
import java.util.*;
import java.util.List;

public class NoxisBossPattern extends BossPattern implements IBossPattern {
	/** Horizontal speed used in phase 1 movement. */
	private static final int HORIZONTAL_SPEED = 2;
	/** Pattern select threshold: Pinned(58%), Another(42%) */
	private static final double PHASE1_ATTACK_SELECT_RATIO = 0.58;
	/** HP ratio threshold: phase 1 → phase 2. */
	private static final double PHASE1_TO_PHASE2_TRIGGER = (double) 3 /5;
	/** HP ratio threshold: phase 2 → phase 3. */
	private static final double PHASE2_TO_PHASE3_TRIGGER = (double) 3 /10;

	/** Screen dimensions. */
	private final int screenWidth;
	private final int screenHeight;

	/** Currently selected attack pattern. */
	private IBossPattern attackPattern;
	/** Currently selected movement pattern. */
	private IBossPattern movePattern;
	/** Selected background patterns. */
	private List<IBossPattern> currentBackPatterns = new ArrayList<>();

	/** Cooldown for 1 cycle */
	private Cooldown cycleCooldown;
	/** Cooldown for attack switching/firing. */
	private Cooldown attackCooldown;
	/** Cooldown for movement switching. */
	private Cooldown dashCooldown;
	/** Cooldown for background switching */
	private Cooldown backCooldown;

	/** Cycle cooldown values for each phase. */
	private final int cycleCooldownMillis = 15000;
	/** Attack cooldown values for each phase. */
	private final int[][] attackCooldownMillis = {
			{5000, 9000},
			{8000, 10000},
			{8000, 16000}
	};
	/** Movement cooldown value shared across phases. */
	private final int dashCooldownMillis = 8000;
	private final int backCooldownMillis = 10000;

	/** Currently active phase (1, 2, or 3). */
	private int currentPhase=0;

	/** Reference to the owning boss. */
	private final MidBoss boss;
	/** Reference to the player target. */
	private List<Ship> targetShips;

	/** Current sub-pattern in the cycle (for phase 2 and 3). */
	private NoxisBossPattern.PatternCycleState cycleState = NoxisBossPattern.PatternCycleState.ATTACK;
	/** Flag to track if in dash cooldown. */
	private boolean isInDashCooldown = false;

	/** Track if using ZigZag or TimeGap for current attack cycle. */
	private boolean usingZigZag = false;
	/** Number of attack cycles completed (for ZigZag). */
	private int attackCyclesCompleted = 0;
	/** Required attack cycles before switching to dash. */
	private static final int REQUIRED_ATTACK_CYCLES = 2;

	/**
	 * Enum for tracking current pattern cycle state.
	 */
	private enum PatternCycleState {
		ATTACK,  // ZigZag or TimeGap attack
		DASH
	}

	/**
	 * Enum for tracking current pattern cycle state.
	 */
	private enum BackgroundPatternState {
		DEFAULT,
		BLACKHOLE,
		BOMB,
		MISSILE
	}

	/**
	 * Creates a new Omega boss pattern controller.
	 *
	 * @param boss   The owning mid-boss instance.
	 * @param ships The current player target.
	 */
	public NoxisBossPattern(MidBoss boss, List<Ship> ships, int screenWidth, int screenHeight) {
		super(new Point(boss.getPositionX(),boss.getPositionY()));
		this.boss = boss;
		this.targetShips = ships;
		this.screenWidth = screenWidth;
		this.screenHeight = screenHeight;
		attackCooldown = new Cooldown(attackCooldownMillis[0][0]);
		dashCooldown = new Cooldown(dashCooldownMillis);
		Core.getLogger().info("NoxisBossPattern:  Finish Initializing");
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
		updateBackPattern(currentPhase);
	}

	private List<IBossPattern> getRandomPatterns(List<IBossPattern> patterns, int howMany){
		List<IBossPattern> randomPatterns = new ArrayList<>(patterns);
		Collections.shuffle(randomPatterns);
		return randomPatterns.subList(0, howMany);
	}

	private void updateBackPattern(int howMany){
		if(currentBackPatterns.isEmpty()) return;
		if(backCooldown==null){
			backCooldown = new Cooldown(backCooldownMillis);
		}
		else if(backCooldown.checkFinished()){
			backCooldown.reset();
		}
		for (IBossPattern backPattern : currentBackPatterns) {
			backPattern.attack();
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
		boolean isPinnedAttack = Math.random()< PHASE1_ATTACK_SELECT_RATIO;
		if(isInit){
			Core.getLogger().info("NoxisBossPattern: phase1 start");

			movePattern = new SpreadShotPattern(boss, targetShips.getFirst());
			attackPattern = new SpreadShotPattern(boss, targetShips.getFirst());
			attackCooldown.setMilliseconds(attackCooldownMillis[2][0]);
			resetCooldown();
			attackPattern.setCooldown(attackCooldown);
			addBlackHoleBackGround();
		}
//		if(attackCooldown.checkFinished()){
//			movePattern = isPinnedAttack ? new HorizontalPattern(boss, 3) : new  SpreadShotPattern(boss, targetShips.getFirst());
//			attackPattern = isPinnedAttack ? patterns.get(1) : patterns.getLast();
//			if(isPinnedAttack) resetCooldown();
//		}
	}

//	private void phase1Cycle()


	/**
	 * Phase 2 configuration: pinned movement and attack.
	 *
	 * @param isInit {@code true} if phase just started.
	 */
	private void phase2(boolean isInit){
		currentPhase=2;
		if(isInit){
			Core.getLogger().info("NoxisBossPattern: phase2 start");

//			movePattern=patterns.getLast();
//			attackPattern=patterns.getLast();
//			attackCooldown.setMilliseconds(attackCooldownMillis[1][0]);
//			resetCooldown();
//			attackPattern.setCooldown(attackCooldown);
		}
	}

	/**
	 * Phase 3 configuration: mixes pinned and spread-shot patterns.
	 *
	 * @param isInit {@code true} if phase just started.
	 */
	private void phase3(boolean isInit){
		currentPhase=3;
		boolean isPinnedAttack = Math.random()< PHASE1_ATTACK_SELECT_RATIO;
		if(isInit){
			Core.getLogger().info("NoxisBossPattern: phase3 start");

//			movePattern=patterns.getFirst();
//			attackPattern = patterns.get(1);
//			attackCooldown.setMilliseconds(attackCooldownMillis[2][0]);
//			resetCooldown();
//			attackPattern.setCooldown(attackCooldown);
		}
//		if(attackCooldown.checkFinished()){
//			movePattern = isPinnedAttack ? patterns.getFirst() : patterns.getLast();
//			attackPattern = isPinnedAttack ? patterns.get(1) : patterns.getLast();
//			if(isPinnedAttack) resetCooldown();
//		}
	}

	private void addBlackHoleBackGround() {
		Core.getLogger().info("NoxisBossPattern: Black Hole Background Pattern Activated!");

		int cx = this.boss.getPositionX() + this.boss.getWidth() / 2;
		int cy = this.boss.getPositionY() + this.boss.getHeight() + 60;
		int radius = GameConstant.SCREEN_HEIGHT;

		currentBackPatterns.add(new BlackHolePattern(boss, targetShips, cx, cy, radius, 0.005, 7000));
	}

	/**
	 * Resets both attack and movement cooldowns.
	 */
	private void resetCooldown(){
		attackCooldown.reset();
		dashCooldown.reset();
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
		return new HashSet<>(this.attackPattern.getBullets());
	}

	@Override
	public Set<LaserBeam> getLasers() {
		if (this.attackPattern == null) {
			return java.util.Collections.emptySet();
		}
		return new HashSet<>(this.attackPattern.getLasers());
	}

	@Override
	public Set<BlackHole> getBlackHoles() {
		Set<BlackHole>  blackHoles = new HashSet<>();
		for(IBossPattern backPattern : currentBackPatterns){
			if(backPattern == null){
				return java.util.Collections.emptySet();
			}
			if(backPattern instanceof BlackHolePattern blackHolePattern){
				blackHoles.addAll(blackHolePattern.getBlackHoles());
//				if(blackHoles!=null) Core.getLogger().info("NoxisBossPattern: "+ blackHoles.size());
			}
		}
		return blackHoles;
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
