package entity;

import audio.SoundManager;
import engine.Core;

import java.awt.*;

public class Explosion extends Entity {
    private int timer = 0;
    private final int warningTime = 120;
    private final int explodeTime = 30;
    private boolean isDestroyed = false;
    private boolean isBoom = false;

    public Explosion(int x, int y) {
        super(x, y, 150, 150, Color.RED);
    }

    public void update() {
        timer++;
        if (timer > warningTime && timer <= warningTime + explodeTime) {
            isBoom= true;
        }
        else if (timer > warningTime + explodeTime) {
            isBoom = false;
            SoundManager.stop("sfx/impact.wav");
            SoundManager.play("sfx/impact.wav");
            this.destroy();
        }
    }

    public final void destroy() {
        isDestroyed = true;
    }

    @Override
    public void onCollision(Collidable other, GameModel gameModel){
        if(!this.isBoom){
            return;
        }
        if( other instanceof Ship){
            gameModel.requestShipDamage((Ship) other,1);
        }
    }
    public boolean isDestroyed() {
        return isDestroyed;
    }
    public double getWarningProgress(){
        double progress = (double) timer / (double) warningTime;
        if (progress > 1) return 1.0;
        return progress;
    }

    public boolean isBoom() { return isBoom; }
}