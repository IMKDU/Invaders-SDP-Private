package entity;

import engine.Core;

public class GameConstant {
	/** Height of the interface separation line. */
	public static int STAT_SEPARATION_LINE_HEIGHT = (int) (Core.FRAME_HEIGHT * 0.08);
	/** Height of the items separation line (above items). */
	public static int ITEMS_SEPARATION_LINE_HEIGHT = (int) (Core.FRAME_HEIGHT * 0.9);
	/** Width of the Screen */
	public static int SCREEN_WIDTH=Core.FRAME_WIDTH;
	/** Max fps of current screen. */
	public static final int FPS = 60;
	/** Set if this run is for debugging */
	public static boolean isTest = false;
}
