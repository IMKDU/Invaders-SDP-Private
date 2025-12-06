package screen;

import engine.Cooldown;
import engine.DTO.HUDInfoDTO;
import engine.GameState;
import engine.level.Level;
import entity.GameConstant;
import entity.GameModel;
import entity.Ship;

import java.awt.event.KeyEvent;

/**
 * Implements the game screen, where the action happens.
 * Acts as the CONTROLLER in the MVC pattern.
 * - Controls game flow, inputs, and timing
 * - Updates the GameModel (Model)
 * - Passes data to GameView (View) via HUDInfoDTO
 */
public class GameScreen extends Screen {

    /** Current level data. */
    private final Level currentLevel;
    private final boolean bonusLife;
    private final int maxLives;
    private final GameState gameState;

    /** MVC Components */
    private GameModel model;   // Model
    private GameView view;     // View

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
     * Initializes the Model and View.
     */
    @Override
    public final void initialize() {
        super.initialize();

        // Create Model and View
        this.model = new GameModel(
                this.gameState, this.currentLevel,
                this.bonusLife, this.maxLives,
                this.width, GameConstant.ITEMS_SEPARATION_LINE_HEIGHT
        );
        this.view = new GameView(this.model,this.drawManager);

        // Initialize Model
        this.model.initialize();

        this.inputDelay = new Cooldown(GameModel.INPUT_DELAY);
        this.inputDelay.reset();
    }

    /**
     * Game loop.
     */
    @Override
    public final int run() {
        super.run();

        int finalScore = this.model.calculateFinalScore();
        this.logger.info("Screen cleared with a score of " + finalScore);

        return this.returnCode;
    }

    /**
     * Main Controller Loop (Model ↔ View coordination)
     */
    @Override
    protected final void update() {
        super.update();

        // Input Handling
        if (this.inputDelay.checkFinished() && !this.model.isLevelFinished()) {

            if (!this.model.isTimerRunning()) {
                this.model.startTimer();
            }
			// Handle W,A,S,D and arrow keys to move Player 1 & 2
			handleP1MovementInput();
			handleP2MovementInput();
			// Handle keyboard input to activate player skills
			handlePlayerSkillInput();

            // Update game world (Model)
            this.model.updateGameWorld();
        }

        // Timer
        if (this.model.isTimerRunning()) {
            this.model.updateElapsedTime();
        }

        // Create DTO and render via View
        HUDInfoDTO hudInfo = createHUDInfoDTO();
        this.view.render(hudInfo);

        // Game Over / Level Finish check
        if (this.model.isGameOver() && !this.model.isLevelFinished()) {
            this.model.setGameOver();
        }

        if (this.model.isLevelFinished() && this.model.getScreenFinishedCooldown().checkFinished()) {
            this.model.processLevelCompletion();
            this.isRunning = false;
        }
    }
	private void handleP1MovementInput() {
		// Player 1 Input
        model.setIsTelportP1(false);
		if (model.getLivesP1() > 0 && model.getShip() != null && !model.getShip().isDestroyed()) {

			boolean shift1 = inputManager.isLeftShiftDown();

			boolean up    = inputManager.isP1KeyDown(KeyEvent.VK_W);
			boolean down  = inputManager.isP1KeyDown(KeyEvent.VK_S);
			boolean left  = inputManager.isP1KeyDown(KeyEvent.VK_A);
			boolean right = inputManager.isP1KeyDown(KeyEvent.VK_D);

			if (right && up) {
				model.playerMoveOrTeleport(1, "RIGHT_UP", shift1);
			} else if (right && down) {
				model.playerMoveOrTeleport(1, "RIGHT_DOWN", shift1);
			} else if (left && up) {
				model.playerMoveOrTeleport(1, "LEFT_UP", shift1);
			} else if (left && down) {
				model.playerMoveOrTeleport(1, "LEFT_DOWN", shift1);
			} else if (right) {
				model.playerMoveOrTeleport(1, "RIGHT", shift1);
			} else if (left) {
				model.playerMoveOrTeleport(1, "LEFT", shift1);
			} else if (up) {
				model.playerMoveOrTeleport(1, "UP", shift1);
			} else if (down) {
				model.playerMoveOrTeleport(1, "DOWN", shift1);
			}
			if (inputManager.isP1KeyDown(KeyEvent.VK_SPACE)) {
				model.playerFire(1);
			}
		}
	}

	private void handleP2MovementInput(){
		model.setIsTelportP2(false);
		// Player 2 Input
		if (model.getShipP2() != null && model.getLivesP2() > 0 && !model.getShipP2().isDestroyed()) {

			boolean isSlashDown = inputManager.isRightShiftDown();

			boolean up    = inputManager.isP2KeyDown(KeyEvent.VK_UP);
			boolean down  = inputManager.isP2KeyDown(KeyEvent.VK_DOWN);
			boolean left  = inputManager.isP2KeyDown(KeyEvent.VK_LEFT);
			boolean right = inputManager.isP2KeyDown(KeyEvent.VK_RIGHT);

			if (right && up) {
				model.playerMoveOrTeleport(2, "RIGHT_UP", isSlashDown);
			} else if (right && down) {
				model.playerMoveOrTeleport(2, "RIGHT_DOWN", isSlashDown);
			} else if (left && up) {
				model.playerMoveOrTeleport(2, "LEFT_UP", isSlashDown);
			} else if (left && down) {
				model.playerMoveOrTeleport(2, "LEFT_DOWN", isSlashDown);
			} else if (right) {
				model.playerMoveOrTeleport(2, "RIGHT", isSlashDown);
			} else if (left) {
				model.playerMoveOrTeleport(2, "LEFT", isSlashDown);
			} else if (up) {
				model.playerMoveOrTeleport(2, "UP", isSlashDown);
			} else if (down) {
				model.playerMoveOrTeleport(2, "DOWN", isSlashDown);
			}

			if (inputManager.isP2KeyDown(KeyEvent.VK_ENTER)) {
				model.playerFire(2);
			}
		}
	}

    /**
     * Handles player skill input (C key for Player 1, Right Ctrl for Player 2).
     * Manages charging skill activation and cancellation.
     */
    private void handlePlayerSkillInput() {

        boolean isChargingDownP1 = inputManager.isP1KeyDown(KeyEvent.VK_CONTROL);
        boolean isChargingDownP2 = inputManager.isP2KeyDown(KeyEvent.VK_BACK_SLASH);

        if (model.getShip() != null && model.getLivesP1() > 0 && !model.getShip().isDestroyed()) {
            handleChargingInput(model.getShip(), isChargingDownP1);
        }

        if (model.getShipP2() != null && model.getLivesP2() > 0 && !model.getShipP2().isDestroyed()) {
            handleChargingInput(model.getShipP2(), isChargingDownP2);
        }


        boolean isOriginDown = inputManager.isKeyDown(KeyEvent.VK_O);
        if (isOriginDown) {
            handleOriginSkillInput();
        }
    }


    private void handleChargingInput(Ship ship, boolean isKeyPressed) {

        if (isKeyPressed) {
            ship.startCharging();
        } else {
            if (ship.isCharging()) {
                ship.stopCharging();
            }
        }
    }

    private void handleOriginSkillInput() {
        Ship s1 = model.getShip();
        Ship s2 = model.getShipP2();

        if ((s1 != null && s1.isControlsDisabled()) ||
                (s2 != null && s2.isControlsDisabled())) {
            return;
        }

        if (s1 != null && !s1.isDestroyed() && model.getLivesP1() > 0) {
            s1.useSkill(Ship.SkillType.ORIGIN);
            return;
        }

        if (s2 != null && !s2.isDestroyed() && model.getLivesP2() > 0) {
            s2.useSkill(Ship.SkillType.ORIGIN);

        }
    }


    /**
     * Builds the DTO that passes data from Model to View.
     */
	private HUDInfoDTO createHUDInfoDTO() {

		float teleportCooldownP1 =
				(model.getShip() != null)
						? model.getShip().getTeleportCooldownProgress()
						: 1f;

		float teleportCooldownP2 =
				(model.getShipP2() != null)
						? model.getShipP2().getTeleportCooldownProgress()
						: 1f;

		return new HUDInfoDTO(
				getWidth(),
				getHeight(),
				model.getScoreP1(),
				model.getScoreP2(),
				model.getCoin(),
				model.getLivesP1(),
				model.getLivesP2(),
				model.getLevel(),
				model.getShip(),
				model.getShipP2(),
				model.getElapsedTime(),
				model.getCurrentLevel().getLevelName(),
				model.getAchievementText(),
				model.getHealthPopupText(),
				teleportCooldownP1,
				teleportCooldownP2,
                GameModel.getUsed_origin()
		);
	}


    /**
     * Returns the game state for other systems.
     */
    public final GameState getGameState() {
        return this.model.getGameState();
    }
}
