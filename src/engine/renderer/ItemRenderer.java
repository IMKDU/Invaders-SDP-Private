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

	public void render(DropItem item) {
        Graphics2D g2d = (Graphics2D) backBuffer.getGraphics();
        BufferedImage img = spriteMap.get(this.getSprite(item.getItemType()));
        if (img == null) {
            return;
        }
        int originalW = img.getWidth();
        int originalH = img.getHeight();
        int scaledW = (int) (originalW * scale * 2);
        int scaledH = (int) (originalH * scale * 2);

        int drawX = item.getPositionX(); // 가운데 정렬: int drawX = positionX- scaledW / 2
        int drawY = item.getPositionY(); // 가운데 정렬: int drawY = positionY - scaledH / 2
        g2d.drawImage(img, drawX, drawY, scaledW, scaledH, null);
	}


	private SpriteType getSprite(DropItem.ItemType type) {
		switch (type) {
            case SubShip: return SpriteType.Item_SubShip;
			case Slow:    return SpriteType.Item_Slow;
			case Stop:    return SpriteType.Item_Stop;
			case Push:    return SpriteType.Item_Push;
			case Shield:  return SpriteType.Item_Shield;
			case Heal:    return SpriteType.Item_Heal;
		}
		throw new IllegalArgumentException("Unknown ItemType: " + type);
	}

	private Color getColor(DropItem.ItemType type) {
		switch (type) {
			case SubShip: return Color.RED;
			case Slow:    return Color.BLUE;
			case Stop:    return Color.YELLOW;
			case Push:    return Color.ORANGE;
			case Shield:  return Color.CYAN;
			case Heal:    return Color.GREEN;
		}
		throw new IllegalArgumentException("Unknown ItemType: " + type);
	}
}
