package entity.movements;

import engine.Cooldown;
import entity.EnemyShip;
import entity.GameConstant;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VerticalLoopFormationMovement implements IMovementStrategy{

    private final int SPAWN_HEIGHT;
    private final int BOTTOM_LIMITS;
    private final int RESPAWN_DELAY_MS;
    private final int FALL_SPEED_START;
    private final int FALL_SPEED;
    private final int SEPARATION;

    private List<EnemyShip> row;
    private Map<EnemyShip, EnemyState> states = new HashMap<>();

    // serve as struct in c++
    private class EnemyState {
        boolean isFalling = true;
        Cooldown respawnCooldown = new Cooldown(RESPAWN_DELAY_MS);
    }

    public VerticalLoopFormationMovement(List<EnemyShip> enemyShips, int separation){
        this.row = enemyShips;
        this.SPAWN_HEIGHT = GameConstant.STAT_SEPARATION_LINE_HEIGHT-enemyShips.getFirst().getHeight()*5/2;
        this.BOTTOM_LIMITS = GameConstant.ITEMS_SEPARATION_LINE_HEIGHT-GameConstant.STAT_SEPARATION_LINE_HEIGHT;
        this.RESPAWN_DELAY_MS = 200;
        this.FALL_SPEED_START = 15;
        this.FALL_SPEED = 2;
        this.SEPARATION = separation;
        initRow();
    }

    public void initRow(){

        int setX = SEPARATION;
        for(EnemyShip enemy:row){
            EnemyState s = new EnemyState();
            states.put(enemy,s);

            enemy.setPositionX(setX);
            enemy.setPositionY(SPAWN_HEIGHT);

            setX += (SEPARATION +enemy.getWidth());
        }
    }

    @Override
    public void updateMovement(){
        for(EnemyShip enemy:row){
            if(enemy.isDestroyed()) continue;

            EnemyState es = states.get(enemy);
            if(es.isFalling){
                updateFalling(enemy,es);
            }
            else{
                updateRespawn(enemy,es);
            }
        }
    }

    private void updateFalling(EnemyShip enemy, EnemyState es){
        int fastDownLimit = SPAWN_HEIGHT + (BOTTOM_LIMITS - SPAWN_HEIGHT) / 6;
        int speed = (enemy.getPositionY() < fastDownLimit) ? FALL_SPEED_START : FALL_SPEED;

        enemy.move(0,speed,false);
        if(enemy.getPositionY() > BOTTOM_LIMITS){
            es.isFalling  = false;
            es.respawnCooldown.setMilliseconds(RESPAWN_DELAY_MS);
            es.respawnCooldown.reset();

            enemy.setPositionY(2*BOTTOM_LIMITS);
        }
    }

    private void updateRespawn(EnemyShip enemy, EnemyState es){
        if(es.respawnCooldown.checkFinished()){
            es.isFalling = true;
            enemy.setPositionY(SPAWN_HEIGHT);
        }
    }

    @Override
    public boolean needsSmoothMovement(){
        return true;
    }
}
