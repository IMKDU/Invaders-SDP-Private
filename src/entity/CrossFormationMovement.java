package entity;

import java.awt.Point;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import engine.Cooldown;
/**
 * Ships move diagonally from corner to corner using a "Look-Ahead Vector" strategy
 * to ensure smooth movement without stuttering.
 */
public class CrossFormationMovement implements IMovementStrategy {

    private final int SHIP_SPEED = 8;
    private final int LOOK_AHEAD_DISTANCE = 80;
    private final float LINE_SPEED = 0.005f;

    private final int PHASE_COOLDOWN_MS = 2000;
    private final int STAGGER_INTERVAL_MS = 500;
    private final int OVERSHOOT_X = 100;
    private final int ARRIVAL_THRESHOLD = 20;
    private final double DISTANCE_THRESHOLD_FOR_SPEED_BOOST = 2.0;

    private List<List<EnemyShip>> enemyShips;
    private Map<EnemyShip, ShipState> shipStates;

    private int screenWidth;
    private int shipWidth;
    private int safeStartY;
    private int safeEndY;
    private Point tempPoint = new Point();

    private boolean isLeftToRightPhase;
    private int finishedShipCount;
    private Cooldown phaseCooldown;

    private Point currentStart;
    private Point currentEnd;

    /**
     * Inner class to track individual ship state.
     */
    private class ShipState {
        float t;
        Cooldown cooldown;
        boolean isFinished;
        boolean hasStarted;

        /**
         * Constructor for ShipState.
         * @param delayMs Initial delay in milliseconds before the ship starts moving.
         */
        public ShipState(int delayMs) {
            this.isFinished = false;
            this.hasStarted = false;
            this.t = 0.0f;
            this.cooldown = new Cooldown(delayMs);
            this.cooldown.reset();
        }
    }

    /**
     * Initializes the Cross Formation movement strategy.
     * @param enemyShips The list of enemy ships to control.
     */
    public CrossFormationMovement(List<List<EnemyShip>> enemyShips) {
        this.enemyShips = enemyShips;
        this.shipStates = new HashMap<>();
        this.screenWidth = GameConstant.SCREEN_WIDTH;

        this.shipWidth = 0;
        if (enemyShips != null && !enemyShips.isEmpty() && !enemyShips.getFirst().isEmpty()) {
            this.shipWidth = enemyShips.getFirst().getFirst().getWidth();
        }

        this.safeStartY = GameConstant.STAT_SEPARATION_LINE_HEIGHT + 50;
        this.safeEndY = GameConstant.ITEMS_SEPARATION_LINE_HEIGHT - 50;

        this.isLeftToRightPhase = true;
        this.phaseCooldown = new Cooldown(0);
        this.phaseCooldown.reset();

        setupPhaseCoordinates();
        initializeShips();
    }

    /**
     * Calculates and sets the start and end coordinates based on the current phase direction.
     * Adjusts positions to ensure ships spawn outside the visible area.
     */
    private void setupPhaseCoordinates() {

        if (isLeftToRightPhase) {
            this.currentStart = new Point(-OVERSHOOT_X, safeStartY);
            this.currentEnd = new Point(screenWidth + OVERSHOOT_X, safeEndY);
        } else {
            this.currentStart = new Point(screenWidth + OVERSHOOT_X - shipWidth, safeStartY);
            this.currentEnd = new Point(-OVERSHOOT_X, safeEndY);
        }
    }

    /**
     * Resets all ships to their waiting positions and assigns staggered start delays.
     */
    private void initializeShips() {
        if (enemyShips == null) return;

        this.finishedShipCount = 0;
        int count = 0;
        int waitingX = isLeftToRightPhase ? -1000 : screenWidth + 1000;

        for (List<EnemyShip> column : enemyShips) {
            for (EnemyShip ship : column) {
                ship.setPositionY(safeStartY);
                ship.setPositionX(waitingX);

                int delayMs = count * STAGGER_INTERVAL_MS;
                shipStates.put(ship, new ShipState(delayMs));
                count++;
            }
        }
    }

    /**
     * Updates the movement logic for all ships every frame.
     * Handles phase transitions, individual start delays, and vector movement.
     */
    @Override
    public void updateMovement() {
        if (enemyShips == null) return;
        // Wait for phase cooldown
        if (!phaseCooldown.checkFinished()) {
            return;
        }
        // If the previous phase just finished, switch to the next phase
        if (finishedShipCount > 0 && isAllShipsFinished()) {
            switchPhase();
            return;
        }
        int totalShips = 0;

        for (List<EnemyShip> column : enemyShips) {
            for (EnemyShip ship : column) {
                if (ship.isDestroyed()) continue;
                totalShips++;
                ShipState state = shipStates.get(ship);
                if (state == null) continue;
                if (state.isFinished) continue;
                // Check start delay
                if (!state.cooldown.checkFinished()) {
                    continue;
                }
                // Snap to start position on the first frame of movement
                if (!state.hasStarted) {
                    ship.setPositionX(currentStart.x);
                    ship.setPositionY(currentStart.y);
                    state.hasStarted = true;
                }
                moveShipWithLookAhead(ship, state);
            }
        }
        // Check if the current phase is complete
        if (finishedShipCount >= totalShips && totalShips > 0) {
            if (phaseCooldown.checkFinished() && phaseCooldown.getTotal() == 0) {
                startGlobalCooldown();
            }
        }
    }

    /**
     * Checks if all active ships have finished their path.
     * @return True if all active ships are finished, false otherwise.
     */
    private boolean isAllShipsFinished() {
        int active = 0;
        int finished = 0;
        for (List<EnemyShip> column : enemyShips) {
            for (EnemyShip ship : column) {
                if(!ship.isDestroyed()) {
                    active++;
                    if(shipStates.get(ship).isFinished) finished++;
                }
            }
        }
        return active > 0 && active == finished;
    }

    /**
     * Calculates the look-ahead target position and moves the ship.
     * @param ship The ship to move.
     * @param state The current state of the ship.
     */
    private void moveShipWithLookAhead(EnemyShip ship, ShipState state) {
        int currentX = ship.getPositionX();
        int currentY = ship.getPositionY();
        updateLinearPoint(state.t);
        double dx = tempPoint.x - currentX;
        double dy = tempPoint.y - currentY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        while (distance < LOOK_AHEAD_DISTANCE && state.t < 1.0f) {
            state.t += LINE_SPEED;
            updateLinearPoint(state.t);
            dx = tempPoint.x - currentX;
            dy = tempPoint.y - currentY;
            distance = Math.sqrt(dx * dx + dy * dy);
        }

        // Check if path is finished
        if (state.t >= 1.0f && distance < ARRIVAL_THRESHOLD) {
            state.isFinished = true;
            finishedShipCount++;
            if (isLeftToRightPhase) ship.setPositionX(screenWidth + 1000);
            else ship.setPositionX(-1000);
            return;
        }
        moveToTargetVector(ship, tempPoint.x, tempPoint.y);
    }

    /**
     * Moves the ship towards the target using vector calculation.
     * @param ship The ship to move.
     * @param targetX The X coordinate of the target.
     * @param targetY The Y coordinate of the target.
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


    private void updateLinearPoint(float t) {
        if (t > 1.0f) t = 1.0f;
        int x = (int) (currentStart.x + (currentEnd.x - currentStart.x) * t);
        int y = (int) (currentStart.y + (currentEnd.y - currentStart.y) * t);
        this.tempPoint.setLocation(x, y);
    }
    /**
     * Starts the cooldown timer between phases.
     */
    private void startGlobalCooldown() {
        phaseCooldown.setMilliseconds(PHASE_COOLDOWN_MS);
        phaseCooldown.reset();
    }
    /**
     * Switches the movement direction and resets ships for the new phase.
     */
    private void switchPhase() {
        isLeftToRightPhase = !isLeftToRightPhase;
        setupPhaseCoordinates();
        initializeShips();
        phaseCooldown.setMilliseconds(0);
        phaseCooldown.reset();
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