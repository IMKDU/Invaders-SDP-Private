package engine.view.renderer;

import audio.SoundManager;
import engine.view.BackBuffer;
import engine.core.Cooldown;
import entity.GameConstant;

import java.awt.image.BufferedImage;

public class SpecialAnimationRenderer {

    private enum Phase {
        SHIP, HUMAN, BUTTON, FIRE, END
    }

    private Phase phase = Phase.SHIP;

    private final AnimationLoader loader;

    private final BufferedImage[] shipFrames;
    private final BufferedImage[] humanFrames;
    private final BufferedImage[] buttonFrames;
    private final BufferedImage[] fireFrames;

    private BufferedImage[] currentFrames;
    private int imgHeight = GameConstant.ITEMS_SEPARATION_LINE_HEIGHT - GameConstant.STAT_SEPARATION_LINE_HEIGHT;
    private int imgWidth = GameConstant.SCREEN_WIDTH;

    private int frame = 0;
    private Cooldown frameCooldown;
    private final BackBuffer backBuffer;
    private boolean finished = false;
    private boolean soundPlay;

    public SpecialAnimationRenderer(BackBuffer backBuffer) {
        this.loader = new AnimationLoader();
        this.backBuffer = backBuffer;
        this.shipFrames = loader.load("res/images/Animation/01_ship");
        this.humanFrames = loader.load("res/images/Animation/02_human");
        this.buttonFrames = loader.load("res/images/Animation/03_button");
        this.fireFrames = loader.load("res/images/Animation/04_fire");
        this.soundPlay = false;
        this.currentFrames = shipFrames;
        this.frameCooldown = new Cooldown(60);
        start(shipFrames);

    }

    private void start(BufferedImage[] frames) {

        this.currentFrames = frames;
        this.frame = 0;
        this.finished = false;
        frameCooldown.reset();
    }

    public void update(int currentLevel) {

        if (phase == Phase.END) {
            this.resetAnimation();
        }


        if (!finished && frameCooldown.checkFinished()) {
            frameCooldown.reset();
            frame++;

            if (frame >= currentFrames.length) {
                frame = currentFrames.length - 1;
                finished = true;
            }
        }



        if (finished) {
            switch (phase) {
                case SHIP:
                    phase = Phase.HUMAN;
                    start(humanFrames);
                    break;

                case HUMAN:
                    phase = Phase.BUTTON;
                    start(buttonFrames);
                    break;

                case BUTTON:
                    phase = Phase.FIRE;
                    start(fireFrames);
                    break;

                case FIRE:
                    phase = Phase.END;
                    SoundManager.playLoop("sfx/level" + currentLevel + ".wav");
                    break;
            }
        }
    }
    public void resetAnimation() {
        this.phase = Phase.SHIP;
        this.frame = 0;
        this.finished = false;
        start(shipFrames);
        soundPlay = false;
    }

    public void draw() {
        if (phase != Phase.END) {
            backBuffer.getGraphics().drawImage(currentFrames[frame], 0, GameConstant.STAT_SEPARATION_LINE_HEIGHT, imgWidth, imgHeight, null);
            if (currentFrames == shipFrames && !soundPlay){
                SoundManager.stopAll();
                SoundManager.play("sfx/OriginBgm.wav");
                soundPlay = true;
            }
        }
    }

    public boolean isFinished() {

        return phase == Phase.END;
    }
}
