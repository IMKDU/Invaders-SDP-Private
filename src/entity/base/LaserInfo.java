package entity.base;

import java.awt.*;

public interface LaserInfo extends HasBounds {

	Point getStartPosition();
	Point getEndPosition();
	Color getColor();
	boolean isActive();

}
