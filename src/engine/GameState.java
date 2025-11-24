package engine;

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
	/** Lives currently remaining. */
	private int livesRemaining;
	/** Bullets shot until now. */
	private int bulletsShot;
	/** Ships destroyed until now. */
	private int shipsDestroyed;
    /** Current coin. */
    private int coin;




	//cheat list
	private static boolean invincible = false;

    private static boolean infiniteLives = false;
  
    private static boolean maxScoreActive = false;

	private static boolean dcreaseEnemyPower = false;

	private static boolean unlimitedCoins = false;



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
	 * @param bulletsShot
	 *            Bullets shot until now.
	 * @param shipsDestroyed
	 *            Ships destroyed until now.
	 */
	public GameState(final int level, final int score,
			final int livesRemaining, final int bulletsShot,
			final int shipsDestroyed, final int coin) {
		this.level = level;
		this.score = score;
		this.livesRemaining = livesRemaining;
		this.bulletsShot = bulletsShot;
        this.shipsDestroyed = shipsDestroyed;
        this.coin = coin;
		    }

    /**
	 * Constructor that provides a default coin value of 0.
	 * 
	 * @param level
	 *            Current game level.
	 * @param score
	 *            Current score.
	 * @param livesRemaining
	 *            Lives currently remaining.
	 * @param bulletsShot
	 *            Bullets shot until now.
	 * @param shipsDestroyed
	 *            Ships destroyed until now.
	 */
	public GameState(final int level, final int score,
			final int livesRemaining, final int bulletsShot,
			final int shipsDestroyed) {
		this(level, score, livesRemaining, bulletsShot, shipsDestroyed, 0); // Delegates to the 6-argument constructor
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

	/**
	 * @return the livesRemaining
	 */
	public final int getLivesRemaining() {
		return livesRemaining;
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
	public final int getShipsDestroyed() {
		return shipsDestroyed;
	}

    public final int getCoin() { return coin; }

	public final boolean deductCoins(final int amount) {
		if (GameState.isUnlimitedCoins()) {
			return true;
		}
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


	//cheat
	public static void setInvincible(boolean state) {
        invincible = state;
    }

	public static void setDecreaseEnemyPower(boolean state) {
		dcreaseEnemyPower = state;
	}

  
    public static void setInfiniteLives(boolean state) {
        infiniteLives = state;
    }

 
    public static void setMaxScoreActive(boolean state) {
        maxScoreActive = state;
    }

   
    public static boolean isInvincible() {
        return invincible;
    }

	public static boolean isDecreaseEnemyPower() {
		return dcreaseEnemyPower;
	}

 
    public static boolean isInfiniteLives() {
        return infiniteLives;
    }

  
    	public static boolean isMaxScoreActive() {
            return maxScoreActive;
        }
    
    	public static void setUnlimitedCoins(boolean state) {
    		unlimitedCoins = state;
    	}
    
    	public static boolean isUnlimitedCoins() {
    		return unlimitedCoins;
    	}}
