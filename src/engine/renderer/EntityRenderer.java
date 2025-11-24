package engine.renderer;

import java.awt.Graphics;
import java.awt.Color;
import java.awt.*;
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

	private final Map<SpriteType, boolean[][]> spriteMap;
	private final BackBuffer backBuffer;
	private final double scale;
    private static final Color BLACK_HOLE_COLOR = new Color(200, 0, 255);
	public EntityRenderer(Map<SpriteType, boolean[][]> spriteMap, BackBuffer backBuffer, double scale) {
		this.spriteMap = spriteMap;
		this.backBuffer = backBuffer;
		this.scale = scale;
	}

	/** Draws a single entity on the back buffer. */
	public void drawEntity(final Entity entity, final int positionX, final int positionY) {
		boolean[][] image = spriteMap.get(entity.getSpriteType());
		Graphics g = backBuffer.getGraphics();
		g.setColor(entity.getColor());

		for (int i = 0; i < image.length; i++) {
			for (int j = 0; j < image[i].length; j++) {
				if (image[i][j]) {
					int pixelSize = (int) Math.max(1, 2 * scale);
					int scaledX = positionX + (int)(i * pixelSize);
					int scaledY = positionY + (int)(j * pixelSize);
					g.fillRect(scaledX, scaledY, pixelSize, pixelSize);
				}
			}
		}
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

		g.drawLine(sx, sy, ex, ey);
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

    /** Draw circle for pull_attack pattern */
    public void drawBlackHole(final int cx, final int cy, final int radius){
        int x = cx - radius;
        int y = cy - radius;

        Graphics g = backBuffer.getGraphics();
        g.setColor(BLACK_HOLE_COLOR);
        g.drawOval(x, y, radius * 2, radius * 2);

    }

	/**
	 * Draws a charging progress bar above the ship.
	 * @param x X position (ship position)
	 * @param y Y position (above the ship)
	 * @param width Bar width (ship width)
	 * @param progress Charge progress (0.0 to 1.0)
	 */
	public void drawChargingBar(int x, int y, int width, double progress) {
		Graphics g = backBuffer.getGraphics();
		Graphics2D g2d = (Graphics2D) g;

		int barHeight = 4;
		int barWidth = width;
		int filledWidth = (int) (barWidth * progress);

		// Draw background (empty portion)
		g2d.setColor(new Color(50, 50, 50, 180));
		g2d.fillRect(x, y, barWidth, barHeight);

		// Draw filled portion with color gradient based on progress
		Color chargeColor;
		if (progress < 0.33) {
			// Red to Yellow (0-33%)
			chargeColor = new Color(255, (int)(255 * progress * 3), 0, 200);
		} else if (progress < 0.66) {
			// Yellow to Green (33-66%)
			chargeColor = new Color((int)(255 * (1 - (progress - 0.33) * 3)), 255, 0, 200);
		} else {
			// Green to Cyan (66-100%)
			chargeColor = new Color(0, 255, (int)(255 * (progress - 0.66) * 3), 200);
		}

		g2d.setColor(chargeColor);
		g2d.fillRect(x, y, filledWidth, barHeight);

		// Draw border
		g2d.setColor(Color.WHITE);
		g2d.drawRect(x, y, barWidth, barHeight);

		// Draw charging text
		if (progress >= 1.0) {
			g2d.setColor(new Color(0, 255, 255));
			g2d.setFont(new Font("Monospaced", Font.BOLD, 12));
			String text = "READY!";
			FontMetrics fm = g2d.getFontMetrics();
			int textWidth = fm.stringWidth(text);
			g2d.drawString(text, x + (barWidth - textWidth) / 2, y - 2);
		}
	}

	/**
	 * Draws the charging laser beam from the ship upward.
	 * @param centerX Center X position of the ship
	 * @param shipY Y position of the ship
	 * @param beamWidth Width of the laser beam (ship width)
	 * @param screenHeight Screen height for extending beam upward
	 */
	public void drawChargingLaser(int centerX, int shipY, int beamWidth, int screenHeight) {
		Graphics g = backBuffer.getGraphics();
		Graphics2D g2d = (Graphics2D) g;

		// Enable anti-aliasing for smoother laser
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// Laser beam properties
		int beamX = centerX - beamWidth / 2;
		int beamY = 0; // Extend to top of screen
		int beamHeight = shipY;

		// Draw outer glow (wider, more transparent)
		g2d.setColor(new Color(0, 255, 255, 30));
		g2d.fillRect(beamX - beamWidth / 2, beamY, beamWidth * 2, beamHeight);

		// Draw middle layer (medium transparency)
		g2d.setColor(new Color(0, 255, 255, 80));
		g2d.fillRect(beamX - beamWidth / 4, beamY, beamWidth + beamWidth / 2, beamHeight);

		// Draw core beam (solid, bright)
		g2d.setColor(new Color(0, 255, 255, 200));
		g2d.fillRect(beamX, beamY, beamWidth, beamHeight);

		// Draw center line (pure white for intensity)
		g2d.setColor(new Color(255, 255, 255, 255));
		int centerLineWidth = Math.max(2, beamWidth / 4);
		g2d.fillRect(centerX - centerLineWidth / 2, beamY, centerLineWidth, beamHeight);

		// Draw animated particles/sparkles along the beam
		drawLaserParticles(g2d, centerX, beamY, beamHeight, beamWidth);

		// Reset rendering hints
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
	}

	/**
	 * Draws animated particles along the laser beam for visual effect.
	 */
	private void drawLaserParticles(Graphics2D g2d, int centerX, int beamY, int beamHeight, int beamWidth) {
		// Create random-looking particles based on time
		long time = System.currentTimeMillis();
		int particleCount = 15;

		for (int i = 0; i < particleCount; i++) {
			// Use time and index to create pseudo-random but consistent positions
			long seed = time / 100 + i * 137; // 137 is a prime number for better distribution
			int particleY = beamY + (int)((seed % beamHeight));
			int particleX = centerX + (int)((seed * 17 % (beamWidth * 2)) - beamWidth);
			int particleSize = 2 + (int)(seed % 3);

			// Fade in and out based on position
			int alpha = (int)(150 * Math.sin((double)particleY / beamHeight * Math.PI));
			g2d.setColor(new Color(255, 255, 255, Math.max(50, alpha)));
			g2d.fillOval(particleX, particleY, particleSize, particleSize);
		}
	}
}