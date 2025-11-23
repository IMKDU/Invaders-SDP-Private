package entity.pattern;

import engine.Cooldown;
import entity.Explosion;
import entity.GameConstant;
import java.awt.*;

public class BackgroundExplosionPattern extends BossPattern {

    protected Cooldown cooldown;
    public Explosion explosionEntity = null;
    public BackgroundExplosionPattern() {
        super(new Point(0,0));
        this.cooldown = new Cooldown(3000);
        this.cooldown.reset();
    }

    public void update() {
        if (cooldown.checkFinished()) {
            spawnExplosion();
            cooldown.reset(); // 쿨타임 초기화
        }else if (explosionEntity != null) {
            if(explosionEntity.isDestroyed()){
                explosionEntity = null;
                return;
            }
            explosionEntity.update();
        }
    }

    private void spawnExplosion() {
        int randomX = (int) (Math.random() * (GameConstant.SCREEN_WIDTH - 150));
        int randomY = GameConstant.STAT_SEPARATION_LINE_HEIGHT + (int) (Math.random() * (GameConstant.ITEMS_SEPARATION_LINE_HEIGHT - GameConstant.STAT_SEPARATION_LINE_HEIGHT - 200));
        explosionEntity = new Explosion(randomX, randomY);
    }

    @Override
    public void attack() {
    }

    @Override
    public void move() {
    }

    public Explosion getboom(){
        return explosionEntity;
    }
}