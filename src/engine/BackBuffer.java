package engine;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

public final class BackBuffer {

    private final Frame frame;
    private BufferedImage buffer;
    private Graphics graphics;
    private Graphics backGraphics;
    private int shakeDuration;
    private int shakeIntensity;

    public BackBuffer(Frame frame) {
        this.frame = frame;
    }

    public void initDraw(final int screenWidth, final int screenHeight) {
        buffer = new BufferedImage(screenWidth, screenHeight, BufferedImage.TYPE_INT_RGB);
        graphics = frame.getGraphics();
        backGraphics = buffer.getGraphics();

        backGraphics.setColor(Color.BLACK);
        backGraphics.fillRect(0, 0, screenWidth, screenHeight);
    }
    public void setShake(int shakeDuration, int shakeIntensity){
        this.shakeDuration = shakeDuration;
        this.shakeIntensity = shakeIntensity;
    }

    public void end() {
        int offsetX = 0;
        int offsetY = 0;

        if (shakeDuration > 0) {
            offsetX = (int)(Math.random() * shakeIntensity * 2 - shakeIntensity);
            offsetY = (int)(Math.random() * shakeIntensity * 2 - shakeIntensity);
            shakeDuration--;
        }
        graphics.drawImage(buffer, frame.getInsets().left + offsetX, frame.getInsets().top + offsetY, frame);
    }

    public Graphics getGraphics() {
        return backGraphics;
    }
}
