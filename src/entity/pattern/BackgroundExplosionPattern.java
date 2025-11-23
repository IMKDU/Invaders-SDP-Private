package entity.pattern;

import engine.Cooldown;
import entity.Explosion;
import entity.GameConstant;
import java.util.List;
import java.awt.*;

public class BackgroundExplosionPattern extends BossPattern {

    // 쿨타임 관리 (10초마다 폭탄 생성)
    protected Cooldown cooldown;
    public Explosion explosionEntity;
    public BackgroundExplosionPattern() {
        super(new Point(0,0));
        this.cooldown = new Cooldown(600);
        this.cooldown.reset();
    }

    public void update() {
        if (cooldown.checkFinished()) {
            spawnExplosion();
            cooldown.reset(); // 쿨타임 초기화
        }
    }

    private void spawnExplosion() {
        int randomX = (int) (Math.random() * (GameConstant.SCREEN_WIDTH - 100));
        int randomY = (int) (Math.random() * (GameConstant.ITEMS_SEPARATION_LINE_HEIGHT- 100));
        explosionEntity = new Explosion(randomX, randomY);
    }

    @Override
    public void attack() {
    }

    @Override
    public void move() {
    }

    public boolean isBoomActive() {
        if(explosionEntity == null) {
            return false;
        }else {
            return true;
        }
    }
    public void clearBoom(){
        explosionEntity = null;
    }

    public boolean isBoomDestroyed() {
        return explosionEntity.isDestroyed();
    }

    public Explosion getboom(){
        return explosionEntity;
    }
}