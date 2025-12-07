package engine.core;

/**
 * Implements an object that stores the state of the game between levels.
 * 
 * @author <a href="mailto:RobertoIA1987@gmail.com">Roberto Izquierdo Amo</a>
 * 
 */
public class GameState {

	/** Current game level. */
	private int level;
	/** Current score. */
	private int score;
	private int scoreP1;
	private int scoreP2;
	/** Lives currently remaining. */
	private int livesRemaining;
	private int livesRemainingP2;
	/** Bullets shot until now. */
	private int bulletsShot;
	/** Ships destroyed until now. */
	private int shipsKill;
    /** Current coin. */
    private int coin;
    private int hittingCnt;
    private int deathCnt;


    /**
	 * Constructor.
	 * 
	 * @param level
	 *            Current game level.
	 * @param score
	 *            Current score.
     * @param coin
     *            Current coin.
	 * @param livesRemaining
	 *            Lives currently remaining.
	 * @param livesRemainingP2
	 *            Lives currently remainingP2.
	 * @param bulletsShot
	 *            Bullets shot until now.
	 * @param shipsDestroyed
	 *            Ships destroyed until now.
	 */
	public GameState(final int level, final int score, final int scoreP1, final int scoreP2,
			final int livesRemaining,final int livesRemainingP2, final int bulletsShot,
			final int shipsDestroyed, final int coin) {
		this.level = level;
		this.score = score;
		this.scoreP1 = scoreP1;
		this.scoreP2 = scoreP2;
		this.livesRemaining = livesRemaining;
		this.livesRemainingP2 = livesRemainingP2;
		this.bulletsShot = bulletsShot;
        this.shipsKill = shipsDestroyed;
        this.coin = coin;
    }

    // --- setters for sync ---

    public void setLevel(int level) {
        this.level = level;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public void setScoreP1(int scoreP1) {
        this.scoreP1 = scoreP1;
    }

    public void setScoreP2(int scoreP2) {
        this.scoreP2 = scoreP2;
    }

    public void setLivesRemaining(int livesRemaining) {
        this.livesRemaining = livesRemaining;
    }

    public void setLivesRemainingP2(int livesRemainingP2) {
        this.livesRemainingP2 = livesRemainingP2;
    }

    public void setBulletsShot(int bulletsShot) {
        this.bulletsShot = bulletsShot;
    }

    public void setShipsKill(int shipsKill) {
        this.shipsKill = shipsKill;
    }

    public void setCoin(int coin) {
        this.coin = coin;
    }
    public void setHittingCnt(int hittingCnt) {
        this.hittingCnt = hittingCnt;
    }
    public void setDeathCnt(int deathCnt){
        this.deathCnt = deathCnt;
    }

    /**
	 * @return the level
	 */
	public final int getLevel() {
		return level;
	}

	/**
	 * @return the score
	 */
	public final int getScore() {
		return score;
	}

	public final int getScoreP1(){
		return scoreP1;
	}

	public final int getScoreP2(){
		return scoreP2;
	}

	/**
	 * @return the livesRemaining
	 */
	public final int getLivesRemaining() {
		return livesRemaining;
	}

	public final int getLivesRemainingP2() {
		return livesRemainingP2;
	}

	/**
	 * @return the bulletsShot
	 */
	public final int getBulletsShot() {
		return bulletsShot;
	}

	/**
	 * @return the shipsDestroyed
	 */
	public final int getShipsKill() {
		return shipsKill;
	}

    public final int getCoin() { return coin; }

    public int getHittingCnt() { return hittingCnt; }

	public final boolean deductCoins(final int amount) {
		if (amount < 0) {
			return false;
		}
		if (this.coin >= amount) {
			this.coin -= amount;
			return true;
		}
		return false;
	}

	public final void addCoins(final int amount) {
		if (amount > 0) {
			this.coin += amount;
		}
	}

	public final void setCoins(final int amount) {
		if (amount >= 0) {
			this.coin = amount;
		}
	}

    public int getDeathCnt() {
        return deathCnt;
    }
}
