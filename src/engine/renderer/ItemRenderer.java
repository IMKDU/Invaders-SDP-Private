package engine.renderer;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Map;

import engine.BackBuffer;
import engine.DrawManager;
import engine.DrawManager.SpriteType;
import entity.DropItem;

public class ItemRenderer {

	private final BackBuffer backBuffer;
	private final Map<SpriteType, BufferedImage> spriteMap;
	private final double scale;

	public ItemRenderer(BackBuffer backBuffer, Map<SpriteType, BufferedImage> spriteMap, double scale) {
		this.backBuffer = backBuffer;
		this.spriteMap = spriteMap;
		this.scale = scale;
	}

	private void drawItemPlaceholder(Graphics2D g2d, DropItem item,
									 Color fill, Color border) {
		int size = (int) (20 * scale * 1.5);
		int x = item.getPositionX();
		int y = item.getPositionY();

		g2d.setColor(fill);
		g2d.fillOval(x, y, size, size);

		g2d.setColor(border);
		g2d.drawOval(x, y, size, size);
	}

	public void render(DropItem item) {
        Graphics2D g2d = (Graphics2D) backBuffer.getGraphics();

		// === Coin ===
		if (item.getItemType() == DropItem.ItemType.Coin) {
			drawItemPlaceholder(g2d, item, Color.YELLOW, Color.ORANGE);
			return;
		}

        BufferedImage img = spriteMap.get(this.getSprite(item.getItemType()));
        if (img == null) {
            return;
        }
        int originalW = img.getWidth();
        int originalH = img.getHeight();
        int scaledW = (int) (originalW * scale * 2);
        int scaledH = (int) (originalH * scale * 2);

        int drawX = item.getPositionX(); // middle alignment: int drawX = positionX- scaledW / 2
        int drawY = item.getPositionY(); // middle alignment: int drawY = positionY - scaledH / 2
        g2d.drawImage(img, drawX, drawY, scaledW, scaledH, null);
	}


	private SpriteType getSprite(DropItem.ItemType type) {
		switch (type) {
			case Explode: return SpriteType.Item_Explode;
			case Stop:    return SpriteType.Item_Stop;
			case Shield:  return SpriteType.Item_Shield;
			case Heal:    return SpriteType.Item_Heal;
            case Bomb:    return SpriteType.Item_Bomb;
		}
		throw new IllegalArgumentException("Unknown ItemType: " + type);
	}
}
