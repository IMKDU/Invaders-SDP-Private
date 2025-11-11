package screen;

import engine.Core;
import engine.GameState;
import engine.level.Level;
import entity.GameModel;



/**
 * Implements the game screen, where the action happens.
 * This class acts as the CONTROLLER in the MVC pattern.
 * It manages the game loop, handles input, and coordinates
 * the GameModel (state/logic) and GameView (drawing).
 *
 * @author <a href="mailto:RobertoIA1987@gmail.com">Roberto Izquierdo Amo</a>
 *
 */
public class GameScreen extends Screen {

    /** Height of the interface separation line. */
    public static final int SEPARATION_LINE_HEIGHT = 45;
    /** Height of the items separation line (above items). */
    public static final int ITEMS_SEPARATION_LINE_HEIGHT = 400;
    /** Returns the Y-coordinate of the bottom boundary for enemies (above items HUD) */
    public static int getItemsSeparationLineHeight() {
        return ITEMS_SEPARATION_LINE_HEIGHT;
    }

    /** Current level data (direct from Level system). */
    private Level currentLevel;
	/** Current difficulty level number. */
	private int level;
	/** Formation of enemy ships. */
	private EnemyShipFormation enemyShipFormation;
	/** Formation of special enemy ships. */
	private EnemyShipSpecialFormation enemyShipSpecialFormation;
	/** Player's ship. */
	private Ship ship;
	/** Second Player's ship. */
	private Ship shipP2;
	/** Bonus enemy ship that appears sometimes. */
	private EnemyShip enemyShipSpecial;
	/** Minimum time between bonus ship appearances. */
	private Cooldown enemyShipSpecialCooldown;
	/** team drawing may implement */
	private FinalBoss finalBoss;
	/** Time until bonus ship explosion disappears. */
	private Cooldown enemyShipSpecialExplosionCooldown;
	/** Time until Boss explosion disappears. */
	private Cooldown bossExplosionCooldown;
	/** Time from finishing the level to screen change. */
	private Cooldown screenFinishedCooldown;
	/** OmegaBoss */
	private MidBoss omegaBoss;
	/** Set of all bullets fired by on-screen ships. */
	private Set<Bullet> bullets;
	/** Set of all dropItems dropped by on screen ships. */
	private Set<DropItem> dropItems;
	/** Current score. */
	private int score;
    // === [ADD] Independent scores for two players ===
    private int scoreP1 = 0;
    private int scoreP2 = 0;
	/** current level parameter */
	public Level currentlevel;
    /** Player lives left. */
	private int livesP1;
	private int livesP2;
	/** Total bullets shot by the player. */
	private int bulletsShot;
	/** Total ships destroyed by the player. */
	private int shipsDestroyed;
	/** Moment the game starts. */
	private long gameStartTime;
	/** Checks if the level is finished. */
	private boolean levelFinished;
	/** Checks if a bonus life is received. */
	private boolean bonusLife;
  /** Maximum number of lives. */
	private int maxLives;
	/** Current coin. */
	private int coin;
    // Unified scoring entry: maintains both P1/P2 and legacy this.score (total score)
    private void addPointsFor(Bullet bullet, int pts) {
        Integer owner = (bullet != null ? bullet.getOwnerId() : null);
        if (owner != null && owner == 2) {
            this.scoreP2 += pts;   // P2
        } else {
            this.scoreP1 += pts;   // Default to P1 (for null compatibility)

        }
        this.score += pts;        // Keep maintaining the total score, for legacy process compatibility

    }

    /** bossBullets carry bullets which Boss fires */
	private Set<BossBullet> bossBullets;
	/** Is the bullet on the screen erased */
  private boolean is_cleared = false;
  /** Timer to track elapsed time. */
  private GameTimer gameTimer;
  /** Elapsed time since the game started. */
  private long elapsedTime;
  // Achievement popup
  private String achievementText;
  private Cooldown achievementPopupCooldown;
  private enum StagePhase{wave, boss_wave};
  private StagePhase currentPhase;
  /** Health change popup. */
  private String healthPopupText;
  private Cooldown healthPopupCooldown;

	    /**
	     * Constructor, establishes the properties of the screen.
	     *
	     * @param gameState
	     *            Current game state.	 * @param level
	 *            Current level settings.
	 * @param bonusLife
	 *            Checks if a bonus life is awarded this level.
	 * @param maxLives
	 *            Maximum number of lives.
	 * @param width
	 *            Screen width.
	 * @param height
	 *            Screen height.
	 * @param fps
	 *            Frames per second, frame rate at which the game is run.
	 */
	public GameScreen(final GameState gameState,
			final Level level, final boolean bonusLife, final int maxLives,
			final int width, final int height, final int fps) {
		super(width, height, fps);

        this.currentLevel = level;
		this.bonusLife = bonusLife;
		this.currentlevel = level;
		this.maxLives = maxLives;
		        this.level = gameState.getLevel();
		        this.score = gameState.getScore();
                this.coin = gameState.getCoin();
		        this.livesP1 = gameState.getLivesRemaining();
				this.livesP2 = gameState.getLivesRemainingP2();
		        this.gameState = gameState;
				if (this.bonusLife) {
					this.livesP1++;
					this.livesP2++;
				}
		this.bulletsShot = gameState.getBulletsShot();
		this.shipsDestroyed = gameState.getShipsDestroyed();
	}

	/**
	 * Initializes basic screen properties, and adds necessary elements.
	 */
	public final void initialize() {
		super.initialize();
		/** Initialize the bullet Boss fired */
		this.bossBullets = new HashSet<>();
        enemyShipFormation = new EnemyShipFormation(this.currentLevel);
		enemyShipFormation.attach(this);
        this.enemyShipFormation.applyEnemyColorByLevel(this.currentLevel);
		this.ship = new Ship(this.width / 2 - 100, ITEMS_SEPARATION_LINE_HEIGHT - 20,Color.green);
		    this.ship.setPlayerId(1);   //=== [ADD] Player 1 ===

        this.shipP2 = new Ship(this.width / 2 + 100, ITEMS_SEPARATION_LINE_HEIGHT - 20,Color.pink);
        this.shipP2.setPlayerId(2); // === [ADD] Player2 ===
        // special enemy initial
		enemyShipSpecialFormation = new EnemyShipSpecialFormation(this.currentLevel,
				Core.getVariableCooldown(BONUS_SHIP_INTERVAL, BONUS_SHIP_VARIANCE),
				Core.getCooldown(BONUS_SHIP_EXPLOSION));
		enemyShipSpecialFormation.attach(this);
		this.bossExplosionCooldown = Core
				.getCooldown(BOSS_EXPLOSION);
		this.screenFinishedCooldown = Core.getCooldown(SCREEN_CHANGE_INTERVAL);
		this.bullets = new HashSet<Bullet>();
        this.dropItems = new HashSet<DropItem>();

		// Special input delay / countdown.
		this.gameStartTime = System.currentTimeMillis();
		this.inputDelay = Core.getCooldown(INPUT_DELAY);
		this.inputDelay.reset();


		this.gameTimer = new GameTimer();
        this.elapsedTime = 0;
		this.finalBoss = null;
		this.omegaBoss = null;
		this.currentPhase = StagePhase.wave;
	}

	/**
	 * Starts the action.
	 *
	 * @return Next screen code.
	 */
	public final int run() {
		super.run();

		this.score += LIFE_SCORE * (this.livesP1 - 1);
		this.score += LIFE_SCORE * (this.livesP2 - 1);
		this.logger.info("Screen cleared with a score of " + this.score);

		return this.returnCode;
	}

	/**
	 * Updates the elements on screen and checks for events.
	 */
	protected final void update() {
		super.update();

		if (this.inputDelay.checkFinished() && !this.levelFinished) {

			if (!this.gameTimer.isRunning()) {
				this.gameTimer.start();
			}

			if (this.livesP1 > 0 && !this.ship.isDestroyed()) {
				boolean p1Right = inputManager.isP1KeyDown(java.awt.event.KeyEvent.VK_D);
				boolean p1Left  = inputManager.isP1KeyDown(java.awt.event.KeyEvent.VK_A);
				boolean p1Up    = inputManager.isP1KeyDown(java.awt.event.KeyEvent.VK_W);
				boolean p1Down  = inputManager.isP1KeyDown(java.awt.event.KeyEvent.VK_S);
				boolean p1Fire  = inputManager.isP1KeyDown(java.awt.event.KeyEvent.VK_SPACE);

				boolean isRightBorder = this.ship.getPositionX()
						+ this.ship.getWidth() + this.ship.getSpeed() > this.width - 1;
				boolean isLeftBorder = this.ship.getPositionX() - this.ship.getSpeed() < 1;
				boolean isUpBorder = this.ship.getPositionY() - this.ship.getSpeed() < SEPARATION_LINE_HEIGHT;
				boolean isDownBorder = this.ship.getPositionY()
						+ this.ship.getHeight() + this.ship.getSpeed() > ITEMS_SEPARATION_LINE_HEIGHT;

				if (p1Right && !isRightBorder) this.ship.moveRight();
				if (p1Left  && !isLeftBorder)  this.ship.moveLeft();
				if (p1Up    && !isUpBorder)    this.ship.moveUp();
				if (p1Down  && !isDownBorder)  this.ship.moveDown();

				if (p1Fire) {
					if (this.ship.shoot(this.bullets)) {
						this.bulletsShot++;
						AchievementManager.getInstance().onShotFired();
					}
				}
			}

			if (this.shipP2 != null && this.livesP2 > 0 && !this.shipP2.isDestroyed()) {
				boolean p2Right = inputManager.isP2KeyDown(java.awt.event.KeyEvent.VK_RIGHT);
				boolean p2Left  = inputManager.isP2KeyDown(java.awt.event.KeyEvent.VK_LEFT);
				boolean p2Up    = inputManager.isP2KeyDown(java.awt.event.KeyEvent.VK_UP);
				boolean p2Down  = inputManager.isP2KeyDown(java.awt.event.KeyEvent.VK_DOWN);
				boolean p2Fire  = inputManager.isP2KeyDown(java.awt.event.KeyEvent.VK_ENTER);

				boolean p2RightBorder = this.shipP2.getPositionX()
						+ this.shipP2.getWidth() + this.shipP2.getSpeed() > this.width - 1;
				boolean p2LeftBorder = this.shipP2.getPositionX() - this.shipP2.getSpeed() < 1;
				boolean p2UpBorder = this.shipP2.getPositionY() - this.shipP2.getSpeed() < SEPARATION_LINE_HEIGHT;
				boolean p2DownBorder = this.shipP2.getPositionY()
						+ this.shipP2.getHeight() + this.shipP2.getSpeed() > ITEMS_SEPARATION_LINE_HEIGHT;

				if (p2Right && !p2RightBorder) this.shipP2.moveRight();
				if (p2Left  && !p2LeftBorder)  this.shipP2.moveLeft();
				if (p2Up    && !p2UpBorder)    this.shipP2.moveUp();
				if (p2Down  && !p2DownBorder)  this.shipP2.moveDown();

				if (p2Fire) {
					if (this.shipP2.shoot(this.bullets)) {
						this.bulletsShot++;
						AchievementManager.getInstance().onShotFired();
					}
				}
			}
			switch (this.currentPhase) {
				case wave:
					if (!DropItem.isTimeFreezeActive()) {
						this.enemyShipFormation.update();
						this.enemyShipFormation.shoot(this.bullets);
					}
					if (this.enemyShipFormation.isEmpty()) {
						this.currentPhase = StagePhase.boss_wave;
					}
					break;
				case boss_wave:
					if (this.finalBoss == null && this.omegaBoss == null){
						bossReveal();
						this.enemyShipFormation.clear();
					}
					if(this.finalBoss != null){
						finalbossManage();
					}
					else if (this.omegaBoss != null){
						this.omegaBoss.update();
						if (this.omegaBoss.isDestroyed()) {
							if ("omegaAndFinal".equals(this.currentlevel.getBossId())) {
								this.omegaBoss = null;
                                this.finalBoss = new FinalBoss(this.width / 2 - 50, 50, this.width, this.height);
                                this.logger.info("Final Boss has spawned!");
							} else {
								this.levelFinished = true;
								this.screenFinishedCooldown.reset();
							}
						}
					}
					else{
						if(!this.levelFinished){
							this.levelFinished = true;
							this.screenFinishedCooldown.reset();
						}
					}
					break;
			}
			this.ship.update();
			if (this.shipP2 != null) {
				this.shipP2.update();
			}
			// special enemy update
			this.enemyShipSpecialFormation.update();
		}

		if (this.gameTimer.isRunning()) {
            this.elapsedTime = this.gameTimer.getElapsedTime();
				AchievementManager.getInstance().onTimeElapsedSeconds((int)(this.elapsedTime / 1000));
        }
        cleanItems();
        manageBulletShipCollisions();
        manageShipEnemyCollisions();
        manageItemCollisions();
		cleanBullets();
		draw();

		if (((this.livesP1 == 0) && (this.shipP2 == null || this.livesP2 == 0)) && !this.levelFinished) {
			this.levelFinished = true;
			this.screenFinishedCooldown.reset();
			if (this.gameTimer.isRunning()) {
				this.gameTimer.stop();
			}

			if ((this.livesP1 > 0) || (this.shipP2 != null && this.livesP2 > 0)) {
				if (this.level == 1) {
					AchievementManager.getInstance().unlockAchievement("Beginner");
				} else if (this.level == 3) {
					AchievementManager.getInstance().unlockAchievement("Intermediate");
				}
			}
		}
		if (this.levelFinished && this.screenFinishedCooldown.checkFinished()) {
			if (this.livesP1 > 0 || (this.shipP2 != null && this.livesP2 > 0)) { // Check for win condition
				if (this.currentlevel.getCompletionBonus() != null) {
					this.coin += this.currentlevel.getCompletionBonus().getCurrency();
					this.logger.info("Awarded " + this.currentlevel.getCompletionBonus().getCurrency() + " coins for level completion.");
				}

				String achievement = this.currentlevel.getAchievementTrigger();
				if (achievement != null && !achievement.isEmpty()) {
					AchievementManager.getInstance().unlockAchievement(achievement);
					this.logger.info("Unlocked achievement: " + achievement);
				}
			}
			this.isRunning = false;
		}
	}


	/**
	 * Draws the elements associated with the screen.
	 */
	private void draw() {
		drawManager.initDrawing(this);

		if (this.livesP1 > 0) {
			drawManager.getEntityRenderer().drawEntity(this.ship, this.ship.getPositionX(),
					this.ship.getPositionY());
		}

		if (this.shipP2 != null && this.livesP2 > 0) {
			drawManager.getEntityRenderer().drawEntity(this.shipP2, this.shipP2.getPositionX(), this.shipP2.getPositionY());
		}

		// special enemy draw
		enemyShipSpecialFormation.draw();

		/** draw final boss at the field */
		/** draw final boss bullets */
		if(this.finalBoss != null && !this.finalBoss.isDestroyed()){
			for (BossBullet bossBullet : bossBullets) {
				drawManager.getEntityRenderer().drawEntity(bossBullet, bossBullet.getPositionX(), bossBullet.getPositionY());
			}
			drawManager.getEntityRenderer().drawEntity(finalBoss, finalBoss.getPositionX(), finalBoss.getPositionY());
		}

		enemyShipFormation.draw();

		if(this.omegaBoss != null) {
			this.omegaBoss.draw(drawManager);
		}

		for (Bullet bullet : this.bullets)
			drawManager.getEntityRenderer().drawEntity(bullet, bullet.getPositionX(),
					bullet.getPositionY());

		for (DropItem dropItem : this.dropItems)
			drawManager.getEntityRenderer().drawEntity(dropItem, dropItem.getPositionX(), dropItem.getPositionY());

		// Interface.
        drawManager.getHUDRenderer().drawScoreP1(this, this.scoreP1);   // Top line still displays P1
        drawManager.getHUDRenderer().drawScoreP2(this, this.scoreP2); // Added second line for P2
        drawManager.getHUDRenderer().drawCoin(this,this.coin);
		drawManager.getHUDRenderer().drawLivesP1(this, this.livesP1);
		drawManager.getHUDRenderer().drawLivesP2(this, this.livesP2);
		drawManager.getHUDRenderer().drawTime(this, this.elapsedTime);
		drawManager.getHUDRenderer().drawItemsHUD(this);
		drawManager.getHUDRenderer().drawLevel(this, this.currentLevel.getLevelName());
		drawManager.getUIRenderer().drawHorizontalLine(this, SEPARATION_LINE_HEIGHT - 1);
		drawManager.getUIRenderer().drawHorizontalLine(this, ITEMS_SEPARATION_LINE_HEIGHT);

		if (this.achievementText != null && !this.achievementPopupCooldown.checkFinished()) {
			drawManager.getHUDRenderer().drawAchievementPopup(this, this.achievementText);
		} else {
			this.achievementText = null; // clear once expired
		}

		// Health notification popup
		if(this.healthPopupText != null && !this.healthPopupCooldown.checkFinished()) {
			drawManager.getHUDRenderer().drawHealthPopup(this, this.healthPopupText);
		} else {
			this.healthPopupText = null;
		}

		// Countdown to game start.
		if (!this.inputDelay.checkFinished()) {
			int countdown = (int) ((INPUT_DELAY
					- (System.currentTimeMillis()
					- this.gameStartTime)) / 1000);
			drawManager.getUIRenderer().drawCountDown(this, this.level, countdown,
					this.bonusLife);
			drawManager.getUIRenderer().drawHorizontalLine(this, this.height / 2 - this.height
					/ 12);
			drawManager.getUIRenderer().drawHorizontalLine(this, this.height / 2 + this.height
					/ 12);
		}

		drawManager.completeDrawing(this);
	}


	/**
	 * Cleans bullets that go off screen.
	 */
	private void cleanBullets() {
		Set<Bullet> recyclable = new HashSet<Bullet>();
		for (Bullet bullet : this.bullets) {
			bullet.update();
			if (bullet.getPositionY() < SEPARATION_LINE_HEIGHT
					|| bullet.getPositionY() > this.height)
				recyclable.add(bullet);
		}
		this.bullets.removeAll(recyclable);
		BulletPool.recycle(recyclable);
	}
    /** Checks if a bonus life is received. */
    private boolean bonusLife;
    /** Maximum number of lives. */
    private int maxLives;
    /** Current game state. */
    private GameState gameState;

    /** The Model component */
    private GameModel model;
    /** The View component */
    private GameView view;

    /**
     * Constructor, establishes the properties of the screen.
     *
     * @param gameState
     * Current game state.	 * @param level
     * Current level settings.
     * @param bonusLife
     * Checks if a bonus life is awarded this level.
     * @param maxLives
     * Maximum number of lives.
     * @param width
     * Screen width.
     * @param height
     * Screen height.
     * @param fps
     * Frames per second, frame rate at which the game is run.
     */
    public GameScreen(final GameState gameState,
                      final Level level, final boolean bonusLife, final int maxLives,
                      final int width, final int height, final int fps) {
        super(width, height, fps);

        this.currentLevel = level;
        this.bonusLife = bonusLife;
        this.maxLives = maxLives;
        this.gameState = gameState;
    }

    /**
     * Initializes basic screen properties, and adds necessary elements.
     */
    public final void initialize() {
        super.initialize();

        // Create Model and View
        this.model = new GameModel(this.gameState, this.currentLevel, this.bonusLife, this.maxLives, this.width, this.height, this);
        this.view = new GameView(this.model, this.drawManager, this.width, this.height);

        // Initialize the model's state
        this.model.initialize();

        this.inputDelay = Core.getCooldown(GameModel.INPUT_DELAY);
        this.inputDelay.reset();
    }

    /**
     * Starts the action.
     *
     * @return Next screen code.
     */
    public final int run() {
        super.run();

        // Calculate final score from the model
        int finalScore = this.model.calculateFinalScore();
        this.logger.info("Screen cleared with a score of " + finalScore);

        return this.returnCode;
    }

    /**
     * Updates the elements on screen and checks for events.
     * This is the main Controller loop.
     */
    protected final void update() {
        super.update();

        // Check if input delay is over and game is not finished
        if (this.inputDelay.checkFinished() && !this.model.isLevelFinished()) {

            if (!this.model.isTimerRunning()) {
                this.model.startTimer();
            }

            // 1. (Controller) Process user input and tell Model to update
            // Player 1 Input
            // (Get player object from model to check status)
            if (model.getLivesP1() > 0 && model.getShip() != null && !model.getShip().isDestroyed()) {
                // (Controller detects input and sends a 'command' to the model)
                if (inputManager.isP1KeyDown(java.awt.event.KeyEvent.VK_D))
                    model.playerMove(1, "RIGHT");
                if (inputManager.isP1KeyDown(java.awt.event.KeyEvent.VK_A))
                    model.playerMove(1, "LEFT");
                if (inputManager.isP1KeyDown(java.awt.event.KeyEvent.VK_W))
                    model.playerMove(1, "UP");
                if (inputManager.isP1KeyDown(java.awt.event.KeyEvent.VK_S))
                    model.playerMove(1, "DOWN");
                if (inputManager.isP1KeyDown(java.awt.event.KeyEvent.VK_SPACE))
                    model.playerFire(1);
            }

            // Player 2 Input
            if (model.getShipP2() != null && model.getLivesP2() > 0 && !model.getShipP2().isDestroyed()) {
                if (inputManager.isP2KeyDown(java.awt.event.KeyEvent.VK_RIGHT))
                    model.playerMove(2, "RIGHT");
                if (inputManager.isP2KeyDown(java.awt.event.KeyEvent.VK_LEFT))
                    model.playerMove(2, "LEFT");
                if (inputManager.isP2KeyDown(java.awt.event.KeyEvent.VK_UP))
                    model.playerMove(2, "UP");
                if (inputManager.isP2KeyDown(java.awt.event.KeyEvent.VK_DOWN))
                    model.playerMove(2, "DOWN");
                if (inputManager.isP2KeyDown(java.awt.event.KeyEvent.VK_ENTER))
                    model.playerFire(2);
            }

            // 2. (Controller) Tell Model to update all game logic
            this.model.updateGameWorld();
        }

        // Update elapsed time (if timer is running)
        if (this.model.isTimerRunning()) {
            this.model.updateElapsedTime();
        }

        // 3. (Controller) Tell View to draw the current Model state
        this.view.draw(this);

        // 4. (Controller) Check Model state for game over
        if (this.model.isGameOver() && !this.model.isLevelFinished()) {
            this.model.setGameOver();
        }

        // 5. (Controller) Check Model state for level finished
        if (this.model.isLevelFinished() && this.model.getScreenFinishedCooldown().checkFinished()) {
            this.model.processLevelCompletion();
            this.isRunning = false;
        }
    }

    /**
     * Returns a GameState object representing the status of the game.
     * (Passthrough to the model)
     *
     * @return Current game state.
     */
    public final GameState getGameState() {
        return this.model.getGameState();
    }
}