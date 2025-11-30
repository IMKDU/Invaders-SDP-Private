package entity.skills;

import engine.Cooldown;
import entity.*;
import entity.pattern.ISkill;

public class OriginSkill implements ISkill {

    private static final int TOTAL_DAMAGE = 80;
    private static final int DURATION_MS = 11000;

    private static final int BURST_WINDOW_MS = 1000;
    private static final int BURST_HITS = (int)(TOTAL_DAMAGE*0.2);
    private static final int BURST_INTERVAL = BURST_WINDOW_MS / BURST_HITS;

    private Ship ship;
    private GameModel model;

    private boolean used = false;
    private boolean active = false;

    private Cooldown burstTick;
    private long startTime;
    private int burstDamagePerHit;
    private int burstHitsRemaining;

    @Override
    public void use(Ship ship) {
        if (used || active) return;

        this.ship = ship;
        this.model = ship.getModel();
        if (this.model.getUsedOrigin()) return;

        this.used = true;
        this.active = true;
        this.model.setUsedOrigin(true);
        this.model.setOriginSkillActivated(true);

        this.startTime = System.currentTimeMillis();

        int instantDamage = (int) (TOTAL_DAMAGE * 0.2);
        applyDamageToAll(instantDamage);

        int remainingDamage = TOTAL_DAMAGE - instantDamage;
        this.burstDamagePerHit = remainingDamage / BURST_HITS;
        this.burstHitsRemaining = BURST_HITS;

        this.burstTick = new Cooldown(BURST_INTERVAL);
        this.burstTick.reset();

        for (Ship s : model.getShips()) {
            if (s != null && !s.isDestroyed()) {
                s.activateInvincibility(DURATION_MS);
            }
        }

        ship.disableAllControls(true);
    }

    public void update() {
        if (!active) return;

        long now = System.currentTimeMillis();
        long elapsed = now - startTime;
        if (elapsed >= DURATION_MS) {
            active = false;
            ship.disableAllControls(false);
            return;
        }
        if (elapsed >= DURATION_MS - BURST_WINDOW_MS && burstHitsRemaining > 0) {
            if (burstTick.checkFinished()) {
                burstTick.reset();
                applyDamageToAll(burstDamagePerHit);
                burstHitsRemaining--;
            }
        }
    }

    private void applyDamageToAll(int dmg) {
        EnemyShipFormationModel formation = model.getEnemyShipFormationModel();
        if (formation != null) {
            for (EnemyShip e : formation) {
                if (e != null && !e.isDestroyed()) {
                    formation.destroy(e);
                }
            }
        }

        EnemyShipSpecialFormation specialFormation = model.getEnemyShipSpecialFormation();
        if (specialFormation != null) {
            for (EnemyShip e : specialFormation) {
                if (e != null && !e.isDestroyed()) {
                    specialFormation.destroy(e);
                }
            }
        }

        if (model.getFinalBoss() != null && !model.getFinalBoss().isDestroyed()) {
            model.getFinalBoss().takeDamage(dmg);
        }

        if (model.getOmegaBoss() != null && !model.getOmegaBoss().isDestroyed()) {
            model.getOmegaBoss().takeDamage(dmg);
        }

        if (model.getZetaBoss() != null && !model.getZetaBoss().isDestroyed()) {
            model.getZetaBoss().takeDamage(dmg);
        }
    }

    public boolean isActive() { return active; }
    public boolean isUsed() { return used; }
}
