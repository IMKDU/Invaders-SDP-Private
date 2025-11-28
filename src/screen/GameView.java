package screen;

import audio.SoundManager;
import engine.DrawManager;
import engine.DTO.HUDInfoDTO;
import entity.*;
import entity.pattern.ApocalypseAttackPattern;

import java.util.ArrayList;
import java.util.List;

/**
 * GameView
 * ----------
 * - View layer (the V in MVC)
 * - Does not depend on the Controller (GameScreen) or any Screen objects
 * - Receives HUD data from HUDInfoDTO
 *   and directly gets the list of entities to render from the Model
 */
public class GameView {

    private final GameModel model;
    private final DrawManager drawManager;
    private final List<Entity> shipRenderQueue = new ArrayList<>();

    public GameView(GameModel model, DrawManager drawManager) {
        this.model = model;
        this.drawManager = drawManager;
    }

    public void render(final HUDInfoDTO dto) {

        /** frame initialize */
        drawManager.initDrawing(dto.getWidth(), dto.getHeight());
        //오리진 스킬 애니메이션 스킬횟수 > 0 그리고 p1점수 100이상일때 발동
        // 구현 해야할거 발동후 로직, 아군 ship 무적
        if (model.getFinalSkillCnt() > 0 && (dto.getScoreP1() >= 100)) {
            drawManager.getSpecialAnimationRenderer().update(model.getCurrentLevel().getLevel());
            drawManager.getSpecialAnimationRenderer().draw();
            if (drawManager.getSpecialAnimationRenderer().isFinished()){
                model.useFinalSkill();
            }
        }

        else {
            if (model.isBlackHoleActive()) {
                drawManager.getEntityRenderer().drawBlackHole(
                        model.getBlackHoleCX(),
                        model.getBlackHoleCY(),
                        model.getBlackHoleRadius()
                );
            }
            if (dto.getShipP1().isInvincible()) {
                drawManager.getEntityRenderer().drawShield(dto.getShipP1().getPositionX(), dto.getShipP1().getWidth(), dto.getShipP1().getPositionY(), dto.getShipP1().getHeight(), dto.getShipP1().getInvincibilityRatio());
            }
            if (dto.getShipP2().isInvincible()) {
                drawManager.getEntityRenderer().drawShield(dto.getShipP2().getPositionX(), dto.getShipP2().getWidth(), dto.getShipP2().getPositionY(), dto.getShipP2().getHeight(), dto.getShipP2().getInvincibilityRatio());
            }

            shipRenderQueue.clear();
            /** Entity Rendering */
            if (model.getEntitiesToRender() != null) {
                for (int i = 0; i < model.getEntitiesToRender().size(); i++) {
                    var e = model.getEntitiesToRender().get(i);

                    if (e instanceof DropItem) {
                        drawManager.getItemRenderer().render((DropItem) e);
                        continue;
                    }
                    if (e instanceof Ship) { // ship을 맨 마지막에 그리기
                        shipRenderQueue.add(e);
                        continue;
                    }
                    drawManager.getEntityRenderer().drawEntity(e);
                }
                for (Entity s : shipRenderQueue) {
                    drawManager.getEntityRenderer().drawEntity(s);
                }
            }
        }

        drawManager.getHUDRenderer().drawScore(dto.getWidth(), dto.getScoreP1(), 25, 1);
        drawManager.getHUDRenderer().drawScore(dto.getWidth(), dto.getScoreP2(), 50, 2);
        drawManager.getHUDRenderer().drawCoin(dto.getWidth(), dto.getHeight(), dto.getCoin());
        drawManager.getHUDRenderer().drawLivesP1(dto.getLivesP1());
        drawManager.getHUDRenderer().drawLivesP2(dto.getLivesP2());
        drawManager.getHUDRenderer().drawTime(GameConstant.ITEMS_SEPARATION_LINE_HEIGHT, dto.getElapsedTimeMillis());
        drawManager.getHUDRenderer().drawItemsHUD(dto.getWidth(), dto.getHeight());
        drawManager.getHUDRenderer().drawLevel(GameConstant.ITEMS_SEPARATION_LINE_HEIGHT, dto.getLevelName());

        /** draw Line */
        drawManager.getUIRenderer().drawHorizontalLine(dto.getWidth(), GameConstant.STAT_SEPARATION_LINE_HEIGHT - 1);
        drawManager.getUIRenderer().drawHorizontalLine(dto.getWidth(), GameConstant.ITEMS_SEPARATION_LINE_HEIGHT);

        /** achievement popup */
        if (dto.getAchievementText() != null && !model.getAchievementPopupCooldown().checkFinished()) {
            drawManager.getHUDRenderer().drawAchievementPopup(dto.getWidth(), dto.getAchievementText());
        }

        /** health popup */
        if (dto.getHealthPopupText() != null && !model.getHealthPopupCooldown().checkFinished()) {
            drawManager.getHUDRenderer().drawHealthPopup(dto.getWidth(), dto.getHealthPopupText());
        }


        /** countdown */
        if (!model.isInputDelayFinished()) {
            int countdown = (int) ((GameModel.INPUT_DELAY
                    - (System.currentTimeMillis() - model.getGameStartTime())) / 1000);

            drawManager.getUIRenderer().drawCountDown(
                    dto.getWidth(),
                    dto.getHeight(),
                    dto.getLevel(),
                    countdown,
                    model.isBonusLife()
            );

            drawManager.getUIRenderer().drawHorizontalLine(dto.getWidth(), dto.getHeight() / 2 - dto.getHeight() / 12);
            drawManager.getUIRenderer().drawHorizontalLine(dto.getWidth(), dto.getHeight() / 2 + dto.getHeight() / 12);
        }




        /** frame complete */
        drawManager.completeDrawing();
    }
}
