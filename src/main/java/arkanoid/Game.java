package arkanoid;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.io.IOException;
import java.util.*;

public class Game extends Pane {
    private final double width;
    private final double height;

    private final Canvas canvas;
    private final GraphicsContext gc;

    private final Paddle paddle;
    private final List<Ball> balls = new ArrayList<>();
    private final List<Brick> bricks = new ArrayList<>();

    private int lives = 3; // số mạng
    private int score = 0; // điểm
    private double scoreMultiplier = 1.0; // hệ số nhân điểm (mặc định 1.0)
    private boolean running = false; // bắt đầu false để hiển thị menu
    private boolean win = false; // thắng hay thua
    private boolean showMessage = false; // hiển thị overlay thông báo

    private long lastTime = 0; // thời gian vòng lặp để tính deltaTime

    private boolean gameStarted = false; // true nếu đã từng bắt đầu 1 trận (dùng cho Continue)

    // Overlay trạng thái tạm dừng / confirm
    private boolean pauseOverlay = false; // true khi đang pause (P)
    private boolean confirmOverlay = false; // true khi ESC hiện confirm (Y/N)

    private final Runnable returnToMenuCallback;

    private final String[] levelFiles = new String[] { "/levels/level1.txt", "/levels/level2.txt", "/levels/level3.txt" };
    private int currentLevelIndex = 0;

    // Power-ups
    private final List<PowerUp> powerUps = new ArrayList<>();
    private final List<ActiveEffect> activeEffects = new ArrayList<>();
    private final List<Bullet> bullets = new ArrayList<>();

    // cooldown bắn (s)
    private double shootCooldown = 0.25; // giây giữa 2 lượt bắn
    private double timeSinceLastShot = 0.0;

    // Barrier năng lượng: khi active, nó sẽ chặn bóng rơi 1 lần rồi mất
    private boolean barrierActive = false;
    private double barrierY = -1;      // y của rào chắn (tính khi kích hoạt)
    private double barrierThickness = 6; // chiều cao hiển thị

    // HUD messages khi nhặt power-up
    private final java.util.List<HUDMessage> hudMessages = new ArrayList<>();

    private final Random random = new Random();

    public Game(double w, double h,Runnable returnToMenuCallback) {
        this.width = w; this.height = h;
        this.returnToMenuCallback = returnToMenuCallback;

        canvas = new Canvas(width, height);
        gc = canvas.getGraphicsContext2D();
        getChildren().add(canvas);

        // Tạo paddle và bóng
        paddle = new Paddle((width - 120)/2, height - 40, 120, 16, width);
        // tạo 1 quả bóng ban đầu
        Ball b = new Ball(paddle.getX() + paddle.getWidth()/2, paddle.getY() - 10, 8, paddle);
        balls.add(b);

        loadLevelFromFile(0);

        // Cấu hình nhận sự kiện bàn phím
        setFocusTraversable(true);
        setOnKeyPressed(e -> handleKeyPressed(e.getCode()));
        setOnKeyReleased(e -> paddle.release(e.getCode()));
        // bắn bằng click chuột (nếu paddle có laser)
        setOnMousePressed(e -> {
            if (!running) return;
            if (paddle.hasLaser()) {
                tryShoot();
            }
        });

        // AnimationTimer làm game loop: gọi handle( now ) mỗi frame
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastTime == 0) lastTime = now;
                double deltaTime = (now - lastTime) / 1e9; // chuyển sang giây
                lastTime = now;
                if (running) {
                    update(deltaTime);
                    render();
                } else {
                    // Khi dừng vẫn vẽ overlay (Game Over / Win)
                    render();
                }
            }
        };
        timer.start();
    }

    // hẹn giờ hoạt động effects
    private class ActiveEffect {
        PowerUp.PowerType type;
        double remaining; // seconds
        //lưu trữ các giá trị ban đầu để phục hồi
        double originalScoreMultiplier = 1.0;
        double originalPaddleWidth = -1;
        Map<Ball, Double> originalSpeeds = new HashMap<>();
        Map<Ball, Double> originalRadii = new HashMap<>();
        public boolean originalHasLaser = false;
        public Map<Ball, Boolean> originalFireball = new HashMap<>();


        ActiveEffect(PowerUp.PowerType type, double duration) {
            this.type = type; this.remaining = duration;
        }

        void update(double dt) {
            remaining -= dt;
        }

        boolean isExpired() { return remaining <= 0; }
    }

    // HUD message class: hiển thị text ngắn ở phía trên màn hình khi nhặt vật phẩm
    private static class HUDMessage {
        String text;
        double life; // remaining seconds
        double maxLife;

        HUDMessage(String text, double life) {
            this.text = text;
            this.maxLife = life;
            this.life = life;
        }

        void update(double dt) { life -= dt; }

        boolean isAlive() { return life > 0; }

        void render(javafx.scene.canvas.GraphicsContext gc, double canvasWidth) {
            double alpha = Math.max(0, life / maxLife);
            gc.setGlobalAlpha(alpha);
            gc.setFill(javafx.scene.paint.Color.WHITE);
            gc.setFont(javafx.scene.text.Font.font(18));
            // vẽ text ở giữa trên màn hình; ước tính độ rộng bằng số ký tự * 6 px (ước lượng đơn giản)
            double estWidth = text.length() * 6;
            gc.fillText(text, canvasWidth / 2.0 - estWidth / 2.0, 40);
            gc.setGlobalAlpha(1.0);
        }
    }

    // Bắt đầu 1 trận mới
    public void startNewGame() {
        lives = 3;
        score = 0;
        win = false;
        showMessage = false;
        loadLevelFromFile(currentLevelIndex);
        balls.clear();
        Ball b = new Ball(paddle.getX() + paddle.getWidth()/2, paddle.getY() - 10, 8, paddle);
        applyActiveEffectsToBall(b);
        balls.add(b);
        running = true;
        gameStarted = true;
        pauseOverlay = false;
        confirmOverlay = false;
    }

    // tiếp tục trận hiện tại
    public void resume() {
        pauseOverlay = false;
        confirmOverlay = false;
        running = true;
    }

    // Tạm dừng game
    public void pause() {
        running = false;
        pauseOverlay = true;
    }

    public boolean isGameStarted() { return gameStarted; }

    private void handleKeyPressed(KeyCode code) {
        // Nếu đang hiển thị confirm (ESC) thì chỉ xử lý Y/N
        if (confirmOverlay) {
            if (code == KeyCode.N) {
                confirmOverlay = false;
                running = true;
            } else if (code == KeyCode.Y) {
                Platform.exit();
            }
            return;
        }

        // Nếu đang pause overlay, xử lý P để resume hoặc '0' để quay về menu
        if (pauseOverlay) {
            if (code == KeyCode.P) {
                resume();
            } else if (code == KeyCode.O) {
                pauseOverlay = false;
                running = false;
                returnToMenuCallback.run(); // gọi callback để trở về menu
            }
            return;
        }

        if(!running){
            if(code == KeyCode.S) startNewGame();
            else if(code == KeyCode.R) returnToMenuCallback.run();
        }

        // Phím tắt trong trận
        if (code == KeyCode.SPACE) {
            if (!running) return;
            if (paddle.hasLaser()) tryShoot();
            for (Ball bl : new ArrayList<>(balls)) if (bl.isStuck()) bl.launch();
        }

        if (code == KeyCode.P) {
            // P để tạm dừng/tiếp tục
            if (running) pause();
            else resume();
        }

        if (code == KeyCode.ESCAPE) {
            // ESC để hiện confirm (Y: tiếp tục, N: thoát)
            confirmOverlay = true;
            running = false;
        }

        if (!running && (code == KeyCode.R)) startNewGame();

        paddle.press(code);
    }

    // Cập nhật logic game từng khung
    private void update(double dt) {
        timeSinceLastShot += dt;
        paddle.update(dt);
        List<Ball> ballsToRemove = new ArrayList<>();

        for (Ball bl : new ArrayList<>(balls)) {
            // Va chạm tường và paddle
            bl.update(dt);
            bl.collideWithWalls(width, height);
            bl.collideWithPaddle(paddle);

            // Va chạm với bricks: lặp qua và xử lý 1 collision/khung để ổn định
            Iterator<Brick> it = bricks.iterator();
            while (it.hasNext()) {
                Brick b = it.next();
                if (bl.collideWithBrick(b)) {
                    Brick.Type t = b.getType();
                    // Nếu ball là fireball -> phá ngay cả indestructible
                    if (bl.isFireball()) {
                        // remove directly
                        double bx = b.getX() + b.getWidth()/2.0;
                        double by = b.getY() + b.getHeight()/2.0;
                        it.remove();
                        addScore(100);
                        trySpawnPowerUp(bx, by);
                        // ball vẫn nảy như bình thường (không thay đổi vx/vy)
                    } else {
                        boolean removed = b.hit();
                        if (removed) {
                            // toạ độ cho power-up
                            double bx = b.getX() + b.getWidth() / 2.0;
                            double by = b.getY() + b.getHeight() / 2.0;
                            it.remove();
                            addScore(100); // điểm khi phá hư

                            // tỉ lệ ra power-up
                            trySpawnPowerUp(bx, by);

                            // Nếu là gạch nổ, xử lý nổ phá các gạch xung quanh
                            if (t == Brick.Type.EXPLOSIVE) {
                                handleExplosion(b);
                            }
                        } else {
                            if (b.isDestructible()) addScore(50); // điểm khi chỉ bị trúng nhưng chưa hư
                        }
                    }
                    break; // break để tránh nhiều phản xạ trong 1 frame
                }
            }

            // ví dụ trong vòng for mỗi ball (nếu bạn đang xử lý balls list)
            if (bl.getY() > height) {
                if (barrierActive) {
                    // Barrier chặn 1 lần: bật bóng lên 1 lần và barrier biến mất
                    bl.bounceUp();
                    // đặt bóng ở ngay trên barrier để tránh bị tính rơi lại ngay
                    double newY = barrierY - bl.getRadius() - 1;
                    bl.setPositionY(newY);
                    barrierActive = false;
                } else {
                    ballsToRemove.add(bl); // hoặc logic cũ của bạn
                }
            }

            // cập nhật bullets
            Iterator<Bullet> bit = bullets.iterator();
            while (bit.hasNext()) {
                Bullet bullet = bit.next();
                bullet.update(dt);
                boolean removedBullet = false;
                Iterator<Brick> bIt2 = bricks.iterator();
                while (bIt2.hasNext()) {
                    Brick br2 = bIt2.next();
                    if (bullet.collidesWithBrick(br2)) {
                        // đạn phá luôn brick (bất kể type)
                        double bx = br2.getX() + br2.getWidth()/2.0;
                        double by = br2.getY() + br2.getHeight()/2.0;
                        bIt2.remove();
                        score += 100;
                        trySpawnPowerUp(bx, by);
                        bullet.kill();
                        removedBullet = true;
                        break;
                    }
                }
                if (!bullet.isAlive() || removedBullet) bit.remove();
            }

        }

        for (Ball rem : ballsToRemove) balls.remove(rem);

        // Bóng rơi ra đáy -> mất mạng
        if (balls.isEmpty()) {
            lives--;
            if (lives <= 0) {
                running = false;
                win = false;
                showMessage = true;
            } else {
                balls.clear();
                Ball b = new Ball(paddle.getX() + paddle.getWidth()/2, paddle.getY() - 10, 8, paddle);
                applyActiveEffectsToBall(b);
                balls.add(b);
                b.resetToPaddle();
            }
        }

        // update powerUps (falling items)
        Iterator<PowerUp> pIt = powerUps.iterator();
        while (pIt.hasNext()) {
            PowerUp pu = pIt.next();
            pu.update(dt);
            if (pu.collidesWithPaddle(paddle)) {
                // pickup
                applyPowerUp(pu.type);
                pIt.remove();
            } else if (pu.y > height + 50) {
                // missed
                pIt.remove();
            }
        }

        // update activeEffects
        Iterator<ActiveEffect> effIt = activeEffects.iterator();
        while (effIt.hasNext()) {
            ActiveEffect ae = effIt.next();
            ae.update(dt);
            if (ae.isExpired()) {
                // restore based on type
                switch (ae.type) {
                    case SHRINK_PADDLE:
                    case EXPAND_PADDLE:
                        if (ae.originalPaddleWidth > 0) paddle.setWidth(ae.originalPaddleWidth);
                        break;
                    case TINY_BALL:
                        for (Map.Entry<Ball, Double> e : ae.originalRadii.entrySet()) {
                            Ball bl = e.getKey(); if (balls.contains(bl)) bl.setRadius(e.getValue());
                        }
                        break;
                    case SLOW_BALL:
                    case FAST_BALL:
                        for (Map.Entry<Ball, Double> e : ae.originalSpeeds.entrySet()) {
                            Ball bl = e.getKey(); if (balls.contains(bl)) bl.setBaseSpeed(e.getValue());
                        }
                        break;
                    case LASER_PADDLE:
                        paddle.setHasLaser(ae.originalHasLaser);
                        break;
                    case FIREBALL:
                        for (Map.Entry<Ball, Boolean> e : ae.originalFireball.entrySet()) {
                            Ball bl = e.getKey();
                            if (balls.contains(bl)) bl.setFireball(e.getValue());
                        }
                        break;
                    case SCORE_MULTIPLIER:
                        scoreMultiplier = ae.originalScoreMultiplier;
                        break;
                    default: break;
                }
                effIt.remove();
            }
        }

        // update HUD messages
        Iterator<HUDMessage> hIt = hudMessages.iterator();
        while (hIt.hasNext()) {
            HUDMessage hm = hIt.next();
            hm.update(dt);
            if (!hm.isAlive()) hIt.remove();
        }

        // Kiểm tra còn gạch có thể phá không (chỉ những loại có thể phá mới được tính)
        if (countRemainingDestructibleBricks() == 0) {
            nextLevel();
        }
    }

    // Xử lý nổ cho gạch explosive: phá các gạch có thể phá trong bán kính
    private void handleExplosion(Brick center) {
        double stepX = center.getWidth() + 8;
        double stepY = center.getHeight() + 6;

        int[] dir = new int[] { -1, 0, 1 };

        Queue<Brick> q = new java.util.LinkedList<>();
        Set<Brick> visited = new java.util.HashSet<>(); // tránh thêm lại
        Set<Brick> toRemoveSet = new java.util.HashSet<>(); // tránh xóa trùng

        q.add(center);
        visited.add(center);

        while (!q.isEmpty()) {
            Brick cur = q.poll();

            // Nếu cur có thể phá thì đánh dấu xóa
            if (cur.isDestructible()) toRemoveSet.add(cur);

            double cx = cur.getX() + cur.getWidth() / 2.0;
            double cy = cur.getY() + cur.getHeight() / 2.0;

            // 1) Kiểm tra 8 hướng lân cận (immediate neighbours) để tìm các brick explosive mới và enqueue
            for (int dx : dir) {
                for (int dy : dir) {
                    if (dx == 0 && dy == 0) continue;
                    double tx = cx + dx * stepX;
                    double ty = cy + dy * stepY;

                    // tìm ô brick có tâm gần tx,ty
                    for (Brick b : bricks) {
                        if (visited.contains(b)) continue;
                        double bx = b.getX() + b.getWidth() / 2.0;
                        double by = b.getY() + b.getHeight() / 2.0;
                        double tolX = b.getWidth() * 0.6;
                        double tolY = b.getHeight() * 0.6;
                        if (Math.abs(bx - tx) <= tolX && Math.abs(by - ty) <= tolY) {
                            // Nếu là explosive -> enqueue (chain)
                            if (b.getType() == Brick.Type.EXPLOSIVE) {
                                visited.add(b);
                                q.add(b);
                            }
                            break; // dừng tìm trong list bricks cho vị trí này
                        }
                    }
                }
            }

            // 2) Phá theo 4 hướng hàng/cột tối đa 2 ô
            int[][] card = new int[][] { {1,0}, {-1,0}, {0,1}, {0,-1} };
            for (int[] d : card) {
                int dx = d[0], dy = d[1];
                for (int step = 1; step <= 2; step++) {
                    double tx = cx + dx * step * stepX;
                    double ty = cy + dy * step * stepY;

                    Brick found = null;
                    for (Brick b : bricks) {
                        double bx = b.getX() + b.getWidth() / 2.0;
                        double by = b.getY() + b.getHeight() / 2.0;
                        double tolX = b.getWidth() * 0.6;
                        double tolY = b.getHeight() * 0.6;
                        if (Math.abs(bx - tx) <= tolX && Math.abs(by - ty) <= tolY) {
                            found = b;
                            break;
                        }
                    }

                    if (found != null) {
                        // Nếu found là explosive và chưa được enqueue, enqueue để chain reaction
                        if (found.getType() == Brick.Type.EXPLOSIVE && !visited.contains(found)) {
                            visited.add(found);
                            q.add(found);
                        }
                        // Nếu có thể phá thì đánh dấu xóa
                        if (found.isDestructible()) toRemoveSet.add(found);
                    }
                }
            }
        }

        // Xóa các brick đã đánh dấu và cộng điểm
        for (Brick b : new ArrayList<>(toRemoveSet)) {
            if (bricks.remove(b)) {
                score += 100;
            }
        }
    }

    // Đếm số gạch có thể phá còn lại (không tính indestructible)
    private int countRemainingDestructibleBricks() {
        int cnt = 0;
        for (Brick b : bricks) {
            if (b.isDestructible()) cnt++;
        }
        return cnt;
    }

    // Tạo layout level: grid bricks
    private void buildLevel() {
        bricks.clear();
        int rows = 6;
        int cols = 10;
        double brickW = (width - 60) / cols; // tính khoảng cách + padding
        double brickH = 24;
        double startX = 30;
        double startY = 60;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int hits = 1 + (r / 2); // hàng trên cùng chịu nhiều hit hơn
                double x = startX + c * brickW;
                double y = startY + r * (brickH + 6);
                bricks.add(new Brick(x, y, brickW - 8, brickH, hits));
            }
        }
    }

    private void loadLevelFromFile(int index) {
        if (index < 0) return;
        if(index >= levelFiles.length){
            running = false;
            win = true;
            showMessage = true;
        }
        try {
            LevelData ld = LevelLoader.loadLevel(levelFiles[index], width);
            bricks.clear();
            bricks.addAll(ld.bricks);
        } catch (IOException ex) {
            buildLevel();
        }
    }

    private void nextLevel() {
        currentLevelIndex++;
        if (currentLevelIndex >= levelFiles.length) {
            // hết level -> win và trả về menu
            running = false; win = true; showMessage = true; returnToMenuCallback.run();
        } else {
            loadLevelFromFile(currentLevelIndex);
            balls.clear();
            Ball b = new Ball(paddle.getX() + paddle.getWidth()/2, paddle.getY() - 10, 8, paddle);
            applyActiveEffectsToBall(b);
            balls.add(b);
        }
    }

    // sinh ra power-up
    private void trySpawnPowerUp(double x, double y) {
        double spawnChance = 0.3; // 30% chance
        if (random.nextDouble() > spawnChance) return;

        // trọng số cho các loại
        Map<PowerUp.PowerType, Double> weights = new LinkedHashMap<>();
        weights.put(PowerUp.PowerType.SHRINK_PADDLE, 1.0);
        weights.put(PowerUp.PowerType.EXPAND_PADDLE, 1.0);
        weights.put(PowerUp.PowerType.TINY_BALL, 0.8);
        weights.put(PowerUp.PowerType.MULTI_BALL, 1.2);
        weights.put(PowerUp.PowerType.SLOW_BALL, 0.8);
        weights.put(PowerUp.PowerType.FAST_BALL, 0.6);
        weights.put(PowerUp.PowerType.NEXT_LEVEL, 0.25);
        weights.put(PowerUp.PowerType.EXTRA_LIFE, 0.4);
        weights.put(PowerUp.PowerType.SUDDEN_DEATH, 0.2);
        weights.put(PowerUp.PowerType.LASER_PADDLE, 0.5);
        weights.put(PowerUp.PowerType.FIREBALL, 0.5);
        weights.put(PowerUp.PowerType.BARRIER, 0.6);
        weights.put(PowerUp.PowerType.WEAKEN, 0.7);
        weights.put(PowerUp.PowerType.SCORE_MULTIPLIER, 0.7);

        double total = 0; for (double v : weights.values()) total += v;
        double r = random.nextDouble() * total;
        double cum = 0;
        PowerUp.PowerType chosen = PowerUp.PowerType.SHRINK_PADDLE;
        for (Map.Entry<PowerUp.PowerType, Double> e : weights.entrySet()) {
            cum += e.getValue(); if (r <= cum) { chosen = e.getKey(); break; }
        }

        powerUps.add(new PowerUp(x, y, chosen));
    }

    // Áp dụng power-up ngay lập tức hoặc theo thời gian
    private void applyPowerUp(PowerUp.PowerType type) {
        switch (type) {
            case SHRINK_PADDLE: {
                ActiveEffect eff = new ActiveEffect(type, 10.0);
                eff.originalPaddleWidth = paddle.getWidth();
                paddle.setWidth(eff.originalPaddleWidth * 0.7);
                activeEffects.add(eff);
                hudMessages.add(new HUDMessage("SHRINK PADDLE -10s", 2.5));
                break;
            }
            case EXPAND_PADDLE: {
                ActiveEffect eff = new ActiveEffect(type, 10.0);
                eff.originalPaddleWidth = paddle.getWidth();
                paddle.setWidth(eff.originalPaddleWidth * 1.4);
                activeEffects.add(eff);
                hudMessages.add(new HUDMessage("EXPAND PADDLE +10s", 2.5));
                break;
            }
            case TINY_BALL: {
                ActiveEffect eff = new ActiveEffect(type, 10.0);
                for (Ball bl : balls) {
                    eff.originalRadii.put(bl, bl.getRadius());
                    bl.setRadius(bl.getRadius() * 0.6);
                }
                activeEffects.add(eff);
                hudMessages.add(new HUDMessage("TINY BALL -10s", 2.5));
                break;
            }
            case MULTI_BALL: {
                // sinh ra thêm 2 quả bóng từ paddle
                List<Ball> newBalls = new ArrayList<>();
                for (int i = 0; i < 2; i++) {
                    Ball nb = new Ball(paddle.getX() + paddle.getWidth()/2, paddle.getY() - 10, 8, paddle);
                    nb.setBaseSpeed(300);
                    applyActiveEffectsToBall(nb);
                    nb.launch();
                    // điều chỉnh tốc độ lan truyền
                    double vx = (i==0) ? -Math.abs(nb.centerX()-paddle.getX()) : Math.abs(nb.centerX()-paddle.getX());
                    // tạo góc tách nhỏ cho các quả mới
                    double angle = Math.toRadians(20) * (i==0 ? -1 : 1);
                    double speedVal = nb.getBaseSpeed();
                    double vxNew = Math.sin(angle) * speedVal;
                    double vyNew = -Math.cos(angle) * speedVal;
                    nb.setVelocity(vxNew, vyNew);
                    nb.setStuck(false);
                    newBalls.add(nb);
                }
                balls.addAll(newBalls);
                hudMessages.add(new HUDMessage("MULTI BALL", 2.5));
                break;
            }
            case SLOW_BALL: {
                ActiveEffect eff = new ActiveEffect(type, 8.0);
                for (Ball bl : balls) {
                    eff.originalSpeeds.put(bl, bl.getBaseSpeed());
                    bl.setBaseSpeed(bl.getBaseSpeed() * 0.7);
                }
                activeEffects.add(eff);
                hudMessages.add(new HUDMessage("SLOW BALL -8s", 2.5));
                break;
            }
            case FAST_BALL: {
                ActiveEffect eff = new ActiveEffect(type, 8.0);
                for (Ball bl : balls) {
                    eff.originalSpeeds.put(bl, bl.getBaseSpeed());
                    bl.setBaseSpeed(bl.getBaseSpeed() * 1.3);
                }
                activeEffects.add(eff);
                hudMessages.add(new HUDMessage("FAST BALL -8s", 2.5));
                break;
            }
            case NEXT_LEVEL: {
                hudMessages.add(new HUDMessage("NEXT LEVEL", 2.5));
                nextLevel(); break;
            }
            case EXTRA_LIFE: {
                lives++;
                hudMessages.add(new HUDMessage("+1 LIFE", 2.5));
                break;
            }
            case SUDDEN_DEATH: {
                lives = Math.max(1, lives);
                hudMessages.add(new HUDMessage("SUDDEN DEATH", 2.5));
                break;
            }
            case LASER_PADDLE: {
                ActiveEffect eff = new ActiveEffect(type, 10.0);
                // lưu trạng thái cũ (thêm field originalHasLaser vào ActiveEffect)
                eff.originalHasLaser = paddle.hasLaser();
                paddle.setHasLaser(true);
                activeEffects.add(eff);
                hudMessages.add(new HUDMessage("LASER PADDLE +10s", 2.5));
                break;
            }
            case FIREBALL: {
                ActiveEffect eff = new ActiveEffect(type, 8.0);
                // cho tất cả ball hiện tại: setFireball(true) và lưu trạng thái
                for (Ball bl : balls) {
                    eff.originalFireball.put(bl, bl.isFireball());
                    bl.setFireball(true);
                }
                activeEffects.add(eff);
                hudMessages.add(new HUDMessage("FIREBALL -8s", 2.5));
                break;
            }
            case BARRIER: {
                // Kích hoạt barrier: đặt barrierActive = true. Barrier sẽ tồn tại cho đến khi dùng 1 lần.
                barrierActive = true;
                // đặt vị trí ngay trên khu vực mất mạng (gần dưới cùng). Ví dụ
                barrierY = height - 24; // hoặc height - paddle.getHeight() - 30; tùy layout
                hudMessages.add(new HUDMessage("BARRIER (1 lần)", 2.5));
                break;
            }
            case WEAKEN: {
                // Giảm 1 hit cho tất cả gạch NORMAL có hits > 1
                int reduced = 0;
                for (Brick br : new ArrayList<>(bricks)) {
                    if (br.getType() == Brick.Type.NORMAL) {
                        // chỉ giảm nếu >1
                        if (br.getHits() > 1) {
                            br.weaken(); // đã đảm bảo ko phá gạch 1-hit
                            reduced++;
                        }
                    }
                }
                hudMessages.add(new HUDMessage("WEAKEN: -" + reduced + " hits", 2.5));
                break;
            }
            case SCORE_MULTIPLIER: {
                ActiveEffect eff = new ActiveEffect(type, 15.0); // 15 giây
                eff.originalScoreMultiplier = scoreMultiplier;
                scoreMultiplier = scoreMultiplier * 2.0; // nhân đôi
                activeEffects.add(eff);
                hudMessages.add(new HUDMessage("SCORE x2 -15s", 2.5));
                break;
            }
        }
    }

    // Áp dụng các hiệu ứng đang hoạt động vào một quả bóng mới tạo
    private void applyActiveEffectsToBall(Ball bl) {
        // tính toán hệ số nhân từ các hiệu ứng đang hoạt động hiện tại
        double radiusMultiplier = 1.0;
        double speedMultiplier = 1.0;
        for (ActiveEffect ae : activeEffects) {
            if (ae.type == PowerUp.PowerType.TINY_BALL) radiusMultiplier *= 0.6;
            if (ae.type == PowerUp.PowerType.SLOW_BALL) speedMultiplier *= 0.7;
            if (ae.type == PowerUp.PowerType.FAST_BALL) speedMultiplier *= 1.3;
            if (ae.type == PowerUp.PowerType.FIREBALL) {
                ae.originalFireball.put(bl, bl.isFireball());
                bl.setFireball(true);
            }
        }
        if (radiusMultiplier != 1.0) {
            bl.setRadius(bl.getRadius() * radiusMultiplier);
        }
        if (speedMultiplier != 1.0) {
            bl.setBaseSpeed(bl.getBaseSpeed() * speedMultiplier);
        }
    }

    // Thử bắn (nếu cooldown cho phép), tạo 2 bullets từ 2 nòng paddle
    private void tryShoot() {
        if (!paddle.hasLaser()) return;
        if (timeSinceLastShot < shootCooldown) return;
        timeSinceLastShot = 0.0;

        double[] pos = paddle.getLaserGunPositions(); // [x1,y1,x2,y2]
        Bullet b1 = new Bullet(pos[0], pos[1]);
        Bullet b2 = new Bullet(pos[2], pos[3]);
        bullets.add(b1);
        bullets.add(b2);
    }

    private void addScore(int base) {
        int added = (int) Math.round(base * scoreMultiplier);
        score += added;
    }

    private void render() {
        // background
        gc.setFill(Color.rgb(20, 24, 30));
        gc.fillRect(0, 0, width, height);

        // HUD top
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font(18));
        gc.fillText("Score: " + score, 10, 22);
        gc.fillText("Lives: " + lives, width - 110, 22);

        // draw objects
        paddle.render(gc);
        for (Ball bl : balls) bl.render(gc);
        for (Brick b : bricks) b.render(gc);
        for (PowerUp pu : powerUps) pu.render(gc);
        for (Bullet bu : bullets) bu.render(gc);

        // center message
        if (showMessage) {
            gc.setFill(Color.color(0,0,0,0.6));
            gc.fillRect(0, 0, width, height);
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font(36));
            String text = win ? "YOU WIN!" : "GAME OVER";
            gc.fillText(text, width/2 - 110, height/2 - 20);
            gc.setFont(Font.font(20));
            gc.fillText("Score: " + score, width/2 - 60, height/2 + 10);
            gc.fillText("Press S to startNewGame", width/2 - 115, height/2 + 50);
            gc.fillText("Press R to return menu", width/2 - 110, height/2 + 90);
        }

        // Restart lại game: đặt lại mạng, điểm, level và bóng dính paddle
        if (balls.stream().anyMatch(Ball::isStuck) && running) {
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font(14));
            gc.fillText("Press SPACE to launch ball", width/2 - 90, height - 10);
        }

        // Vẽ pause overlay nếu đang pause (text-based)
        if (pauseOverlay) {
            gc.setFill(Color.color(0,0,0,0.6));
            gc.fillRect(0, 0, width, height);
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font(28));
            gc.fillText("PAUSED", width/2 - 60, height/2 - 20);
            gc.setFont(Font.font(16));
            gc.fillText("Nhấn P để tiếp tục", width/2 - 80, height/2 + 10);
            gc.fillText("Nhấn 0 để quay về Menu", width/2 - 95, height/2 + 35);
        }

        // Vẽ confirm overlay khi nhấn ESC
        if (confirmOverlay) {
            gc.setFill(Color.color(0,0,0,0.6));
            gc.fillRect(0, 0, width, height);
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font(20));
            String line1 = "Do you want to quit?";
            String line2 = "Press Y to quit, N to continue";

            // Đo kích thước thật của từng dòng
            Text temp1 = new Text(line1);
            temp1.setFont(gc.getFont());
            double w1 = temp1.getLayoutBounds().getWidth();
            double h1 = temp1.getLayoutBounds().getHeight();

            Text temp2 = new Text(line2);
            temp2.setFont(gc.getFont());
            double w2 = temp2.getLayoutBounds().getWidth();
            double h2 = temp2.getLayoutBounds().getHeight();

            // Tính toạ độ căn giữa
            double centerX1 = (width - w1) / 2;
            double centerX2 = (width - w2) / 2;
            double centerY = height / 2;

            // Vẽ text
            gc.fillText(line1, centerX1, centerY);
            gc.fillText(line2, centerX2, centerY + h1 + 15); // cách nhau 15px
        }

        for (HUDMessage hm : hudMessages) hm.render(gc, width);

        if (barrierActive) {
            gc.setGlobalAlpha(0.9);
            gc.setFill(javafx.scene.paint.Color.rgb(30, 180, 255, 0.25));
            gc.fillRect(0, barrierY - barrierThickness/2.0, width, barrierThickness);
            gc.setStroke(javafx.scene.paint.Color.rgb(100, 220, 255));
            gc.setLineWidth(2);
            gc.strokeRect(0, barrierY - barrierThickness/2.0, width, barrierThickness);
            gc.setGlobalAlpha(1.0);
        }

    }

}
