package arkanoid;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class Game extends Pane {
    // Core components
    private final double width;
    private final double height;
    private final Canvas canvas;
    private final GraphicsContext gc;
    private final Paddle paddle;
    private final Runnable returnToMenuCallback;
    private final Runnable returnToLevelSelectCallback;
    private long lastTime = 0;
    private Boss boss;
    private boolean bossLevel = false;

    // Các Trình quản lý (Managers)
    private final GameState gameState;
    private final EntityManager entityManager;
    private final GameRenderer gameRenderer;
    private final PowerUpManager powerUpManager;
    private final CollisionManager collisionManager;
    private Runnable onPauseCallback; // Sẽ được gọi khi game pause
    private Runnable onResumeCallback; // Sẽ được gọi khi game resume

    public GameState getGameState() {
        return this.gameState;
    }

    /**
     * Đăng ký một hành động (Runnable) để chạy khi game vào trạng thái Pause.
     * GameContainer sẽ sử dụng hàm này.
     */
    public void setOnPause(Runnable r) {
        this.onPauseCallback = r;
    }

    /**
     * Đăng ký một hành động (Runnable) để chạy khi game thoát trạng thái Pause (resume).
     * GameContainer sẽ sử dụng hàm này.
     */
    public void setOnResume(Runnable r) {
        this.onResumeCallback = r;
    }

    /**
     * Public hàm để hủy bỏ việc thoát game (nhấn "No").
     * Sẽ xóa cờ 'confirm' và giữ game ở trạng thái pause
     * (trigger callback để GameContainer hiển thị lại menu pause).
     */
    public void cancelQuit() {
        gameState.setConfirmOverlay(false);
        // Game vẫn đang pause, trigger lại callback để UI cập nhật
        if (onPauseCallback != null) {
            onPauseCallback.run();
        }
    }

    /**
     * Public hàm để quay về Menu.
     * GameContainer sẽ gọi hàm này khi nhấn nút Menu.
     */
    public void returnToMenu() {
        gameState.setLevelComplete(false); // Reset cờ khi về home
        gameState.setGameComplete(false); // Reset cờ khi về home
        pause(); // Đảm bảo game dừng
        if (returnToMenuCallback != null) {
            returnToMenuCallback.run();
        }
    }

    /**
     * Public hàm để chơi lại màn hiện tại.
     * GameContainer sẽ gọi hàm này khi nhấn nút Restart.
     */
    public void restartCurrentLevel() {
        gameState.setLevelComplete(false); // Reset cờ khi restart
        gameState.setGameComplete(false); // Reset cờ khi restart
        // Tải lại level hiện tại dựa trên index
        int currentLevel = gameState.getCurrentLevelIndex();
        switch (currentLevel) {
            case 0: startNewGame(); break; // Level 1
            case 1: startLevel2(); break;
            case 2: startLevel3(); break;
            case 3: startLevel4(); break;
            case 4: startLevel5(); break;
            case 5: startLevel6(); break;
            default: startNewGame(); // Mặc định
        }
        resume();
    }

    // Level data
    private final String[] levelFiles = new String[] {
            "/levels/level1.txt",
            "/levels/level2.txt",
            "/levels/level3.txt",
            "/levels/level4.txt",
            "/levels/level5.txt",
            "/levels/level6.txt"
    };

    // Cooldown bắn
    private double shootCooldown = 0.25;
    private double timeSinceLastShot = 0.0;

    public Game(double w, double h, Runnable returnToMenuCallback, Runnable returnToLevelSelectCallback) {
        this.width = w; this.height = h;
        this.returnToMenuCallback = returnToMenuCallback;
        this.returnToLevelSelectCallback = returnToLevelSelectCallback;

        canvas = new Canvas(width, height);
        gc = canvas.getGraphicsContext2D();
        getChildren().add(canvas);

        // Khởi tạo đối tượng cốt lõi
        paddle = new Paddle((width - 120) / 2, height - 40, 120, 16, width);

        // Khởi tạo các Manager
        this.gameState = new GameState();
        this.entityManager = new EntityManager();
        this.gameRenderer = new GameRenderer(gc, width, height);
        this.powerUpManager = new PowerUpManager();
        this.collisionManager = new CollisionManager();

        // Tải level 0 (nhưng chưa chạy)
        loadLevel(0);
        createNewBall(); // Tạo bóng, bóng sẽ dính vào paddle

        // Cài đặt Input
        setFocusTraversable(true);
        setOnKeyPressed(e -> handleKeyPressed(e.getCode()));
        setOnKeyReleased(e -> paddle.release(e.getCode()));
        setOnMousePressed(e -> {
            if (!gameState.isRunning()) return;
            if (paddle.hasLaser()) tryShoot();
        });

        // Cài đặt Game Loop
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastTime == 0) lastTime = now;
                double deltaTime = (now - lastTime) / 1e9; // giây
                lastTime = now;

                if (gameState.isRunning()) {
                    update(deltaTime);
                }
                render(); // Vẫn render kể cả khi không chạy (để vẽ overlay)
            }
        };
        timer.start();
    }

    public void startNewGame() {
        boss = null;         // Xóa boss cũ
        bossLevel = false;   // Tắt cờ boss level
        gameState.resetForNewGame();
        entityManager.clearAll();
        paddle.setWidth(120); // Reset paddle width
        paddle.setHasLaser(false); // Reset laser
        loadLevel(0);
        createNewBall();
        resume();
    }

    public void startLevel2() {
        boss = null;         // Xóa boss cũ
        bossLevel = false;   // Tắt cờ boss level
        gameState.resetForNewGame();
        entityManager.clearAll();
        paddle.setWidth(120); // Reset paddle width
        paddle.setHasLaser(false); // Reset laser
        loadLevel(1);
        createNewBall();
        resume();
    }

    public void startLevel3() {
        boss = null;         // Xóa boss cũ
        bossLevel = false;   // Tắt cờ boss level
        gameState.resetForNewGame();
        entityManager.clearAll();
        paddle.setWidth(120); // Reset paddle width
        paddle.setHasLaser(false); // Reset laser
        loadLevel(2);
        createNewBall();
        resume();
    }

    public void startLevel4() {
        boss = null;         // Xóa boss cũ
        bossLevel = false;   // Tắt cờ boss level
        gameState.resetForNewGame();
        entityManager.clearAll();
        paddle.setWidth(120); // Reset paddle width
        paddle.setHasLaser(false); // Reset laser
        loadLevel(3);
        createNewBall();
        resume();
    }


    public void startLevel5() {
        boss = null;         // Xóa boss cũ
        bossLevel = false;   // Tắt cờ boss level
        gameState.resetForNewGame();
        entityManager.clearAll();
        paddle.setWidth(120); // Reset paddle width
        paddle.setHasLaser(false); // Reset laser
        loadLevel(4);
        createNewBall();
        resume();
    }

    public void startLevel6() {
        gameState.resetForNewGame();
        entityManager.clearAll();
        paddle.setWidth(120);
        paddle.setHasLaser(false);
        loadLevel(5);
        createNewBall();
        resume();
    }

    public void resume() {
        gameState.setLevelComplete(false); // Luôn reset cờ khi resume
        gameState.setGameComplete(false); // Luôn reset cờ khi resume
        gameState.setConfirmOverlay(false); // Đảm bảo cờ confirm tắt khi resume
        if (!gameState.isGameStarted()) {
            startNewGame();
            return;
        }

        if (onResumeCallback != null) {
            onResumeCallback.run(); // Thông báo cho container ẩn menu
        }
        gameState.setPauseOverlay(false);
        gameState.setConfirmOverlay(false);
        gameState.setRunning(true);
    }

    public void pause() {
        gameState.setRunning(false);
        gameState.setPauseOverlay(true);
        if (onPauseCallback != null) {
            onPauseCallback.run(); // Thông báo cho container hiện menu
        }
    }

    public boolean isGameStarted() {
        return gameState.isGameStarted();
    }

    // Vòng lặp Update chính (đã gọn gàng hơn)
    private void update(double dt) {
        timeSinceLastShot += dt;

        // 1. Cập nhật vị trí các đối tượng
        paddle.update(dt);
        entityManager.updateAll(dt); // Cập nhật đạn, power-up rơi, HUD...
        for (Ball b : entityManager.getBalls()) {
            b.update(dt);
        }

        // 2. Xử lý Va chạm
        CollisionResult collisionResult = collisionManager.handleCollisions(
                entityManager, gameState, paddle, powerUpManager, width, height
        );

        // 3. Xử lý các Sự kiện từ va chạm
        // 3a. Xử lý nhặt PowerUp
        for (PowerUp pu : collisionResult.collectedPowerUps) {
            if (pu.type == PowerUp.PowerType.NEXT_LEVEL) {
                nextLevel(); // Xử lý trường hợp đặc biệt
            } else {
                powerUpManager.applyPowerUp(pu.type, entityManager, gameState, paddle, height);
            }
        }

        // 3b. Xử lý Mất bóng
        if (collisionResult.ballLost) {
            handleBallLost();
        }

        // 3c. Xử lý Qua màn
        if (collisionResult.allDestructibleBricksCleared) {
            boolean bossDead = !bossLevel || (boss != null && boss.isDead());
            if (bossDead) {
                nextLevel();
            }
        }

        // cập nhật gạch di chuyển và không đè lên nhau
        List<Brick> bricks = entityManager.getBricks();
        for (int i = 0; i < bricks.size(); i++) {
            Brick brick = bricks.get(i);
            if (brick instanceof MovingBrick mb) {
                mb.update(dt);
                // Giới hạn biên
                if (mb.getX() <= 0) {
                    mb.setX(0);
                    mb.setDirection(1);
                } else if (mb.getX() + mb.getWidth() >= width) {
                    mb.setX(width - mb.getWidth());
                    mb.setDirection(-1);
                }

                // Kiểm tra va chạm với các gạch khác
                for (int j = i + 1; j < bricks.size(); j++) {
                    Brick other = bricks.get(j);
                    if (other instanceof MovingBrick ob) {
                        if (mb.getBounds().intersects(ob.getBounds())) {
                            // Hai viên gạch chạm nhau → ép sát rồi đổi hướng
                            if (mb.getX() < ob.getX()) {
                                mb.setX(ob.getX() - mb.getWidth());
                                mb.setDirection(-1);
                                ob.setDirection(1);
                            } else {
                                mb.setX(ob.getX() + ob.getWidth());
                                mb.setDirection(1);
                                ob.setDirection(-1);
                            }
                        }
                    }
                }
            }
        }


        // Cập nhật boss
        if (bossLevel && boss != null) {
            boss.update(dt);

            for (Ball b : entityManager.getBalls()) {
                Boss.CollisionSide side = boss.checkCollision(b);
                if (side != Boss.CollisionSide.NONE) {
                    boss.takeDamage(5);
                    switch (side) {
                        case LEFT -> {
                            b.reverseX();
                            b.setX(boss.getX() - b.getRadius());
                        }
                        case RIGHT -> {
                            b.reverseX();
                            b.setX(boss.getX() + boss.getWidth() + b.getRadius());
                        }
                        case TOP -> {
                            b.reverseY();
                            b.setY(boss.getY() - b.getRadius());
                        }
                        case BOTTOM -> {
                            b.reverseY();
                            b.setY(boss.getY() + boss.getHeight() + b.getRadius());
                        }
                    }
                }
            }


            //Đạn boss trúng paddle
            List<BossBullet> toRemove = new ArrayList<>();
            boolean bossHitThisFrame = false;

            for (BossBullet bullet : boss.getBullets()) {
                if (bullet.collidesWith(paddle)) {
                    bossHitThisFrame = true;   // ghi nhận có ít nhất 1 viên trúng
                    toRemove.add(bullet);      // xóa các viên trúng để tránh trừ tiếp ở frame sau
                }
            }

            boss.getBullets().removeAll(toRemove);

            // Chỉ trừ 1 mạng cho frame này, dù có nhiều viên trúng
            if (bossHitThisFrame) {
                handleBallLost();
            }

        }

        // 4. Cập nhật các Hiệu ứng (kiểm tra hết hạn)
        powerUpManager.updateActiveEffects(dt, entityManager, gameState, paddle);
    }

    // Vòng lặp Render chính
    private void render() {
        gameRenderer.render(gameState, entityManager, paddle, boss, bossLevel);
    }

    // --- Logic Luồng Game (Game Flow) ---

    private void handleBallLost() {
        gameState.decrementLives();
        if (gameState.getLives() <= 0) {
            // Game Over
            gameState.setRunning(false);
            gameState.setWin(false);
            gameState.setShowMessage(true);
            pause();
        } else {
            // Mất 1 mạng, tạo bóng mới
            createNewBall();
        }
    }

    public void loadNextLevel() {
        gameState.setLevelComplete(false); // Reset cờ
        int nextLevelIndex = gameState.getCurrentLevelIndex() + 1;

        if (nextLevelIndex >= levelFiles.length) {
            gameState.setGameComplete(true); // Đặt cờ
            pause(); // Dừng game (sẽ trigger UI trong GameContainer)
        } else {
            // Tải level tiếp theo (dựa trên switch)
            switch (nextLevelIndex) {
                case 1: startLevel2(); break;
                case 2: startLevel3(); break;
                case 3: startLevel4(); break;
                case 4: startLevel5(); break;
                case 5: startLevel6(); break;
                default: startNewGame();
            }
            resume(); // Bắt đầu màn mới
        }
    }

    /**
     * Hàm nextLevel() cũ giờ chỉ có nhiệm vụ set cờ và Pause
     */
    private void nextLevel() {
        // Kiểm tra xem đây có phải màn cuối không
        if (gameState.getCurrentLevelIndex() >= levelFiles.length - 1) {
            // Đây là màn cuối (Level 6)
            gameState.setGameComplete(true);
        } else {
            // Đây chỉ là thắng màn thường
            gameState.setLevelComplete(true); // Đặt cờ
        }
        pause(); // Dừng game (sẽ trigger onPauseCallback trong GameContainer)
    }

    // Tạo bóng mới và dán vào paddle
    private void createNewBall() {
        entityManager.clearBalls(); // Xóa bóng cũ (nếu có)
        Ball b = new Ball(paddle.getX() + paddle.getWidth() / 2, paddle.getY() - 10, 8, paddle);
        // Áp dụng các hiệu ứng (như tiny ball) nếu chúng còn hoạt động
        powerUpManager.applyActiveEffectsToBall(b, entityManager.getActiveEffects());
        entityManager.addBall(b);
        b.resetToPaddle();
    }

    // Tải dữ liệu gạch từ file
    private void loadLevel(int levelIndex) {
        if (levelIndex < 0 || levelIndex >= levelFiles.length) {
            return;
        }
        try {
            LevelData ld = LevelLoader.loadLevel(levelFiles[levelIndex], width);
            entityManager.clearBricks(); // Xóa gạch cũ
            entityManager.getBricks().addAll(ld.bricks);
            gameState.setCurrentLevelIndex(levelIndex);
            //Tạo boss
            if (ld.hasBoss) {
                boss = new Boss(width / 2 - 100, 50, 200, 60, 100, width - 30);
                bossLevel = true;
            }

        } catch (IOException ex) {
            System.err.println("Failed to load level: " + levelFiles[levelIndex]);
            // (Có thể thêm logic tạo level mặc định ở đây)
        }
    }

    // Bắn đạn
    private void tryShoot() {
        if (!paddle.hasLaser() || timeSinceLastShot < shootCooldown) return;
        timeSinceLastShot = 0.0;
        double[] pos = paddle.getLaserGunPositions();
        entityManager.addBullet(new Bullet(pos[0], pos[1]));
        entityManager.addBullet(new Bullet(pos[2], pos[3]));
    }

    // Xử lý Input

    private void handleKeyPressed(KeyCode code) {
        // Xử lý khi game đang dừng (Game Over / Win)
        if (!gameState.isRunning()) {
            if (gameState.isPauseOverlay()) {
                if (code == KeyCode.P) resume();
                else if (code == KeyCode.M) { // 'M' để về Menu
                    returnToMenu();
                }
                return;
            }
            return;
        }

        // Xử lý input khi game đang chạy
        switch (code) {
            case SPACE :
                if (paddle.hasLaser()) tryShoot();
                for (Ball bl : entityManager.getBalls()) {
                    if (bl.isStuck()) bl.launch();
                }
                break;
            case P:
                if (gameState.isRunning()) pause();
                break;
            case ESCAPE:
                if (gameState.isRunning()) {
                    gameState.setConfirmOverlay(true); // Đặt cờ
                    pause(); // Dừng game VÀ trigger onPauseCallback
                }
                break;
            default:
                // Các phím di chuyển
                paddle.press(code);
                break;
        }
    }

    /**
     * Chuyển đổi trạng thái Pause/Resume.
     * Được gọi bởi nút bấm UI bên ngoài (từ GameContainer).
     */
    public void togglePause() {
        if (gameState.isRunning()) {
            pause(); // Nếu đang chạy -> Dừng
        } else if (gameState.isPauseOverlay()) {
            resume(); // Nếu đang dừng (ở màn hình Pause) -> Tiếp tục
        }
        // Nếu đang ở màn hình Confirm (Y/N) hoặc Game Over -> Không làm gì

        this.requestFocus(); // Yêu cầu focus lại Pane game để nhận phím
    }

    /**
     * Mở cài đặt (tạm thời chỉ pause và in ra console).
     * Được gọi bởi nút bấm UI bên ngoài (từ GameContainer).
     */
    public void openSettings() {
        if (gameState.isRunning()) {
            pause(); // Dừng game
            // (Sau này sẽ thêm logic mở cửa sổ settings)
            System.out.println("Setting clicked - Not implemented");
        }
        // Nếu game đã pause, không làm gì thêm (tránh gọi onPauseCallback nhiều lần)

        this.requestFocus(); // Yêu cầu focus lại
    }
}
