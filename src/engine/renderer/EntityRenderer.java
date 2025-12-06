package engine.renderer;

import java.awt.Graphics;
import java.awt.Color;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Map;

import audio.SoundManager;
import engine.*;
import entity.*;
import engine.DrawManager.SpriteType;
import entity.pattern.ApocalypseAttackPattern;
import entity.pattern.BossPattern;
import entity.pattern.DashPattern;
import screen.HealthBar;

/**
 * Handles rendering of all game entities using the shared back buffer.
 * Acts as a sub-view in MVC architecture.
 */
public final class EntityRenderer {
    private final Map<SpriteType, BufferedImage> spriteMap;
    private final BackBuffer backBuffer;
    private final double scale;
    private final Cooldown blackholeAnimationCooldown = new Cooldown(100);
    private final Cooldown TeleportCooldownP1 = new Cooldown(600);
    private final Cooldown TeleportCooldownP2 = new Cooldown(600);
    private final Cooldown BombImageCooldown = new Cooldown(200);
    private SpriteType blackHoleType = SpriteType.BlackHole1;
    private final Cooldown frameCooldown;
    private BufferedImage[] apo;
    private int apoFrameIndex = 0;
    private static final double RED_YELLOW_THRESHOLD = 1.0 / 3.0;
    private static final double YELLOW_GREEN_THRESHOLD = 2.0 / 3.0;
    private int EXPLOSION_SIZE_CHANGE = 10;
    private boolean bombToggle;
    private static final int SPARKLE_COUNT = 10;

    private double[] sparkleX = new double[SPARKLE_COUNT];
    private double[] sparkleY = new double[SPARKLE_COUNT];
    private double[] sparkleVX = new double[SPARKLE_COUNT];
    private double[] sparkleVY = new double[SPARKLE_COUNT];

    private boolean sparkleInitialized = false;
    private Color[] colorPalette = {
            new Color( 0xFF4081),
            new Color( 0xFCDD8A),
            new Color( 0xFF5722),
            new Color( 0x8BC34A),
            new Color( 0x9C27B0),
            new Color( 0x6A89FF),
            new Color( 0x6756C9),
            new Color( 0xF2606F),
            new Color( 0xF5A5A5),
            new Color( 0x6F5E77),
            new Color( 0x32A9B3),
            new Color( 0x8303EE)
    };
    private HealthBar bossHealthBar;

    public EntityRenderer(Map<SpriteType, BufferedImage> spriteMap, BackBuffer backBuffer, double scale ,AnimationLoader loader) {
        this.spriteMap = spriteMap;
        this.backBuffer = backBuffer;
        this.scale = scale;
        this.frameCooldown = new Cooldown(70);
        this.apo = loader.load("res/images/apo1");
    }

    /** Draws a single entity on the back buffer. */
    public void drawEntity(final Entity entity, final int positionX, final int positionY) {
        Graphics2D g2d = (Graphics2D) backBuffer.getGraphics();
        BufferedImage img = spriteMap.get(entity.getSpriteType());
        if (img == null) {
            return;
        }
        boolean isSubShip = (entity instanceof entity.SubShip);
        double currentScale = isSubShip ? this.scale * 0.5 : this.scale;

        int originalW = img.getWidth();
        int originalH = img.getHeight();
        int scaledW = (int) (originalW * currentScale * 2);
        int scaledH = (int) (originalH * currentScale * 2);

        if (entity.getSpriteType() == SpriteType.SoundOn ||
                entity.getSpriteType() == SpriteType.SoundOff) {

            img = tintImage(img, entity.getColor());
        }
        int drawX = positionX;
        int drawY = positionY;
        g2d.drawImage(img, drawX, drawY, scaledW, scaledH, null);
    }

    private BufferedImage tintImage(BufferedImage src, Color color) {
        int w = src.getWidth();
        int h = src.getHeight();

        BufferedImage tinted = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                int argb = src.getRGB(x, y);


                if ((argb >> 24) == 0) continue;


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
                if (originalAlpha == 0) continue;

                int newAlpha = Math.min(alpha, originalAlpha);

                int rgb = (argb & 0x00FFFFFF) | (newAlpha << 24);

                result.setRGB(x, y, rgb);
            }
        }

        return result;
    }
    public void drawHealthBarWithHP(final BossEntity boss) {
        if(bossHealthBar==null) {
            bossHealthBar = new HealthBar(boss.getHealPoint());
        }
        else {
            Graphics2D g2 = (Graphics2D) backBuffer.getGraphics();
            int height = GameConstant.STAT_SEPARATION_LINE_HEIGHT;
            int barWidth = GameConstant.SCREEN_WIDTH * 3 / 10;
            int barHeight = height / 2;
            int barY = (height - barHeight) / 2;
            int barX = GameConstant.SCREEN_WIDTH * 4 / 10;
            bossHealthBar.setCurrent_HP(boss.getHealPoint());
            float ratio_hp = bossHealthBar.getRatio_HP();

            int greenWidth = (int) (barWidth * ratio_hp);
            int redWidth = barWidth - greenWidth;

            Stroke oldStroke = g2.getStroke();
            g2.setStroke(new BasicStroke(8));
            g2.setColor(Color.LIGHT_GRAY);
            g2.drawRect(barX, barY, barWidth, barHeight);
            g2.setStroke(oldStroke);

            g2.setColor(Color.GREEN);
            g2.fillRect(barX + 1, barY + 1, greenWidth - 1, barHeight - 1);

            if (redWidth > 0 && redWidth < barWidth) {
                g2.setColor(Color.RED);
                g2.fillRect(barX + greenWidth, barY + 1, redWidth - 1, barHeight - 1);
            }
        }
        if(boss.isDestroyed()) {
            bossHealthBar = null;
        }

    }

	public void drawEntity(final Entity entity) {
		if (entity instanceof ZetaBoss) {
           ZetaBoss zetaBoss = (ZetaBoss) entity;
           drawZetaBoss(zetaBoss);
        }
		else if (entity instanceof FinalBoss finalBoss){
			drawFinalBoss(finalBoss);
		}
		else if (entity instanceof MidBossMob) {
			MidBossMob midBossMob = (MidBossMob) entity;
			drawMidBossMob(midBossMob);
		}
        else if (entity instanceof GuidedBullet gb) {
            drawGuidedRotated(gb);
        }
		drawEntity(entity, entity.getPositionX(), entity.getPositionY());
	}

    private void drawZetaBoss(ZetaBoss zetaBoss) {

        // 2. Draw pattern effects
        if (zetaBoss.getBossPattern() != null) {
            BossPattern activePattern = zetaBoss.getBossPattern().getActivePattern();
            if (activePattern != null) {
                drawBossPattern(zetaBoss, activePattern);
            }
        }
    }

	private void drawFinalBoss(FinalBoss finalBoss) {

		// 1. Draw pattern effects
		BossPattern apocalypsePattern = finalBoss.getApocalypsePattern();
		BossPattern dashPattern = finalBoss.getDashPattern();
		if (apocalypsePattern != null) {
			drawBossPattern(finalBoss, apocalypsePattern);
		}
		if (dashPattern != null) {
			drawBossPattern(finalBoss, dashPattern);
		}
	}

    /**
     * Draws GammaBoss entity with pattern-specific visualizations.
     */
    public void drawGammaBoss(GammaBoss gammaBoss) {
        // Check if showing dash path and draw it
        if (gammaBoss.isShowingPath()) {
            int[] targetPoint = gammaBoss.getDashEndPoint();
            int bossWidth = gammaBoss.getWidth();
            int bossHeight = gammaBoss.getHeight();
            int startX = gammaBoss.getPositionX() + bossWidth / 2;
            int startY = gammaBoss.getPositionY() + bossHeight / 2;
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

        drawEntity(gammaBoss, gammaBoss.getPositionX(), gammaBoss.getPositionY());
    }

    private void drawMidBossMob(MidBossMob midBossMob) {
        int colorID = midBossMob.getColorID();
        midBossMob.setColor(colorPalette[colorID%colorPalette.length]);
        drawEntity(midBossMob, midBossMob.getPositionX(), midBossMob.getPositionY());
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
            if (frameCooldown.checkFinished()) {
                frameCooldown.reset();
                apoFrameIndex = (apoFrameIndex + 1) % apo.length;
            }

            BufferedImage frame = apo[apoFrameIndex];
            if (frame == null) return;
            g.drawImage(frame, 0, 0, screenWidth, screenHeight, null);
            g.setColor(new Color(63, 253, 0, 100));
            g.fillRect(safeZoneColumn * columnWidth, 0, columnWidth, screenHeight);

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
		targetPoint = dashPattern.getDashEndPoint(boss.getWidth(), boss.getHeight());


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

	public void drawLaser(LaserInfo laser) {
		Graphics g = backBuffer.getGraphics();
		g.setColor(Color.green);

		int sx = laser.getStartPosition().x;
		int sy = laser.getStartPosition().y;
		int ex = laser.getEndPosition().x;
		int ey = laser.getEndPosition().y;

		double len = Math.sqrt((ex-sx)*(ex-sx) + (ey-sy)*(ey-sy));
		if (len == 0) {
			return;
		}

        if (laser.getColor() == Color.GREEN){
            g.drawLine(sx, sy, ex, ey);
        }
        else {
            BufferedImage laserImg = spriteMap.get(SpriteType.Laser);
            Graphics2D g2 = (Graphics2D) g;
            double angle = Math.atan2(ey - sy, ex - sx);
            double length = Math.hypot(ex - sx, ey - sy);
            double scaleX = length / laserImg.getWidth() * scale;
            double scaleY = 6.0 * scale;
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

                if (alpha == 0) continue;

                tinted.setRGB(x, y, newColor.getRGB());
            }
        }

        return tinted;
    }

    /** Draw circle for pull_attack pattern */
    public void drawBlackHole(HasCircleBounds blackHole) {
		int cx = blackHole.getPositionCX();
		int cy =  blackHole.getPositionCY();
		int size = blackHole.getRadius();
        Graphics2D g2d = (Graphics2D) backBuffer.getGraphics();

        if (this.blackholeAnimationCooldown.checkFinished()) {
            this.blackholeAnimationCooldown.reset();
            blackHoleType = (blackHoleType == SpriteType.BlackHole1)
                    ? SpriteType.BlackHole2
                    : SpriteType.BlackHole1;
        }

        BufferedImage img = spriteMap.get(blackHoleType);
        int drawX = cx - size / 2;
        int drawY = cy - size / 2;


        Shape oldClip = g2d.getClip();


        g2d.setClip(new java.awt.geom.Ellipse2D.Float(drawX, drawY, size, size));

        g2d.drawImage(img, drawX, drawY, size, size, null);
        g2d.setClip(oldClip);
    }

    /**
     * Renders the explosion visual, displaying either an expanding warning circle or the final blast.
     * */
    public void drawExplosion(boolean isBoom, HasBounds boom, double time) {
        Graphics g = backBuffer.getGraphics();
        if (!isBoom) {
            g.setColor(new Color(255,0,0,100));
            g.drawOval(boom.getPositionX(),boom.getPositionY(),boom.getWidth(),boom.getHeight());
            int currentWidth = (int) (boom.getWidth() * time);
            int currentHeight = (int) (boom.getHeight() * time);
            int offsetX = (boom.getWidth() - currentWidth) / 2;
            int offsetY = (boom.getHeight() - currentHeight) / 2;

            g.fillOval(boom.getPositionX() + offsetX, boom.getPositionY() + offsetY, currentWidth, currentHeight);

            BufferedImage bomb1 = spriteMap.get(SpriteType.Bomb1);
            BufferedImage bomb2 = spriteMap.get(SpriteType.Bomb2);
            int sw = bomb1.getWidth();
            int sh = bomb1.getHeight();

            int scaledW = (int) (sw * scale * 2);
            int scaledH = (int) (sh * scale * 2);

            int centerX = boom.getPositionX() + boom.getWidth() / 2;
            int centerY = boom.getPositionY() + boom.getHeight() / 2;

            int drawX = centerX - scaledW / 2;
            int drawY = centerY - scaledH / 2;

            if (this.BombImageCooldown.checkFinished()) {
                this.BombImageCooldown.reset();
                bombToggle = !bombToggle;
            }

            g.drawImage(bombToggle ? bomb1 : bomb2, drawX, drawY, scaledW, scaledH, null);

        }
        else {
            BufferedImage bombexplosion = spriteMap.get(SpriteType.BombExplosion);
            int sw = bombexplosion.getWidth();
            int sh = bombexplosion.getHeight();
            int scaledW = (int) (sw * scale * 2);
            int scaledH = (int) (sh * scale * 2);
            int centerX = boom.getPositionX() + boom.getWidth() / 2;
            int centerY = boom.getPositionY() + boom.getHeight() / 2;
            int drawX = centerX - scaledW / 2;
            int drawY = centerY - scaledH / 2;
            g.drawImage(bombexplosion, drawX, drawY, scaledW, scaledH, null);
        }
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
        if (progress < RED_YELLOW_THRESHOLD) {
            // Red to Yellow
            double phaseProgress = progress / RED_YELLOW_THRESHOLD;
            int green = (int)(255 * phaseProgress);
            chargeColor = new Color(255, green, 0, 200);
        } else if (progress < YELLOW_GREEN_THRESHOLD) {
            // Yellow to Green
            double phaseProgress = (progress - RED_YELLOW_THRESHOLD) / RED_YELLOW_THRESHOLD;
            int red = (int)(255 * (1 - phaseProgress));
            chargeColor = new Color(red, 255, 0, 200);
        } else {
            // Green to Cyan
            double phaseProgress = (progress - YELLOW_GREEN_THRESHOLD) / RED_YELLOW_THRESHOLD;
            int blue = (int)(255 * phaseProgress);
            chargeColor = new Color(0, 255, blue, 200);
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
            if (beamHeight <= 0) return;
            int particleY = beamY + (int)((seed % beamHeight));
            int particleX = centerX + (int)((seed * 17 % (beamWidth * 2)) - beamWidth);
            int particleSize = 2 + (int)(seed % 3);

            // Fade in and out based on position
            int alpha = (int)(150 * Math.sin((double)particleY / beamHeight * Math.PI));
            g2d.setColor(new Color(255, 255, 255, Math.max(50, alpha)));
            g2d.fillOval(particleX, particleY, particleSize, particleSize);
        }
    }
    public void drawTeleport(int positionX, int width, int positionY, int height, boolean isTeleport, int playerId, int afterPositionX, int afterPositionY){
        Cooldown cd = (playerId == 1) ? TeleportCooldownP1 : TeleportCooldownP2;

        if (isTeleport)
            cd.reset();

        if (!cd.checkFinished()) {
            Graphics2D g2d = (Graphics2D) backBuffer.getGraphics();

            BufferedImage hole = spriteMap.get(SpriteType.Teleport);
            int drawW = (int) (hole.getWidth() * scale * 2);
            int drawH = (int) (hole.getHeight() * scale * 2);

            // --- BEFORE position (centered)
            int cx = positionX + width / 2;
            int cy = positionY + height / 2;

            int drawX = cx - drawW / 2;
            int drawY = cy - drawH / 2;

            // --- AFTER position (centered)
            int acx = afterPositionX + width / 2;
            int acy = afterPositionY + height / 2;

            int drawAfterX = acx - drawW / 2;
            int drawAfterY = acy - drawH / 2;

            double fade = cd.getTotal() == 0
                    ? 0.0
                    : (double) cd.getRemaining() / cd.getTotal();   // 1 -> 0

            int alpha = (int)(255 * fade);
            if (alpha < 0) alpha = 0;

            BufferedImage tinted = tintAlpha(hole, alpha);
            g2d.drawImage(tinted, drawX, drawY, drawW, drawH, null);
            drawFlashDust(g2d, cx, cy, drawW, fade);
            drawFlashDust(g2d, acx, acy, drawW, fade);

        }
    }
    private void drawFlashDust(Graphics2D g2d,
                               int cx,
                               int cy,
                               int size,
                               double fade) {

        int spawnRadius = size / 3;
        int riseHeight = size / 2;

        // ----------------------------
        // 초기화 : 중심 기준 offset 생성
        // ----------------------------
        if (!sparkleInitialized) {

            for (int i = 0; i < SPARKLE_COUNT; i++) {

                double ang = Math.random() * Math.PI * 2;
                double dist = Math.random() * spawnRadius;

                sparkleX[i] = Math.cos(ang) * dist;   // 상대좌표
                sparkleY[i] = Math.sin(ang) * dist;

                // 위로 올라가는 속도
                sparkleVX[i] = (Math.random() - 0.5) * 0.25;
                sparkleVY[i] = -(0.3 + Math.random() * 0.5);
            }

            sparkleInitialized = true;
        }

        // ----------------------------
        // 페이드
        // ----------------------------
        int alpha = (int)(200 * fade);
        if (alpha < 0) alpha = 0;

        // ----------------------------
        // 이동 + 리스폰
        // ----------------------------
        for (int i = 0; i < SPARKLE_COUNT; i++) {

            sparkleX[i] += sparkleVX[i];
            sparkleY[i] += sparkleVY[i];

            // 너무 멀어지면 재생성
            if (-sparkleY[i] > riseHeight) {

                double ang = Math.random() * Math.PI * 2;
                double dist = Math.random() * spawnRadius;

                sparkleX[i] = Math.cos(ang) * dist;
                sparkleY[i] = Math.sin(ang) * dist;

                sparkleVX[i] = (Math.random() - 0.5) * 0.25;
                sparkleVY[i] = -(0.3 + Math.random() * 0.5);
            }

            // ----------------------------
            // 실제 위치 계산 (중심 기준)
            // ----------------------------
            int px = cx + (int) sparkleX[i];
            int py = cy + (int) sparkleY[i];

            int s = 2;

            Polygon p = new Polygon(
                    new int[]{ px, px + s, px - s },
                    new int[]{ py - s, py + s, py + s },
                    3
            );

            g2d.setColor(new Color(255, 255, 0, alpha));
            g2d.fillPolygon(p);
        }
    }

    private void drawGuidedRotated(GuidedBullet bullet) {

        Graphics2D g2d = (Graphics2D) backBuffer.getGraphics();
        BufferedImage img = spriteMap.get(bullet.getSpriteType());
        if (img == null) return;

        double scaleValue = scale * 2;

        int drawW = (int) (img.getWidth() * scaleValue);
        int drawH = (int) (img.getHeight() * scaleValue);

        int cx = bullet.getPositionX() + bullet.getWidth() / 2;
        int cy = bullet.getPositionY() + bullet.getHeight() / 2;

        AffineTransform old = g2d.getTransform();

        AffineTransform at = new AffineTransform();

        at.translate(cx, cy);
        at.rotate(bullet.getAngle());
        at.translate(-drawW / 2.0, -drawH / 2.0);
        at.scale(scaleValue, scaleValue);

        g2d.drawImage(img, at, null);

        g2d.setTransform(old);
    }



}