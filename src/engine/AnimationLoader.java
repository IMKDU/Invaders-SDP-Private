package engine;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;

public class  AnimationLoader {

    public BufferedImage[] load(String folderPath) {
        File folder = new File(folderPath);

        File[] files = folder.listFiles((dir, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".png") || lower.endsWith(".jpg");
        });

        if (files == null || files.length == 0) {
            System.out.println("[AnimationLoader] No frames found in " + folderPath);
            return new BufferedImage[0];
        }

        // 파일 이름 순
        Arrays.sort(files);

        BufferedImage[] frames = new BufferedImage[files.length];

        try {
            for (int i = 0; i < files.length; i++) {
                frames[i] = ImageIO.read(files[i]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return frames;
    }
}