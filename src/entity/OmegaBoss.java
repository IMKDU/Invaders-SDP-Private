package entity;

import engine.DrawManager;

import java.awt.*;

/**
 * Omega - Middle Boss
 */
public class OmegaBoss extends MidBoss {

	/** Initial position in the x-axis. */
	private static final int INIT_POS_X = 224;
	/** Initial position in the y-axis. */
	private static final int INIT_POS_Y = 80;
	/** Width of Omega */
	private static final int OMEGA_WIDTH = 64;
	/** Height of Omega */
	private static final int OMEGA_HEIGHT = 28;
	/** Current Health of Omega */
	private static final int OMEGA_HEALTH = 45;
	/** Point of Omega when destroyed */
	private static final int OMEGA_POINT_VALUE = 500;
	/** Speed of x in pattern 1 */
	private static final int PATTERN_1_X_SPEED = 1;
	/** Speed of x in pattern 2 */
	private static final int PATTERN_2_X_SPEED = 4;
	/** Speed of y in pattern 2 */
	private static final int PATTERN_2_Y_SPEED = 3;
	/** Color of pattern 2 */
	private static final Color PATTERN_2_COLOR = Color.MAGENTA;
	/** Current horizontal movement direction. true for right, false for left. */
	private boolean isRight = true;
	/** Current vertical movement direction. true for down, false for up. */
	private boolean isDown = true;
	/** Boss cannot move over this boundary. */
	private final int widthBoundary;
	/** Boss cannot move below this boundary. */
	private final int bottomBoundary;

	private boolean isDashing = false;
	private boolean isShowingPath = false;
	private Ship targetShip;
	private int dashTargetX;
	private int dashTargetY;
	private long pathShowStartTime;
	private static final long PATH_SHOW_DURATION = 2000; // 2초
	private static final int DASH_SPEED = 5; // 돌진 속도

	/**
	 * Constructor, establishes the boss entity's generic properties.
	 *
 	 * @param color             Color of the boss entity.
 	 * @param widthBoundary		The rightmost X-coordinate for the boss's movement. The boss cannot move over this value.
 	 * @param bottomBoundary    The lowermost Y-coordinate for the boss's movement. The boss cannot move below this value.
	 */
	public OmegaBoss(Color color, int widthBoundary, int bottomBoundary, Ship player) {
		super(INIT_POS_X, INIT_POS_Y, OMEGA_WIDTH, OMEGA_HEIGHT, OMEGA_HEALTH, OMEGA_POINT_VALUE, color);
		this.widthBoundary = widthBoundary;
		this.bottomBoundary = bottomBoundary;
		this.spriteType= DrawManager.SpriteType.OmegaBoss1;
		this.targetShip = player;
		this.logger.info("OMEGA : Initializing Boss OMEGA");
		this.logger.info("OMEGA : move using the default pattern");
	}

	/** move simple */
	@Override
	public void move(int distanceX, int distanceY) {
		this.positionX += distanceX;
		this.positionY += distanceY;
	}

	/**
	 * Executes the appropriate move pattern based on the boss's health status.
	 * Calls {@link #patternFirst()} if health is greater than half, otherwise
	 * Calls {@link #patternSecond()}.
	 *
	 * @see #patternFirst()
	 * @see #patternSecond()
	 */
	private void movePatterns(){
		if(this.pattern!=2 && this.healPoint < this.maxHp/2 && this.healPoint > this.maxHp/3){
			this.pattern=2;
			this.color=PATTERN_2_COLOR;
			this.spriteType = DrawManager.SpriteType.OmegaBoss2;
			logger.info("OMEGA : move using second pattern");
		}
		else if (this.healPoint <= this.maxHp/3){
			this.pattern=3;
		}

		switch(pattern){
			case 1:
				this.patternFirst();
				break;
			case 2:
				this.patternSecond();
				break;
			case 3:
				this.skillDash(targetShip.getPositionX(), targetShip.getPositionY());

		}
	}

	/**
	 * The boss's phase first pattern, which makes it move from side to side across the screen.
	 * @see #move(int, int)
	 */
	private void patternFirst(){
		int dx = this.isRight ? PATTERN_1_X_SPEED : -PATTERN_1_X_SPEED;
		this.move(dx, 0);

		if (this.positionX <= 0) {
			this.isRight = true;
		} else if (this.positionX + this.width >= widthBoundary) {
			this.isRight = false;
		}
	}

	/**
	 * The boss's phase Second pattern, which combines horizontal and vertical movement
	 * Horizontally, it patrols from side to side at a faster speed than in {@link #patternFirst()}.
	 * @see #move(int, int)
	 */
	private void patternSecond(){
		int dx = this.isRight ? PATTERN_2_X_SPEED : -PATTERN_2_X_SPEED;
		int dy = this.isDown ? PATTERN_2_Y_SPEED : -PATTERN_2_Y_SPEED;

		this.move(dx, dy);

		if (this.positionX <= 0) {
			this.positionX = 0;
			this.isRight = true;
		} else if (this.positionX + this.width >= widthBoundary) {
			this.positionX = widthBoundary - this.width;
			this.isRight = false;
		}

		if (this.positionY <= INIT_POS_Y) {
			this.positionY = INIT_POS_Y;
			this.isDown = true;
		} else if (this.positionY + this.height >= bottomBoundary) {
			this.positionY = bottomBoundary - this.height;
			this.isDown = false;
		}
	}

	/**
	 * 플레이어를 향해 돌진하는 스킬
	 * 1. 2초간 돌진 경로 표시
	 * 2. 2초 후 플레이어 위치로 돌진
	 */
	/**
	 * 플레이어를 향해 돌진하는 스킬
	 * 1. 2초간 돌진 경로 표시
	 * 2. 2초 후 플레이어 방향으로 벽까지 돌진
	 */
	public void skillDash(int player_dx, int player_dy) {
		// 이미 스킬 사용 중이면 돌진만 실행
		if (isDashing) {
			dashToTarget();
			return;
		}

		// 경로 표시 중이면 시간 체크
		if (isShowingPath) {
			long elapsedTime = System.currentTimeMillis() - pathShowStartTime;

			if (elapsedTime >= PATH_SHOW_DURATION) {
				// 2초 지나면 돌진 시작
				isShowingPath = false;
				isDashing = true;
			}
			return;
		}

		// 새로운 돌진 스킬 시작
		// 플레이어 방향 저장 (방향만 저장, 목적지는 벽)
		int dx = player_dx - this.positionX;
		int dy = player_dy - this.positionY;
		double distance = Math.sqrt(dx * dx + dy * dy);

		dashTargetX = (int)(dx / distance);  // 정규화된 방향 X
		dashTargetY = (int)(dy / distance);  // 정규화된 방향 Y

		// 경로 표시 시작
		isShowingPath = true;
		pathShowStartTime = System.currentTimeMillis();
	}


	/**
	 * 타겟 방향으로 벽까지 돌진
	 */
	private void dashToTarget() {
		// 방향으로 이동
		this.positionX += dashTargetX * DASH_SPEED;
		this.positionY += dashTargetY * DASH_SPEED;

		// 벽에 닿으면 돌진 종료
		if (this.positionX <= 0 || this.positionX >= widthBoundary ||
				this.positionY <= 0 || this.positionY >= bottomBoundary) {
			isDashing = false;
			// 돌진 종료 후 패턴1로 복귀
			this.pattern = 1;
			this.color = super.color;
			this.spriteType = DrawManager.SpriteType.OmegaBoss1;
			logger.info("OMEGA : Dash completed, hit the wall");
		}
	}

	/** Marks the entity as destroyed and changes its sprite to an explosion. */
	@Override
	public void destroy() {
		this.isDestroyed = true;
		this.spriteType = DrawManager.SpriteType.OmegaBossDeath;
		this.logger.info("OMEGA : Boss OMEGA destroyed!");
	}

	/**
	 * Reduces health and destroys the entity if it drops to zero or below.
	 *
	 * @param damage The amount of damage to inflict.
	 */
	@Override
	public void takeDamage(int damage) {
		this.healPoint -= damage;
	}

	/**
	 * Updates the entity's state for the current game frame.
	 * This method is called on every tick of the game loop and is responsible for
	 * executing the boss's movement patterns.
	 */
	@Override
	public void update() {
		this.movePatterns();
	}
}
