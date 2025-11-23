package entity;

import audio.SoundManager;

import java.awt.*;

public class Explosion extends Entity {
    private int timer = 0;
    private final int warningTime = 120; // 약 2초 (경고)
    private final int explodeTime = 30;  // 약 0.5초 (실제 폭발)
    private boolean isDestroyed = false;
    private boolean isBoom = false;  // 데미지 판정 여부

    public Explosion(int x, int y) {
        super(x, y, 100, 100, Color.RED);
    }

    public void update() {
        timer++;

        // 1. 경고 시간이 지나면 폭발 시작
        if (timer > warningTime && timer <= warningTime + explodeTime) {
            isBoom= true;
            // 이때 색을 바꾸거나 크기를 키울 수 있음
        }
        // 2. 폭발 시간도 끝나면 객체 삭제
        else if (timer > warningTime + explodeTime) {
            isBoom = false;
            this.destroy(); // 화면에서 사라짐
        }
    }

    // 그리기 (GameScreen draw에서 호출)
    public void draw(Graphics2D g) {
        // 경고 단계 (반투명 빨강)
        if (!isBoom) {
            g.setColor(new Color(255, 0, 0, 100)); // 빨강, 투명도 100
            g.fillOval(this.getPositionX(), this.getPositionY(), this.width, this.height);
            g.setColor(Color.RED);
            g.drawOval(this.getPositionX(), this.getPositionY(), this.width, this.height); // 테두리
        }
        // 폭발 단계 (진한 주황/노랑)
        else {
            g.setColor(Color.ORANGE);
            g.fillOval(this.getPositionX() - 10, this.getPositionY() - 10, this.width + 20, this.height + 20); // 펑 터지며 좀 커짐
            SoundManager.stop("sfx/impact.wav");
            SoundManager.play("sfx/impact.wav");
        }
    }
    public final void destroy() {
        isDestroyed = true;
    }

    @Override
    public void onCollision(Collidable other, GameModel gameModel){
        if(!this.isBoom){
            return;
        }
        if( other instanceof Ship){
            gameModel.requestShipDamage((Ship) other,1);
        }
    }
    public boolean isDestroyed() {
        return isDestroyed;
    }
}