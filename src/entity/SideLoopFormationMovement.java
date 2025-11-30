package entity;

import java.awt.Point;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * Handles the Side Loop movement pattern using a look-ahead vector strategy.
 */
public class SideLoopFormationMovement implements IMovementStrategy {

    private static final int SHIP_SPEED = 4;
    private static final int LOOK_AHEAD_DISTANCE = 80;
    private static final float SEARCH_STEP = 0.005f;
    private static final int RESPAWN_DELAY = 60;
    private static final int STAGGER_INTERVAL = 20;
    private static final int ARC_WIDTH = 700;
    private static final int PATH_HEIGHT = 600;
    private static final double DISTANCE_THRESHOLD_FOR_SPEED_BOOST = 2.0;

    private List<List<EnemyShip>> enemyShips;
    private Map<EnemyShip, ShipState> shipStates;

    private Point l_p0, l_p1, l_p2, l_p3;
    private Point r_p0, r_p1, r_p2, r_p3;

    private int screenWidth;
    private int startY;
    private int shipWidth;

    private class ShipState {
        float t;
        int waitTimer;
        boolean isMoving;
        boolean isLeftTeam;

        public ShipState(int delay, boolean isLeft) {
            this.waitTimer = delay;
            this.isLeftTeam = isLeft;
            this.isMoving = false;
            this.t = 0.0f;
        }
    }
    /**
     * Initializes the movement strategy with the given enemy ships.
     * @param enemyShips List of enemy ships.
     */
    public SideLoopFormationMovement(List<List<EnemyShip>> enemyShips) {
        this.enemyShips = enemyShips;
        this.shipStates = new HashMap<>();
        this.screenWidth = GameConstant.SCREEN_WIDTH;
        this.startY = GameConstant.STAT_SEPARATION_LINE_HEIGHT + 50;

        this.shipWidth = 0;
        if (enemyShips != null && !enemyShips.isEmpty() && !enemyShips.get(0).isEmpty()) {
            this.shipWidth = enemyShips.get(0).get(0).getWidth();
        }

        initializePaths();
        initializeShips();
    }

    /**
     * Sets up the Bezier curve control points for both sides.
     */
    private void initializePaths() {
        l_p0 = new Point(0, startY);
        l_p1 = new Point(ARC_WIDTH, startY);
        l_p2 = new Point(ARC_WIDTH, startY + PATH_HEIGHT);
        l_p3 = new Point(0, startY + PATH_HEIGHT);

        int rightWallX = screenWidth - shipWidth;

        r_p0 = new Point(rightWallX, startY);
        r_p1 = new Point(rightWallX - ARC_WIDTH, startY);
        r_p2 = new Point(rightWallX - ARC_WIDTH, startY + PATH_HEIGHT);
        r_p3 = new Point(rightWallX, startY + PATH_HEIGHT);
    }
    /**
     * Sets the initial position and state for each ship.
     */
    private void initializeShips() {
        if (enemyShips == null) return;

        int totalCols = enemyShips.size();
        int centerCol = totalCols / 2;
        int leftCount = 0;
        int rightCount = 0;

        for (int col = 0; col < totalCols; col++) {
            boolean isLeftTeam = (col < centerCol);
            List<EnemyShip> column = enemyShips.get(col);

            for (EnemyShip ship : column) {
                ship.setPositionX(-500);
                ship.setPositionY(-500);

                int delay = (isLeftTeam ? leftCount++ : rightCount++) * STAGGER_INTERVAL;
                shipStates.put(ship, new ShipState(delay, isLeftTeam));
            }
        }
    }
    /**
     * Updates the movement logic for all ships.
     */
    @Override
    public void updateMovement() {
        if (enemyShips == null) return;

        for (List<EnemyShip> column : enemyShips) {
            for (EnemyShip ship : column) {
                if (ship.isDestroyed()) continue;

                ShipState state = shipStates.get(ship);
                if (state == null) continue;

                if (state.waitTimer > 0) {
                    state.waitTimer--;
                    if (state.waitTimer == 0) startRun(ship, state);
                    continue;
                }

                if (state.isMoving) {
                    moveShipWithLookAhead(ship, state);
                }
            }
        }
    }
    /**
     * Calculates the look-ahead target position on the curve.
     * @param ship The ship to move.
     * @param state The state of the ship.
     */
    private void moveShipWithLookAhead(EnemyShip ship, ShipState state) {
        int currentX = ship.getPositionX();
        int currentY = ship.getPositionY();

        Point targetPos = getBezierPoint(state.t, state.isLeftTeam);
        double dist = Math.sqrt(Math.pow(targetPos.x - currentX, 2) + Math.pow(targetPos.y - currentY, 2));

        while (dist < LOOK_AHEAD_DISTANCE && state.t < 1.0f) {
            state.t += SEARCH_STEP;
            targetPos = getBezierPoint(state.t, state.isLeftTeam);
            dist = Math.sqrt(Math.pow(targetPos.x - currentX, 2) + Math.pow(targetPos.y - currentY, 2));
        }

        if (state.t >= 1.0f && dist < 20) {
            finishRun(ship, state);
            return;
        }

        moveToTargetVector(ship, targetPos.x, targetPos.y);
    }
    /**
     * Moves the ship towards the target using vector calculation.
     * @param ship The ship to move.
     * @param targetX The target X coordinate.
     * @param targetY The target Y coordinate.
     */
    private void moveToTargetVector(EnemyShip ship, int targetX, int targetY) {
        double dx = targetX - ship.getPositionX();
        double dy = targetY - ship.getPositionY();
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance < SHIP_SPEED) {
            ship.setPositionX(targetX);
            ship.setPositionY(targetY);
            return;
        }

        double speedMultiplier = (distance > 10) ? DISTANCE_THRESHOLD_FOR_SPEED_BOOST : 1.0;

        int moveX = (int) (dx * SHIP_SPEED / distance * speedMultiplier);
        int moveY = (int) (dy * SHIP_SPEED / distance * speedMultiplier);

        if (moveX == 0 && dx != 0) moveX = (dx > 0) ? 1 : -1;
        if (moveY == 0 && dy != 0) moveY = (dy > 0) ? 1 : -1;

        ship.move(moveX, moveY, false);
    }
    /**
     * Handles the completion of the path and starts the cooldown.
     * @param ship The ship that finished the path.
     * @param state The state of the ship.
     */
    private void startRun(EnemyShip ship, ShipState state) {
        state.isMoving = true;
        state.t = 0.0f;
        Point start = getBezierPoint(0.0f, state.isLeftTeam);
        ship.setPositionX(start.x);
        ship.setPositionY(start.y);
    }
    /**
     * Handles the completion of the path and starts the cooldown.
     * @param ship The ship that finished the path.
     * @param state The state of the ship.
     */
    private void finishRun(EnemyShip ship, ShipState state) {
        state.isMoving = false;
        state.waitTimer = RESPAWN_DELAY;
        ship.setPositionX(-500);
    }
    /**
     * Returns the appropriate Bezier point based on the team.
     * @param t Time step (0.0 to 1.0).
     * @param isLeft True if left team, false otherwise.
     * @return Calculated point.
     */
    private Point getBezierPoint(float t, boolean isLeft) {
        if (isLeft) return getCubicBezierPoint(t, l_p0, l_p1, l_p2, l_p3);
        else return getCubicBezierPoint(t, r_p0, r_p1, r_p2, r_p3);
    }
    /**
     * Returns the calculated point on the Cubic Bezier curve.
     * @param t Time step (0.0 to 1.0).
     * @param p0 Start point.
     * @param p1 Control point 1.
     * @param p2 Control point 2.
     * @param p3 End point.
     * @return Calculated point.
     */
    private Point getCubicBezierPoint(float t, Point p0, Point p1, Point p2, Point p3) {
        if (t > 1.0f) t = 1.0f;
        float u = 1 - t;
        float tt = t * t;
        float uu = u * u;
        float uuu = uu * u;
        float ttt = tt * t;

        double x = uuu * p0.x + 3 * uu * t * p1.x + 3 * u * tt * p2.x + ttt * p3.x;
        double y = uuu * p0.y + 3 * uu * t * p1.y + 3 * u * tt * p2.y + ttt * p3.y;
        return new Point((int) x, (int) y);
    }

    @Override
    public void activateSlowdown() { }
    /**
     * Returns true to enable smooth per-frame updates.
     * @return True.
     */
    @Override
    public boolean needsSmoothMovement() {
        return true;
    }
}