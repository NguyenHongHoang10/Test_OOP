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

    // Thêm Save/Load + HighScore
    private final SaveManager saveManager;
    private final HighScoreManager highScoreManager;
    private double autosaveTimer = 0;                 // bộ đếm autosave định kỳ

    // Level data
    private final String[] levelFiles = new String[] { "/levels/level1.txt", "/levels/level2.txt", "/levels/level3.txt" };

    // Cooldown bắn
    private double shootCooldown = 0.25;
    private double timeSinceLastShot = 0.0;

    public Game(double w, double h, Runnable returnToMenuCallback) {
        // Giữ nguyên Constructor cũ để tương thích ngược
        this(w, h, returnToMenuCallback, null, null);
    }

    // Thêm Constructor mở rộng có SaveManager/HighScoreManager
    public Game(double w, double h, Runnable returnToMenuCallback, SaveManager saveManager, HighScoreManager highScoreManager) {
        this.width = w; this.height = h;
        this.returnToMenuCallback = returnToMenuCallback;
        this.saveManager = saveManager;
        this.highScoreManager = highScoreManager;

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

        // Đồng bộ highScore ngay từ đầu (để HUD có số liệu đúng nếu cần)
        syncHighScoreFromManager();

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

    // Bắt đầu game với tên người chơi
    public void startNewGame(String playerName) {
        gameState.setPlayerName(playerName);
        startNewGame();
        // lưu ngay bản lưu khởi tạo
        autosaveImmediate();
    }

    public void startNewGame() {
        gameState.resetForNewGame();
        entityManager.clearAll();
        paddle.setWidth(120); // Reset paddle width
        paddle.setHasLaser(false); // Reset laser
        paddle.press(KeyCode.A); // Đảm bảo nhận focus input
        paddle.release(KeyCode.A);

        loadLevel(0);
        createNewBall();

        // Đồng bộ highScore để HUD hiển thị đúng ngay khi vào trận
        syncHighScoreFromManager();
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
        // Autosave khi pause
        autosaveImmediate();
    }

    public boolean isGameStarted() {
        return gameState.isGameStarted();
    }

    // Load từ SaveManager; trả true nếu thành công
    public boolean loadFromSave() {
        if (saveManager == null || !saveManager.hasSave()) return false;
        GameSnapshot s = saveManager.load();
        if (s == null) return false;

        // Áp trạng thái chung
        gameState.setPlayerName(s.playerName);
        gameState.setLives(s.lives);
        gameState.setScore(s.score);
        gameState.setScoreMultiplier(s.scoreMultiplier);
        gameState.setCurrentLevelIndex(s.currentLevelIndex);
        gameState.setBarrierActive(s.barrierActive, s.barrierY);
        gameState.setGameStarted(true);
        gameState.setRunning(true);
        gameState.setShowMessage(false);
        gameState.setPauseOverlay(false);
        gameState.setConfirmOverlay(false);

        // Nạp bricks từ snapshot (bỏ qua LevelLoader)
        entityManager.clearAll();
        for (GameSnapshot.BrickData bd : s.bricks) {
            Brick.Type tp;
            switch (bd.type) {
                case "INDESTRUCTIBLE": tp = Brick.Type.INDESTRUCTIBLE; break;
                case "EXPLOSIVE": tp = Brick.Type.EXPLOSIVE; break;
                default: tp = Brick.Type.NORMAL;
            }
            Brick brick = new Brick(bd.x, bd.y, bd.w, bd.h, tp, bd.hits);
            entityManager.addBrick(brick);
        }

        // Paddle
        paddle.setWidth(s.paddleWidth);
        /* Kéo paddle về vị trí snapshot
         x,y của paddle trong GameObject là protected,
         nhưng có getter để set gián tiếp bằng cách dịch chuyển
         Chỉ cần x do y cố định gần đáy */
        try {
            java.lang.reflect.Field fx = GameObject.class.getDeclaredField("x");
            fx.setAccessible(true);
            fx.set(paddle, s.paddleX);
            java.lang.reflect.Field fy = GameObject.class.getDeclaredField("y");
            fy.setAccessible(true);
            fy.set(paddle, s.paddleY);
        } catch (Exception ignored) { /* an toàn bỏ qua nếu phản chiếu thất bại */ }
        paddle.setHasLaser(s.paddleHasLaser);

        // Balls
        for (GameSnapshot.BallData bd : s.balls) {
            Ball b = new Ball(bd.cx, bd.cy, bd.radius, paddle);
            b.setBaseSpeed(bd.baseSpeed);
            b.setVelocity(bd.vx, bd.vy);
            b.setStuck(bd.stuckToPaddle);
            b.setFireball(bd.fireball);
            entityManager.addBall(b);
        }
        // Nếu không có bóng trong snapshot thì tạo mới để tránh lỗi
        if (entityManager.getBalls().isEmpty()) {
            createNewBall();
        }

        // PowerUps
        for (GameSnapshot.PowerUpData pd : s.powerUps) {
            PowerUp.PowerType type = PowerUp.PowerType.valueOf(pd.type);
            entityManager.addPowerUp(new PowerUp(pd.cx, pd.cy, type));
        }

        // Bullets
        for (GameSnapshot.BulletData bd : s.bullets) {
            entityManager.addBullet(new Bullet(bd.cx, bd.cy));
        }

        // ActiveEffects: khôi phục kèm giá trị gốc theo chỉ số bóng
        for (GameSnapshot.EffectData ed : s.effects) {
            ActiveEffect ae = new ActiveEffect(PowerUp.PowerType.valueOf(ed.type), ed.remaining);
            ae.originalPaddleWidth = ed.originalPaddleWidth;
            ae.originalHasLaser = ed.originalHasLaser;
            ae.originalScoreMultiplier = ed.originalScoreMultiplier;

            // map theo index -> Ball
            for (GameSnapshot.IntDouble pair : ed.originalSpeeds) {
                int idx = pair.index;
                if (idx >= 0 && idx < entityManager.getBalls().size()) {
                    ae.originalSpeeds.put(entityManager.getBalls().get(idx), pair.value);
                }
            }
            for (GameSnapshot.IntDouble pair : ed.originalRadii) {
                int idx = pair.index;
                if (idx >= 0 && idx < entityManager.getBalls().size()) {
                    ae.originalRadii.put(entityManager.getBalls().get(idx), pair.value);
                }
            }
            for (GameSnapshot.IntBool pair : ed.originalFireball) {
                int idx = pair.index;
                if (idx >= 0 && idx < entityManager.getBalls().size()) {
                    ae.originalFireball.put(entityManager.getBalls().get(idx), pair.value);
                }
            }
            entityManager.addActiveEffect(ae);
        }

        // Đồng bộ highScore để HUD hiển thị đúng sau khi load
        syncHighScoreFromManager();

        return true;
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

        // 5. Autosave định kỳ khi đang chạy (mỗi 1 giây)
        autosaveTimer += dt;
        if (autosaveTimer >= 1.0) {
            autosaveImmediate();
            autosaveTimer = 0;
        }

        // Cập nhật highScore trong HUD là max(highScore hiện tại, score đang chơi)
        // (chỉ hiển thị; ghi file thật sự thực hiện ở onGameEnd)
        if (gameState.getScore() > gameState.getHighScore()) {
            gameState.setHighScore(gameState.getScore());
        }
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
            onGameEnd(); // Cập nhật HighScore + dọn save
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
            onGameEnd(); // Cập nhật HighScore + dọn save
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
                // Autosave trước khi thoát (đảm bảo Continue)
                autosaveImmediate();
                Platform.exit(); // Thoát game
            }
            return;
        }

        // Xử lý Pause (P / O)
        if (gameState.isPauseOverlay()) {
            if (code == KeyCode.P) resume();
            else if (code == KeyCode.O) { // 'O' để về Menu
                pause(); // Đảm bảo game dừng
                // Autosave khi về menu
                autosaveImmediate();
                returnToMenuCallback.run();
            }
            return;
        }

        // Xử lý khi game đang dừng (Game Over / Win)
        if (!gameState.isRunning()) {
            if (code == KeyCode.S) {
                startNewGame(gameState.getPlayerName()); // Giữ lại tên hiện tại
            }
            else if (code == KeyCode.R) {
                // Trước khi quay menu, dọn save của ván đã kết thúc
                if (saveManager != null) saveManager.deleteSave();
                returnToMenuCallback.run();
            }
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

    // Tạo snapshot để lưu (Save)
    private GameSnapshot createSnapshot() {
        GameSnapshot s = new GameSnapshot();
        // Thông tin chung
        s.playerName = gameState.getPlayerName();
        s.lives = gameState.getLives();
        s.score = gameState.getScore();
        s.scoreMultiplier = gameState.getScoreMultiplier();
        s.currentLevelIndex = gameState.getCurrentLevelIndex();
        s.barrierActive = gameState.isBarrierActive();
        s.barrierY = gameState.getBarrierY();

        // Paddle
        s.paddleX = paddle.getX();
        s.paddleY = paddle.getY();
        s.paddleWidth = paddle.getWidth();
        s.paddleHeight = paddle.getHeight();
        s.paddleHasLaser = paddle.hasLaser();

        // Balls
        for (int i = 0; i < entityManager.getBalls().size(); i++) {
            Ball b = entityManager.getBalls().get(i);
            GameSnapshot.BallData bd = new GameSnapshot.BallData();
            bd.cx = b.centerX();
            bd.cy = b.centerY();
            bd.radius = b.getRadius();
            bd.vx = b.getVx(); // [THÊM] đã bổ sung getter
            bd.vy = b.getVy();
            bd.baseSpeed = b.getBaseSpeed();
            bd.stuckToPaddle = b.isStuck();
            bd.fireball = b.isFireball();
            s.balls.add(bd);
        }

        // Bricks
        for (Brick br : entityManager.getBricks()) {
            GameSnapshot.BrickData bd = new GameSnapshot.BrickData();
            bd.x = br.getX(); bd.y = br.getY();
            bd.w = br.getWidth(); bd.h = br.getHeight();
            bd.type = br.getType().name();
            bd.hits = br.getHits();
            s.bricks.add(bd);
        }

        // PowerUps
        for (PowerUp pu : entityManager.getPowerUps()) {
            GameSnapshot.PowerUpData pd = new GameSnapshot.PowerUpData();
            pd.cx = pu.x; pd.cy = pu.y;
            pd.type = pu.type.name();
            s.powerUps.add(pd);
        }

        // Bullets
        for (Bullet bu : entityManager.getBullets()) {
            GameSnapshot.BulletData bd = new GameSnapshot.BulletData();
            bd.cx = bu.x + bu.w/2.0; // lưu theo tâm
            bd.cy = bu.y + bu.h/2.0;
            s.bullets.add(bd);
        }

        // ActiveEffects – lưu cả giá trị gốc
        for (ActiveEffect ae : entityManager.getActiveEffects()) {
            GameSnapshot.EffectData ed = new GameSnapshot.EffectData();
            ed.type = ae.type.name();
            ed.remaining = ae.remaining;
            ed.originalPaddleWidth = ae.originalPaddleWidth;
            ed.originalHasLaser = ae.originalHasLaser;
            ed.originalScoreMultiplier = ae.originalScoreMultiplier;

            // map Ball->index
            for (int idx = 0; idx < entityManager.getBalls().size(); idx++) {
                Ball b = entityManager.getBalls().get(idx);
                if (ae.originalSpeeds.containsKey(b)) {
                    ed.originalSpeeds.add(new GameSnapshot.IntDouble(idx, ae.originalSpeeds.get(b)));
                }
                if (ae.originalRadii.containsKey(b)) {
                    ed.originalRadii.add(new GameSnapshot.IntDouble(idx, ae.originalRadii.get(b)));
                }
                if (ae.originalFireball.containsKey(b)) {
                    ed.originalFireball.add(new GameSnapshot.IntBool(idx, ae.originalFireball.get(b)));
                }
            }
            s.effects.add(ed);
        }

        return s;
    }

    // Lưu ngay snapshot (dùng khi pause/confirm/định kỳ)
    private void autosaveImmediate() {
        if (saveManager == null) return;
        // Không lưu nếu đang ở màn kết thúc
        if (gameState.isShowMessage()) return;
        saveManager.save(createSnapshot());
    }

    // Khi trận đấu kết thúc: cập nhật HighScore + xóa save (không Continue vào ván đã kết thúc)
    private void onGameEnd() {
        if (highScoreManager != null) {
            if (highScoreManager.isTop10Candidate(gameState.getScore())) {
                highScoreManager.submitScore(gameState.getPlayerName(), gameState.getScore());
            }
            // Đồng bộ lại highScore sau khi ghi nhận điểm để HUD/menu luôn mới nhất
            syncHighScoreFromManager();
        }
        if (saveManager != null) {
            saveManager.deleteSave();
        }
    }

    // Đọc Top1 từ HighScoreManager và đặt vào GameState để HUD vẽ "High"
    private void syncHighScoreFromManager() {
        if (highScoreManager == null) return;
        int top = 0;
        for (HighScoreManager.Entry e : highScoreManager.getTop10()) {
            if (e.score > top) top = e.score;
        }
        gameState.setHighScore(top);
    }
}
