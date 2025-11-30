package entity;

import java.awt.Point;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CrossFormationMovement implements IMovementStrategy {

    private static final int SHIP_SPEED = 5;
    private static final int LOOK_AHEAD_DISTANCE = 80;
    private static final float LINE_SPEED = 0.005f;
    private static final int PHASE_COOLDOWN = 30;
    private static final int STAGGER_INTERVAL = 10;
    private static final double DISTANCE_THRESHOLD_FOR_SPEED_BOOST = 2.0;

    private List<List<EnemyShip>> enemyShips;
    private Map<EnemyShip, ShipState> shipStates;

    private int screenWidth;
    private int shipWidth;

    // Coordinates
    private int safeStartY;
    private int safeEndY;

    private boolean isLeftToRightPhase;
    private int finishedShipCount;
    private int globalCooldownTimer;

    private Point currentStart;
    private Point currentEnd;

    private class ShipState {
        float t;
        int waitTimer;
        boolean isFinished;
        boolean hasStarted;

        public ShipState(int delay) {
            this.waitTimer = delay;
            this.isFinished = false;
            this.hasStarted = false; // 처음엔 시작 안 함
            this.t = 0.0f;
        }
    }

    public CrossFormationMovement(List<List<EnemyShip>> enemyShips) {
        this.enemyShips = enemyShips;
        this.shipStates = new HashMap<>();
        this.screenWidth = GameConstant.SCREEN_WIDTH;

        this.shipWidth = 0;
        if (enemyShips != null && !enemyShips.isEmpty() && !enemyShips.get(0).isEmpty()) {
            this.shipWidth = enemyShips.get(0).get(0).getWidth();
        }

        this.safeStartY = GameConstant.STAT_SEPARATION_LINE_HEIGHT + 50;
        this.safeEndY = GameConstant.ITEMS_SEPARATION_LINE_HEIGHT - 50;

        this.isLeftToRightPhase = true;
        this.globalCooldownTimer = 0;

        setupPhaseCoordinates();
        initializeShips();
    }

    private void setupPhaseCoordinates() {
        int overShootX = 100;

        if (isLeftToRightPhase) {
            this.currentStart = new Point(-overShootX, safeStartY);
            this.currentEnd = new Point(screenWidth + overShootX, safeEndY);
        } else {
            this.currentStart = new Point(screenWidth + overShootX - shipWidth, safeStartY);
            this.currentEnd = new Point(-overShootX, safeEndY);
        }
    }


    private void initializeShips() {
        if (enemyShips == null) return;

        this.finishedShipCount = 0;
        int count = 0;

        int waitingX;
        if (isLeftToRightPhase) {
            waitingX = -1000;
        } else {
            waitingX = screenWidth + 1000;
        }

        for (List<EnemyShip> column : enemyShips) {
            for (EnemyShip ship : column) {
                ship.setPositionY(safeStartY);
                ship.setPositionX(waitingX);

                int delay = count * STAGGER_INTERVAL;
                shipStates.put(ship, new ShipState(delay));
                count++;
            }
        }
    }

    @Override
    public void updateMovement() {
        if (enemyShips == null) return;

        if (globalCooldownTimer > 0) {
            globalCooldownTimer--;
            if (globalCooldownTimer == 0) {
                switchPhase();
            }
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

                if (state.waitTimer > 0) {
                    state.waitTimer--;
                    continue;
                }

                if (!state.hasStarted) {
                    ship.setPositionX(currentStart.x);
                    ship.setPositionY(currentStart.y);
                    state.hasStarted = true;
                }

                moveShipWithLookAhead(ship, state);
            }
        }

        if (finishedShipCount >= totalShips && totalShips > 0) {
            startGlobalCooldown();
        }
    }

    private void moveShipWithLookAhead(EnemyShip ship, ShipState state) {
        int currentX = ship.getPositionX();
        int currentY = ship.getPositionY();

        Point targetPos = getLinearPoint(state.t);
        double dist = Math.sqrt(Math.pow(targetPos.x - currentX, 2) + Math.pow(targetPos.y - currentY, 2));

        while (dist < LOOK_AHEAD_DISTANCE && state.t < 1.0f) {
            state.t += LINE_SPEED;
            targetPos = getLinearPoint(state.t);
            dist = Math.sqrt(Math.pow(targetPos.x - currentX, 2) + Math.pow(targetPos.y - currentY, 2));
        }

        if (state.t >= 1.0f && dist < 20) {
            state.isFinished = true;
            finishedShipCount++;

            if (isLeftToRightPhase) ship.setPositionX(screenWidth + 1000);
            else ship.setPositionX(-1000);

            return;
        }

        moveToTargetVector(ship, targetPos.x, targetPos.y);
    }

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

    private Point getLinearPoint(float t) {
        if (t > 1.0f) t = 1.0f;
        int x = (int) (currentStart.x + (currentEnd.x - currentStart.x) * t);
        int y = (int) (currentStart.y + (currentEnd.y - currentStart.y) * t);
        return new Point(x, y);
    }

    private void startGlobalCooldown() {
        globalCooldownTimer = PHASE_COOLDOWN;
    }

    private void switchPhase() {
        isLeftToRightPhase = !isLeftToRightPhase;
        setupPhaseCoordinates();
        initializeShips();
    }

    @Override
    public void activateSlowdown() { }

    @Override
    public boolean needsSmoothMovement() {
        return true;
    }
}