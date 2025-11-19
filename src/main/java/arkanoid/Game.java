package arkanoid;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class Game extends Pane {
    // Thành phần cốt lõi
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

    // Các Trình quản lý
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

    // Chạy khi game vào trạng thái Pause.
    public void setOnPause(Runnable r) {
        this.onPauseCallback = r;
    }

    // Chạy khi game thoát trạng thái Pause (resume).
    public void setOnResume(Runnable r) {
        this.onResumeCallback = r;
    }

    // Public hàm để hủy thoát game.
    public void cancelQuit() {
        gameState.setConfirmOverlay(false);
        if (onPauseCallback != null) {
            onPauseCallback.run();
        }
    }

    // Public hàm để về Menu chính.
    public void returnToMenu() {
        SoundManager.get().play(SoundManager.Sfx.PAUSE);

        gameState.setLevelComplete(false); // Reset cờ khi về home
        gameState.setGameComplete(false); // Reset cờ khi về home
        pause(); // Đảm bảo game dừng
        if (returnToMenuCallback != null) {
            returnToMenuCallback.run();
            SoundManager.get().stopLoop(SoundManager.Sfx.FIRE_LOOP);
            SoundManager.get().stopBgm();
        }
        SoundManager.get().startBgm(SoundManager.Bgm.MENU);

    }

    // Public hàm để về Level Select.
    public void restartCurrentLevel() {
        SoundManager.get().stopLoop(SoundManager.Sfx.FIRE_LOOP);
        SoundManager.get().play(SoundManager.Sfx.PAUSE);

        // Tải lại level hiện tại dựa trên index
        int currentLevel = gameState.getCurrentLevelIndex();
        gameState.setLevelComplete(false);
        gameState.setGameComplete(false);
        gameState.setScore(0);
        gameState.setLives(3);
        gameState.resetForNewGame();
        paddle.clearKeys();

        switch (currentLevel) {
            case 0:
                startNewGame(0);
                break; // Level 1
            case 1:
                startNewGame(1);
                break;
            case 2:
                startNewGame(2);
                break;
            case 3:
                startNewGame(3);
                break;
            case 4:
                startNewGame(4);
                break;
            case 5:
                startLevel6();
                break;
            default:
                startNewGame(0); // Mặc định
        }
        resume();
    }

    // Level data
    private final String[] levelFiles = new String[]{
            "/levels/level2.txt",
            "/levels/level3.txt",
            "/levels/level1.txt",
            "/levels/level4.txt",
            "/levels/level5.txt",
            "/levels/level6.txt"
    };

    // Cooldown bắn
    private double shootCooldown = 0.25;
    private double timeSinceLastShot = 0.0;

    public enum WarpStyle {FADE_IN, SCALE_UP, RIPPLE_LTR, RIPPLE_CENTER}

    private boolean warpInProgress = false;

    // Vệt khói của paddle khi di chuyển
    private double lastPaddleX = 0.0;
    private double paddleTrailTimer = 0.0;
    private final double PADDLE_TRAIL_INTERVAL = 0.02;   // sinh 1 hạt mỗi 0.03s
    private final double PADDLE_TRAIL_MIN_SPEED = 60.0;  // px/s (ngưỡng để bắt đầu sinh trail)

    public Game(double w, double h, Runnable returnToMenuCallback, Runnable returnToLevelSelectCallback) {
        this.width = w;
        this.height = h;
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
                render(); // Vẫn render kể cả khi không chạy để vẽ overlay
            }
        };
        timer.start();
    }

    public void startNewGame(int levelIndex) {
        boss = null;         // Xóa boss cũ
        bossLevel = false;   // Tắt cờ boss level
        gameState.resetForNewGame();
        entityManager.clearAll();
        paddle.setWidth(120); // Reset paddle width
        paddle.setHasLaser(false); // Reset laser
        loadLevel(levelIndex);
        createNewBall();
        resume();
        SoundManager.get().stopBgm();
        SoundManager.get().startBgm(SoundManager.Bgm.LEVEL);
    }

    public void startLevel6() {
        gameState.resetForNewGame();
        entityManager.clearAll();
        paddle.setWidth(120);
        paddle.setHasLaser(false);
        loadLevel(5);
        createNewBall();
        resume();
        gameState.setScore(0); // Reset điểm
        SoundManager.get().stopBgm();
        SoundManager.get().startBgm(SoundManager.Bgm.BOSS);
    }

    public void resetScore() {
        gameState.setScore(0);
    }

    public void resetLives() {
        gameState.setLives(3);
    }

    public void resume() {
        SoundManager.get().play(SoundManager.Sfx.PAUSE);

        gameState.setLevelComplete(false); // Luôn reset cờ khi resume
        gameState.setGameComplete(false); // Luôn reset cờ khi resume
        gameState.setConfirmOverlay(false); // Đảm bảo cờ confirm tắt khi resume
        if (!gameState.isGameStarted()) {
            startNewGame(0);
            return;
        }

        if (onResumeCallback != null) {
            onResumeCallback.run();
        }
        gameState.setPauseOverlay(false);
        gameState.setConfirmOverlay(false);
        gameState.setRunning(true);
    }

    public void pause() {
        gameState.setRunning(false);

        if (onPauseCallback != null) {
            onPauseCallback.run();
        }
    }

    public boolean isGameStarted() {
        return gameState.isGameStarted();
    }

    // Vòng lặp Update chính
    private void update(double dt) {
        timeSinceLastShot += dt;
        Brick.updateAnimation(dt);
        Boss.updateAnimation(dt);

        // 1. Cập nhật vị trí các đối tượng
        for (Brick b : entityManager.getBricks()) {
            b.updateWarp(dt);
            b.updateFlash(dt);
        }
        paddle.update(dt);
        collisionManager.getEmitter().update(dt);
        collisionManager.getDebrisEmitter().update(dt);
        entityManager.updateAll(dt); // Cập nhật đạn, power-up rơi, HUD...
        for (Ball b : entityManager.getBalls()) {
            b.update(dt);
        }

        // Kiểm tra kết thúc warp-in
        if (warpInProgress) {
            boolean anyWarping = false;
            for (Brick b : entityManager.getBricks()) {
                if (b.isWarpAnimating()) {
                    anyWarping = true;
                    break;
                }
            }
            if (!anyWarping) {
                // kết thúc warp
                warpInProgress = false;
                // dán bóng vào paddle
                for (Ball bl : entityManager.getBalls()) bl.setStuck(true);
            } else {
                return; // nếu vẫn đang warp thì dừng update game chính
            }
        }

        // vệt tên lửa Paddle (tạo khói phía sau paddle khi di chuyển)
        if (dt > 0) {
            double paddleVX = (paddle.getX() - lastPaddleX) / dt; // px/s
            if (Math.abs(paddleVX) > PADDLE_TRAIL_MIN_SPEED) {
                paddleTrailTimer += dt;
                while (paddleTrailTimer >= PADDLE_TRAIL_INTERVAL) {
                    paddleTrailTimer -= PADDLE_TRAIL_INTERVAL;
                    // tính vị trí spawn hạt
                    double spawnX;
                    if (paddleVX > 0) {
                        // di chuyển phải => trail phía trái
                        spawnX = paddle.getX() - 6;
                    } else {
                        // di chuyển trái => trail phía phải
                        spawnX = paddle.getX() + paddle.getWidth() + 6;
                    }
                    // spawn một chút thấp hơn tâm paddle để giống khói đẩy ra
                    double spawnY = paddle.getY() + paddle.getHeight() * 0.55 + 6;
                    if (collisionManager.getEmitter() != null) {
                        collisionManager.getEmitter().emitSmoke(spawnX, spawnY, 1); // 1 particle mỗi lần; tăng count nếu muốn vệt dày hơn
                    }
                }
            } else {
                // reset timer nếu không di chuyển đủ nhanh để tránh "tụ" hạt
                paddleTrailTimer = 0.0;
            }
            lastPaddleX = paddle.getX();
        }

        // 2. Xử lý Va chạm
        CollisionResult collisionResult = collisionManager.handleCollisions(
                entityManager, gameState, paddle, powerUpManager, width, height, dt
        );

        // 3. Xử lý các Sự kiện từ va chạm
        // 3a. Xử lý nhặt PowerUp
        for (PowerUp pu : collisionResult.collectedPowerUps) {
            powerUpManager.applyPowerUp(pu.type, entityManager, gameState, paddle, height);
        }

        // 3b. Xử lý Mất bóng
        if (collisionResult.ballLost) {
            handleBallLost();
        }

        // 3c. Xử lý Qua màn
        if (collisionResult.allDestructibleBricksCleared) {
            boolean bossDead = !bossLevel || (boss != null && boss.isDead());
            if (bossDead) {
                if (!powerUpManager.isNextLevelInProgress()) {
                    powerUpManager.triggerNextLevelEffect(-1, -1, entityManager);
                }
            }
        }

        // weanken
        if (powerUpManager.isWeakenInProgress() && powerUpManager.getCurrentShockwave() != null) {
            powerUpManager.getCurrentShockwave().update(dt);

            for (Brick b : entityManager.getBricks()) {
                if (powerUpManager.getShockAffected().contains(b)) continue;
                if (!b.isDestructible()) continue;
                if (powerUpManager.getCurrentShockwave().touchesBrick(b)) {
                    powerUpManager.getShockAffected().add(b);
                    b.flash(Color.WHITE, Brick.FLASH_DURATION);
                    if (collisionManager.getEmitter() != null) {
                        double cx = b.getX() + b.getWidth() / 2.0;
                        double cy = b.getY() + b.getHeight() / 2.0;
                        collisionManager.getEmitter().emitExplosion(cx, cy, 6);
                    }
                }
            }
            if (powerUpManager.getCurrentShockwave().finished) {
                powerUpManager.setWeakenInProgress(false);
                powerUpManager.setCurrentShockwave(null);
                powerUpManager.getShockAffected().clear();
            } else {
                // trong khi wave đang diễn ra, vẫn cập nhật các hạt/mảnh vụn/cửa sổ bật lên, v.v.
                if (collisionManager.getEmitter() != null) collisionManager.getEmitter().update(dt);
                if (collisionManager.getDebrisEmitter() != null) collisionManager.getDebrisEmitter().update(dt);
            }
        }

        // 3d. Next level effect
        if (powerUpManager.isNextLevelInProgress()) {

            Iterator<FlyingBrick> it = entityManager.getFlyingBricks().iterator();
            while (it.hasNext()) {
                FlyingBrick fb = it.next();


                double dx = powerUpManager.getPortalX() - fb.x;
                double dy = powerUpManager.getPortalY() - fb.y;
                double dist = Math.max(1.0, Math.hypot(dx, dy));
                double pull = 1200.0 / (dist + 120.0);

                fb.vx += (dx / dist) * pull * dt;
                fb.vy += (dy / dist) * pull * dt;


                fb.x += fb.vx * dt;
                fb.y += fb.vy * dt;
                fb.angle += fb.angularV * dt;

                double arrivalFactor = Math.min(1.0, Math.max(0.0, 1.0 - (dist / (Math.hypot(width, height)))));
                fb.scale = Math.max(0.12, 1.0 - 0.9 * (1.0 - Math.exp(-dist * 0.01)));


                if (dist < Math.max(12.0, powerUpManager.getPortalBaseRadius() * 0.9)) {
                    // tạo ra vụ nổ nhỏ ở cổng
                    if (collisionManager.getEmitter() != null)
                        collisionManager.getEmitter().emitExplosion(powerUpManager.getPortalX() + (Math.random() - 0.5) * 8, powerUpManager.getPortalY() + (Math.random() - 0.5) * 8, 6);
                    if (collisionManager.getDebrisEmitter() != null)
                        collisionManager.getDebrisEmitter().emitDebris(powerUpManager.getPortalX(), powerUpManager.getPortalY(), fb.brick.getWidth(), fb.brick.getHeight(), 6, Color.rgb(220, 180, 80)); // optional
                    it.remove();
                }
            }

            // cổng phát sáng xung tăng lên trong khi có những viên gạch bay
            powerUpManager.setPortalGlow(Math.min(1.0, powerUpManager.getPortalGlow() + dt * 3.5));

            // khi tất cả các viên gạch bay được loại bỏ -> kích hoạt đèn flash màu trắng rồi cấp độ tiếp theo
            if (entityManager.getFlyingBricks().isEmpty() && !powerUpManager.isWhiteFlashActive()) {
                powerUpManager.setWhiteFlashActive(true);
                powerUpManager.setWhiteFlashAlpha(1.0);
            }

            // xử lý flash màu trắng mờ dần
            if (powerUpManager.isWhiteFlashActive()) {
                double x = powerUpManager.getWhiteFlashAlpha();
                powerUpManager.setWhiteFlashAlpha(x -= dt / powerUpManager.getWHITE_FLASH_DURATION());
                if (powerUpManager.getWhiteFlashAlpha() <= 0.0) {
                    powerUpManager.setWhiteFlashAlpha(0.0);
                    powerUpManager.setWhiteFlashActive(false);
                    powerUpManager.setNextLevelInProgress(false);
                    nextLevel();
                }
            }

            // cập nhật các hạt/mảnh vụn/cửa sổ bật lên trong khi hiệu ứng tiếp tục
            if (collisionManager.getEmitter() != null) collisionManager.getEmitter().update(dt);
            if (collisionManager.getDebrisEmitter() != null) collisionManager.getDebrisEmitter().update(dt);

            // bỏ qua quá trình xử lý trò chơi thông thường trong khi chạy quá trình chuyển đổi
            return;
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
                    boss.takeDamage(5, powerUpManager, entityManager);
                    gameState.addScore(200);
                    collisionManager.getScorePopups().add(new ScorePopup(boss.getX() + boss.getWidth() / 2, boss.getY() + 2 * boss.getHeight(), "+200"));
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

            // Laser của người chơi trúng Boss -> Boss mất máu, có thể rơi PowerUp
            List<Bullet> bulletsHitBoss = new ArrayList<>();
            for (Bullet bu : entityManager.getBullets()) {
                double bx1 = bu.x, by1 = bu.y, bx2 = bu.x + bu.w, by2 = bu.y + bu.h;
                double ox1 = boss.getX(), oy1 = boss.getY(), ox2 = boss.getX() + boss.getWidth(), oy2 = boss.getY() + boss.getHeight();
                boolean overlap = (bx1 < ox2 && bx2 > ox1 && by1 < oy2 && by2 > oy1);
                if (overlap) {
                    bulletsHitBoss.add(bu);                       // xóa đạn sau vòng lặp
                    boss.takeDamage(5, powerUpManager, entityManager); // sát thương tùy chỉnh
                    gameState.addScore(100);
                    collisionManager.getScorePopups().add(new ScorePopup(bx1, boss.getY() + 2 * boss.getHeight(), "+100"));

                }
            }
            entityManager.getBullets().removeAll(bulletsHitBoss);

        }

        // 4. Cập nhật các Hiệu ứng (kiểm tra hết hạn)
        powerUpManager.updateActiveEffects(dt, entityManager, gameState, paddle);

        // cập nhật thời gian rung
        if (collisionManager.getShakeTime() > 0) {
            double k = collisionManager.getShakeTime() - dt;
            collisionManager.setShakeTime(k);
            if (collisionManager.getShakeTime() < 0) collisionManager.setShakeTime(0.0);
        }

        // cập nhật flash fade
        if (collisionManager.getFlashAlpha() > 0) {
            // giảm nhanh để là 1 chớp ngắn; điều chỉnh tốc độ (2.5)
            double g = collisionManager.getFlashAlpha() - dt * 2.8;
            collisionManager.setFlashAlpha(g);
            if (collisionManager.getFlashAlpha() < 0) collisionManager.setFlashAlpha(0.0);
        }
    }

    // Đếm số gạch có thể phá còn lại
    private int countRemainingDestructibleBricks() {
        int cnt = 0;
        for (Brick b : entityManager.getBricks()) {
            if (b.isDestructible()) cnt++;
        }
        return cnt;
    }

    // Vòng lặp Render chính
    private void render() {
        gameRenderer.render(gameState, entityManager, collisionManager, powerUpManager, paddle, boss, bossLevel);
    }

    // Logic Luồng Game (Game Flow)

    private void handleBallLost() {
        gameState.decrementLives();
        SoundManager.get().play(SoundManager.Sfx.BALL_LOST);
        if (gameState.getLives() <= 0) {
            SoundManager.get().stopLoop(SoundManager.Sfx.FIRE_LOOP);
            SoundManager.get().stopBgm();
            SoundManager.get().play(SoundManager.Sfx.GAME_OVER);

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
            pause(); // Dừng game
        } else {
            // Tải level tiếp theo
            switch (nextLevelIndex) {
                case 1:
                    startNewGame(1);
                    break;
                case 2:
                    startNewGame(2);
                    break;
                case 3:
                    startNewGame(3);
                    break;
                case 4:
                    startNewGame(4);
                    break;
                case 5:
                    startLevel6();
                    break;
                default:
                    startNewGame(0);
            }
            resume(); // Bắt đầu màn mới
        }
    }


    private void nextLevel() {
        // Kiểm tra xem đây có phải màn cuối không
        if (gameState.getCurrentLevelIndex() >= levelFiles.length - 1) {
            // Đây là màn cuối (Level 5)
            gameState.setGameComplete(true);
            SoundManager.get().stopBgm();
            SoundManager.get().play(SoundManager.Sfx.VICTORY);
            SoundManager.get().startBgm(SoundManager.Bgm.VICTORY_T);
        } else {
            // Đây chỉ là thắng màn thường
            gameState.setLevelComplete(true); // Đặt cờ
            SoundManager.get().stopBgm();
        }
        pause(); // Dừng game
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
            WarpStyle style = getWarpStyleForLevel(levelIndex);
            double[] params = getWarpParamsForLevel(levelIndex);
            warpInBricks(style, params[0], params[1], entityManager);
            warpInProgress = true;
            //Tạo boss
            if (ld.hasBoss) {
                boss = new Boss(width / 2 - 100, 50, 150, 80, 100, width - 30);
                bossLevel = true;
            }

        } catch (IOException ex) {
            System.err.println("Failed to load level: " + levelFiles[levelIndex]);
        }
    }

    // Bắn đạn
    private void tryShoot() {
        if (!paddle.hasLaser() || timeSinceLastShot < shootCooldown) return;
        timeSinceLastShot = 0.0;
        double[] pos = paddle.getLaserGunPositions();
        entityManager.addBullet(new Bullet(pos[0], pos[1]));
        entityManager.addBullet(new Bullet(pos[2], pos[3]));
        SoundManager.get().play(SoundManager.Sfx.LASER_SHOT);
    }

    private void warpInBricks(WarpStyle style, double duration, double maxStagger, EntityManager entities) {
        if (entities.getBricks() == null || entities.getBricks().isEmpty()) return;

        // tìm minX,minY, brickW, brickH
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        double brickW = -1, brickH = -1;
        for (Brick b : entities.getBricks()) {
            minX = Math.min(minX, b.getX());
            minY = Math.min(minY, b.getY());
            maxX = Math.max(maxX, b.getX());
            maxY = Math.max(maxY, b.getY());
            if (brickW < 0) brickW = b.getWidth();
            if (brickH < 0) brickH = b.getHeight();
        }

        // trung tâm gợn sóng
        double centerX = (minX + maxX + brickW) / 2.0;
        double centerY = (minY + maxY + brickH) / 2.0;

        // xác định độ trễ trên mỗi viên gạch
        double minDelay = 0.0;
        double maxDelay = maxStagger;

        for (Brick b : entities.getBricks()) {
            double delay = 0.0;
            if (style == WarpStyle.FADE_IN || style == WarpStyle.SCALE_UP) {
                delay = 0.0;
            } else if (style == WarpStyle.RIPPLE_LTR) {
                int col = (int) Math.round((b.getX() - minX) / Math.max(1.0, brickW));
                int row = (int) Math.round((b.getY() - minY) / Math.max(1.0, brickH));
                delay = (row * 10 + col) * (maxStagger / 100.0);
            } else if (style == WarpStyle.RIPPLE_CENTER) {
                double dx = (b.getX() + b.getWidth() / 2.0) - centerX;
                double dy = (b.getY() + b.getHeight() / 2.0) - centerY;
                double dist = Math.sqrt(dx * dx + dy * dy);
                double maxDist = Math.sqrt((maxX - minX) * (maxX - minX) + (maxY - minY) * (maxY - minY)) + 0.0001;
                double norm = dist / maxDist;
                delay = norm * maxStagger;
            }
            if (delay < minDelay) delay = minDelay;
            if (delay > maxDelay) delay = maxDelay;

            Brick.WarpMode mode = (style == WarpStyle.SCALE_UP) ? Brick.WarpMode.SCALE : Brick.WarpMode.FADE;
            b.startWarp(mode, delay, duration);
        }

        warpInProgress = true;
    }

    // Lấy WarpStyle cho level (cố định theo levelIndex 1-based)
    private WarpStyle getWarpStyleForLevel(int levelIdx) {
        switch (levelIdx % 3) {
            case 0:
                return WarpStyle.FADE_IN;
            case 1:
                return WarpStyle.SCALE_UP;
            default:
                return WarpStyle.RIPPLE_LTR;
        }
    }

    // Lấy duration và maxStagger phù hợp với level
    private double[] getWarpParamsForLevel(int levelIdx) {
        // trả về array {duration, maxStagger}
        switch (levelIdx % 3) {
            case 0: // FADE_IN - nhanh, cùng lúc
                return new double[]{0.5, 0.0};
            case 1: // SCALE_UP - hơi pop
                return new double[]{0.5, 0.05};
            default: // RIPPLE_LTR - ripple rõ ràng
                return new double[]{0.5, 0.32};
        }
    }

    // Xử lý Input
    private void handleKeyPressed(KeyCode code) {
        // Xử lý khi game đang dừng (Game Over / Win)
        if (!gameState.isRunning()) {
            if (gameState.isSettingsOverlay()) {
                return;
            }

            if (gameState.isConfirmOverlay()) {
                return;
            }

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
            case SPACE:
                if (paddle.hasLaser()) tryShoot();
                for (Ball bl : entityManager.getBalls()) {
                    if (bl.isStuck()) bl.launch();
                }
                break;
            case P:
                if (gameState.isRunning()) {
                    gameState.setPauseOverlay(true);
                    pause();
                }
                break;
            case ESCAPE:
                if (gameState.isRunning()) {
                    gameState.setConfirmOverlay(true); // Đặt cờ
                    pause();
                }
                break;
            default:
                // Các phím di chuyển
                paddle.press(code);
                break;
        }
    }

    // Chuyển đổi trạng thái Pause/Resume.
    public void togglePause() {
        SoundManager.get().play(SoundManager.Sfx.PAUSE);

        if (gameState.isRunning()) {
            pause(); // Nếu đang chạy thì dừng
        } else if (gameState.isPauseOverlay()) {
            resume(); // Nếu đang dừng thì tiếp tục
        }

        this.requestFocus(); // Yêu cầu focus lại Pane game để nhận phím
    }

    // Mở cài đặt
    public void openSettings() {
        if (gameState.isRunning()) {
            gameState.setSettingsOverlay(true);
            pause();
        }

        this.requestFocus(); // Yêu cầu focus lại
    }

    // Đóng settings và quay lại menu pause chính.
    public void closeSettingsAndPause() {
        gameState.setSettingsOverlay(false);
        if (onPauseCallback != null) {
            onPauseCallback.run();
        }
    }
}
