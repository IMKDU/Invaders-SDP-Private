package entity.pattern;

import engine.Cooldown;
import engine.Core;
import entity.*;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;

public class NoxisBossPattern extends BossPattern implements IBossPattern {

	/** Pattern select threshold: Pinned(58%), Another(42%) */
	private static final double PHASE1_ATTACK_SELECT_RATIO = 0.58;

	/** HP ratio thresholds for phase transitions */
	private static final double PHASE1_TO_PHASE2_TRIGGER = 0.70;
	private static final double PHASE2_TO_PHASE3_TRIGGER = 0.40;

	// BlackHole parameters
	private static final int BLACKHOLE_RADIUS = 1000;
	private static final double BLACKHOLE_PULL_CONSTANT = 0.005;

	/** Horizontal speed used in phase 1 movement. */
	private static final int HORIZONTAL_SPEED = 4;

	// Pattern speeds
	private static final int DIAGONAL_X_SPEED = 4;
	private static final int DIAGONAL_Y_SPEED = 3;
	private static final Color DIAGONAL_COLOR = Color.MAGENTA;

	private static final int DASH_MAXIMUM = 4;

	// Screen dimensions
	private int SCREEN_WIDTH = GameConstant.SCREEN_WIDTH;
	private int SCREEN_HEIGHT = GameConstant.SCREEN_HEIGHT;

	/** BlackHole states */
	private enum BlackHoleState {
		ACTIVE,         // BlackHole is pulling players
		COOLDOWN,       // BlackHole is inactive
		FORCED_STOP     // BlackHole stopped by Apocalypse
	}

	/** Dash states */
	private enum DashState {
		DASHING,
		COOLDOWN        // Waiting for cooldown
	}

	/** Apocalypse states */
	private enum ApocalypseState {
		READY,          // Ready to activate (cooldown finished)
		CHARGING,       // 3 seconds charging phase
		FIRING,         // 2 seconds firing phase
		COOLDOWN        // Waiting for cooldown
	}

	/** Currently active phase (1, 2, or 3). */
	private int currentPhase=1;

	/** Reference to the owning boss. */
	private final MidBoss boss;
	/** Reference to the player target. */
	private List<Ship> ships;
	/** Logger instance */
	private final Logger logger;
	/** Random generator for random selections */
	private final Random random;

	/** Selected background patterns. */
	private List<IBossPattern> currentBackPatterns = new ArrayList<>();

	// Current patterns
	private BossPattern attackPattern;
	private BossPattern movementPattern;

	/** Variable for Guided Missile Pattern */
	private GuidedMissilePattern guidedMissilePattern;

	// BlackHole state management
	private BlackHoleState blackHoleState = BlackHoleState.COOLDOWN;
	private Cooldown blackHoleDurationTimer;
	private Cooldown blackHoleCooldownTimer;
	private int blackHoleDuration;
	private int blackHoleCooldown;

	// Apocalypse state management
	private ApocalypseState apocalypseState = ApocalypseState.COOLDOWN;
	private ApocalypseAttackPattern apocalypsePattern;
	private Cooldown apocalypseCooldownTimer;
	private int apocalypseCooldown;

	// Dash pattern management
	private DashState dashState = DashState.COOLDOWN;
	private DashPattern dashPattern;
	private Cooldown dashCooldownTimer;
	private int dashCooldown;
	private int maxDash;
	private int currentDashNum;

	// SpawnMob pattern management
	private SpawnMobPattern spawnMobPattern;
	private Cooldown spawnMobCooldownTimer;
	private int spawnMobCooldown;

	/** Cooldown for 1 cycle */
	private Cooldown cycleCooldownTimer;
	private int cycleCooldown;

	/**
	 * Creates a new Omega boss pattern controller.
	 *
	 * @param boss   The owning mid-boss instance.
	 * @param ships The current player target.
	 */
	public NoxisBossPattern(MidBoss boss, List<Ship> ships) {
		super(new Point(boss.getPositionX(),boss.getPositionY()));
		this.boss = boss;
		this.ships = ships;
		this.logger = Core.getLogger();
		this.random = new Random();

		// Initialize Apocalypse Pattern
		this.apocalypsePattern = new ApocalypseAttackPattern(boss);
		this.guidedMissilePattern = new GuidedMissilePattern(this.boss, ships);

		updateTimersForPhase();

		// Initialize pattern
		this.movementPattern = createMovementPattern();
		this.attackPattern = createAttackPattern();

		this.logger.info("NoxisBossPattern:  Finish Initializing");
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
		switch (currentPhase) {
			case 1:
				break;
			case 2:
				// Priority 1: Check if Apocalypse cooldown finished
				if (dashState==DashState.COOLDOWN && apocalypseState == ApocalypseState.COOLDOWN && apocalypseCooldownTimer.checkFinished()) {
					forceStopBlackHole();
					startApocalypse();
					stopMovement();
					return;
				}

				// Priority 2: Handle active Apocalypse
				if (dashState==DashState.COOLDOWN && (apocalypseState == ApocalypseState.CHARGING || apocalypseState == ApocalypseState.FIRING)) {
					updateApocalypse();
					stopMovement();
					return;
				}

				// Priority 3: Handle BlackHole cycle
				updateBlackHoleCycle();

				// Priority 3: Handle Dash cycle
				if (dashState==DashState.COOLDOWN && dashCooldownTimer.checkFinished() || dashState==DashState.DASHING) {
					updateDash();
					stopMovement();
					return;
				}

				// Priority 4: Update movement (always except during Apocalypse)
				if (guidedMissilePattern != null) {
					guidedMissilePattern.attack();
				}

				// intentional fall-through
			case 3:

		}
		updatePhase();

		this.move();
		this.attack();
	}

	/**
	 * Updates the current phase based on health percentage
	 */
	private void updatePhase() {
		int previousPhase = this.currentPhase;
		double healthPercent = (double) boss.getHealPoint() / boss.getMaxHealPoint();
		if (healthPercent > PHASE1_TO_PHASE2_TRIGGER) {
			this.currentPhase = 1;
		} else if (healthPercent > PHASE2_TO_PHASE3_TRIGGER) {
			this.currentPhase = 2;
		} else {
			this.currentPhase = 3;
		}
		// Update timers if phase changed
		if (previousPhase != this.currentPhase) {
			updateTimersForPhase();
			// Recreate movement pattern for new phase
			this.movementPattern = createMovementPattern();
			this.attackPattern = createAttackPattern();
			logger.info("NoxisBossPattern: Phase changed to " + this.currentPhase);
		}
		// Update patterns if cycle cooldown is done
		if (cycleCooldownTimer.checkFinished()) {
			logger.info("NoxisBossPattern: Cycle ended - change patterns");
			this.movementPattern = createMovementPattern();
			this.attackPattern = createAttackPattern();
			renewTimers();
			this.cycleCooldownTimer.reset();
		}
	}

	/**
	 * Updates timers based on current phase
	 */
	private void updateTimersForPhase() {
		switch (this.currentPhase) {
			case 1: // 100-70%
				this.blackHoleDuration = 5000; // 5 seconds
				this.blackHoleCooldown = 10000; // 10 seconds
				this.apocalypseCooldown = 20000; // 20 seconds
				this.dashCooldown = 0; // No used
				this.cycleCooldown = 11000; // 11 seconds
				break;
			case 2: // 70-40%
				this.blackHoleDuration = 7000; // 7 seconds
				this.blackHoleCooldown = 7000; // 7 seconds
				this.apocalypseCooldown = 10000; // 15 seconds
				this.dashCooldown = 8000; // 8 seconds
				this.cycleCooldown = 10000; // 11 seconds
				break;
			case 3: // 40-0%
				this.blackHoleDuration = 9000; // 9 seconds
				this.blackHoleCooldown = 5000; // 5 seconds
				this.apocalypseCooldown = 8000; // 10 seconds
				this.dashCooldown = 5000; // 8 seconds
				this.cycleCooldown = 16000; // 11 seconds
				break;
		}
		// Create new timers with updated durations
		this.blackHoleDurationTimer = new Cooldown(this.blackHoleDuration);
		this.blackHoleCooldownTimer = new Cooldown(this.blackHoleCooldown);
		this.apocalypseCooldownTimer = new Cooldown(this.apocalypseCooldown);
		renewTimers();
	}

	private void renewTimers(){
		this.dashCooldownTimer = new Cooldown(this.dashCooldown);
		this.cycleCooldownTimer = new Cooldown(this.cycleCooldown);
	}

	/**
	 * Force stops BlackHole when Apocalypse starts
	 */
	private void forceStopBlackHole() {
		if (blackHoleState == BlackHoleState.ACTIVE) {
			logger.info("NoxisBossPattern: BlackHole force-stopped by Apocalypse");
		}
		blackHoleState = BlackHoleState.FORCED_STOP;
		if(currentBackPatterns.isEmpty()) return;

		Set<IBossPattern> removeBlackHoles = new HashSet<>();
		for(IBossPattern back: currentBackPatterns){
			if(back instanceof BlackHolePattern){
				removeBlackHoles.add(back);
			}
		}
		currentBackPatterns.removeAll(removeBlackHoles);
	}

	/**
	 * Starts Apocalypse attack
	 */
	private void startApocalypse() {
		apocalypseState = ApocalypseState.CHARGING;
		apocalypsePattern.start(1);
		logger.info("NoxisBossPattern: Apocalypse activated!");
	}

	/**
	 * Updates Apocalypse state
	 */
	private void updateApocalypse() {
		apocalypsePattern.attack();
		// Check if Apocalypse finished
		if (!apocalypsePattern.isPatternActive()) {
			apocalypseState = ApocalypseState.COOLDOWN;
			apocalypseCooldownTimer.reset();
			// Restart BlackHole fresh from Active state
			restartBlackHoleFresh();
			// Resume movement
			this.movementPattern = createMovementPattern();
			this.attackPattern = createAttackPattern();
			logger.info("NoxisBossPattern: Apocalypse ended, restarting BlackHole");
		}
	}

	/**
	 * Updates Dash state
	 */
	private void updateDash() {
		if(dashPattern==null){
			dashState = DashState.DASHING;
			dashPattern = new DashPattern(boss, getRandomShip());
		}
		if(maxDash == 0){
			maxDash = random.nextInt(DASH_MAXIMUM)+1;
		}
		// Check if Dash finished
		if (dashPattern.isDashCompleted()) {
			if(maxDash == currentDashNum){
				logger.info("NoxisBossPattern: Dashing ended, Dash-count: " + maxDash);

				dashState = DashState.COOLDOWN;
				dashCooldownTimer.reset();
				currentDashNum = 0;
				maxDash = 0;
			}
			else{
				dashPattern = null;
				++currentDashNum;
			}
		}
		else{
			dashPattern.move();
			movementPattern.bossPosition = dashPattern.getBossPosition();
		}
	}

	/**
	 * Restarts BlackHole from fresh Active state after Apocalypse
	 */
	private void restartBlackHoleFresh() {
		activateBlackHole();
	}

	/**
	 * Updates BlackHole cycle (Active ↔ Cooldown)
	 */
	private void updateBlackHoleCycle() {
		switch (blackHoleState) {
			case ACTIVE:
				// Check if duration finished
				if (blackHoleDurationTimer.checkFinished()) {
					deactivateBlackHoles();
				}
				break;
			case COOLDOWN:
				// Check if cooldown finished
				if (blackHoleCooldownTimer.checkFinished()) {
					activateBlackHole();
				}
				break;
			case FORCED_STOP:
				// Wait for Apocalypse to finish (handled in updateApocalypse)
				break;
		}
	}

	/**
	 * Activates BlackHole at a random position
	 */
	private void activateBlackHole() {
		// Generate random center position for BlackHole
		int centerX = random.nextInt(SCREEN_WIDTH - 200) + 100;
		int centerY = random.nextInt(200) + 150;
		currentBackPatterns.add(new BlackHolePattern(
				boss,
				ships,
				centerX,
				centerY,
				BLACKHOLE_RADIUS,
				BLACKHOLE_PULL_CONSTANT,
				blackHoleDuration
		));
		blackHoleState = BlackHoleState.ACTIVE;
		blackHoleDurationTimer.reset();
		logger.info("NoxisBossPattern: BlackHole activated at (" + centerX + ", " + centerY + ")");
	}

	/**
	 * Deactivates BlackHole and starts cooldown
	 */
	private void deactivateBlackHoles() {
		blackHoleState = BlackHoleState.COOLDOWN;
		if(currentBackPatterns.isEmpty()) return;

		Set<IBossPattern> removeBlackHoles = new HashSet<>();
		for(IBossPattern back: currentBackPatterns){
			if(back instanceof BlackHolePattern){
				removeBlackHoles.add(back);
			}
		}
		currentBackPatterns.removeAll(removeBlackHoles);
		blackHoleCooldownTimer.reset();
		logger.info("NoxisBossPattern: BlackHole deactivated, cooldown started");
	}

	/**
	 * Creates movement pattern based on current phase
	 */
	private BossPattern createAttackPattern() {
		switch (this.currentPhase) {
			case 1: // Phase 1: DiagonalPattern - SpreadShotPattern - TimeGapAttackPattern
				if(movementPattern instanceof HorizontalPattern){
					return new SpawnMobPattern(boss, boss.getMaxHealPoint());
				}
				logger.info("NoxisBossPattern: Attack - following movement");
				return movementPattern;
			case 2: // Phase 2: ZigZagAngryPattern - DashPattern
				logger.info("NoxisBossPattern: Attack - ZigZag Angry");
				return movementPattern;
			case 3: // Phase 3: Random between Diagonal and ZigZagAngry
				if (random.nextBoolean()) {
					logger.info("NoxisBossPattern: Attack - Diagonal (random)");
					return new DiagonalPattern(boss, DIAGONAL_X_SPEED, DIAGONAL_Y_SPEED, DIAGONAL_COLOR);
				} else {
					logger.info("NoxisBossPattern: Attack - ZigZag Angry (random)");
					return new ZigZagAngryPattern(boss, SCREEN_WIDTH, SCREEN_HEIGHT);
				}
			default:
				return new DiagonalPattern(boss, DIAGONAL_X_SPEED, DIAGONAL_Y_SPEED, DIAGONAL_COLOR);
		}
	}

	/**
	 * Creates movement pattern based on current phase
	 */
	private BossPattern createMovementPattern() {

		int choice = random.nextInt(3);
		switch (this.currentPhase) {
			case 1: // Phase 1: Horizontal - SpreadShot - TimeGap
				return createPhase1Pattern();
			case 2: // Phase 2: ZigZagAngryPattern - Dash - Apocalypse
				return createPhase2Pattern();
			case 3: // Phase 3: Random between Diagonal and ZigZagAngry
				return createPhase3Pattern();
			default:
				return new DiagonalPattern(boss, DIAGONAL_X_SPEED, DIAGONAL_Y_SPEED, DIAGONAL_COLOR);
		}
	}

	private BossPattern createPhase1Pattern(){
		int choice = random.nextInt(3);
		if(choice == 0){
			logger.info("NoxisBossPattern: Movement - Horizontal");
			return new HorizontalPattern(boss, HORIZONTAL_SPEED);
		}
		else if(choice == 1){
			logger.info("NoxisBossPattern: Movement - SpreadShot");
			return new SpreadShotPattern(boss, getRandomShip());
		}
		else if(choice == 2){
			logger.info("NoxisBossPattern: Movement - TimeGap");
			return new TimeGapAttackPattern(boss, ships, SCREEN_WIDTH, SCREEN_HEIGHT);
		}
		return new ZigZagAngryPattern(boss, SCREEN_WIDTH, SCREEN_HEIGHT);
	}

	private BossPattern createPhase2Pattern(){
		if(dashCooldownTimer.checkFinished())
		logger.info("NoxisBossPattern: Movement - ZigZag Angry");
		return new ZigZagAngryPattern(boss, SCREEN_WIDTH, SCREEN_HEIGHT);
	}

	private BossPattern createPhase3Pattern(){
		int choice = random.nextInt(3);
		if(choice == 0){
			if (random.nextBoolean()) {
				logger.info("NoxisBossPattern: Movement - Diagonal (random)");
				return new DiagonalPattern(boss, DIAGONAL_X_SPEED, DIAGONAL_Y_SPEED, DIAGONAL_COLOR);
			} else {
				logger.info("NoxisBossPattern: Movement - ZigZag Angry (random)");
				return new ZigZagAngryPattern(boss, SCREEN_WIDTH, SCREEN_HEIGHT);
			}
		}
		return new ZigZagAngryPattern(boss, SCREEN_WIDTH, SCREEN_HEIGHT);
	}

	/**
	 * Stops attack
	 */
	private void stopAttack(int milli) {

	}

	/**
	 * Stops movement (during Apocalypse)
	 */
	private void stopMovement() {
		// Movement is handled by not calling movementPattern.move()
	}

	@Override
	public void attack() {
		// Attack is handled in update() for each pattern
		if (blackHoleState == BlackHoleState.ACTIVE && currentBackPatterns != null) {
			for (IBossPattern back : currentBackPatterns) {
				back.attack();
			}
		}
		// Update position from movement pattern
		if (attackPattern != null) {
			attackPattern.attack();
		}
	}
	@Override
	public void move() {
		// Movement handled in update()
		// During Apocalypse, don't move
		if (apocalypseState == ApocalypseState.CHARGING || apocalypseState == ApocalypseState.FIRING) {
			// Boss doesn't move during Apocalypse
			return;
		}
		if (dashState == DashState.DASHING) {
			// Boss doesn't move during Apocalypse
			return;
		}
		// Update position from movement pattern
		if (movementPattern != null) {
			movementPattern.move();
			this.bossPosition.x = movementPattern.getBossPosition().x;
			this.bossPosition.y = movementPattern.getBossPosition().y;
		}
	}
	@Override
	public Point getBossPosition() {
		return new Point(this.movementPattern.getBossPosition().x, this.movementPattern.getBossPosition().y);
	}
	@Override
	public Set<Bullet> getBullets() {
		Set<Bullet> allBullets = new java.util.HashSet<>();
		if (attackPattern != null) {
			allBullets.addAll(attackPattern.getBullets());
		}
		if (guidedMissilePattern != null) {
			allBullets.addAll(guidedMissilePattern.getBullets());
		}
		if (apocalypsePattern != null) {
			allBullets.addAll(apocalypsePattern.getBullets());
		}
		return allBullets;
	}
	@Override
	public void setTarget(HasBounds target) {
		// Not used for NoxisBoss patterns
	}

	private Ship getRandomShip(){
		int choice = random.nextInt(ships.size());
		return ships.get(choice);
	}

	@Override
	public Set<LaserBeam> getLasers() {
		if (this.attackPattern == null) {
			return java.util.Collections.emptySet();
		}
		return this.attackPattern.getLasers();
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
			}
		}
		return blackHoles;
	}

	/**
	 * Get the Apocalypse pattern instance
	 */
	public ApocalypseAttackPattern getApocalypsePattern() {
		return apocalypsePattern;
	}

	/**
	 * Get the Apocalypse pattern instance
	 */
	public DashPattern getDashPattern() {
		return dashPattern;
	}

	public List<MidBossMob> getChildShips() {
		if(attackPattern == null) return List.of();
		if(attackPattern instanceof SpawnMobPattern spawnPattern){
			return spawnPattern.getChildShips();
		}
		return List.of();
	}

	@Override
	public void setCooldown(Cooldown cooldown) {
		attackPattern.setCooldown(cooldown);
		movementPattern.setCooldown(cooldown);
	}

	@Override
	public void validateBackgroundPattern(boolean condition) {
		attackPattern.validateBackgroundPattern(condition);
	}
}
