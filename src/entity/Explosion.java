package entity;

import audio.SoundManager;

import java.awt.*;

public class Explosion extends Entity {
    private int timer = 0;
    private final int warningTime = 120;
    private final int explodeTime = 30;
    private boolean isDestroyed = false;
    private boolean isBoom = false;
    private static final int DIAMETER = 150;

    /**
     * Initializes the explosion at the specified coordinates.
     * */
    public Explosion(int x, int y) {
        super(x, y, DIAMETER, DIAMETER, Color.RED);
    }

    /**
     * Updates the timer and manages the explosion's lifecycle (Warning -> Boom -> Destroy).
     * */
    public void update() {
        timer++;
        if (timer > warningTime && timer <= warningTime + explodeTime) {
            isBoom= true;
        }
        else if (timer > warningTime + explodeTime) {
            isBoom = false;
            SoundManager.play("sfx/impact.wav");
            this.destroy();
        }
    }

    /**
     * Marks the entity to be removed from the game.
     * */
    public final void destroy() {
        isDestroyed = true;
    }

    /**
     * Checks for collision with the Ship using Circle-AABB logic and applies damage.
     * */
    @Override
    public void onCollision(Collidable other, GameModel gameModel){
        if(!this.isBoom){
            return;
        }
        if( other instanceof Ship ship){
            double radius = this.width / 2.0;
            double centerX = this.getPositionX() + radius;
            double centerY = this.getPositionY() + radius;
            double rectX = ship.getPositionX();
            double rectY = ship.getPositionY();
            double rectWidth = ship.getWidth();
            double rectHeight = ship.getHeight();
            double closestX = Math.max(rectX, Math.min(centerX, rectX + rectWidth));
            double closestY = Math.max(rectY, Math.min(centerY, rectY + rectHeight));
            double distanceX = closestX - centerX;
            double distanceY = closestY - centerY;
            double distanceSquared = distanceX * distanceX + distanceY * distanceY;
            if( distanceSquared < radius * radius ){ gameModel.requestShipDamage(ship,1);}
        }
    }

    /**
     * Returns whether the entity is destroyed.
     * */
    public boolean shouldBeRemoved() {
        return isDestroyed;
    }

    /**
     * Calculates the progress of the warning phase (0.0 to 1.0) for animation.
     * */
    public double getWarningProgress(){
        double progress = (double) timer / (double) warningTime;
        if (progress > 1) return 1.0;
        return progress;
    }

    /**
     * Returns true if the explosion is currently active and dealing damage.
     * */
    public boolean isBoom() { return isBoom; }
}