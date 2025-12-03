package entity;

import engine.Cooldown;
import engine.DrawManager;
import entity.GameModel;

import java.awt.*;

public class GuidedBullet extends BossBullet {

    private Ship target;
    private double preciseX;
    private double preciseY;
    private double speedValue;
    private Cooldown animationCooldown;
    private double angle;

    /**
     * Constructor for GuidedBullet
     * @param x Initial X position
     * @param y Initial Y position
     * @param target Target to track (player ship)
     */
    public GuidedBullet(int x, int y, Ship target) {
        // dx, dy are calculated every frame, so initial values are 0. Size is 8x8, color is RED.
        super(x, y, 0, 0, 30, 30, "OmegaBoss");
        this.animationCooldown = new Cooldown(200);
        this.setColor(Color.RED);
        this.target = target;
        this.preciseX = x;
        this.preciseY = y;
        this.speedValue = 2.0; // Speed of guided missile
        this.spriteType = DrawManager.SpriteType.GuidedBullet1; // Use enemy bullet sprite (or change to desired shape)
    }

    public Ship getTarget() {
        return this.target;
    }

    public void setTarget(Ship target) {
        this.target = target;
    }

    @Override
    public void update() {
        chooseSprite();

        if (target != null && !target.isDestroyed()) {
            // Calculate target direction
            double targetX = target.getPositionX() + target.getWidth() / 2.0;
            double targetY = target.getPositionY() + target.getHeight() / 2.0;

            double dx = targetX - this.preciseX;
            double dy = targetY - this.preciseY;

            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance != 0) {
                // Normalize direction and apply speed
                double moveX = (dx / distance) * speedValue;
                double moveY = (dy / distance) * speedValue;
                double targetAngle = Math.atan2(dy, dx);
                this.angle += (targetAngle - this.angle) * 0.15;
                this.preciseX += moveX;
                this.preciseY += moveY;
            }
        } else {
            // Move straight down if no target
            this.preciseY += speedValue;
        }

        // Update actual integer coordinates
        this.positionX = (int) this.preciseX;
        this.positionY = (int) this.preciseY;



    }
    private void chooseSprite() {
        if (this.animationCooldown.checkFinished()) {
            this.animationCooldown.reset();
            if (this.spriteType == DrawManager.SpriteType.GuidedBullet1){
                this.spriteType = DrawManager.SpriteType.GuidedBullet2;
            }
            else {
                this.spriteType = DrawManager.SpriteType.GuidedBullet1;
            }
        }
    }
    public double getAngle() {
        return angle;
    }

    @Override
    public void onCollision(Collidable other, GameModel model) {
        // Logic for cancellation when colliding with player's bullet
        if (other instanceof Bullet) {
            Bullet otherBullet = (Bullet) other;
            // Check if it's a player bullet (speed is negative or owner is player)
            if (otherBullet.getSpeed() < 0 || otherBullet.getOwnerId() != null) {
                model.requestRemoveBossBullet(this); // Remove guided bullet
                model.requestRemoveBullet(otherBullet); // Remove player bullet
                return;
            }
        }
        // Other collisions (player ship hit, etc.) follow parent class logic
        super.onCollision(other, model);
    }
}