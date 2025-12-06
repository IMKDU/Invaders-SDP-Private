package entity.pattern;

import audio.SoundManager;
import engine.Cooldown;
import entity.Explosion;
import entity.GameConstant;
import java.awt.*;

public class BackgroundExplosionPattern extends BossPattern {

    private Cooldown cooldown;
    private Explosion explosionEntity = null;
    private final int MARGIN = 150;
    private final int MARGIN_2 = 200;
    private boolean isSoundPlayed;

    /**
     * Initializes the background explosion pattern and sets the spawn cooldown.
     * */
    public BackgroundExplosionPattern() {
        super(new Point(0,0));
        this.cooldown = new Cooldown(3000);
        this.cooldown.reset();
    }

    /**
     * Spawns a new explosion entity at a random position within the valid game area.
     * */
    private void spawnExplosion() {
        int randomX = (int) (Math.random() * (GameConstant.SCREEN_WIDTH - MARGIN));
        int randomY = GameConstant.STAT_SEPARATION_LINE_HEIGHT + (int) (Math.random() * (GameConstant.ITEMS_SEPARATION_LINE_HEIGHT - GameConstant.STAT_SEPARATION_LINE_HEIGHT - MARGIN_2));
        explosionEntity = new Explosion(randomX, randomY);
    }

    /**
     * Executes the attack logic
     */
    @Override
    public void attack() {
        if (explosionEntity != null) {
            explosionEntity.update();
            if (explosionEntity.isBoom() && !isSoundPlayed) {
                SoundManager.play("sfx/BombExplosion.wav");
                isSoundPlayed = true;
            }
	        if (explosionEntity.shouldBeRemoved()) {
                isSoundPlayed = false;
				spawnExplosion();
                this.cooldown.reset();
            }
        } else if (cooldown.checkFinished()) {
            spawnExplosion();
            cooldown.reset();
        }
    }

    /**
     * Handles movement logic (unused in this pattern).
     */
    @Override
    public void move() {
    }

    /**
     * Returns the currently active explosion entity.
     * @return The active Explosion object, or null if none exists.
     */
    public Explosion getBoom(){
        return explosionEntity;
    }
}