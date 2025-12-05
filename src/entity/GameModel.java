package entity;

import engine.*;
import engine.level.Level;

import java.awt.*;
import java.util.*;

import entity.pattern.ApocalypseAttackPattern;

import java.util.List;
import java.util.logging.Logger;

import engine.level.ItemDrop;
import entity.pattern.ISkill;
import entity.skills.OriginSkill;

/**
 * Implements the Model for the game screen.
 * Contains all game state and game logic.
 */
public class GameModel {

    /** Milliseconds until the screen accepts user input. */
    public static final int INPUT_DELAY = 6000;
    /** Bonus score for each life remaining at the end of the level. */
    private static final int LIFE_SCORE = 100;
    /** Minimum time between bonus ship's appearances. */
    private static final int BONUS_SHIP_INTERVAL = 20000;
    /** Maximum variance in the time between bonus ship's appearances. */
    private static final int BONUS_SHIP_VARIANCE = 10000;
    /** Time until bonus ship explosion disappears. */
    private static final int BONUS_SHIP_EXPLOSION = 500;
    /** Time until bonus ship explosion disappears. */
    private static final int BOSS_EXPLOSION = 600;
    /** Time from finishing the level to screen change. */
    private static final int SCREEN_CHANGE_INTERVAL = 1500;

    /** Current level data (direct from Level system). */
    private Level currentLevel;
    /** Current difficulty level number. */
    private int level;
    /** Formation of enemy ships. */
    private EnemyShipFormationModel enemyShipFormationModel;
    /** Formation of special enemy ships. */
    private EnemyShipSpecialFormation enemyShipSpecialFormation;
    /** Player's ship. */
    private Ship ship;
    /** Second Player's ship. */
    private Ship shipP2;
    /** Bonus enemy ship that appears sometimes. */
    private EnemyShip enemyShipSpecial; // This seems unused, but keeping for compatibility
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
    /** ZetaBoss */
    private MidBoss zetaBoss;
    /** GammaBoss */
    private MidBoss gammaBoss;
    /** Set of all bullets fired by on-screen ships. */
    private Set<Bullet> bullets;
    /** Set of all dropItems dropped by on screen ships. */
    private Set<DropItem> dropItems;
    /** List of subship */
    private List<SubShip> subShips;
    /** Current score. */
    private int score;
    // === [ADD] Independent scores for two players ===
    private int scoreP1 = 0;
    private int scoreP2 = 0;
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

    /** bossBullets carry bullets which Boss fires */
    private Set<Bullet> bossBullets;
	private Set<LaserBeam> bossLasers;
    /** Is the bullet on the screen erased */
    private boolean is_cleared = false;
    /** Timer to track elapsed time. */
    private GameTimer gameTimer;
    /** Elapsed time since the game started. */
    private long elapsedTime;
    // Achievement popup
    private String achievementText;
    private Cooldown achievementPopupCooldown;
    private boolean isTeleportP1;
    private boolean isTeleportP2;
    private enum StagePhase{wave, boss_wave};
    private StagePhase currentPhase;
    /** Health change popup. */
    private String healthPopupText;
    private Cooldown healthPopupCooldown;

    private GameState gameState;
    private Logger logger;
    private int width;
    private int height;

    /** Milliseconds until the screen accepts user input. */
    private Cooldown inputDelay;
    private int FinalSkillCnt;

    private List<Ship> ships;
	private Set<BlackHole> blackHoles;
	private Set<MidBossMob> midBossMobs;
    private Set<Explosion> explosions;

    private int teleportFromP1X;
    private int teleportFromP1Y;
    private int afterTeleportFromP1X;
    private int afterTeleportFromP1Y;
    private int teleportFromP2X;
    private int teleportFromP2Y;
    private int afterTeleportFromP2X;
    private int afterTeleportFromP2Y;


    public GameModel(GameState gameState, Level level, boolean bonusLife, int maxLives, int width, int height) {
        this.logger = Core.getLogger();
        this.width = width;
        this.height = height;
        this.FinalSkillCnt = 1;

        this.currentLevel = level;
        this.bonusLife = bonusLife;
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
     * Initializes basic model properties, and adds necessary elements.
     */
    public final void initialize() {
        /** Initialize the bullet Boss fired */
        this.bossBullets = new HashSet<>();
		this.bossLasers = new HashSet<>();
		this.blackHoles = new HashSet<>();
		this.midBossMobs = new HashSet<>();
		this.explosions = new HashSet<>();

        enemyShipFormationModel = new EnemyShipFormationModel(this.currentLevel, width);
        this.enemyShipFormationModel.applyEnemyColor(this.currentLevel.getColorForLevel());
        this.ship = new Ship(this.width / 4, GameConstant.ITEMS_SEPARATION_LINE_HEIGHT * 13 / 15,Color.GREEN,true);
        this.ship.setPlayerId(1);   //=== [ADD] Player 1 ===

        this.shipP2 = new Ship(this.width * 3 / 4, GameConstant.ITEMS_SEPARATION_LINE_HEIGHT * 13 / 15,Color.RED,false);
        this.shipP2.setPlayerId(2); // === [ADD] Player2 ===
        // special enemy initial

        this.subShips = new ArrayList<>();

        GameSettings specialSettings = new GameSettings(
				currentLevel.getFormationWidth(),
		        currentLevel.getFormationHeight(),
		        currentLevel.getBaseSpeed(),
		        currentLevel.getShootingFrecuency()
	    );

	    enemyShipSpecialFormation = new EnemyShipSpecialFormation(specialSettings,
                Core.getVariableCooldown(BONUS_SHIP_INTERVAL, BONUS_SHIP_VARIANCE),
                new Cooldown(BONUS_SHIP_EXPLOSION));
        this.bossExplosionCooldown = new Cooldown(BOSS_EXPLOSION);
        this.screenFinishedCooldown = new Cooldown(SCREEN_CHANGE_INTERVAL);
        this.bullets = new HashSet<Bullet>();
        this.dropItems = new HashSet<DropItem>();

        // Special input delay / countdown.
        this.gameStartTime = System.currentTimeMillis();
        this.inputDelay = new Cooldown(INPUT_DELAY);
        this.inputDelay.reset();


        this.gameTimer = new GameTimer();
        this.elapsedTime = 0;
        this.finalBoss = null;
        this.omegaBoss = null;
        this.zetaBoss = null;
        this.gammaBoss = null;
        this.currentPhase = StagePhase.wave;

        /** ships list for boss argument */
        this.ships = new ArrayList<>();
        if (this.ship != null && this.livesP1 > 0) ships.add(this.ship);
        if (this.shipP2 != null && this.livesP2 > 0) ships.add(this.shipP2);

        this.ship.setModel(this);
        this.shipP2.setModel(this);
    }

    /**
     * Unified scoring entry: maintains both P1/P2 and legacy this.score (total score)
     */
    private void addPointsFor(Bullet bullet, int pts) {
        Integer owner = (bullet != null ? bullet.getOwnerId() : null);
        if (owner != null && owner == 2) {
            this.scoreP2 += pts;   // P2
        } else {
            this.scoreP1 += pts;   // Default to P1 (for null compatibility)
        }
        this.score += pts;        // Keep maintaining the total score, for legacy process compatibility
    }

    /**
     * Processes a player move command received from the Controller.
     * (Includes boundary checking logic)
     * @param playerNum (1 or 2)
     * @param direction ("RIGHT", "LEFT", "UP", "DOWN")
     */
	public void playerMoveOrTeleport(int playerNum, String direction, boolean teleport) {
		Ship ship = (playerNum == 1) ? this.ship : this.shipP2;
		if (ship == null || ship.isDestroyed()) return;
		if (teleport && ship.canTeleport() && ship.getPlayerId() == 1) {
            this.teleportFromP1X = ship.positionX;
            this.teleportFromP1Y = ship.positionY;
            this.isTeleportP1 = true;
			ship.teleport(direction, width, height);
            this.afterTeleportFromP1X = ship.positionX;
            this.afterTeleportFromP1Y = ship.positionY;
		}
        else if (teleport && ship.canTeleport() && ship.getPlayerId() == 2){
            this.teleportFromP2X = ship.positionX;
            this.teleportFromP2Y = ship.positionY;
            this.isTeleportP2 = true;
            ship.teleport(direction, width, height);
            this.afterTeleportFromP2X = ship.positionX;
            this.afterTeleportFromP2Y = ship.positionY;
        }
        else {
			ship.move(direction, this.width, this.height);

		}
	}

    /**
     * Processes a player fire command received from the Controller.
     * (Includes firing logic, bulletsShot count, and Achievement management)
     * @param playerNum (1 or 2)
     */
    public void playerFire(int playerNum) {
        Ship ship = (playerNum == 1) ? this.ship : this.shipP2;
        // If the ship doesn't exist or is destroyed, do nothing
        if (ship == null || ship.isDestroyed()) return;

        // Firing logic brought over from the original processPlayerInput
        if (ship.shoot(this.bullets)) {
            this.bulletsShot++;
            AchievementManager.getInstance().onShotFired();

            // Sub-ships fire together
            if (this.subShips != null) {
                for (SubShip sub : subShips) {
                    // Fire only if the sub-ship is not destroyed and belongs to the player firing
                    if (!sub.isDestroyed() && sub.getOwner().getPlayerId() == playerNum) {
                        sub.shoot(this.bullets);
                    }
                }
            }
        }
    }

    public void updateGameWorld() {
        // Phase 1: Update state/position of ALL entities
        this.updateAllEntities();

        // Phase 2: Process interactions and collisions
        this.processAllCollisions();

        // Phase 2.5: Process charging laser collisions
        this.processChargingLaserCollisions();

        // Phase 3: Clean up destroyed or off-screen entities
        this.cleanupAllEntities();
    }

    /**
     * Updates all non-player-controlled game logic.
     */
    public void updateAllEntities() {
        switch (this.currentPhase) {
            case wave:
                if (!DropItem.isTimeFreezeActive()) {
                    this.enemyShipFormationModel.update();
                    this.enemyShipFormationModel.shoot(this.bullets);
                }
                if (this.enemyShipFormationModel.isEmpty()) {
                    this.currentPhase = StagePhase.boss_wave;
                }
                break;
            case boss_wave:
                if (this.finalBoss == null && this.omegaBoss == null && this.zetaBoss == null && this.gammaBoss == null){
                    bossReveal();
                    this.enemyShipFormationModel.clear();
                }
                if(this.finalBoss != null){
                    finalbossManage();
                }
                if (this.omegaBoss != null){
                    this.omegaBoss.update();
                    if (this.omegaBoss instanceof OmegaBoss omega) {
                        bossBullets.addAll(omega.getBullets());
                    }

                    if (this.omegaBoss.isDestroyed()) {
                        if ("omegaAndZetaAndFinal".equals(this.currentLevel.getBossId())) {
                            this.omegaBoss = null;
                            this.zetaBoss = new ZetaBoss(Color.MAGENTA, this.ship, this.ships);
                            this.logger.info("Zeta Boss has spawned!");
                        } else {

                        }
                    }
                }

                // ZetaBoss logic added
                if (this.zetaBoss != null) {
                    this.zetaBoss.update();

                    if (this.zetaBoss instanceof ZetaBoss zeta) {
                        if (zeta.getBossPattern() != null) {
                            bossBullets.addAll(zeta.getBossPattern().getBullets());
                        }
                    }
                    validateBlackHolesSet();

                    // Handle BlackHole pattern for visualization
	                if(this.zetaBoss.getCurrentBlackHole()!=null && this.zetaBoss.getCurrentBlackHole().getBlackHoles()!=null){
		                this.blackHoles = this.zetaBoss.getCurrentBlackHole().getBlackHoles();
	                }

                    // Handle Apocalypse pattern damage
                    ApocalypseAttackPattern pattern = this.zetaBoss.getApocalypsePattern();
                    if (pattern != null && pattern.isAttacking()) {
                        float progress = pattern.getAttackAnimationProgress();
                        executeApocalypseDamage(pattern.getSafeZoneColumn(), progress);
                    }

                    if (this.zetaBoss.isDestroyed()) {
                        if ("omegaAndZetaAndFinal".equals(this.currentLevel.getBossId())) {
                            this.zetaBoss = null;
                            this.finalBoss = new FinalBoss(ships);
                            this.logger.info("Final Boss has spawned!");
                        }
                    }
                }

                // GammaBoss logic added
                if (this.gammaBoss != null) {
                    this.gammaBoss.update();
                    if (this.gammaBoss instanceof GammaBoss gamma) {
						if(gamma.getBoom()!=null){
							this.explosions.add(gamma.getBoom());
						}
                        this.bossBullets.addAll(gamma.getBossPattern().getBullets());
						bossLasers.addAll(gamma.getBossPattern().getLasers());
                    }

                    if (this.gammaBoss.isDestroyed()) {
                        this.logger.info("Gamma Boss destroyed!");
                    }
                }

				// Remove expired boss-derived entities from Set
	            validateBossBulletsSet();
	            validateLaserBeamsSet();
				validateExplosionsSet();
				validateBlackHolesSet();

                boolean isFinalBossAlive = (this.finalBoss != null && !this.finalBoss.isDestroyed());
                boolean isOmegaBossAlive = (this.omegaBoss != null && !this.omegaBoss.isDestroyed());
                boolean isZetaBossAlive = (this.zetaBoss != null && !this.zetaBoss.isDestroyed());
                boolean isGammaBossAlive = (this.gammaBoss != null && !this.gammaBoss.isDestroyed());
                if (!GameConstant.origin_skill_activated) {
                    if (!isFinalBossAlive && !isOmegaBossAlive && !isZetaBossAlive && !isGammaBossAlive) {
                        if (!this.levelFinished) {
                            this.levelFinished = true;
                            this.screenFinishedCooldown.reset();
                        }
                    }
                    break;
                }
        }
        this.ship.update();
        if (this.shipP2 != null) {
            this.shipP2.update();
        }

        // Update SubShips
        if (this.subShips != null) {
            for (SubShip subShip : subShips) {
                subShip.update();
            }
            // Remove destroyed or expired sub-ships
            subShips.removeIf(SubShip::isDestroyed);
        }

        // special enemy update
        this.enemyShipSpecialFormation.update();

        for (Bullet bullet : this.bullets) {
            bullet.update();
        }

        for (DropItem dropItem : this.dropItems) {
            dropItem.update();
        }

        if (ship != null) {
            ISkill origin = ship.getSkill(Ship.SkillType.ORIGIN);
            if (origin instanceof OriginSkill os) {
                os.update();
            }
        }

        if (shipP2 != null) {
            ISkill origin = shipP2.getSkill(Ship.SkillType.ORIGIN);
            if (origin instanceof OriginSkill os) {
                os.update();
            }
        }
    }

	/**
	 * Handles collision detection exclusively for BombBullet.
	 * BombBullets are excluded from the normal entity collision system,
	 * so this method manually checks their collisions and safely removes them after explosion.
	 */
	private void processBombBulletCollisions() {

		// Snapshot used to safely iterate (BombBullet may remove itself during explosion)
		List<Bullet> snapshot = new ArrayList<>(bullets);

		// Separate removal set
		Set<BombBullet> toRemove = new HashSet<>();

		// Prepare enemy formations list
		List<Iterable<EnemyShip>> enemyFormations = new ArrayList<>();
		if (enemyShipFormationModel != null) enemyFormations.add(enemyShipFormationModel);
		if (enemyShipSpecialFormation != null) enemyFormations.add(enemyShipSpecialFormation);

		// Prepare bosses list
		List<BossEntity> bosses = new ArrayList<>();
		if (omegaBoss != null) bosses.add(omegaBoss);
		if (zetaBoss != null) bosses.add(zetaBoss);
		if (finalBoss != null) bosses.add(finalBoss);

		for (Bullet b : snapshot) {

			if (!(b instanceof BombBullet)) continue;
			BombBullet bomb = (BombBullet) b;
			boolean exploded = false;

			// --- Check Enemy Formations ---
			for (Iterable<EnemyShip> formation : enemyFormations) {
				for (EnemyShip e : formation) {
					if (!e.isDestroyed() && checkCollision(bomb, e)) {
						bomb.explode(this);
						exploded = true;
						break;
					}
				}
				if (exploded) break;
			}

			// --- Check Bosses ---
			if (!exploded) {
				for (BossEntity boss : bosses) {
					if (!boss.isDestroyed() && checkCollision(bomb, (Entity) boss)) {
						bomb.explode(this);
						exploded = true;
						break;
					}
				}
			}

			// --- Check Off-Screen ---
			if (!exploded && bomb.getPositionY() < GameConstant.STAT_SEPARATION_LINE_HEIGHT) {
				exploded = true;
			}

			if (exploded) {
				toRemove.add(bomb);
			}
		}
		// Safe removal after iteration
		bullets.removeAll(toRemove);
	}

	/**
	 * Detects collisions between all active entities.
	 * Each pair of collidables is checked, and their collision handlers are invoked.
	 */
	private void processAllCollisions() {

		List<Entity> entities = new ArrayList<>();

		if (ship != null && livesP1 > 0 && !ship.isDestroyed()) {
			entities.add(ship);
		}

		if (shipP2 != null && livesP2 > 0 && !shipP2.isDestroyed()) {
			entities.add(shipP2);
		}

        // Add sub-ships to collision check targets
        for (SubShip sub : subShips) {
            if (!sub.isDestroyed()) entities.add(sub);
        }

		for (EnemyShip e : enemyShipFormationModel) {
			if (e != null && !e.isDestroyed()) entities.add(e);
		}

		for (EnemyShip e : enemyShipSpecialFormation) {
			if (e != null && !e.isDestroyed()) entities.add(e);
		}

		if (finalBoss != null && !finalBoss.isDestroyed()) entities.add(finalBoss);
		if (omegaBoss != null && !omegaBoss.isDestroyed()) entities.add(omegaBoss);
        if (zetaBoss != null && !zetaBoss.isDestroyed()) entities.add(zetaBoss);
        if (gammaBoss != null && !gammaBoss.isDestroyed()) entities.add(gammaBoss);

        if (midBossMobs != null){ entities.addAll(midBossMobs); }
        if (explosions != null) entities.addAll(explosions);
		// First, handle BombBullet collisions separately because they do NOT participate in normal collision logic.
		processBombBulletCollisions();

		// Add only regular bullets to the collision entity list.
		bullets.stream()
				.filter(b -> !(b instanceof BombBullet))
				.forEach(entities::add);
		entities.addAll(bossBullets);
		entities.addAll(dropItems);

		for (int i = 0; i < entities.size(); i++) {
			Entity a = entities.get(i);

			for (int j = i + 1; j < entities.size(); j++) {
				Entity b = entities.get(j);

				if (checkCollision(a, b)) {
					a.onCollision(b, this);
					b.onCollision(a, this);
				}
			}
		}
		for (LaserBeam laser : bossLasers){
			for (Entity a : entities) {
				if(laser.isActive() && checkLaserRotatedCollision(a, laser)){
					a.onCollision(laser, this);
				}
			}
		}
		entities.clear();
	}

    /**
     * Processes collisions between charging laser beams and enemies.
     * The laser beam destroys all enemies in its vertical path.
     */
    private void processChargingLaserCollisions() {
        // Process Player 1's laser
        if (ship != null && livesP1 > 0 && !ship.isDestroyed() && ship.isLaserActive()) {
            processLaserCollision(ship, 1);
        }

        // Process Player 2's laser
        if (shipP2 != null && livesP2 > 0 && !shipP2.isDestroyed() && shipP2.isLaserActive()) {
            processLaserCollision(shipP2, 2);
        }
    }

    /**
     * Processes collision for a single ship's laser beam.
     * @param ship The ship firing the laser
     * @param playerNum Player number (1 or 2)
     */
    private void processLaserCollision(Ship ship, int playerNum) {
        int laserCenterX = ship.getPositionX() + ship.getWidth() / 2;
        int laserLeft = laserCenterX - ship.getWidth() / 2;
        int laserRight = laserCenterX + ship.getWidth() / 2;
        int laserTop = 0;
        int laserBottom = ship.getPositionY();

        // Check collision with regular enemies
        for (EnemyShip enemy : enemyShipFormationModel) {
            if (enemy != null && !enemy.isDestroyed()) {
                if (checkLaserEntityCollision(enemy, laserLeft, laserRight, laserTop, laserBottom)) {
                    destroyEnemyByLaser(enemy, playerNum);
                }
            }
        }

        // Check collision with special enemies
        for (EnemyShip enemy : enemyShipSpecialFormation) {
            if (enemy != null && !enemy.isDestroyed()) {
                if (checkLaserEntityCollision(enemy, laserLeft, laserRight, laserTop, laserBottom)) {
                    destroyEnemyByLaser(enemy, playerNum);
                }
            }
        }

        // Check collision with final boss
        if (finalBoss != null && !finalBoss.isDestroyed()) {
            if (checkLaserEntityCollision(finalBoss, laserLeft, laserRight, laserTop, laserBottom)) {
                // Deal damage to boss (laser deals 1 damage per frame it's active)
                finalBoss.takeDamage(1);
                if (finalBoss.isDestroyed()) {
                    handleAnyBossDestruction(finalBoss, playerNum);
                }
            }
        }

        // Check collision with omega boss
        if (omegaBoss != null && !omegaBoss.isDestroyed()) {
            if (checkLaserEntityCollision(omegaBoss, laserLeft, laserRight, laserTop, laserBottom)) {
                // Deal damage to omega boss (laser deals 1 damage per frame it's active)
                omegaBoss.takeDamage(1);
                if (omegaBoss.isDestroyed()) {
                    handleAnyBossDestruction(omegaBoss, playerNum);
                }
            }
        }

        // Check collision with gamma boss
        if (gammaBoss != null && !gammaBoss.isDestroyed()) {
            if (checkLaserEntityCollision(gammaBoss, laserLeft, laserRight, laserTop, laserBottom)) {
                // Deal damage to gamma boss (laser deals 1 damage per frame it's active)
                gammaBoss.takeDamage(1);
                if (gammaBoss.isDestroyed()) {
                    handleAnyBossDestruction(gammaBoss, playerNum);
                }
            }
        }

        // Check collision with zeta boss
        if (zetaBoss != null && !zetaBoss.isDestroyed()) {
            if (checkLaserEntityCollision(zetaBoss, laserLeft, laserRight, laserTop, laserBottom)) {
                // Deal damage to gamma boss (laser deals 1 damage per frame it's active)
                zetaBoss.takeDamage(1);
                if (zetaBoss.isDestroyed()) {
                    handleAnyBossDestruction(zetaBoss, playerNum);
                }
            }
        }
    }

    /**
     * Checks if an entity collides with the laser beam area.
     * @param bounds The entity to check collision with
     * @param laserLeft Left boundary of the laser beam
     * @param laserRight Right boundary of the laser beam
     * @param laserTop Top boundary of the laser beam
     * @param laserBottom Bottom boundary of the laser beam
     * @return True if the entity collides with the laser beam area
     */
    private boolean checkLaserEntityCollision(HasBounds bounds, int laserLeft, int laserRight, int laserTop, int laserBottom) {
        int entityLeft = bounds.getPositionX();
        int entityRight = bounds.getPositionX() + bounds.getWidth();
        int entityTop = bounds.getPositionY();
        int entityBottom = bounds.getPositionY() + bounds.getHeight();

        // Check if entity overlaps with laser beam area
        boolean horizontalOverlap = (entityRight > laserLeft) && (entityLeft < laserRight);
        boolean verticalOverlap = (entityBottom > laserTop) && (entityTop < laserBottom);

        return horizontalOverlap && verticalOverlap;
    }

	/**
	 * Checks if an entity collides with the laser beam area.
	 * @param entityBounds The entity to check collision with
	 * @param laserBounds Bottom boundary of the laser beam
	 * @return True if the entity collides with the laser beam area
	 */
	private boolean checkLaserRotatedCollision(HasBounds entityBounds, LaserInfo laserBounds) {
		Point p1 = laserBounds.getStartPosition();
		Point p2 = laserBounds.getEndPosition();

		// except if p1 or p2 are not valid
		if (p1 == null || p2 == null) return false;

		// calculate points
		int rX = entityBounds.getPositionX();
		int rY = entityBounds.getPositionY();
		int rW = entityBounds.getWidth();
		int rH = entityBounds.getHeight();
		int rRight = rX + rW;
		int rBottom = rY + rH;

		// check if the laser's startPoint and endPoint is included in the bounding box
		boolean p1Inside = (p1.x >= rX && p1.x <= rRight && p1.y >= rY && p1.y <= rBottom);
		boolean p2Inside = (p2.x >= rX && p2.x <= rRight && p2.y >= rY && p2.y <= rBottom);

		if (p1Inside || p2Inside) {
			return true;
		}

		// check laser passes across one of the (Top, Bottom, Left, Right)
		// Top Edge
		if (java.awt.geom.Line2D.linesIntersect(p1.x, p1.y, p2.x, p2.y, rX, rY, rRight, rY)) return true;
		// Bottom Edge
		if (java.awt.geom.Line2D.linesIntersect(p1.x, p1.y, p2.x, p2.y, rX, rBottom, rRight, rBottom)) return true;
		// Left Edge
		if (java.awt.geom.Line2D.linesIntersect(p1.x, p1.y, p2.x, p2.y, rX, rY, rX, rBottom)) return true;
		// Right Edge
		if (java.awt.geom.Line2D.linesIntersect(p1.x, p1.y, p2.x, p2.y, rRight, rY, rRight, rBottom)) return true;

		return false;
	}

    /**
     * Destroys an enemy hit by the laser and awards points.
     */
    private void destroyEnemyByLaser(EnemyShip enemy, int playerNum) {
        int pts = enemy.getPointValue();

        // Award points to the appropriate player
        if (playerNum == 2) {
            this.scoreP2 += pts;
        } else {
            this.scoreP1 += pts;
        }
        this.score += pts;
        coin += pts / 10;

        AchievementManager.getInstance().onEnemyDefeated();

        attemptItemDrop(enemy);

        String type = enemy.getEnemyType();
        if ("enemySpecial".equals(type)) {
            if (enemyShipSpecialFormation != null) {
                enemyShipSpecialFormation.destroy(enemy);
            }
        } else {
            if (enemyShipFormationModel != null) {
                enemyShipFormationModel.destroy(enemy);
            }
        }
    }

    /**
     * Handles boss destruction and score award.
     */
    private void handleAnyBossDestruction(BossEntity boss, int playerNum) {
        int bossPoints = boss.getPointValue();
        if (playerNum == 2) {
            this.scoreP2 += bossPoints;
        } else {
            this.scoreP1 += bossPoints;
        }
        this.score += bossPoints;
        coin += bossPoints / 10;
        bossExplosionCooldown.reset();
    }

    /**
	 * Handles damage and rewards when a player bullet hits a normal enemy.
	 */
	public void requestEnemyHitByPlayerBullet(Bullet bullet, EnemyShip enemy) {
		if (!bullets.contains(bullet)) return;
		if (enemy.isDestroyed()) return;

		int pts = enemy.getPointValue();
		addPointsFor(bullet, pts);
		coin += pts / 10;
		AchievementManager.getInstance().onEnemyDefeated();

		attemptItemDrop(enemy);

		String type = enemy.getEnemyType();
		if ("enemySpecial".equals(type)) {
			if (enemyShipSpecialFormation != null) {
				enemyShipSpecialFormation.destroy(enemy);
			}
		} else {
			if (enemyShipFormationModel != null) {
				enemyShipFormationModel.destroy(enemy);
			}
		}

		if (!bullet.penetration()) {
			bullets.remove(bullet);
		}
	}

	/**
	 * Applies damage to a player ship.
	 * Handles hit effect, invincibility, life reduction, and game-over check.
	 */
	public void requestShipDamage(Ship ship, int amount) {

		if (ship.isInvincible()) return;

		ship.destroy();

		if (ship.getPlayerId() == 1) {
			livesP1 = Math.max(0, livesP1 - amount);
		} else {
			livesP2 = Math.max(0, livesP2 - amount);
		}

        if ((ship.getPlayerId() == 1 && livesP1 == 0) ||
                (ship.getPlayerId() == 2 && livesP2 == 0)) {

            ships.remove(ship);

            if (this.bossBullets != null) {
                for (Bullet b : this.bossBullets) {
                    // Check if bullet is guided missile
                    if (b instanceof GuidedBullet) {
                        GuidedBullet gb = (GuidedBullet) b;
                        // if the target died
                        if (gb.getTarget() == ship) {
                            gb.setTarget(null);
                        }
                    }
                }
            }
        }
		if (this.isGameOver()) {
			this.setGameOver();
		}
	}

	public void requestRemoveBullet(Bullet bullet) {
		bullets.remove(bullet);
	}

	public void requestRemoveBossBullet(BossBullet bullet) {
		bossBullets.remove(bullet);
	}


	public void requestBossHitByPlayerBullet(Bullet bullet, BossEntity boss) {


		boss.takeDamage(1);

		if (!bullet.penetration()) {
			bullets.remove(bullet);
		}

		if (boss.getHealPoint() <= 0) {
			boss.destroy();

			int pts = boss.getPointValue();
			addPointsFor(bullet, pts);
			this.coin += pts / 10;

			AchievementManager.getInstance().unlockAchievement("Boss Slayer");
		}
	}

	/**
	 * When the player collides with an enemy, apply crash damage.
	 */
	public void requestPlayerCrash(Ship ship, Entity enemy) {

		if (enemy instanceof EnemyShip e) {
			if (!e.isDestroyed()) {
				String type = e.getEnemyType();

				if ("enemySpecial".equals(type)) {
					if (enemyShipSpecialFormation != null) {
						enemyShipSpecialFormation.destroy(e);
					}
				} else {
					if (enemyShipFormationModel != null) {
						enemyShipFormationModel.destroy(e);
					}
				}
			}
		}
		requestShipDamage(ship, 1);
	}


	/**
	 * Applies the effect of a collected drop item to the player ship.
	 */
	public void requestApplyItem(Ship ship, DropItem item) {

		if (!dropItems.contains(item)) return;

		ItemHUDManager.getInstance().addDroppedItem(item.getItemType());

		switch (item.getItemType()) {
			case Heal:
				if (ship.getPlayerId() == 1) gainLife();
				else gainLifeP2();
				break;

			case Shield:
				ship.activateInvincibility(5000);
				break;

			case Stop:
				DropItem.applyTimeFreezeItem(3000);
				break;

			case Bomb:
				ship.enableBomb(GameConstant.BOMB_ITEM_SHOTS);
				break;

			case Coin:
				this.coin += GameConstant.COIN_ITEM_VALUE;
				break;

			case SubShip:
                DropItem.activateSubShip(ship, this.subShips);
                break;

		}

		dropItems.remove(item);
	}


	/**
	 * Randomly drops an item from a defeated enemy based on drop chance.
	 */
	public void attemptItemDrop(EnemyShip enemy) {

		String enemyType = enemy.getEnemyType();
		if (enemyType == null) return;

		if ("enemySpecial".equals(enemyType)) return;

		List<ItemDrop> drops = currentLevel.getItemDrops();
		if (drops == null || drops.isEmpty()) return;

		for (ItemDrop drop : drops) {

			if (!enemyType.equalsIgnoreCase(drop.getEnemyType())) continue;

			if (Math.random() > drop.getDropChance()) continue;

			DropItem.ItemType type = DropItem.fromString(drop.getItemId());
			if (type == null) {
				logger.warning("Invalid itemId in level config: " + drop.getItemId());
				continue;
			}

			DropItem item = ItemPool.getItem(
					enemy.getPositionX() + enemy.getWidth() / 2,
					enemy.getPositionY() + enemy.getHeight() / 2,
					2,
					type
			);

			dropItems.add(item);
			return;
		}
	}



    private void cleanupAllEntities() {
        cleanBullets();
        cleanItems();
    }

    /**
     * Determines the damage for the boss's area-wide attack. (General method)
     * @param safeZoneColumn (0-9) Safe zone column index
     */
    public void executeApocalypseDamage(int safeZoneColumn, float progress) {
        if (safeZoneColumn < 0 || safeZoneColumn > 9) {
            return;
        }

        // Calculate the current "bottom" Y-coordinate of the attack based on animation progress
        int currentAttackHeight = (int) (this.height * progress);

        int columnWidth = this.width / 10;

        // --- Player 1 Check ---
        if (this.livesP1 > 0 && this.ship != null && !this.ship.isDestroyed() && !this.ship.isInvincible()) {
            // Based on the player's end X-coordinate
            int playerLeftX = this.ship.getPositionX();
            int playerRightX = this.ship.getPositionX() + this.ship.getWidth() - 1;

            // Player's Y-coordinate (top)
            int playerTopY = this.ship.getPositionY();

            int leftColumn = playerLeftX / columnWidth;
            int rightColumn = playerRightX / columnWidth;

            // Is the player in a column that is not the safe zone?
            boolean isInRedZone = (leftColumn != safeZoneColumn || rightColumn != safeZoneColumn);
            // Is the "bottom" of the attack animation below the "top" of the player?
            //    (i.e., has the attack reached the player?)
            boolean isHitByAnimation = (currentAttackHeight >= playerTopY);

            // If the player is not in the safe zone AND is hit by the animation
            if (isInRedZone && isHitByAnimation) {
                this.ship.destroy();
                this.livesP1--;
                showHealthPopup("-1 Life (Apocalypse!)");
                this.logger.info("Hit by Apocalypse, " + this.livesP1 + " lives remaining.");
            }
        }

        // --- Player 2 Check ---
        if (this.shipP2 != null && this.livesP2 > 0 && !this.shipP2.isDestroyed() && !this.shipP2.isInvincible()) {
            int playerLeftX = this.shipP2.getPositionX(); //
            int playerRightX = this.shipP2.getPositionX() + this.shipP2.getWidth() - 1;

            int playerTopY = this.shipP2.getPositionY();

            int leftColumn = playerLeftX / columnWidth;
            int rightColumn = playerRightX / columnWidth;

            boolean isInRedZone = (leftColumn != safeZoneColumn || rightColumn != safeZoneColumn);
            boolean isHitByAnimation = (currentAttackHeight >= playerTopY);

            if (isInRedZone && isHitByAnimation) {
                this.shipP2.destroy();
                this.livesP2--;
                showHealthPopup("-1 Life (Apocalypse!)");
                this.logger.info("P2 Hit by Apocalypse, " + this.livesP2 + " lives remaining.");
            }
        }

        // 3. SubShip Check
        if (this.subShips != null) {
            for (SubShip sub : this.subShips) {
                if (!sub.isDestroyed()) {
                    int subX = sub.getPositionX();
                    int subY = sub.getPositionY();
                    int subRightX = subX + sub.getWidth() - 1;

                    int leftColumn = subX / columnWidth;
                    int rightColumn = subRightX / columnWidth;

                    boolean isInRedZone = (leftColumn != safeZoneColumn || rightColumn != safeZoneColumn);
                    boolean isHitByAnimation = (currentAttackHeight >= subY);

                    if (isInRedZone && isHitByAnimation) {
                        sub.destroy();
                        this.logger.info("SubShip destroyed by Apocalypse!");
                    }
                }
            }
        }
    }


    /**
     * Cleans bullets that go off screen.
     */
    private void cleanBullets() {
        Set<Bullet> recyclable = new HashSet<Bullet>();
        for (Bullet bullet : this.bullets) {
            if (bullet.getPositionY() < GameConstant.STAT_SEPARATION_LINE_HEIGHT
                    || bullet.getPositionY() > this.height){
				recyclable.add(bullet);
			}
        }
        this.bullets.removeAll(recyclable);
		Set<Bullet> normalBullets = new HashSet<>();

		for (Bullet b : recyclable) {
			if (b instanceof BombBullet) {
				BulletPool.recycleBomb((BombBullet) b);
			} else {
				normalBullets.add(b);
			}
		}

		BulletPool.recycle(normalBullets);
    }

    /**
     * Cleans Items that go off screen.
     */

    private void cleanItems() {
        Set<DropItem> recyclable = new HashSet<DropItem>();
        for (DropItem dropItem : this.dropItems) {
            if (dropItem.getPositionY() < GameConstant.STAT_SEPARATION_LINE_HEIGHT
                    || dropItem.getPositionY() > this.height)
                recyclable.add(dropItem);
        }
        this.dropItems.removeAll(recyclable);
        ItemPool.recycle(recyclable);
    }

	private boolean inRange(Entity e, int cx, int cy, int radius) {

		int ex = e.getPositionX() + e.getWidth() / 2;
		int ey = e.getPositionY() + e.getHeight() / 2;

		int dx = ex - cx;
		int dy = ey - cy;

		return dx * dx + dy * dy <= radius * radius;
	}

	private void applyBombDamageToEnemy(Bullet source, EnemyShip enemy) {
		if (enemy == null || enemy.isDestroyed()) return;

		int pts = enemy.getPointValue();
		addPointsFor(source, pts);
		this.coin += pts / GameConstant.POINTS_TO_COIN_CONVERSION;
		AchievementManager.getInstance().onEnemyDefeated();

		attemptItemDrop(enemy);

		String type = enemy.getEnemyType();
		if ("enemySpecial".equals(type)) {
			if (enemyShipSpecialFormation != null) {
				enemyShipSpecialFormation.destroy(enemy);
			}
		} else {
			if (enemyShipFormationModel != null) {
				enemyShipFormationModel.destroy(enemy);
			}
		}
	}

	private void applyBombDamageToBoss(Bullet source, BossEntity boss) {
		if (boss == null || boss.isDestroyed()) return;

		boss.takeDamage(GameConstant.BOMB_DAMAGE_TO_BOSS);

		if (boss.getHealPoint() <= 0) {
			boss.destroy();
			int pts = boss.getPointValue();
			addPointsFor(source, pts);
			this.coin += pts / 10;
			AchievementManager.getInstance().unlockAchievement("Boss Slayer");
		}
	}

	public void requestBombAoEDamage(Bullet source) {
		if (source == null) return;

		int cx = source.getPositionX() + source.getWidth() / 2;
		int cy = source.getPositionY() + source.getHeight() / 2;
		final int radius = GameConstant.BOMB_AOE_RADIUS;

        List<Iterable<EnemyShip>> enemyFormations = new java.util.ArrayList<>();
		if (enemyShipFormationModel != null) {
			enemyFormations.add(enemyShipFormationModel);
		}
		if (enemyShipSpecialFormation != null) {
			enemyFormations.add(enemyShipSpecialFormation);
		}

		for (Iterable<EnemyShip> formation : enemyFormations) {
			for (EnemyShip e : formation) {
				if (e != null && !e.isDestroyed() && inRange(e, cx, cy, radius)) {
					applyBombDamageToEnemy(source, e);
				}
			}
		}

        List<BossEntity> allBosses = new java.util.ArrayList<>();
		if (omegaBoss != null) {
			allBosses.add(omegaBoss);
		}
		if (zetaBoss != null) {
			allBosses.add(zetaBoss);
		}
		if (finalBoss != null) {
			allBosses.add(finalBoss);
		}
		if (midBossMobs != null) {
			allBosses.addAll(midBossMobs);
		}

		for (BossEntity boss : allBosses) {
			if (!boss.isDestroyed() && inRange((Entity) boss, cx, cy, radius)) {
				applyBombDamageToBoss(source, boss);
			}
		}
	}


	/**
     * Checks if two entities are colliding.
     *
     * @param a
     * First entity, the bullet.
     * @param b
     * Second entity, the ship.
     * @return Result of the collision test.
     */
    private boolean checkCollision(final HasBounds a, final HasBounds b) {
        // Calculate center point of the entities in both axis.
        int centerAX = a.getPositionX() + a.getWidth() / 2;
        int centerAY = a.getPositionY() + a.getHeight() / 2;
        int centerBX = b.getPositionX() + b.getWidth() / 2;
        int centerBY = b.getPositionY() + b.getHeight() / 2;
        // Calculate maximum distance without collision.
        int maxDistanceX = a.getWidth() / 2 + b.getWidth() / 2;
        int maxDistanceY = a.getHeight() / 2 + b.getHeight() / 2;
        // Calculates distance.
        int distanceX = Math.abs(centerAX - centerBX);
        int distanceY = Math.abs(centerAY - centerBY);

        return distanceX < maxDistanceX && distanceY < maxDistanceY;
    }

    /**
     * Shows an achievement popup message on the HUD.
     *
     * @param message
     * Text to display in the popup.
     */
    public void showAchievement(String message) {
        this.achievementText = message;
        this.achievementPopupCooldown = new Cooldown(2500); // Show for 2.5 seconds
        this.achievementPopupCooldown.reset();
    }

    /**
     * Displays a notification popup when the player gains or loses health
     *
     * @param message
     * Text to display in the popup
     */

    public void showHealthPopup(String message) {
        this.healthPopupText = message;
        this.healthPopupCooldown = new Cooldown(500);
        this.healthPopupCooldown.reset();
    }

    /**
     * Returns a GameState object representing the status of the game.
     *
     * @return Current game state.
     */
    public final GameState getGameState() {
        if (this.coin > 2000) {
            AchievementManager.getInstance().unlockAchievement("Mr. Greedy");
        }
        return new GameState(this.level, this.score, this.livesP1,this.livesP2,
                this.bulletsShot, this.shipsDestroyed,this.coin);
    }
    /**
     * Adds one life to the player.
     */
    public final void gainLife() {
        if (this.livesP1 < this.maxLives) {
            this.livesP1++;
        }
    }

    public final void gainLifeP2() {
        if (this.livesP2 < this.maxLives) {
            this.livesP2++;
        }
    }

    private void bossReveal() {
        String bossName = this.currentLevel.getBossId();

        if (bossName == null || bossName.isEmpty()) {
			if(GameConstant.origin_skill_activated) return;
            this.logger.info("No boss for this level. Proceeding to finish.");
            return;
        }

        this.logger.info("Spawning boss: " + bossName);
        switch (bossName) {
            case "finalBoss":
                this.finalBoss = new FinalBoss(ships);
                this.logger.info("Final Boss has spawned!");
                break;
            case "omegaBoss", "omegaAndZetaAndFinal":
                this.omegaBoss = new OmegaBoss(ship);
                this.logger.info("Omega Boss has spawned!");
                break;
            case "ZetaBoss":
                this.zetaBoss = new ZetaBoss(Color.ORANGE, ship, ships);
                this.logger.info("Zeta Boss has spawned!");
                break;
            case "gammaBoss":
                this.gammaBoss = new GammaBoss(Color.CYAN, ships, this.width, this.height);
                this.logger.info("Gamma Boss has spawned!");
                break;
	        default:
                this.logger.warning("Unknown bossId: " + bossName);
                break;
        }
    }


    public void finalbossManage(){
        if (this.finalBoss != null && !this.finalBoss.isDestroyed()) {
            this.finalBoss.update();
	        ApocalypseAttackPattern pattern = this.finalBoss.getApocalypsePattern();
	        if (pattern != null && pattern.isAttacking()) {
		        float progress = pattern.getAttackAnimationProgress();
		        executeApocalypseDamage(pattern.getSafeZoneColumn(), progress);
	        }
			this.bossBullets.addAll(this.finalBoss.getBullets());
	        this.bossLasers.addAll(this.finalBoss.getLasers());
	        this.blackHoles = this.finalBoss.getBlackHoles();
	        this.explosions = this.finalBoss.getBoom();
	        this.midBossMobs=this.finalBoss.getChildShips();
        }
        if (this.finalBoss != null && this.finalBoss.isDestroyed()) {
            this.levelFinished = true;
            this.screenFinishedCooldown.reset();
        }
    }

	private void validateBossBulletsSet() {
		/** bullets to erase */
		Set<Bullet> bulletsToRemove = new HashSet<>();

		for (Bullet b : bossBullets) {
			b.update();
			/** If the bullet goes off the screen */
			if (b.isOffScreen(width, height) || b.shouldBeRemoved()) {
				/** bulletsToRemove carry bullet */
				bulletsToRemove.add(b);
			}
		}
		/** all bullets are removed */
		bossBullets.removeAll(bulletsToRemove);
	}

	private void validateLaserBeamsSet() {
		/** Lasers to erase */
		Set<LaserBeam> lasersToRemove = new HashSet<>();

		for(LaserBeam laser : bossLasers) {
			laser.update();
			if(laser.shouldBeRemoved()){
				lasersToRemove.add(laser);
			}
		}
		bossLasers.removeAll(lasersToRemove);

	}

	private void validateExplosionsSet() {
		/** explosions to erase */
		explosions.removeIf(Explosion::shouldBeRemoved);
	}

	private void validateBlackHolesSet() {
		/** black-holes to erase */
		blackHoles.removeIf(BlackHole::shouldBeRemoved);
	}

		// --- Timer and State Management Methods for Controller ---

    public boolean isTimerRunning() {
        return this.gameTimer.isRunning();
    }

    public void startTimer() {
        this.gameTimer.start();
    }

    public void stopTimer() {
        if (this.gameTimer.isRunning()) {
            this.gameTimer.stop();
        }
    }

    public void updateElapsedTime() {
        if (this.gameTimer.isRunning()) {
            this.elapsedTime = this.gameTimer.getElapsedTime();
            AchievementManager.getInstance().onTimeElapsedSeconds((int)(this.elapsedTime / 1000));
        }
    }

    public boolean isGameOver() {
        return (this.livesP1 == 0) && (this.shipP2 == null || this.livesP2 == 0);
    }

    public void setGameOver() {
        this.levelFinished = true;
        this.screenFinishedCooldown.reset();
        this.stopTimer();

        if ((this.livesP1 > 0) || (this.shipP2 != null && this.livesP2 > 0)) {
            if (this.level == 1) {
                AchievementManager.getInstance().unlockAchievement("Beginner");
            } else if (this.level == 3) {
                AchievementManager.getInstance().unlockAchievement("Intermediate");
            }
        }
    }

    public void processLevelCompletion() {
        if (this.livesP1 > 0 || (this.shipP2 != null && this.livesP2 > 0)) { // Check for win condition
            if (this.currentLevel.getCompletionBonus() != null) {
                this.coin += this.currentLevel.getCompletionBonus().getCurrency();
                this.logger.info("Awarded " + this.currentLevel.getCompletionBonus().getCurrency() + " coins for level completion.");
            }

            String achievement = this.currentLevel.getAchievementTrigger();
            if (achievement != null && !achievement.isEmpty()) {
                AchievementManager.getInstance().unlockAchievement(achievement);
                this.logger.info("Unlocked achievement: " + achievement);
            }
        }
    }

    public int calculateFinalScore() {
        this.score += LIFE_SCORE * Math.max(0, this.livesP1 - 1);
        this.score += LIFE_SCORE * Math.max(0, this.livesP2 - 1);
        return this.score;
    }

    // --- Getters for View ---

    public boolean isInputDelayFinished() {
        return this.inputDelay.checkFinished();
    }

    public long getGameStartTime() {
        return this.gameStartTime;
    }

    public Ship getShip() { return ship; }
    public Ship getShipP2() { return shipP2; }
    public int getLivesP1() { return livesP1; }
    public int getLivesP2() { return livesP2; }
    public EnemyShipSpecialFormation getEnemyShipSpecialFormation() { return enemyShipSpecialFormation; }
    public FinalBoss getFinalBoss() { return finalBoss; }
    public Set<Bullet> getBossBullets() { return bossBullets; }
	public Set<LaserBeam> getBossLasers() { return bossLasers; }
	public Set<BlackHole> getBlackHoles() { return blackHoles; }
    public EnemyShipFormationModel getEnemyShipFormationModel() { return enemyShipFormationModel; }
    public MidBoss getOmegaBoss() { return omegaBoss; }
    public MidBoss getZetaBoss() { return zetaBoss; }
    public MidBoss getGammaBoss() { return gammaBoss; }
//    public Set<MidBossMob> getMidBossChilds() { return midBossMobs; }
    public Set<Bullet> getBullets() { return bullets; }
    public Set<DropItem> getDropItems() { return dropItems; }
    public int getScoreP1() { return scoreP1; }
    public int getScoreP2() { return scoreP2; }
    public int getCoin() { return coin; }
    public long getElapsedTime() { return elapsedTime; }
    public Level getCurrentLevel() { return currentLevel; }
    public String getAchievementText() { return achievementText; }
    public Cooldown getAchievementPopupCooldown() { return achievementPopupCooldown; }
    public String getHealthPopupText() { return healthPopupText; }
    public Cooldown getHealthPopupCooldown() { return healthPopupCooldown; }
    public int getLevel() { return level; }
    public boolean isBonusLife() { return bonusLife; }
    public boolean isLevelFinished() { return levelFinished; }
    public Cooldown getScreenFinishedCooldown() { return screenFinishedCooldown; }
    public boolean getIsTeleportP1(){ return this.isTeleportP1;}
    public int getTeleportFromP1X() { return teleportFromP1X; }
    public int getTeleportFromP1Y() { return teleportFromP1Y; }
    public boolean getIsTeleportP2(){ return this.isTeleportP2;}
    public int getTeleportFromP2X() { return teleportFromP2X; }
    public int getTeleportFromP2Y() { return teleportFromP2Y; }
    public int getAfterTeleportFromP1X() { return afterTeleportFromP1X; }
    public int getAfterTeleportFromP1Y() { return afterTeleportFromP1Y; }
    public int getAfterTeleportFromP2Y() { return afterTeleportFromP2Y; }
    public int getAfterTeleportFromP2X() { return afterTeleportFromP2X; }
    public void setIsTelportP1(boolean isTeleportP1){
        this.isTeleportP1 = isTeleportP1;
    }
    public void setIsTelportP2(boolean isTeleportP2){
        this.isTeleportP2 = isTeleportP2;
    }
    public int getFinalSkillCnt(){
        return FinalSkillCnt;
    }
    public void useFinalSkill(){
        this.FinalSkillCnt--;
    }
    public List<Ship> getShips() {
        return ships;
    }

//    public boolean isExplosionBoom() { return explosions.isBoom(); }
    public Set<Explosion> getExplosions() { return explosions; }
//    public double getWarningExplosion() { return explosions.getWarningProgress(); }

    public List<Entity> getEntitiesToRender() {
        List<Entity> renderList = new ArrayList<>();

        // 1. added player ships
        if (getLivesP1() > 0 && getShip() != null) {
            renderList.add(getShip());
        }
        if (getShipP2() != null && getLivesP2() > 0) {
            renderList.add(getShipP2());
        }

        // added subships
        if (this.subShips != null) {
            renderList.addAll(this.subShips);
        }

        // 2. added special enemyship
        if (getEnemyShipSpecialFormation() != null) {
            for (EnemyShip specialEnemy : getEnemyShipSpecialFormation()) {
                renderList.add(specialEnemy);
            }
        }

        // 3. added enemyship
        if (getEnemyShipFormationModel() != null) {
            for (EnemyShip enemy : getEnemyShipFormationModel()) {
                renderList.add(enemy);
            }
        }
        if (midBossMobs != null) {
			renderList.addAll(midBossMobs);
        }
        // 4. added boss
        if (getOmegaBoss() != null) {
            renderList.add(getOmegaBoss());
        }

        // [추가] ZetaBoss 렌더링 리스트 추가
        if (getZetaBoss() != null) {
            renderList.add(getZetaBoss());
        }

        // [add] GammaBoss Rendering List
        if (getGammaBoss() != null) {
            renderList.add(getGammaBoss());
        }

        if (getFinalBoss() != null && !getFinalBoss().isDestroyed()) {
            renderList.add(getFinalBoss());
        }
        // 5. added items and bullets
        if (getBullets() != null) {
            renderList.addAll(getBullets());
        }

		if (getBossBullets() != null && !getBossBullets().isEmpty()) {
			renderList.addAll(getBossBullets());
		}

        if (getDropItems() != null) {
            renderList.addAll(getDropItems());
        }

        return renderList;
    }
}