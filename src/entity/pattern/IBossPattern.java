package entity.pattern;

import engine.Cooldown;
import entity.Bullet;
import entity.HasBounds;

import java.awt.*;
import java.util.Set;

public interface IBossPattern {
	void attack();
	void move();
	Point getBossPosition();
	Set<Bullet> getBullets();
	void setTarget(HasBounds target);
	void setCooldown(Cooldown cooldown);
	void validateBackgroundPattern(boolean condition);
}
