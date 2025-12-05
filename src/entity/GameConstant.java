package entity;


public final class GameConstant {

    /** Height of the interface separation line. */
    public static int STAT_SEPARATION_LINE_HEIGHT;
    /** Height of the items separation line (above items). */
    public static int ITEMS_SEPARATION_LINE_HEIGHT;
	/** Width of the Screen */
	public static int SCREEN_WIDTH;
	/** Width of the Screen */
	public static int SCREEN_HEIGHT;
	/** Max fps of current screen. */
	public static final int FPS = 60;
	/** Set if this run is for debugging */
	public static final boolean isTest = false;
	/** Number of bomb shots granted */
	public static final int BOMB_ITEM_SHOTS = 3;
	/** Coins awarded when collecting a coin item */
	public static final int COIN_ITEM_VALUE = 30;
	/** Points-to-coin conversion rate */
	public static final int POINTS_TO_COIN_CONVERSION = 10;
	/** Damage dealt to bosses by player Bullet */
	public static final int PLAYER_BULLET_DAMAGE = 5;
	/** Damage dealt to bosses by bomb explosion */
	public static final int BOMB_DAMAGE_TO_BOSS = 2;
	/** Bomb explosion area radius (in pixels) */
	public static final int BOMB_AOE_RADIUS = 100;

	public static final int TELEPORT_INVINCISIBLE_TIME = 60;

	private static boolean origin_skill_activated = false;
	private static boolean used_origin = false;

	private GameConstant() {}
    private static boolean initialized = false;

    public static void initialize(int screenWidth, int screenHeight) {
        if (initialized) {
            throw new IllegalStateException("GameConstant has already been initialized.");
        }
        STAT_SEPARATION_LINE_HEIGHT = (int) (screenHeight * 0.08);
        ITEMS_SEPARATION_LINE_HEIGHT = (int) (screenHeight * 0.9);
        SCREEN_WIDTH = screenWidth;
        SCREEN_HEIGHT = screenHeight;

        initialized = true;
    }

	public static boolean getOrigin_skill_activated() {
		return origin_skill_activated;
	}
	public static boolean getUsed_origin() {
		return used_origin;
	}
	public static void setOrigin_skill_activated(boolean value) {
		origin_skill_activated = value;
	}
	public static void setUsed_origin(boolean value) {
		used_origin = value;
	}
}
