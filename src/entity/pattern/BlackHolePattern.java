package entity.pattern;

import engine.core.Core;
import entity.*;
import entity.base.HasBounds;
import entity.ship.Ship;

import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BlackHolePattern extends BossPattern{

    private HasBounds boss;
    private List<Ship> ships;

    private Point centerPoint;
    private int radius;

    private final double pullConstant;
	private final int effectDuration;

    public BlackHolePattern(HasBounds boss, List<Ship> ships, final int centerX, final int centerY, final int radius, final double pullConstant, int effectDuration) {
        super(new Point(boss.getPositionX(), boss.getPositionY()));
        this.boss = boss;
        this.ships = ships;
		this.centerPoint = new Point(centerX, centerY);
        this.radius = radius;
        this.pullConstant = pullConstant;
		this.effectDuration = effectDuration;
		this.blackHoles = new HashSet<>();
		addBlackHole();
    }

    @Override
    public void attack(){
		if(!blackHoles.isEmpty()){
			for(BlackHole bkh : blackHoles){
				bkh.update();
			}
			blackHoles.removeAll(invalidBlackHoles());
		}
    }

    @Override
    public void move(){
		// This is a background attack.
    }

	private Set<BlackHole> invalidBlackHoles(){
		Set<BlackHole> removeBlackHoles = new HashSet<>();
		for(BlackHole bkh : blackHoles){
			if(bkh.shouldBeRemoved()){
				removeBlackHoles.add(bkh);
				Core.getLogger().info("BlackHolePattern: Black Hole Removed");
			}
		}
		return removeBlackHoles;
	}

	public void addBlackHole(){
		Core.getLogger().info("BlackHolePattern: Black Hole Added");

		BlackHole newBlackHole = new BlackHole(this.centerPoint, this.radius, this.pullConstant, this.effectDuration, this.ships);
		this.blackHoles.add(newBlackHole);
	}

	public void addBlackHole(Point centerPoint, int radius, double pullConstant, int effectDuration, List<Entity> targets){
		Core.getLogger().info("BlackHolePattern: Black Hole Added Using Params");

		BlackHole newBlackHole = new BlackHole(centerPoint, radius, pullConstant, effectDuration, targets);
		this.blackHoles.add(newBlackHole);
	}

}
