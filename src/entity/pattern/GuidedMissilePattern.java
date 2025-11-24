package entity.pattern;

import engine.Cooldown;
import entity.GuidedBullet;
import entity.HasBounds;
import entity.Ship;
import entity.GameConstant;

import java.awt.*;
import java.util.List;

public class GuidedMissilePattern extends BossPattern {

    private HasBounds boss;
    private List<Ship> players;
    // 20 second cooldown (20000ms)
    private static final int MISSILE_INTERVAL = 20000;

    public GuidedMissilePattern(HasBounds boss, List<Ship> players) {
        super(new Point(boss.getPositionX(), boss.getPositionY()));
        this.boss = boss;
        this.players = players;

        // Set cooldown on pattern creation
        this.shootCooldown = new Cooldown(MISSILE_INTERVAL);
    }

    @Override
    public void attack() {
        if (this.shootCooldown.checkFinished()) {
            this.shootCooldown.reset();

            // Select a random target from alive players
            Ship target = getRandomTarget();
            if (target != null) {
                // X-coordinate: Random value between 0 and screen width
                int startX = (int) (Math.random() * GameConstant.SCREEN_WIDTH);

                // Y-coordinate: Top of the screen (set slightly above to come down)
                int startY = -20;

                GuidedBullet missile = new GuidedBullet(startX, startY, target);
                this.bullets.add(missile);
            }
        }
    }

    @Override
    public void move() {}

    private Ship getRandomTarget() {
        if (players == null || players.isEmpty()) return null;

        // Filter alive players
        java.util.List<Ship> alive = new java.util.ArrayList<>();
        for (Ship s : players) {
            if (s != null && !s.isDestroyed()) {
                alive.add(s);
            }
        }

        if (alive.isEmpty()) return null;
        return alive.get((int)(Math.random() * alive.size()));
    }
}