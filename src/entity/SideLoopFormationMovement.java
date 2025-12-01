package entity;

import java.awt.Point;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import engine.Cooldown;
/**
 * Handles the Side Loop movement pattern using a look-ahead vector strategy.
 * Implements a "Follow-the-Leader" logic to prevent ship overlapping.
 */
public class SideLoopFormationMovement implements IMovementStrategy {

    private final int SHIP_SPEED = 4;
    private final int LOOK_AHEAD_DISTANCE = 80;
    private final float SEARCH_STEP = 0.005f;

    // Distance gap for follow-the-leader logic
    private final float SPAWN_GAP_T = 0.22f;

    private final int RESPAWN_DELAY_MS = 1000;
    private final int ARC_WIDTH = 700;
    private final int PATH_HEIGHT = 600;
    private final double DISTANCE_THRESHOLD_FOR_SPEED_BOOST = 2.0;

    private List<List<EnemyShip>> enemyShips;
    private Map<EnemyShip, ShipState> shipStates;

    private Point l_p0, l_p1, l_p2, l_p3;
    private Point r_p0, r_p1, r_p2, r_p3;

    private int screenWidth;
    private int startY;
    private int shipWidth;

    /**
     * Inner class to track individual ship state.
     * Replaced 'int waitTimer' with 'Cooldown' object.
     */
    private class ShipState {
        float t;
        Cooldown cooldown;
        boolean isMoving;
        boolean isLeftTeam;
        boolean isFinished;

        public ShipState(boolean isLeft) {
            this.isLeftTeam = isLeft;
            this.isMoving = false;
            this.isFinished = false;
            this.t = 0.0f;
            this.cooldown = new Cooldown(0);
        }
    }
    /**
     * Initializes the movement strategy and paths.
     * @param enemyShips The list of enemy ships to control.
     */
    public SideLoopFormationMovement(List<List<EnemyShip>> enemyShips) {
        this.enemyShips = enemyShips;
        this.shipStates = new HashMap<>();
        this.screenWidth = GameConstant.SCREEN_WIDTH;
        this.startY = GameConstant.STAT_SEPARATION_LINE_HEIGHT + 50;
        this.shipWidth = 0;
        if (enemyShips != null && !enemyShips.isEmpty() && !enemyShips.getFirst().isEmpty()) {
            this.shipWidth = enemyShips.getFirst().getFirst().getWidth();
        }
        initializePaths();
        initializeShips();
    }

    /**
     * Defines the Bezier control points for left and right paths.
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
     * Sets initial states for all ships.
     */
    private void initializeShips() {
        if (enemyShips == null) return;

        int totalCols = enemyShips.size();
        int centerCol = totalCols / 2;

        for (int col = 0; col < totalCols; col++) {
            boolean isLeftTeam = (col < centerCol);
            List<EnemyShip> column = enemyShips.get(col);

            for (EnemyShip ship : column) {
                ship.setPositionX(-500);
                ship.setPositionY(-500);

                shipStates.put(ship, new ShipState(isLeftTeam));
            }
        }
    }

    /**
     * Updates movement for all ships every frame.
     */
    @Override
    public void updateMovement() {
        if (enemyShips == null) return;

        ShipState prevLeftShipState = null;
        ShipState prevRightShipState = null;

        for (List<EnemyShip> column : enemyShips) {
            for (EnemyShip ship : column) {
                if (ship.isDestroyed()) continue;

                ShipState state = shipStates.get(ship);
                if (state == null) continue;

                if (!state.cooldown.checkFinished()) {
                    continue;
                }

                if (state.isFinished && state.cooldown.checkFinished()) {
                    state.isFinished = false;
                    state.isMoving = false;
                    state.t = 0.0f;
                }
                if (state.isFinished) continue;

                ShipState leader = state.isLeftTeam ? prevLeftShipState : prevRightShipState;
                boolean canStart = false;

                if (leader == null) {
                    canStart = true;
                } else {
                    if (leader.isFinished || (leader.isMoving && leader.t >= SPAWN_GAP_T)) {
                        canStart = true;
                    }
                }

                if (!state.isMoving && canStart) {
                    startRun(ship, state);
                }

                if (state.isMoving) {
                    moveShipWithLookAhead(ship, state);
                }

                if (state.isLeftTeam) prevLeftShipState = state;
                else prevRightShipState = state;
            }
        }
    }

    /**
     * Calculates look-ahead target and checks for path completion.
     * @param ship The ship to move.
     * @param state The ship's state.
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
     * Executes vector-based movement towards the target.
     * @param ship The ship to move.
     * @param targetX Target X coordinate.
     * @param targetY Target Y coordinate.
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
     * Moves the ship to the start position.
     * @param ship The ship to move.
     * @param state The ship's state.
     */
    private void startRun(EnemyShip ship, ShipState state) {
        state.isMoving = true;
        state.t = 0.0f;
        Point start = getBezierPoint(0.0f, state.isLeftTeam);
        ship.setPositionX(start.x);
        ship.setPositionY(start.y);
    }

    /**
     * @param ship The ship to hide.
     * @param state The ship's state.
     */
    private void finishRun(EnemyShip ship, ShipState state) {
        state.isMoving = false;
        state.isFinished = true;
        state.cooldown.setMilliseconds(RESPAWN_DELAY_MS);
        state.cooldown.reset();
        ship.setPositionX(-500);
    }

    /**
     * Returns the appropriate Bezier point based on the team.
     * @param t Progress (0.0 to 1.0).
     * @param isLeft True if left team, false otherwise.
     * @return The calculated Point.
     */
    private Point getBezierPoint(float t, boolean isLeft) {
        if (isLeft) return getCubicBezierPoint(t, l_p0, l_p1, l_p2, l_p3);
        else return getCubicBezierPoint(t, r_p0, r_p1, r_p2, r_p3);
    }

    /**
     * Calculates a point on a Cubic Bezier curve.
     * @param t Progress (0.0 to 1.0).
     * @param p0 Start point.
     * @param p1 Control point 1.
     * @param p2 Control point 2.
     * @param p3 End point.
     * @return The calculated Point.
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
     * Indicates this strategy requires per-frame updates.
     * @return True.
     */
    @Override
    public boolean needsSmoothMovement() {
        return true;
    }
}