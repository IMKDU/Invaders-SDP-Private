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
        if (model.isOriginSkillActivated()) {

            drawManager.getSpecialAnimationRenderer().update(model.getCurrentLevel().getLevel());
            drawManager.getSpecialAnimationRenderer().draw();

            if (drawManager.getSpecialAnimationRenderer().isFinished()) {
                model.setOriginSkillActivated(false);
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
        if (model.getOmegaBoss() != null) {
            drawManager.getEntityRenderer().drawHealthBarWithHP(model.getOmegaBoss());
            drawManager.getUIRenderer().drawBossName("Omega");
        }
        if (model.getFinalBoss() != null) {
            drawManager.getEntityRenderer().drawHealthBarWithHP(model.getFinalBoss());
            drawManager.getUIRenderer().drawBossName("???");
        }
        if (model.getZetaBoss() != null) {
            drawManager.getEntityRenderer().drawHealthBarWithHP(model.getZetaBoss());
            drawManager.getUIRenderer().drawBossName("Zeta");
        }
		if (model.getBossLasers() != null) {
			for (LaserBeam laser : model.getBossLasers()) {
				drawManager.getEntityRenderer().drawLaser(laser);
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
		drawManager.getHUDRenderer().drawTeleportCooldowns(dto.getWidth(), dto.getHeight(), dto.teleportCooldownP1, dto.teleportCooldownP2);
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


        /** Charging skill visualization for Player 1 */
        if (model.getShip() != null) {
            drawChargingSkill(model.getShip(), dto.getWidth(), dto.getHeight());
        }

        /** Charging skill visualization for Player 2 */
        if (model.getShipP2() != null) {
            drawChargingSkill(model.getShipP2(), dto.getWidth(), dto.getHeight());
        }

        if(model.getExplosionEntity() != null){
            drawManager.getEntityRenderer().drawExplosion(
                    model.isExplosionBoom(),
                    model.getExplosionEntity(),
                    model.getWarningExplosion()
            );
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

    /**
     * Draws the charging skill UI elements including charge bar and laser beam.
     * @param ship The ship to draw charging skill for
     * @param screenWidth Screen width
     * @param screenHeight Screen height
     */
    private void drawChargingSkill(entity.Ship ship, int screenWidth, int screenHeight) {
        // Draw charging progress bar
        if (ship.isCharging()) {
            double progress = ship.getChargeProgress();
            drawManager.getEntityRenderer().drawChargingBar(
                    ship.getPositionX(),
                    ship.getPositionY() - 10,
                    ship.getWidth(),
                    progress
            );
        }

        // Draw laser beam when active
        if (ship.isLaserActive()) {
            drawManager.getEntityRenderer().drawChargingLaser(
                    ship.getPositionX() + ship.getWidth() / 2,
                    ship.getPositionY(),
                    ship.getWidth(),
                    screenHeight
            );
        }
    }
}

