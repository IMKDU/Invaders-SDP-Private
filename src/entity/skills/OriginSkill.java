package entity.skills;

import engine.Cooldown;
import entity.*;
import entity.pattern.ISkill;

public class OriginSkill implements ISkill {

    private static final int TOTAL_DAMAGE = 80;
    private static final int DURATION_MS = 10000;
    private static final int TICK_MS = 1000;
    private Ship ship;
    private GameModel model;

    private boolean used = false;
    private boolean active = false;

    private Cooldown tickCooldown;
    private long startTime;

    @Override
    public void use(Ship ship) {
        if (used || active) return;

        this.ship = ship;
        this.model = ship.getModel();
        if(this.model.getUsedOrigin()) return;

        this.used = true;
        this.active = true;
        this.model.setUsedOrigin(true);

        this.startTime = System.currentTimeMillis();
        this.tickCooldown = new Cooldown(TICK_MS);
        this.tickCooldown.reset();

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

        if (now - startTime >= DURATION_MS) {
            active = false;
            ship.disableAllControls(false);
            return;
        }

        if (tickCooldown.checkFinished()) {
            tickCooldown.reset();
            int dmg = TOTAL_DAMAGE / (DURATION_MS / TICK_MS);
            applyDamageToAll(dmg);
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

        if (model.getFinalBoss() != null && !model.getFinalBoss().isDestroyed()){
            model.getFinalBoss().takeDamage(dmg);
        }

        if (model.getOmegaBoss() != null && !model.getOmegaBoss().isDestroyed()){
            model.getOmegaBoss().takeDamage(dmg);
            model.getOmegaBoss().update();
        }

        if(model.getZetaBoss() != null && !model.getZetaBoss().isDestroyed()){
            model.getZetaBoss().takeDamage(dmg);
        }
    }
    public boolean isActive() { return active; }
    public boolean isUsed() { return used; }
}
