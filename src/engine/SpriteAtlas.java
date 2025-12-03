package engine;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import engine.DrawManager.SpriteType;

public final class SpriteAtlas {

    private Map<SpriteType, BufferedImage> spriteMap = new LinkedHashMap<>();

    public SpriteAtlas(FileManager fileManager) {
        try {
            spriteMap = new LinkedHashMap<SpriteType, BufferedImage>();
            spriteMap.put(SpriteType.ShipP1, new BufferedImage(25,31,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.ShipP2, new BufferedImage(25,31,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.ShipP1Move, new BufferedImage(25,43,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.ShipP2Move, new BufferedImage(25,42,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.ShipP2Explosion1, new BufferedImage(25,31,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.ShipP2Explosion2, new BufferedImage(31,31,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.ShipP2Explosion3, new BufferedImage(31,31,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.Life, new BufferedImage(8,8,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.ShipP1Explosion1, new BufferedImage(25,31,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.ShipP1Explosion2, new BufferedImage(31,31,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.ShipP1Explosion3, new BufferedImage(31,31,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.Bullet, new BufferedImage(3,5,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.EnemyBullet, new BufferedImage(3,5,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.EnemyShipA1, new BufferedImage(25,25,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.EnemyShipA2, new BufferedImage(25,25,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.EnemyShipB1, new BufferedImage(12,8,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.EnemyShipB2, new BufferedImage(12,8,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.EnemyShipC1, new BufferedImage(12,8,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.EnemyShipC2, new BufferedImage(12,8,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.EnemyShipSpecial, new BufferedImage(30,38,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.EnemySpecialExplosion, new BufferedImage(38,38,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.Explosion, new BufferedImage(25,25,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.SoundOn, new BufferedImage(15,15,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.SoundOff, new BufferedImage(15,15,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.Item_Stop, new BufferedImage(20,20,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.Item_Shield, new BufferedImage(20,20,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.Item_Heal, new BufferedImage(20,20,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.Shield, new BufferedImage(61,61,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.FinalBoss1, new BufferedImage(50,40,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.FinalBoss2, new BufferedImage(50,40,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.FinalBossBullet,new BufferedImage(3,5,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.FinalBossDeath, new BufferedImage(50,40,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.OmegaBoss1, new BufferedImage(70,51,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.OmegaBoss2, new BufferedImage(70,51,BufferedImage.TYPE_INT_ARGB));

            spriteMap.put(SpriteType.OmegaBossHitting, new BufferedImage(32,32,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.OmegaBossMoving1, new BufferedImage(32,32,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.OmegaBossMoving2, new BufferedImage(32,32,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.OmegaBossDash1, new BufferedImage(32,32,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.OmegaBossDash2, new BufferedImage(32,32,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.OmegaBossDeath, new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.OmegaBossBullet, new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.Laser, new BufferedImage(5,13,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.BlackHole1, new BufferedImage(1024,1024,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.BlackHole2, new BufferedImage(1024,1024,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.ZetaBoss1, new BufferedImage(70,51,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.ZetaBoss2, new BufferedImage(70,51,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.ZetaBossMoving1, new BufferedImage(70,51,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.ZetaBossMoving2, new BufferedImage(70,51,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.ZetaBossDash1, new BufferedImage(70,51,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.ZetaBossDash2, new BufferedImage(70,51,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.Teleport, new BufferedImage(70,51,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.TeleportCool, new BufferedImage(70,51,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.Bomb1, new BufferedImage(70,51,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.Bomb2, new BufferedImage(70,51,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.BombExplosion, new BufferedImage(70,51,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.Item_Bomb, new BufferedImage(20,20 ,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.BombBullet, new BufferedImage(15,40,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.Item_Coin, new BufferedImage(20,20,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.SubShipP1, new BufferedImage(50,31,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.SubShipP2, new BufferedImage(50,31,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.GammaBoss1, new BufferedImage(50,70,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.GammaBoss2, new BufferedImage(50,70,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.GammaBossDash1, new BufferedImage(50,70,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.GammaBossDash2, new BufferedImage(50,70,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.GammaBossDashing1, new BufferedImage(50,70,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.GammaBossDashing2, new BufferedImage(50,70,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.GuidedBullet1, new BufferedImage(30,30,BufferedImage.TYPE_INT_ARGB));
            spriteMap.put(SpriteType.GuidedBullet2, new BufferedImage(30,30,BufferedImage.TYPE_INT_ARGB));
            fileManager.loadSprite(spriteMap);
            //symatric sprite
            spriteMap.put(SpriteType.OmegaBossDash3, mirrorSprite(spriteMap.get(SpriteType.OmegaBossDash1)));
            spriteMap.put(SpriteType.OmegaBossDash4, mirrorSprite(spriteMap.get(SpriteType.OmegaBossDash2)));
            spriteMap.put(SpriteType.EnemyShipSpecialLeft, mirrorSprite(spriteMap.get(SpriteType.EnemyShipSpecial)));
            spriteMap.put(SpriteType.ZetaBossRight1, mirrorSprite(spriteMap.get(SpriteType.ZetaBoss1)));
            spriteMap.put(SpriteType.ZetaBossRight2, mirrorSprite(spriteMap.get(SpriteType.ZetaBoss2)));
            spriteMap.put(SpriteType.ZetaBossMovingRight1, mirrorSprite(spriteMap.get(SpriteType.ZetaBossMoving1)));
            spriteMap.put(SpriteType.ZetaBossMovingRight2, mirrorSprite(spriteMap.get(SpriteType.ZetaBossMoving2)));
            spriteMap.put(SpriteType.ZetaBossDashRight1, mirrorSprite(spriteMap.get(SpriteType.ZetaBossDash1)));
            spriteMap.put(SpriteType.ZetaBossDashRight2, mirrorSprite(spriteMap.get(SpriteType.ZetaBossDash2)));
            spriteMap.put(SpriteType.GammaBoss1Left, mirrorSprite(spriteMap.get(SpriteType.GammaBoss1)));
            spriteMap.put(SpriteType.GammaBoss2Left, mirrorSprite(spriteMap.get(SpriteType.GammaBoss2)));
            spriteMap.put(SpriteType.GammaBossDash1Left, mirrorSprite(spriteMap.get(SpriteType.GammaBossDash1)));
            spriteMap.put(SpriteType.GammaBossDash2Left, mirrorSprite(spriteMap.get(SpriteType.GammaBossDash2)));
            spriteMap.put(SpriteType.GammaBossDashing1Left, mirrorSprite(spriteMap.get(SpriteType.GammaBossDashing1)));
            spriteMap.put(SpriteType.GammaBossDashing2Left, mirrorSprite(spriteMap.get(SpriteType.GammaBossDashing2)));

        } catch (IOException e) {
            Core.getLogger().warning("[SpriteAtlas] Failed to load sprites: " + e.getMessage());
        }
    }

    public BufferedImage get(SpriteType type) {
        return spriteMap.get(type);
    }

    public Map<DrawManager.SpriteType, BufferedImage> getSpriteMap() {
        return java.util.Collections.unmodifiableMap(spriteMap);
    }

    private BufferedImage mirrorSprite(BufferedImage original) {
        int w = original.getWidth();
        int h = original.getHeight();

        BufferedImage mirrored = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = mirrored.createGraphics();
        g.drawImage(original,
                0, 0, w, h,
                w, 0, 0, h,
                null);

        g.dispose();
        return mirrored;
    }

}
