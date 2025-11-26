package engine.renderer;

import java.awt.Graphics;
import java.awt.Color;
import java.awt.*;
import java.util.Map;

import engine.BackBuffer;
import entity.Entity;
import engine.DrawManager.SpriteType;
import entity.GameConstant;
import entity.LaserBullet;
import entity.OmegaBoss;
import entity.ZetaBoss;
import entity.MidBoss;
import entity.pattern.ApocalypseAttackPattern;
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
        else if (entity instanceof ZetaBoss) {
           ZetaBoss zetaBoss = (ZetaBoss) entity;
           drawZetaBoss(zetaBoss);
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

    private void drawZetaBoss(ZetaBoss zetaBoss) {
        // 1. 보스 본체 그리기
        drawEntity(zetaBoss, zetaBoss.getPositionX(), zetaBoss.getPositionY());

        // 2. 패턴 이펙트 그리기
        BossPattern currentPattern = zetaBoss.getBossPattern();
        if (currentPattern != null) {
            drawBossPattern(zetaBoss, currentPattern);
        }
    }

	/**
	 * Draws pattern-specific visualizations based on pattern type.
	 */
	private void drawBossPattern(MidBoss boss, BossPattern pattern) {
		if (pattern instanceof DashPattern) {
			drawDashPatternVisual(boss, (DashPattern) pattern);
		}

        else if (pattern instanceof ApocalypseAttackPattern) {
            drawApocalypseVisual((ApocalypseAttackPattern) pattern);
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
     * [Added] Apocalypse Pattern Visualization Method
     * (Ported logic from existing GameView/UIRenderer)
     */
    private void drawApocalypseVisual(ApocalypseAttackPattern pattern) {
        Graphics g = backBuffer.getGraphics();
        int screenWidth = GameConstant.SCREEN_WIDTH;
        int screenHeight = GameConstant.SCREEN_HEIGHT;
        int safeZoneColumn = pattern.getSafeZoneColumn();
        int totalColumns = ApocalypseAttackPattern.TOTAL_COLUMNS;
        int columnWidth = screenWidth / totalColumns;

        if (pattern.isWarningActive()) {
            // Draw warning screen (Red translucent)
            Color attackColor = new Color(255, 0, 0, 100);
            Color safeColor = new Color(255, 255, 255, 100);

            for (int i = 0; i < totalColumns; i++) {
                if (i == safeZoneColumn) {
                    g.setColor(safeColor);
                } else {
                    g.setColor(attackColor);
                }
                g.fillRect(i * columnWidth, 0, columnWidth, screenHeight);
            }
        }
        else if (pattern.isAttacking()) {
            // Draw attack animation (Dark red)
            float progress = pattern.getAttackAnimationProgress();
            int currentAttackHeight = (int) (screenHeight * progress);
            Color attackColor = new Color(255, 0, 0, 200);

            g.setColor(attackColor);
            for (int i = 0; i < totalColumns; i++) {
                if (i != safeZoneColumn) {
                    g.fillRect(i * columnWidth, 0, columnWidth, currentAttackHeight);
                }
            }
        }
    }

	/**
	 * Draws visualization for DashPattern.
	 */
	private void drawDashPatternVisual(MidBoss boss, DashPattern dashPattern) {
		if (!dashPattern.isShowingPath()) {
			return;
		}

        int[] targetPoint;
        if (boss instanceof OmegaBoss) {
            targetPoint = ((OmegaBoss) boss).getDashEndPoint();
        } else if (boss instanceof ZetaBoss) {
            targetPoint = ((ZetaBoss) boss).getDashEndPoint();
        } else {
            return;
        }

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
}