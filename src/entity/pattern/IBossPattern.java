package entity.pattern;

import engine.Cooldown;
import entity.HasBounds;

import java.awt.*;

public interface IBossPattern {
	void attack();
	void move();
	Point getBossPosition();
	void setTarget(HasBounds target);
	void setCooldown(Cooldown cooldown);
}
