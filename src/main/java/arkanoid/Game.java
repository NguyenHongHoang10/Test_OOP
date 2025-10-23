package arkanoid;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import java.io.IOException;

public class Game extends Pane {
    // Core components
    private final double width;
    private final double height;
    private final Canvas canvas;
    private final GraphicsContext gc;
    private final Paddle paddle;
    private final Runnable returnToMenuCallback;
    private long lastTime = 0;

    // Các Trình quản lý (Managers)
    private final GameState gameState;
    private final EntityManager entityManager;
    private final GameRenderer gameRenderer;
    private final PowerUpManager powerUpManager;
    private final CollisionManager collisionManager;

    // Level data
    private final String[] levelFiles = new String[] { "/levels/level1.txt", "/levels/level2.txt", "/levels/level3.txt" };

    // Cooldown bắn
    private double shootCooldown = 0.25;
    private double timeSinceLastShot = 0.0;

    public Game(double w, double h, Runnable returnToMenuCallback) {
        this.width = w; this.height = h;
        this.returnToMenuCallback = returnToMenuCallback;

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
        gameState.resetForNewGame();
        entityManager.clearAll();
        paddle.setWidth(120); // Reset paddle width
        paddle.setHasLaser(false); // Reset laser
        loadLevel(0);
        createNewBall();
    }

    public void resume() {
        if (!gameState.isGameStarted()) {
            startNewGame();
            return;
        }
        gameState.setPauseOverlay(false);
        gameState.setConfirmOverlay(false);
        gameState.setRunning(true);
    }

    public void pause() {
        gameState.setRunning(false);
        gameState.setPauseOverlay(true);
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
            nextLevel();
        }

        // 4. Cập nhật các Hiệu ứng (kiểm tra hết hạn)
        powerUpManager.updateActiveEffects(dt, entityManager, gameState, paddle);
    }

    // Vòng lặp Render chính
    private void render() {
        gameRenderer.render(gameState, entityManager, paddle);
    }

    // --- Logic Luồng Game (Game Flow) ---

    private void handleBallLost() {
        gameState.decrementLives();
        if (gameState.getLives() <= 0) {
            // Game Over
            gameState.setRunning(false);
            gameState.setWin(false);
            gameState.setShowMessage(true);
        } else {
            // Mất 1 mạng, tạo bóng mới
            createNewBall();
        }
    }

    private void nextLevel() {
        gameState.incrementLevelIndex();
        if (gameState.getCurrentLevelIndex() >= levelFiles.length) {
            // Thắng toàn bộ game
            gameState.setRunning(false);
            gameState.setWin(true);
            gameState.setShowMessage(true);
        } else {
            // Tải level tiếp theo
            loadLevel(gameState.getCurrentLevelIndex());
            createNewBall(); // Tạo bóng mới cho màn mới
        }
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

    // --- Xử lý Input ---

    private void handleKeyPressed(KeyCode code) {
        // Xử lý Confirm (Y/N)
        if (gameState.isConfirmOverlay()) {
            if (code == KeyCode.N) {
                gameState.setConfirmOverlay(false);
                gameState.setRunning(true); // Tiếp tục game
            } else if (code == KeyCode.Y) {
                Platform.exit(); // Thoát game
            }
            return;
        }

        // Xử lý Pause (P / O)
        if (gameState.isPauseOverlay()) {
            if (code == KeyCode.P) resume();
            else if (code == KeyCode.O) { // 'O' để về Menu
                pause(); // Đảm bảo game dừng
                returnToMenuCallback.run();
            }
            return;
        }

        // Xử lý khi game đang dừng (Game Over / Win)
        if (!gameState.isRunning()) {
            if (code == KeyCode.S) startNewGame();
            else if (code == KeyCode.R) returnToMenuCallback.run();
            return; // Không xử lý input game khi đang dừng
        }

        // Xử lý input khi game đang chạy
        switch (code) {
            case SPACE:
                if (paddle.hasLaser()) tryShoot();
                for (Ball bl : entityManager.getBalls()) {
                    if (bl.isStuck()) bl.launch();
                }
                break;
            case P:
                if (gameState.isRunning()) pause();
                else resume();
                break;
            case ESCAPE:
                gameState.setConfirmOverlay(true);
                gameState.setRunning(false);
                break;
            default:
                // Các phím di chuyển
                paddle.press(code);
                break;
        }
    }
}