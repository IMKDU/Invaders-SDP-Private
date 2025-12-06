package engine.renderer;

import engine.BackBuffer;
import engine.DrawManager;
import engine.FontPack;
import engine.ItemHUDManager;
import entity.GameConstant;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Map;


/**
 * Handles all on-screen HUD rendering such as scores, coins, and timers.
 * Acts as a sub-view in the MVC structure.
 */


public final class HUDRenderer {

    private final BackBuffer backBuffer;
    private final FontPack fontPack;
    private final EntityRenderer entityRenderer;

	// Teleport cooldown UI constants
	private static final int P1_COOLDOWN_X = 200;
	private static final int P2_COOLDOWN_X_OFFSET = 100;
	private static final int COOLDOWN_Y_OFFSET = 50;
	private static final int TELEPORT_GAUGE_RADIUS = 26;
    private int originIconP1X;
    private int originIconP2X;

    private final Map<DrawManager.SpriteType, BufferedImage> spriteMap;
    private int originIconP2Width;
    private int originIconP2Height;
    private int originIconP1Height;
    private int originIconP1Width;

    public HUDRenderer(BackBuffer backBuffer, FontPack fontPack, EntityRenderer entityRenderer, Map<DrawManager.SpriteType, BufferedImage> spriteMap) {
        this.backBuffer = backBuffer;
        this.fontPack = fontPack;
        this.entityRenderer = entityRenderer;
        this.spriteMap = spriteMap;

    }

    /** Draw score. */
    public void drawScore(final int screenWidth, final int score, final int y, final int playerId) {
        Graphics g = backBuffer.getGraphics();
        Font font = fontPack.getRegular();
        g.setFont(font);
        g.setColor(Color.WHITE);
        String scoreString = String.format("P%d:%04d",playerId, score);


        g.drawString(scoreString, screenWidth - 120, y);
    }

    /** Draw elapsed time on screen. */
    public void drawTime(final int screenHeight, final long milliseconds) {
        Graphics g = backBuffer.getGraphics();
        g.setFont(fontPack.getRegular());
        g.setColor(Color.GRAY);

        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        seconds %= 60;
        FontMetrics fm = g.getFontMetrics();
        int fontHeight = fm.getHeight();
        int y = screenHeight + 2 * fontHeight;
        String timeString = String.format("Time: %02d:%02d", minutes, seconds);
        g.drawString(timeString, 10, y);
    }

    /** Draw current coin count on screen (bottom-center). */
    public void drawCoin(final int screenWidth, final int screenHeight, final int coin) {
        Graphics g = backBuffer.getGraphics();
        g.setFont(fontPack.getRegular());
        g.setColor(Color.YELLOW);
        FontMetrics fm = g.getFontMetrics();
        int fontHeight = fm.getHeight();

        String coinString = String.format("%03d$", coin);
        int textWidth = fontPack.getRegularMetrics().stringWidth(coinString);
        int x = screenWidth / 2 - textWidth / 2;
        int y = screenHeight - fontHeight;

        g.drawString(coinString, x, y);
    }

    /** Draw number of remaining lives for Player 1. */
    public void drawLivesP1(final int lives) {
        Graphics g = backBuffer.getGraphics();
        g.setFont(fontPack.getRegular());
        g.setColor(Color.WHITE);
        g.drawString("P1:", 10, 25);
        for (int i = 0; i < lives; i++) {
            entityRenderer.drawLife(50 + 35 * i, 10, 1);
        }
    }

    /** Draw number of remaining lives for Player 2. */
    public void drawLivesP2(final int lives) {
        Graphics g = backBuffer.getGraphics();
        g.setFont(fontPack.getRegular());
        g.setColor(Color.WHITE);
        g.drawString("P2:", 10, 55);
        for (int i = 0; i < lives; i++) {
            entityRenderer.drawLife(50 + 35 * i, 40, 2);
        }
    }

    /** Draw all item icons on HUD. */
    public void drawItemsHUD(final int screenWidth, final int screenHeight) {
        Graphics g = backBuffer.getGraphics();
        ItemHUDManager hud = ItemHUDManager.getInstance();
        hud.setHUDPositions(screenHeight);
        hud.initialize(screenWidth);
        hud.drawItems(g);
    }

    /** Draw current level name (bottom-left). */
    public void drawLevel(final int seperateLine, final String levelName) {
        Graphics g = backBuffer.getGraphics();
        g.setFont(fontPack.getRegular());
        g.setColor(Color.WHITE);
        FontMetrics fm = g.getFontMetrics();
        int fontHeight = fm.getHeight();

        int y = seperateLine + fontHeight;
        g.drawString(levelName, 20, y);
    }

    /** Draw achievement popup at the top center of the screen. */
    public void drawAchievementPopup(final int screenWidth, final String text) {
        Graphics g = backBuffer.getGraphics();
        int popupWidth = 250, popupHeight = 50;
        int x = screenWidth / 2 - popupWidth / 2;
        int y = 80;

        g.setColor(new Color(0, 0, 0, 200));
        g.fillRoundRect(x, y, popupWidth, popupHeight, 15, 15);

        g.setColor(Color.YELLOW);
        g.drawRoundRect(x, y, popupWidth, popupHeight, 15, 15);

        g.setFont(fontPack.getRegular());
        g.setColor(Color.WHITE);
        int textWidth = fontPack.getRegularMetrics().stringWidth(text);
        g.drawString(text, (screenWidth - textWidth) / 2, y + popupHeight / 2 + 5);
    }

    /** Draw health popup (green if heal, red if damage). */
    public void drawHealthPopup(final int screenWidth, final String text) {
        Graphics g = backBuffer.getGraphics();
        int popupWidth = 250, popupHeight = 40;
        int x = screenWidth / 2 - popupWidth / 2;
        int y = 100;

        g.setColor(new Color(0, 0, 0, 200));
        g.fillRoundRect(x, y, popupWidth, popupHeight, 15, 15);

        g.setColor(text.startsWith("+") ? new Color(50, 255, 50) : new Color(255, 50, 50));
        g.setFont(fontPack.getFontBig());
        int textWidth = fontPack.getBigMetrics().stringWidth(text);
        g.drawString(text, (screenWidth - textWidth) / 2, y + popupHeight / 2 + 5);
    }

	/** Draw circular cooldown gauge */
    /** Draw circular cooldown gauge */
    private void drawTeleportCooldown(Graphics2D g, int x, int y, double ratio) {
        int r = TELEPORT_GAUGE_RADIUS;

        g.drawImage(
                spriteMap.get(DrawManager.SpriteType.TeleportCool),
                x, y, r, r, null
        );

        if (ratio < 1.0) {
            float alpha = 0.5f;
            Composite old = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

            g.setColor(Color.BLUE);
            int h = (int) (r * ratio);
            g.fillRect(x, y + (r - h), r, h);

            g.setComposite(old);

            int seconds = Math.max(1, (int) Math.ceil((1.0 - ratio) * 5));
            String text = seconds + "";

            g.setFont(fontPack.getRegular());
            FontMetrics fm = g.getFontMetrics();

            int textX = x + (r - fm.stringWidth(text)) / 2;
            int textY = y + (r + fm.getAscent()) / 2 - 3;

            g.setColor(Color.BLACK);
            g.drawString(text, textX + 1, textY + 1);

            g.setColor(Color.RED);
            g.drawString(text, textX, textY);
        }
    }

    /** Draw teleport cooldowns for P1 and P2 */
    /** Draw teleport cooldowns for P1 and P2 */
    public void drawTeleportCooldowns(int screenWidth, int screenHeight, double cooldownP1, double cooldownP2, boolean originUsed) {
        Graphics2D g = (Graphics2D) backBuffer.getGraphics();
        int r = TELEPORT_GAUGE_RADIUS;
        int y = screenHeight - COOLDOWN_Y_OFFSET;

        // -------- P1 --------
        int p1X = P1_COOLDOWN_X;
        int p1CenterX = p1X + r / 2;
        int p1CenterY = y + r / 2;

        drawTeleportCooldown(g, p1X, y, cooldownP1);

        drawExplainP1Skill(
                p1CenterX,
                p1CenterY,
                y,
                y + r
        );

        // -------- P2 --------
        int p2X = screenWidth / 2 + P2_COOLDOWN_X_OFFSET;
        int p2CenterX = p2X + r / 2;
        int p2CenterY = y + r / 2;

        drawTeleportCooldown(g, p2X, y, cooldownP2);

        drawExplainP2Skill(p2CenterX, p2CenterY, y, y + r);
        if (originUsed) {
            drawOriginUsed(y);
        }
    }


    private void drawPlayerSkillExplain(int centerX, int centerY, int topLineY, int bottomLineY, Color labelColor, String playerLabel, String topMainText, String[] topTexts, String[] bottomTexts, BufferedImage[] icons, boolean isP1) {
        Graphics2D g = (Graphics2D) backBuffer.getGraphics();

        // Label (P1: / P2:)
        g.setFont(fontPack.getRegular());
        g.setColor(labelColor);

        FontMetrics fmLabel = g.getFontMetrics();

        g.drawString(playerLabel, centerX - TELEPORT_GAUGE_RADIUS - fmLabel.stringWidth(playerLabel) - 5, centerY + fmLabel.getAscent() / 2);
        // Main texts
        g.setFont(fontPack.getFontSmallBig());
        g.setColor(Color.WHITE);

        FontMetrics fmTop = g.getFontMetrics();

        int topX = centerX - fmTop.stringWidth(topMainText) / 2;
        int topY = topLineY - fmTop.getHeight() / 2;

        g.drawString(topMainText, topX, topY);

        String teleport = "teleport";

        FontMetrics fmSmall = g.getFontMetrics();

        int bottomX = centerX - fmSmall.stringWidth(teleport) / 2;
        int bottomY = bottomLineY + fmSmall.getHeight();

        g.drawString(teleport, bottomX, bottomY);

        // Skill rows
        topX += 100;

        for (int i = 0; i < topTexts.length; i++) {

            FontMetrics fm = g.getFontMetrics();
            String t = topTexts[i];
            int tWidth = fm.stringWidth(t);

            g.drawString(t, topX, topY);

            int center = topX + tWidth / 2;

            BufferedImage icon = icons[i];
            if (icon != null) {
                int iconX = center - icon.getWidth() / 2;
                g.drawImage(icon, iconX, topLineY, null);


                if (i == 2) {
                    if (isP1) {
                        originIconP1X = iconX;
                        originIconP1Width = icon.getWidth();
                        originIconP1Height = icon.getHeight();
                    } else {
                        originIconP2X = iconX;
                        originIconP2Width = icon.getWidth();
                        originIconP2Height = icon.getHeight();
                    }
                }
            }

            String b = bottomTexts[i];
            int bWidth = fm.stringWidth(b);

            int bX = center - bWidth / 2;
            g.drawString(b, bX, bottomY);

            topX += 100;
        }
    }
    public void drawExplainP1Skill(int centerX,int centerY,int topLineY,int bottomLineY) {
        drawPlayerSkillExplain(centerX, centerY, topLineY, bottomLineY, Color.RED, "P1:", "SHIFT(L)", new String[]{"CTRL(L)", "SPACE", "PRESS  O"}, new String[]{"laser","shoot","origin"}, new BufferedImage[]{spriteMap.get(DrawManager.SpriteType.ChargingLaserP1Icon), spriteMap.get(DrawManager.SpriteType.ShootP1Icon), spriteMap.get(DrawManager.SpriteType.OriginIcon)}, true);
    }
    public void drawExplainP2Skill(int centerX,int centerY,int topLineY,int bottomLineY) {
        drawPlayerSkillExplain(centerX, centerY, topLineY, bottomLineY, Color.CYAN, "P2:", "\\ or |", new String[]{"SHIFT(R)", "ENTER", "PRESS  O"}, new String[]{"laser","shoot","origin"}, new BufferedImage[]{spriteMap.get(DrawManager.SpriteType.ChargingLaserP2Icon), spriteMap.get(DrawManager.SpriteType.ShootP2Icon), spriteMap.get(DrawManager.SpriteType.OriginIcon)}, false);
    }



    public void drawOriginUsed(int topY) {
        Graphics2D g = (Graphics2D) backBuffer.getGraphics();

        float alpha = 0.5f;
        Composite old = g.getComposite();

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g.setColor(Color.BLUE);

        g.fillRect(originIconP1X, topY, originIconP1Width, originIconP1Height);
        g.fillRect(originIconP2X, topY, originIconP2Width, originIconP2Height);
        g.setComposite(old);
    }

}
