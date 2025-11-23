package engine.renderer;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Map;

import engine.BackBuffer;
import engine.Core;
import entity.Entity;
import engine.DrawManager.SpriteType;
import entity.LaserBullet;
import entity.OmegaBoss;
import entity.pattern.BossPattern;
import entity.pattern.DashPattern;

/**
 * Handles rendering of all game entities using the shared back buffer.
 * Acts as a sub-view in MVC architecture.
 */
public final class EntityRenderer {

    private final Map<SpriteType, BufferedImage> spriteMap;
    private final BackBuffer backBuffer;
    private final double scale;

    public EntityRenderer(Map<SpriteType, BufferedImage> spriteMap, BackBuffer backBuffer, double scale) {
        this.spriteMap = spriteMap;
        this.backBuffer = backBuffer;
        this.scale = scale;
    }

    /** Draws a single entity on the back buffer. */
    public void drawEntity(final Entity entity, final int positionX, final int positionY) {
        Graphics2D g2d = (Graphics2D) backBuffer.getGraphics();
        BufferedImage img = spriteMap.get(entity.getSpriteType());
        if (img == null) {
            return;
        }
        int originalW = img.getWidth();
        int originalH = img.getHeight();
        int scaledW = (int) (originalW * scale * 2);
        int scaledH = (int) (originalH * scale * 2);

        if (entity.getSpriteType() == SpriteType.SoundOn ||
                entity.getSpriteType() == SpriteType.SoundOff) {

            img = tintImage(img, entity.getColor());
        }
        int drawX = positionX; // 가운데 정렬: int drawX = positionX- scaledW / 2
        int drawY = positionY; // 가운데 정렬: int drawY = positionY - scaledH / 2
        g2d.drawImage(img, drawX, drawY, scaledW, scaledH, null);
    }

    private BufferedImage tintImage(BufferedImage src, Color color) {
        int w = src.getWidth();
        int h = src.getHeight();

        BufferedImage tinted = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                int argb = src.getRGB(x, y);

                // 알파 0이면 투명 (그리지 않음)
                if ((argb >> 24) == 0) continue;

                // 새 색상 적용 (알파는 유지)
                int alpha = (argb >> 24) & 0xFF;
                int rgb = (color.getRGB() & 0x00FFFFFF) | (alpha << 24);
                tinted.setRGB(x, y, rgb);
            }
        }
        return tinted;
    }
    public void drawShield(int shipX,int shipWidth, int shipY, int shipHeight, double ratio) {
        BufferedImage shield = spriteMap.get(SpriteType.Shield);
        if (shield == null) return;
        int sw = shield.getWidth();
        int sh = shield.getHeight();

        int scaledW = (int) (sw * scale * 2);
        int scaledH = (int) (sh * scale * 2);

        int centerX = shipX + shipWidth / 2;
        int centerY = shipY + shipHeight / 2;

        int drawX = centerX - scaledW / 2;
        int drawY = centerY - scaledH / 2;

        int alpha = (int) (255 * ratio);
        if (alpha < 30) alpha = 30;
        BufferedImage tinted = tintAlpha(shield, alpha);

        Graphics2D g2d = (Graphics2D) backBuffer.getGraphics();
        g2d.drawImage(tinted, drawX, drawY, scaledW, scaledH, null);
    }

    private BufferedImage tintAlpha(BufferedImage src, int alpha) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                int argb = src.getRGB(x, y);

                int originalAlpha = (argb >> 24) & 0xFF;
                if (originalAlpha == 0) continue; // 완전 투명

                int newAlpha = Math.min(alpha, originalAlpha);

                int rgb = (argb & 0x00FFFFFF) | (newAlpha << 24);

                result.setRGB(x, y, rgb);
            }
        }

        return result;
    }

	public void drawEntity(final Entity entity) {
		if (entity instanceof LaserBullet) {
			LaserBullet laser = (LaserBullet) entity;
			drawLaserRotated(laser, laser.getTargetPosition().x, laser.getTargetPosition().y);
		}
		else if (entity instanceof OmegaBoss) {
			OmegaBoss omegaBoss = (OmegaBoss) entity;
			drawOmegaBoss(omegaBoss);
		}
		else {
			drawEntity(entity, entity.getPositionX(), entity.getPositionY());
		}
	}

	/**
	 * Draws OmegaBoss entity with pattern-specific visualizations.
	 */
	private void drawOmegaBoss(OmegaBoss omegaBoss) {
		// Draw boss sprite
		drawEntity(omegaBoss, omegaBoss.getPositionX(), omegaBoss.getPositionY());

		// Draw pattern-specific visuals
		BossPattern currentPattern = omegaBoss.getBossPattern();
		if (currentPattern != null) {
			drawBossPattern(omegaBoss, currentPattern);
		}
	}

	/**
	 * Draws pattern-specific visualizations based on pattern type.
	 */
	private void drawBossPattern(OmegaBoss boss, BossPattern pattern) {
		if (pattern instanceof DashPattern) {
			drawDashPatternVisual(boss, (DashPattern) pattern);
		}
		// Add more pattern types here as needed
		// else if (pattern instanceof ex1Pattern) {
		//     drawLaserPatternVisual(boss, (ex1Pattern) pattern);
		// }
		// else if (pattern instanceof ex2Pattern) {
		//     drawCirclePatternVisual(boss, (ex2Pattern) pattern);
		// }
	}

	/**
	 * Draws visualization for DashPattern.
	 */
	private void drawDashPatternVisual(OmegaBoss boss, DashPattern dashPattern) {
		if (!dashPattern.isShowingPath()) {
			return;
		}

		int[] targetPoint = boss.getDashEndPoint();

		// Calculate boss center point
		int bossWidth = boss.getWidth();
		int bossHeight = boss.getHeight();
		int startX = boss.getPositionX() + bossWidth / 2;
		int startY = boss.getPositionY() + bossHeight / 2;
		int targetX = targetPoint[0];
		int targetY = targetPoint[1];

		// Calculate direction vector and extend to long endpoint
		double dx = targetX - startX;
		double dy = targetY - startY;
		double len = Math.sqrt(dx * dx + dy * dy);

		if (len > 0) {
			dx /= len;
			dy /= len;

			// Extend to sufficient distance (beyond screen)
			double extendDistance = 2000.0;
			int endX = (int) Math.round(startX + dx * extendDistance);
			int endY = (int) Math.round(startY + dy * extendDistance);

			// Draw path
			drawDashPath(startX, startY, endX, endY);
		}
	}

	// Template for future pattern visualizations

	/**
	 * Draws visualization for LaserPattern (example).
	 */
	// private void drawEx1PatternVisual(OmegaBoss boss, LaserPattern laserPattern) {
	//     // Implement Ex1 pattern visualization here
	// }

	/**
	 * Draws visualization for CirclePattern (example).
	 */
	// private void drawEx2PatternVisual(OmegaBoss boss, CirclePattern circlePattern) {
	//     // Implement Ex2 pattern visualization here
	// }

	/**
	 * Draws visualization for SpiralPattern (example).
	 */
	// private void drawEx3PatternVisual(OmegaBoss boss, SpiralPattern spiralPattern) {
	//     // Implement Ex3 pattern visualization here
	// }

	public void drawLaserRotated(Entity entity, int posX, int posY) {
		Graphics g = backBuffer.getGraphics();
		g.setColor(entity.getColor());
		int x1 = posX;
		int y1 = posY;
		int x2 = entity.getPositionX();
		int y2 = entity.getPositionY();

		double dx = x2 - x1;
		double dy = y2 - y1;
		double len = Math.sqrt(dx * dx + dy * dy);
		if (len == 0) {
			return;
		}

		dx /= len;
		dy /= len;

		double big = 2000.0;

		int sx = (int) Math.round(x1 - dx * big);
		int sy = (int) Math.round(y1 - dy * big);
		int ex = (int) Math.round(x1 + dx * big);
		int ey = (int) Math.round(y1 + dy * big);
        if (entity.getColor() == Color.GREEN){
            g.drawLine(sx, sy, ex, ey);
        }
        else {
            BufferedImage laserImg = spriteMap.get(SpriteType.Laser);
            Graphics2D g2 = (Graphics2D) g;
            double angle = Math.atan2(ey - sy, ex - sx);
            double length = Math.hypot(ex - sx, ey - sy);
            double scaleX = length / laserImg.getWidth() * scale;
            double scaleY = 6.0 * scale; // 굵기 유지
            AffineTransform at = new AffineTransform();
            at.translate(sx, sy);
            at.rotate(angle);
            at.scale(scaleX, scaleY);

            g2.drawImage(laserImg, at, null);
        }

	}
    public void drawLife(final int positionX, final int positionY, final int playerId){
        Graphics2D g2d = (Graphics2D) backBuffer.getGraphics();
        BufferedImage image = spriteMap.get(SpriteType.Life);
        if (playerId == 2){
            image = this.tint(image, new Color(20, 74, 246, 180));
        }
        int scaledW = (int) (image.getWidth() * scale * 2);
        int scaledH = (int) (image.getHeight() * scale * 2);
        g2d.drawImage(image, positionX, positionY, scaledW, scaledH, null);
    }


	/**
	 * Draws boss dash path with dashed line style.
	 */
	private void drawDashPath(int startX, int startY, int endX, int endY) {
		Graphics g = backBuffer.getGraphics();
		Graphics2D g2d = (Graphics2D) g;

		Color originalColor = g2d.getColor();
		java.awt.Stroke originalStroke = g2d.getStroke();

		g2d.setColor(new Color(255, 0, 0, 180));

		float[] dashPattern = {10.0f, 5.0f};
		g2d.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT,
				BasicStroke.JOIN_MITER, 10.0f,
				dashPattern, 0.0f));

		g2d.drawLine(startX, startY, endX, endY);
		drawArrowHead(g2d, startX, startY, endX, endY);

		g2d.setColor(originalColor);
		g2d.setStroke(originalStroke);
	}

	/**
	 * Draws an arrow head at the end of the path.
	 */
	private void drawArrowHead(Graphics2D g2d, int startX, int startY, int endX, int endY) {
		double angle = Math.atan2(endY - startY, endX - startX);
		int arrowSize = 12;

		int x1 = (int) (endX - arrowSize * Math.cos(angle - Math.PI / 6));
		int y1 = (int) (endY - arrowSize * Math.sin(angle - Math.PI / 6));
		int x2 = (int) (endX - arrowSize * Math.cos(angle + Math.PI / 6));
		int y2 = (int) (endY - arrowSize * Math.sin(angle + Math.PI / 6));

		g2d.drawLine(endX, endY, x1, y1);
		g2d.drawLine(endX, endY, x2, y2);
	}

    public BufferedImage tint(BufferedImage src, Color newColor) {
        int width = src.getWidth();
        int height = src.getHeight();

        BufferedImage tinted = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int rgba = src.getRGB(x, y);
                int alpha = (rgba >> 24) & 0xff;

                if (alpha == 0) continue; // 투명 픽셀은 건너뜀

                tinted.setRGB(x, y, newColor.getRGB());
            }
        }

        return tinted;
    }
}