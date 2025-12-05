package entity;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import engine.Core;
import engine.level.Level;
import entity.formations.CrossFormationMovement;
import entity.formations.IMovementStrategy;
import entity.formations.SideLoopFormationMovement;
import entity.formations.VerticalLoopFormationMovement;

/**
 * Groups enemy ships into a formation.
 * (MODEL - Contains data and coordinates logic using components)
 */
public class EnemyShipFormationModel implements Iterable<EnemyShip> {

    /** Initial position in the x-axis. */
    private static final int INIT_POS_X = 20;
    /** Initial position in the y-axis. */
    private static final int INIT_POS_Y = 100;
    /** Distance between ships. */
    private static final int SEPARATION_DISTANCE = 40;
    /** Minimum speed allowed. */
    private static final int MINIMUM_SPEED = 10;

    /** Application logger. */
    private Logger logger;
    /** Level reference. */
    private Level levelObj;

    /** List of enemy ships forming the formation. */
    private List<List<EnemyShip>> enemyShips;

    /** Number of ships in the formation - horizontally. */
    private int nShipsWide;
    /** Number of ships in the formation - vertically. */
    private int nShipsHigh;
    /** Initial ship speed. */
    private int baseSpeed;
    /** Speed of the ships. */
    private int movementSpeed;
    /** Interval between movements, in frames. */
    private int movementInterval;
    /** Total width of the formation. */
    private int width;
    /** Total height of the formation. */
    private int height;
    /** Position in the x-axis of the upper left corner of the formation. */
    private int positionX;
    /** Position in the y-axis of the upper left corner of the formation. */
    private int positionY;
    /** Width of one ship. */
    private int shipWidth;
    /** Height of one ship. */
    private int shipHeight;
    /** Number of not destroyed ships. */
    private int shipCount;

    /** The logic component responsible for movement. */
    private IMovementStrategy movementStrategy;
    private List<IMovementStrategy> movementStrategies = new ArrayList<>();
    /** The logic component responsible for shooting. */
    private FormationShootingManager shootingManager;
    private int enemySeparation;

    /**
     * Constructor
     *
     * @param level The level data (for speed, shooting, etc.).
     */
    public EnemyShipFormationModel(final Level level, int screenWidth) {
        this.logger = Core.getLogger();
        this.movementInterval = 0;
        this.levelObj = level;

        this.nShipsWide = level.getFormationWidth();
        this.nShipsHigh = level.getFormationHeight();
        List<String> levelStrategy = level.getMovementStrategy();

        EnemyShipFactory builder = new EnemyShipFactory();
        this.enemyShips = builder.build(
                level,
                nShipsWide,
                nShipsHigh
        );

        this.baseSpeed = level.getBaseSpeed();
        this.movementSpeed = this.baseSpeed;

        this.positionX = INIT_POS_X;
        this.positionY = INIT_POS_Y;
        this.shipCount = 0;
        for (List<EnemyShip> column : this.enemyShips) {
            this.shipCount += column.size();
        }

        this.logger.info("Initializing " + nShipsWide + "x" + nShipsHigh
                + " ship formation in (" + positionX + "," + positionY + ")");

        if (!enemyShips.isEmpty() && !enemyShips.get(0).isEmpty()) {
            EnemyShip first = enemyShips.get(0).get(0);
            this.shipWidth = first.getWidth();
            this.shipHeight = first.getHeight();
        } else {
            this.shipWidth = this.shipHeight = 0;
            logger.warning("EnemyShipFormationModel: No ships were created.");
        }

        this.width = (this.nShipsWide - 1) * SEPARATION_DISTANCE + this.shipWidth;
        this.height = (this.nShipsHigh - 1) * SEPARATION_DISTANCE + this.shipHeight;

        List<List<List<EnemyShip>>> partitionGroup = new ArrayList<>();
        int strategySize = levelStrategy.size();
        this.enemySeparation = builder.getSeparationDistance();
        /** Apply default movement if no strategy is specified or 'default' is chosen */
        if( levelStrategy.isEmpty() || levelStrategy.getFirst().equals("default")) this.movementStrategy = new EnemyShipFormationMovement(this,GameConstant.ITEMS_SEPARATION_LINE_HEIGHT,screenWidth);
        else{
            /** Partition enemy ships into subgroups based on the number of strategies */
            if(strategySize > 1 ) partitionGroup = builder.splitGroup(enemyShips,strategySize);
            else partitionGroup.add(enemyShips);
            List<List<EnemyShip>> finalEnemyShips = new ArrayList<>();

            /** Assign movement logic to each subgroup */
            for (int i = 0; i < strategySize; i++) {
                String strategyName = levelStrategy.get(i);
                List<List<EnemyShip>> targetGroup = partitionGroup.get(i);

                switch (strategyName.toLowerCase()) {
                    case "side":
                        addMovementStrategy(new SideLoopFormationMovement(targetGroup));
                        finalEnemyShips.addAll(targetGroup);
                        break;

                    case "cross":
                        addMovementStrategy(new CrossFormationMovement(targetGroup));
                        finalEnemyShips.addAll(targetGroup);
                        break;

                    case "vertical":
                        List<EnemyShip> flatList = enemyFlatten(targetGroup);
                        addMovementStrategy(new VerticalLoopFormationMovement(flatList, SEPARATION_DISTANCE));
                        if (strategySize == 1) {
                            for (EnemyShip ship : flatList) {
                                List<EnemyShip> individualCol = new ArrayList<>();
                                individualCol.add(ship);
                                finalEnemyShips.add(individualCol);
                            }
                        } else {
                            finalEnemyShips.add(flatList);
                        }
                        break;

                    default:
                        logger.warning("Unknown movement strategy: " + strategyName);
                        break;
                }
            }
            /** Update the main enemy list with the re-organized structure */
            enemyShips = finalEnemyShips;
        }
        this.shootingManager = new FormationShootingManager(level,enemyShips);
    }

    public List<EnemyShip> enemyFlatten(List<List<EnemyShip>> enemyShips){
        int maxCnt = GameConstant.SCREEN_WIDTH/(enemySeparation/2+shipWidth);

        int cnt = 0;
        List<EnemyShip> flattenedEnemy = new ArrayList<>();
        outerLoop:
        for(List<EnemyShip> col:enemyShips){
            for(EnemyShip enemy:col){
                flattenedEnemy.add(enemy);
                cnt++;
                if(cnt >= maxCnt) break outerLoop;
            }
        }

        for (List<EnemyShip> col : enemyShips) {
            int before = col.size();
            col.removeIf(enemy -> !flattenedEnemy.contains(enemy));
            int after = col.size();
            this.shipCount -= (before-after);
        }

        enemyShips.removeIf(col -> col.isEmpty());
        return flattenedEnemy;
    }

    /**
     * Returns the 2D list of enemy ships for the View to draw.
     * @return 2D list of enemy ships.
     */
    public final List<List<EnemyShip>> getEnemyShips() {
        return this.enemyShips;
    }

    /**
     * Updates the position of the ships.
     */
    public final void update() {
        cleanUp();

        double remainingProportion = (double) this.shipCount
                / (this.nShipsHigh * this.nShipsWide);
        this.movementSpeed = (int) (Math.pow(remainingProportion, 2)
                * this.baseSpeed);
        this.movementSpeed += MINIMUM_SPEED;
        if (!this.movementStrategies.isEmpty()) {
            for(IMovementStrategy movement : movementStrategies){
                movement.updateMovement();
            }
        }
        movementInterval++;
        if (movementInterval >= this.movementSpeed) {
            movementInterval = 0;

            if (this.movementStrategies.isEmpty() && !this.movementStrategy.needsSmoothMovement()) {
                this.movementStrategy.updateMovement();
            }

            for (int colIdx = 0; colIdx < this.enemyShips.size(); colIdx++) {
                List<EnemyShip> column = this.enemyShips.get(colIdx);
                final int finalColIdx = colIdx;

                column.removeIf(ship -> {
                    if (ship != null && ship.isExplosionFinished()) {
                        int rowIdx = column.indexOf(ship);
                        this.logger.info("Removed enemy " + rowIdx + " from column " + finalColIdx);
                        return true;
                    }
                    return false;
                });
            }

            for (List<EnemyShip> column : this.enemyShips)
                for (EnemyShip enemyShip : column) {
                    enemyShip.update();
                }
        }
    }

    /**
     * Cleans empty columns, adjusts the width and height of the formation.
     */
    private void cleanUp() {
        for (int i = this.enemyShips.size() - 1; i >= 0; i--) {
            List<EnemyShip> col = enemyShips.get(i);
            if (col.isEmpty()) {
                this.enemyShips.remove(i);
                logger.info("Removed column " + i);
            }
        }

        if (this.enemyShips.isEmpty()) {
            this.width = 0;
            this.height = 0;
            return;
        }

        int minPositionY = Integer.MAX_VALUE;
        int maxPositionY = Integer.MIN_VALUE;
        int leftMostPoint = Integer.MAX_VALUE;
        int rightMostPoint = Integer.MIN_VALUE;
        for (List<EnemyShip> column : this.enemyShips) {
            EnemyShip first = column.get(0);
            EnemyShip last = column.get(column.size() - 1);
            minPositionY = Math.min(minPositionY, first.getPositionY());
            maxPositionY = Math.max(maxPositionY, last.getPositionY());
            int columnX = first.getPositionX();
            leftMostPoint = Math.min(leftMostPoint, columnX);
            rightMostPoint = Math.max(rightMostPoint, columnX);
        }
        this.width = rightMostPoint - leftMostPoint + this.shipWidth;
        this.height = maxPositionY - minPositionY + this.shipHeight;
        this.positionX = leftMostPoint;
        this.positionY = minPositionY;
    }

    /**
     * Shoots a bullet downwards.
     *
     * @param bullets
     * Bullets set to add the bullet being shot.
     */
    public final void shoot(final Set<Bullet> bullets) {
        this.shootingManager.shoot(bullets);
    }

    /**
     * Destroys a ship.
     *
     * @param destroyedShip
     * Ship to be destroyed.
     */
    public final void destroy(final EnemyShip destroyedShip) {

        for (int i = 0; i < this.enemyShips.size(); i++) {
            List<EnemyShip> column = this.enemyShips.get(i);
            int shipIndexInColumn = column.indexOf(destroyedShip);

            if (shipIndexInColumn != -1) {
                if (!column.get(shipIndexInColumn).isDestroyed()) {
                    column.get(shipIndexInColumn).destroy(false);
                    this.logger.info("Destroyed ship in (" + i + "," + shipIndexInColumn + ")");
                    this.shipCount--;

                    this.shootingManager.onShipDestroyed(destroyedShip, column);
                }
                return;
            }
        }
    }

    /**
     * Returns an iterator over the ships in the formation.
     */
    @Override
    public final Iterator<EnemyShip> iterator() {
        return new Iterator<>() {
            private int columnIterator = 0;
            private int rowIterator = 0;

            @Override
            public boolean hasNext() {
                while (columnIterator < enemyShips.size()) {
                    if (rowIterator < enemyShips.get(columnIterator).size()) {
                        return true;
                    }
                    columnIterator++;
                    rowIterator = 0;
                }
                return false;
            }

            @Override
            public EnemyShip next() {
                if (!hasNext()) {
                    throw new java.util.NoSuchElementException();
                }
                return enemyShips.get(columnIterator).get(rowIterator++);
            }
        };
    }

    /**
     * Destroy all ships in the formation.
     *
     * @return The number of destroyed ships.
     */
    public final int destroyAll() {
        int destroyed = 0;
        for (List<EnemyShip> column : enemyShips) {
            for (EnemyShip enemyShip : column) {
                enemyShip.destroy(false);
                destroyed++;
                shootingManager.onShipDestroyed(enemyShip, column);
            }
        }
        shipCount = 0;
        return destroyed;
    }


    /**
     * Checks if there are any ships remaining.
     *
     * @return True when all ships have been destroyed.
     */
    public final boolean isEmpty() {
        return this.shipCount <= 0;
    }



    /**
     * Clears all ships from the formation and resets the shooting manager.
     */
    public final void clear() {
        for (List<EnemyShip> column : this.enemyShips) {
            column.clear();
        }
        this.enemyShips.clear();
        this.shipCount = 0;

        this.shootingManager = new FormationShootingManager(this.levelObj, this.enemyShips);
    }


    /**
     * Gets the current X position of the formation's top-left corner.
     * @return X position.
     */
    public int getPositionX() { return this.positionX; }

    /**
     * Gets the current Y position of the formation's top-left corner.
     * @return Y position.
     */
    public int getPositionY() { return this.positionY; }

    /**
     * Gets the calculated width of the entire formation.
     * @return Formation width.
     */
    public int getWidth() { return this.width; }

    /**
     * Gets the calculated height of the entire formation.
     * @return Formation height.
     */
    public int getHeight() { return this.height; }

    /**
     * Sets the new position of the formation's top-left corner.
     * @param x New X position.
     * @param y New Y position.
     */
    public void setPosition(int x, int y) {
        this.positionX = x;
        this.positionY = y;
    }

    /**
     * Applies movement deltas to all individual ships in the formation.
     * @param movementX Pixels to move horizontally.
     * @param movementY Pixels to move vertically.
     */
    public void moveAllShips(int movementX, int movementY) {
        for (List<EnemyShip> column : this.enemyShips)
            for (EnemyShip enemyShip : column) {
                enemyShip.move(movementX, movementY,false);
            }
    }

	/**
	 * Applies a specific color to all ships in the formation.
	 * @param color The color to apply.
	 */
	public void applyEnemyColor(final Color color) {
		for (java.util.List<EnemyShip> column : this.getEnemyShips()) {
			for (EnemyShip ship : column) {
				if (ship != null && !ship.isDestroyed()) {
					ship.setColor(color);
				}
			}
		}
	}

	public void applyEnemyColorByLevel(final Level level) {
		if (level == null) return;
		final int lv = level.getLevel();
		applyEnemyColor(getColorForLevel(lv));
	}
	private Color getColorForLevel(final int levelNumber) {
		switch (levelNumber) {
			case 1: return new Color(0x3DDC84); // green
			case 2: return new Color(0x00BCD4); // cyan
			case 3: return new Color(0xFF4081); // pink
			case 4: return new Color(0xFFC107); // amber
			case 5: return new Color(0x9C27B0); // purple
			case 6: return new Color(0xFF5722); // deep orange
			case 7: return new Color(0x8BC34A); // light green
			case 8: return new Color(0x03A9F4); // light blue
			case 9: return new Color(0xE91E63); // magenta
			case 10: return new Color(0x607D8B); // blue gray
			default: return Color.WHITE;
		}
	}
    public void addMovementStrategy(IMovementStrategy strategy) {
        movementStrategies.add(strategy);
        this.logger.info("Movement Strategy add: " + strategy.getClass().getSimpleName());
    }
}