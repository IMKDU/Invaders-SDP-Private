package entity.skills;

import audio.SoundManager;
import entity.Ship;
import entity.pattern.ISkill;

/**
 * ChargingSkill: A skill that charges for 5 seconds and fires a laser beam.
 * - Hold C key for 5 seconds to fully charge
 * - When fully charged, fires a straight laser beam the width of the ship
 * - Laser lasts for 1.5 seconds
 */
public class ChargingSkill implements ISkill {

    /** Duration required to fully charge the skill (in milliseconds) */
    private static final long CHARGE_DURATION = 5000;

    /** Duration of the laser beam after firing (in milliseconds) */
    private static final long LASER_DURATION = 1500;

    /** Cooldown duration after using the skill (in milliseconds) */
    private static final long COOLDOWN_DURATION = 0;

    // === Charging Skill State ===
    /** Indicates if the ship is currently charging the skill */

    // === Ship Reference ===
    /** Reference to the ship that owns this skill */
    private Ship ship;

    private boolean isCharging = false;
    /** Time when charging started (in milliseconds) */
    private long chargeStartTime = 0;
    /** Indicates if the laser beam is currently active */
    private boolean isLaserActive = false;
    /** Time when the laser was fired (in milliseconds) */
    private long laserStartTime = 0;
    /** Last time the charging skill was used (for cooldown tracking) */
    private long lastChargingSkillUse = 0;

    @Override
    public void use(Ship ship) {
        this.ship = ship;
    }

    /**
     * Updates the charging skill state, handling charge completion and laser duration.
     */
    public void update() {
        long currentTime = System.currentTimeMillis();

        // Check if charging is complete (5 seconds)
        if (isCharging && (currentTime - chargeStartTime) >= CHARGE_DURATION) {
            // Automatically fire the laser when fully charged
            fireLaser();
        }

        // Check if laser duration has expired (1.5 seconds)
        if (isLaserActive && (currentTime - laserStartTime) >= LASER_DURATION) {
            isLaserActive = false;
        }
    }

    /**
     * Starts charging the skill if not on cooldown.
     * Should be called when the player presses and holds the C key.
     */
    public void startCharging() {
        long currentTime = System.currentTimeMillis();

        // Check if skill is on cooldown
        if (currentTime - lastChargingSkillUse < COOLDOWN_DURATION) {
            return; // Still on cooldown, can't charge
        }

        // Don't allow charging if laser is already active
        if (isLaserActive) {
            return;
        }

        // Start charging
        if (!isCharging) {
            isCharging = true;
            chargeStartTime = currentTime;
        }
    }

    /**
     * Stops charging the skill without firing.
     * Should be called when the player releases the C key before fully charged.
     */
    public void stopCharging() {
        if (isCharging) {
            isCharging = false;
            chargeStartTime = 0;
        }
    }

    /**
     * Fires the laser beam when fully charged.
     * Called automatically when charge duration is complete.
     */
    private void fireLaser() {
        if (!isCharging) {
            return;
        }

        // Stop charging and activate laser
        isCharging = false;
        isLaserActive = true;
        laserStartTime = System.currentTimeMillis();
        lastChargingSkillUse = laserStartTime;

        // Play sound effect
        SoundManager.stop("sfx/laser.wav");
        SoundManager.play("sfx/laser.wav");
    }

    /**
     * Gets the current charge progress as a percentage (0.0 to 1.0).
     * @return Charge progress percentage
     */
    public double getChargeProgress() {
        if (!isCharging) {
            return 0.0;
        }

        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - chargeStartTime;
        double progress = (double) elapsedTime / CHARGE_DURATION;

        return Math.min(progress, 1.0);
    }

    /**
     * Gets the current cooldown progress as a percentage (0.0 to 1.0).
     * @return Cooldown progress percentage (0.0 = ready, 1.0 = just used)
     */
    public double getCooldownProgress() {
        long currentTime = System.currentTimeMillis();
        long timeSinceUse = currentTime - lastChargingSkillUse;

        if (timeSinceUse >= COOLDOWN_DURATION) {
            return 0.0; // Skill is ready
        }

        double remaining = 1.0 - ((double) timeSinceUse / COOLDOWN_DURATION);
        return remaining;
    }

    /**
     * Checks if the ship is currently charging the skill.
     * @return True if charging
     */
    public boolean isCharging() {
        return isCharging;
    }

    /**
     * Checks if the laser beam is currently active.
     * @return True if laser is active
     */
    public boolean isLaserActive() {
        return isLaserActive;
    }

    /**
     * Checks if the charging skill is ready to use (not on cooldown).
     * @return True if skill is ready
     */
    public boolean isChargingSkillReady() {
        long currentTime = System.currentTimeMillis();
        return (currentTime - lastChargingSkillUse) >= COOLDOWN_DURATION;
    }

    /**
     * Gets the ship reference (for future extensions if needed).
     * @return The ship that owns this skill
     */
    public Ship getShip() {
        return ship;
    }
}