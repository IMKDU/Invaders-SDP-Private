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

		int drawX = item.getPositionX(); // middle alignment: int drawX = positionX- scaledW / 2
		int drawY = item.getPositionY(); // middle alignment: int drawY = positionY - scaledH / 2
		g2d.drawImage(img, drawX, drawY, scaledW, scaledH, null);
	}


	private SpriteType getSprite(DropItem.ItemType type) {
		switch (type) {

			case SubShip:
				return SpriteType.Item_SubShip;
			case Stop:
				return SpriteType.Item_Stop;
			case Shield:
				return SpriteType.Item_Shield;
			case Heal:
				return SpriteType.Item_Heal;
			case Bomb:
				return SpriteType.Item_Bomb;
			case Coin:
				return SpriteType.Item_Coin;

		}
		throw new IllegalArgumentException("Unknown ItemType: " + type);
	}
}
