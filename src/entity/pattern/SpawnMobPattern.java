package entity.pattern;

import java.util.*;
import java.awt.*;
import java.util.List;
import java.util.logging.Logger;
import entity.GameConstant;
import entity.HasBounds;
import entity.MidBoss;
import entity.MidBossMob;

public class SpawnMobPattern extends BossPattern {


    private MidBoss boss;
    /** Boss spawn child ships */
    private Set<MidBossMob> childShips;
    /** Current accumulated count of all ships spawned throughout the fight. */
    private int spawnCount = 0;
    /** Movement speed of the spawned child ships. */
    private final int CHILD_SPEED = 1;
    /** Strategy object managing the movement patterns of the child ships. */
    private MidBossMobMovement movementStrategy;
    /** Current active movement and spawn pattern (1 or 2). */
    private int currentPattern = 1;
    /** The current X position of the boss. */
    private int bossPositionX;
    /** The current Y position of the boss. */
    private int bossPositionY;
    /** The width of the boss. */
    private final int BOSS_WIDTH;
    /** The height of the boss. */
    private final int BOSS_HEIGHT;
    /** The maximum health points of the boss. */
    private final int BOSS_MAXHP;
    private Logger logger;
    /** List of ship count increments for each spawn phase (5, 10, 15, 30) */
    private final int[] SPAWN_COUNT_LIST = {
            4,
            6,
            8,
            10
    };
    /** List of required accumulated spawn totals to trigger the next phase (0, 5, 15, 30). */
    private int SPAWN_COUNT_CHECK = 0;
    /** List of boss HP ratios (0.9, 0.7, 0.5, 0.2) required to trigger a spawn phase. */
    private final double[] HP_THRESHOLDS = {
            0.9,
            0.7,
            0.5,
            0.2
    };
    /** The total vertical distance (in pixels) used to spread newly spawned mobs below the boss. */
    private final double SPAWN_VERTICAL_SPACING_BASE = 60.0;
    /** The initial health points assigned to each spawned mob. */
    private final int MOB_HEALTH_POINT = 4;
    /** The score value awarded to the player upon destroying the mob. */
    private final int MOB_POINT_VALUE = 10;
    /**
     * Initializes the SpawnMobPattern component.
     */
    public SpawnMobPattern(MidBoss boss) {
        super(new Point(boss.getPositionX(), boss.getPositionY()));
        this.childShips = new HashSet<>();

		this.boss = boss;
        this.BOSS_WIDTH = boss.getWidth();
        this.BOSS_HEIGHT = boss.getHeight();
        this.BOSS_MAXHP = boss.getMaxHealPoint();
        this.logger = engine.Core.getLogger();
        this.movementStrategy = new MidBossMobMovement(
                this.BOSS_WIDTH,
                this.BOSS_HEIGHT,
                this.CHILD_SPEED
        );
    }

    /**
     * Updates the pattern status, checks for spawning, and updates child ship movement.
     */
	@Override
    public void attack() {
        this.bossPositionX = boss.getPositionX();
        this.bossPositionY = boss.getPositionY();
        this.checkPatternSwitch(boss.getHealPoint());
        this.spawnPattern(boss.getHealPoint());
        this.childMovePattern();
    }
    /**
     * Executes the current movement pattern for all active child ships.
     */
    public void childMovePattern() {
//        if(this.currentPattern == 1) {
//            this.movementStrategy.pattern_1_Movement(
//                    this.bossPositionX,
//                    this.bossPositionY,
//                    this.childShips.stream().toList(),
//                    this.spawnCount
//            );
//        } else {
            this.movementStrategy.pattern_2_Movement(
                    this.bossPositionX,
                    this.bossPositionY,
                    this.childShips.stream().toList(),
                    this.spawnCount
            );
//        }
    }
    /**
     * Checks if the pattern needs to switch based on the boss's current health.
     * Switches to pattern 2 when health drops below 50%.
     * @param bossHealPoint The current health point of the main boss.
     */
    public void checkPatternSwitch(int bossHealPoint) {
        if(bossHealPoint < this.BOSS_MAXHP * 0.5 && this.currentPattern == 1) {
            this.currentPattern = 2;
            this.logger.info("Pattern Change: Switching to Child Movement Pattern 2");
        }
    }
    /**
     * Cleans up destroyed children and checks the boss health to trigger the next spawn wave.
     * @param bossHealPoint The current health point of the main boss.
     */
    public void spawnPattern(int bossHealPoint) {
        this.cleanDestroyedChild();
        double HPRatio = (double) bossHealPoint/ this.BOSS_MAXHP;
        if(this.SPAWN_COUNT_CHECK < HP_THRESHOLDS.length && HPRatio < HP_THRESHOLDS[this.SPAWN_COUNT_CHECK]) {
            this.spawnCount += this.SPAWN_COUNT_LIST[this.SPAWN_COUNT_CHECK];
            createChild(this.SPAWN_COUNT_LIST[this.SPAWN_COUNT_CHECK++]);
        }
    }
    /**
     * Creates new child ships based on the required total count and adds them to the list.
     * @param shipCount The *total* number of children to be created in this wave.
     */
    public void createChild(int shipCount) {
        this.logger.info("Create Child");
        for (int count = 0; count < shipCount; count++) {
            MidBossMob ship = new MidBossMob(
                    this.bossPositionX,
                    bossPositionY + (int)(SPAWN_VERTICAL_SPACING_BASE/shipCount * (count+1)),
                    MOB_HEALTH_POINT,
                    MOB_POINT_VALUE,
                    count
            );
            this.childShips.add(ship);
        }
    }
    /**
     * Removes all child ships from the list that have been destroyed.
     */
    public void cleanDestroyedChild() {
        Iterator<MidBossMob> iterator = this.childShips.iterator();
        while(iterator.hasNext()){
            MidBossMob ship = iterator.next();
            if(ship.isDestroyed()) { iterator.remove(); }
        }
    }
    public void clean() {
        Iterator<MidBossMob> iterator = this.childShips.iterator();
        while(iterator.hasNext()){
            iterator.next().destroy();
        }
    }

    @Override
    public void move(){
		// There's no movement in this pattern.
    }
    /**
     * Returns the list of currently active child ships.
     * NOTE: This is the getter method required by the game controller for collision and rendering.
     * @return The list of MidBossMob children.
     */
    public Set<MidBossMob> getChildShips() { return childShips; }
}