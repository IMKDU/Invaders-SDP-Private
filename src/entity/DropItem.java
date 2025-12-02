package entity;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.Random;

import engine.Core;

public class DropItem extends Entity implements Collidable {
	public enum ItemType {
        SubShip(2),
		Stop(10),
		Bomb(5),
		Shield(5),
		Heal(5),
		Coin(5);

		private final int weight;

		ItemType(final int weight) {
			this.weight = weight;
		}

		private static final ItemType[] VALUES = values();
		private static final Random RANDOM = new Random();
		private static final int TOTAL_WEIGHT;

		static {
			int sum = 0;
			for (ItemType type : VALUES) {
				sum += type.weight;
			}
			TOTAL_WEIGHT = sum;
		}

		/**
		 * Return random Itemtype based on weight
		 *
		 * @return ItemType selected based on weight
		 */

		public static ItemType selectItemType() {
			int randomWeight = RANDOM.nextInt(TOTAL_WEIGHT);
			int cumulativeWeight = 0;

			for (ItemType type : VALUES) {
				cumulativeWeight += type.weight;

				if (randomWeight < cumulativeWeight) {
					return type;
				}
			}
			return VALUES[0];
		}
	}

	public static ItemType fromString(String text) {
		for (ItemType b : ItemType.values()) {
			if (b.name().equalsIgnoreCase(text)) {
				return b;
			}
		}
		return null;
	}

	/** Speed of the item, positive is down. */
	private int speed;
	/** Type of the item. */
	private ItemType itemType;

	public DropItem(final int positionX, final int positionY, final int speed, final ItemType itemType) {
		super(positionX, positionY, 20 * 2, 20 * 2, Color.WHITE);
		this.speed = speed;
		this.itemType = itemType;
	}

	private static long freezeEndTime = 0;

	/**
	 * enemy push
	 *
	 * @param enemyShipFormation
	 * @param distanceY
	 */
	public static void PushbackItem(EnemyShipFormationModel enemyShipFormation, int distanceY) {
		if (enemyShipFormation == null) {
			return;
		}

        // All enemyship push
        for (EnemyShip enemy : enemyShipFormation) {
            if (enemy != null && !enemy.isDestroyed()) {
                enemy.move(0, -distanceY,false);
            }
        }
    }

	/**
	 * Freeze DropItem : all enemy ship never move except special enemy.
	 *
	 * @param durationMillis
	 *                  Freeze duration Time
	 */
	public static void applyTimeFreezeItem(int durationMillis) {
		// current Time + duration Time = End Time
		freezeEndTime = System.currentTimeMillis() + durationMillis;
	}

	/**
	 * check If Freeze item is activated
	 *
	 * @return If returning true, don't move all enemy ship except special enemy
	 */
	public static boolean isTimeFreezeActive() {
		if (freezeEndTime > 0 && System.currentTimeMillis() < freezeEndTime) {
			return true;
		}
		if (freezeEndTime > 0 && System.currentTimeMillis() >= freezeEndTime) {
			freezeEndTime = 0;
		}
		return false;
	}

    /**
     * Logic for applying SubShip item effect.
     * Spawns a sub-ship or extends the duration of an existing one.
     *
     * @param owner          The player ship that acquired the item
     * @param activeSubShips List of all sub-ships currently in the game (managed by GameModel)
     */
    public static void activateSubShip(Ship owner, List<SubShip> activeSubShips) {
        Logger logger = Core.getLogger();

        // 1. Extract list of sub-ships belonging to the current player
        List<SubShip> mySubShips = new ArrayList<>();
        if (activeSubShips != null) {
            for (SubShip s : activeSubShips) {
                if (!s.isDestroyed() && s.getOwner().equals(owner)) {
                    mySubShips.add(s);
                }
            }
        } else {
            return;
        }

        // 2. Branch logic based on sub-ship count
        if (mySubShips.size() < 2) {
            // [Spawn Logic] Spawn new one in empty spot if less than 2
            boolean shouldSpawnLeft = true;
            if (!mySubShips.isEmpty()) {
                SubShip existing = mySubShips.get(0);
                // If existing is left -> Spawn right
                if (existing.isLeft()) {
                    shouldSpawnLeft = false;
                }
            }
            SubShip newSub = new SubShip(owner, shouldSpawnLeft);
            activeSubShips.add(newSub);
            logger.info("SubShip spawned for Player " + owner.getPlayerId());
        } else {
            // If already 2, reset duration of the oldest ship
            SubShip oldest = mySubShips.get(0);

            // Iterate list to find the ship with the earliest creation time
            for (SubShip s : mySubShips) {
                if (s.getCreationTime() < oldest.getCreationTime()) {
                    oldest = s;
                }
            }

            // Reset time (maintain for another 10 seconds)
            oldest.resetCreationTime();
            logger.info("SubShip duration reset for Player " + owner.getPlayerId());
        }
    }

/**
 * Manages the in-game item (enhancement) system.
 * This is a temporary implementation focusing on functionality.
 *
 * Currently implemented: Spread Shot
 *
 * Example usage:
 * DropItem.setSpreadShotLevel(2);  // Purchase level 2 in the shop
 * int bulletCount = DropItem.getSpreadShotBulletCount();  // Returns the number of bullets to fire
 */
	/**
	 * Updates the item's position.
	 */
	public final void update() {
		this.positionY += this.speed;
	}

	public final void setSpeed(final int speed) {
		this.speed = speed;
	}

	public final int getSpeed() {
		return this.speed;
	}

	public final ItemType getItemType() {
		return this.itemType;
	}

	public final void setItemType(final ItemType itemType) {
		this.itemType = itemType;
	}

	public static ItemType getRandomItemType(final double proba) {
		if (Math.random() < proba) {
			return ItemType.selectItemType();
		}
		else {
			return null;
		}
	}

	@Override
	public void onCollision(Collidable other, GameModel model) {
		other.onCollideWithDropItem(this, model);
	}
}