package engine.renderer;

import java.awt.*;
import java.util.Map;

import engine.BackBuffer;
import entity.Entity;
import engine.DrawManager.SpriteType;
import screen.HealthBar;

/**
 * Handles rendering of all game entities using the shared back buffer.
 * Acts as a sub-view in MVC architecture.
 */
public final class EntityRenderer {

    private final Map<SpriteType, boolean[][]> spriteMap;
    private final BackBuffer backBuffer;
    private final double scale;

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
    public void drawHealthBar(final HealthBar healthBar){
        int [] position = healthBar.getPosition();
        float ratio_hp = healthBar.getRatio_HP();

        int width = healthBar.getWidth();
        Graphics2D g2 = (Graphics2D) backBuffer.getGraphics();
        g2.setColor(Color.GREEN);
        Stroke oldStroke = g2.getStroke(); // 선 굵기 백업
        g2.setStroke(new BasicStroke(3));
        g2.drawLine(position[0], position[1], position[2], position[3]);
        if ((int)((float)position[0] + (width * ratio_hp)) != position[2]){
            g2.setColor(Color.RED);
            g2.drawLine((int) ((float)position[0] + (width * ratio_hp)), position[1], position[2], position[3]);
        }
        g2.setStroke(oldStroke); // 백업 받은거 원위치
    }
}
