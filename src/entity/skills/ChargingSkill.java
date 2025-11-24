package entity.pattern.skills;

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
    private static final long COOLDOWN_DURATION = 10000;

    @Override
    public void use(Ship ship) {
        // This method is called when the skill is triggered
        // The actual charging logic is handled by the Ship class
        // This method just marks that the skill should start charging
    }
}