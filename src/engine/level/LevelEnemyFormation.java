package engine.level;

import java.util.ArrayList;
import java.util.Map;
import java.util.List;

public class LevelEnemyFormation {
    private int formationWidth;
    private int formationHeight;
    private int baseSpeed;
    private int shootingFrecuency;
    private List<String> movementStrategy = new ArrayList<>();

    public LevelEnemyFormation(Map<String, Object> map) {
        this.formationWidth = ((Number) map.get("formationWidth")).intValue();
        this.formationHeight = ((Number) map.get("formationHeight")).intValue();
        this.baseSpeed = ((Number) map.get("baseSpeed")).intValue();
        this.shootingFrecuency = ((Number) map.get("shootingFrecuency")).intValue();
        Object rawStrategy = map.get("movementStrategy");
        if (rawStrategy instanceof List<?>) {
            List<?> list = (List<?>) rawStrategy;
            for (Object item : list) {
                this.movementStrategy.add(item.toString());
            }
        } else if (rawStrategy instanceof String) {
            this.movementStrategy.add((String) rawStrategy);
        } else {
            this.movementStrategy.add("default");
        }
    }

    // Getters

    public int getFormationWidth() {
        return formationWidth;
    }

    public int getFormationHeight() {
        return formationHeight;
    }

    public int getBaseSpeed() {
        return baseSpeed;
    }

    public int getShootingFrecuency() {
        return shootingFrecuency;
    }

    public List<String> getMovementStrategy() { return movementStrategy; }
}
