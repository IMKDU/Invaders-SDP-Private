package entity;

import java.util.logging.Logger;
import engine.Core;
import entity.formations.IMovementStrategy;

/**
 * Diagonal movement pattern with slowdown effect and screen-boundary bouncing.
 */
public class EnemyShipFormationMovement implements IMovementStrategy {

    /** Movement direction vector enum */
    private enum Direction {
        DOWN_RIGHT(1, 1),
        DOWN_LEFT(-1, 1),
        UP_RIGHT(1, -1),
        UP_LEFT(-1, -1);

        final int dx;
        final int dy;

        Direction(int dx, int dy) {
            this.dx = dx;
            this.dy = dy;
        }
    }

    private Direction direction = Direction.DOWN_RIGHT;

    private boolean isSlowedDown = false;
    private int slowDownCount = 0;

    private static final int ORIGINAL_X_SPEED = 8;
    private static final int SLOWED_X_SPEED = 4;
    private static final int Y_SPEED = 4;
    private static final int INIT_POS_Y = 100;
    private static final int SIDE_MARGIN = 20;
    private static final int SLOWDOWN_DURATION = 18;

    private final EnemyShipFormationModel model;
    private final Logger logger;
    private final int bottomLimit;
    private final int screenWidth;

    public EnemyShipFormationMovement(EnemyShipFormationModel model, int bottomLimit, int screenWidth) {
        this.model = model;
        this.bottomLimit = bottomLimit;
        this.screenWidth = screenWidth;
        this.logger = Core.getLogger();
    }

    @Override
    public void updateMovement() {

        updateSlowdown();

        updateDirectionBasedOnBoundaries();

        int dx = direction.dx * getCurrentXSpeed();
        int dy = direction.dy * Y_SPEED;

        model.setPosition(model.getPositionX() + dx, model.getPositionY() + dy);
        model.moveAllShips(dx, dy);
    }

    private int getCurrentXSpeed() {
        return isSlowedDown ? SLOWED_X_SPEED : ORIGINAL_X_SPEED;
    }

    private void updateSlowdown() {
        if (isSlowedDown) {
            slowDownCount++;
            if (slowDownCount >= SLOWDOWN_DURATION) {
                isSlowedDown = false;
                slowDownCount = 0;
                logger.info("Slowdown effect ended.");
            }
        }
    }

    private void updateDirectionBasedOnBoundaries() {

        int x = model.getPositionX();
        int y = model.getPositionY();
        int width = model.getWidth();
        int height = model.getHeight();

        boolean atBottom = y + height > bottomLimit;
        boolean atTop = y <= INIT_POS_Y;
        boolean atRight = x + width >= screenWidth - SIDE_MARGIN;
        boolean atLeft = x <= SIDE_MARGIN;

        switch (direction) {

            case DOWN_RIGHT:
                if (atBottom && atRight) direction = Direction.UP_LEFT;
                else if (atBottom)       direction = Direction.UP_RIGHT;
                else if (atRight)        direction = Direction.DOWN_LEFT;
                break;

            case DOWN_LEFT:
                if (atBottom && atLeft) direction = Direction.UP_RIGHT;
                else if (atBottom)      direction = Direction.UP_LEFT;
                else if (atLeft)        direction = Direction.DOWN_RIGHT;
                break;

            case UP_RIGHT:
                if (atTop && atRight) direction = Direction.DOWN_LEFT;
                else if (atTop)       direction = Direction.DOWN_RIGHT;
                else if (atRight)     direction = Direction.UP_LEFT;
                break;

            case UP_LEFT:
                if (atTop && atLeft) direction = Direction.DOWN_RIGHT;
                else if (atTop)      direction = Direction.DOWN_LEFT;
                else if (atLeft)     direction = Direction.UP_RIGHT;
                break;
        }
    }

	/**
	 * Activates the slowdown effect, resetting its duration.
	 */

	@Override
	public boolean needsSmoothMovement() {
		return false;
	}
}