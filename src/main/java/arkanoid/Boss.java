package arkanoid;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Boss {
    private double x, y, width, height;
    private double speed = 1;
    private double direction = 1;
    private double leftBound, rightBound;
    private double shootCooldown = 2;
    private double timeSinceLastShot = 0;

    private double multiShotCooldown = 6;
    private double timeSinceLastMultiShot = 0;

    private double bulletSpeed = 150;
    private double flashTimer = 0;

    private double health;
    private double maxHealth;

    private int hitCount = 0;
    private final List<PowerUp> droppedPowerUps = new ArrayList<>();

    public List<BossBullet> bullets = new ArrayList<>();

    // 1. Danh sách lưu 6 ảnh
    private static final List<Image> bossFrames = new ArrayList<>();

    // 2. Biến điều khiển animation (tĩnh)
    private static double animTimer = 0.0;
    private static int currentFrameIndex = 0;
    private static final int NUM_FRAMES = 6; // Tổng số ảnh (1-6)
    // Tốc độ animation: 0.1s mỗi ảnh (10 FPS)
    private static final double FRAME_DURATION = 0.1;


    static {
        for (int i = 1; i <= NUM_FRAMES; i++) {
            String path = "/Image/Boss/" + i + ".png";
            try {
                Image img = new Image(Boss.class.getResourceAsStream(path));
                if (img.isError()) {
                    System.err.println("Lỗi tải ảnh Boss: " + path);
                    bossFrames.add(null);
                } else {
                    bossFrames.add(img);
                }
            } catch (Exception e) {
                System.err.println("Không tìm thấy tài nguyên Boss: " + path);
                bossFrames.add(null);
            }
        }
    }

    /**
     * Hàm này được gọi bởi Game.java mỗi frame để chạy animation
     */
    public static void updateAnimation(double dt) {
        // Đảm bảo có ảnh để animate
        if (bossFrames.isEmpty() || bossFrames.get(0) == null) return;

        animTimer += dt;
        if (animTimer >= FRAME_DURATION) {
            animTimer -= FRAME_DURATION;
            // Chuyển sang khung hình tiếp theo (ví dụ: 0, 1, 2, 3, 4, 5, 0...)
            currentFrameIndex = (currentFrameIndex + 1) % NUM_FRAMES;
        }
    }

    public Boss(double x, double y, double width, double height, double leftBound, double rightBound) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.leftBound = leftBound;
        this.rightBound = rightBound;

        this.maxHealth = 500;
        this.health = maxHealth;
    }

    public void update(double dt) {
        // Di chuyển trái - phải
        x += direction * speed;
        if (x <= leftBound) {
            x = leftBound;
            direction = 1;
        } else if (x + width >= rightBound) {
            x = rightBound - width;
            direction = -1;
        }

        // Bắn 1 viên ngẫu nhiên
        timeSinceLastShot += dt;
        if (timeSinceLastShot >= shootCooldown) {
            shootRandom();
            timeSinceLastShot = 0;
        }

        // Sau 6 giây bắn 2 viên cùng lúc
        timeSinceLastMultiShot += dt;
        if (timeSinceLastMultiShot >= multiShotCooldown) {
            shootDouble();
            timeSinceLastMultiShot = 0;
        }

        //Cập nhật đạn
        Iterator<BossBullet> it = bullets.iterator();
        while (it.hasNext()) {
            BossBullet b = it.next();
            b.update(dt);
            if (b.getY() > 600) { // bay ra ngoài màn
                it.remove();
            }
        }

        //Cập nhật power-up rơi
        Iterator<PowerUp> pit = droppedPowerUps.iterator();
        while (pit.hasNext()) {
            PowerUp p = pit.next();
            p.update(dt);
            if (p.y > 600) pit.remove();
        }
    }

    // Bắn 1 viên ngẫu nhiên
    private void shootRandom() {
        double bulletY = y + height;
        double randomAngle = Math.toRadians(60 + Math.random() * 60); // 60°–120°
        bullets.add(new BossBullet(x + width / 2 - 4, bulletY, 8, 16, bulletSpeed, randomAngle));
        SoundManager.get().play(SoundManager.Sfx.BOSS_SHOOT);
    }

    // Bắn 2 viên cố định
    private void shootDouble() {
        double bulletY = y + height;
        double[] angles = {
                Math.toRadians(80),
                Math.toRadians(100)
        };
        for (double a : angles) {
            bullets.add(new BossBullet(x + width / 2 - 4, bulletY, 8, 16, bulletSpeed, a));
        }
        SoundManager.get().play(SoundManager.Sfx.BOSS_SHOOT);
    }

    public void render(GraphicsContext gc) {
        Image currentFrame = null;
        if (!bossFrames.isEmpty() && currentFrameIndex < bossFrames.size()) {
            currentFrame = bossFrames.get(currentFrameIndex);
        }

        if (currentFrame != null) {
            // Vẽ ảnh animation
            gc.drawImage(currentFrame, x, y, width, height);
        } else {
            // Dự phòng (fallback): Vẽ hình chữ nhật
            gc.setFill(Color.DARKRED);
            gc.fillRect(x, y, width, height);
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(2);
            gc.strokeRect(x, y, width, height);
        }

        // Thanh máu
        double barWidth = width * (health / maxHealth);
        gc.setFill(Color.LIMEGREEN);
        gc.fillRect(x, y - 10, barWidth, 6);
        gc.setStroke(Color.BLACK);
        gc.strokeRect(x, y - 10, width, 6);

        // Vẽ đạn
        for (BossBullet bullet : bullets) {
            bullet.render(gc);
        }
        // Vẽ power-up boss thả ra
        for (PowerUp p : droppedPowerUps) {
            p.render(gc);
        }
    }

    // Getter
    public List<BossBullet> getBullets() {
        return bullets;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    // Trừ máu boss khi trúng đạn
    public void takeDamage(double damage, PowerUpManager powerUpManager, EntityManager entities) {
        health -= damage;
        if (health < 0) health = 0;
        flashTimer = 0.2;
        SoundManager.get().play(SoundManager.Sfx.BOSS_HIT);
        hitCount++; // tăng đếm khi trúng bóng
        if (hitCount >= 2 + (int) (Math.random() * 2)) {
            dropPowerUp(powerUpManager, entities); // rơi PowerUp
            hitCount = 0;  // reset bộ đếm
        }

        if (health <= 0) {
            // SFX: Boss chết
            SoundManager.get().play(SoundManager.Sfx.BOSS_DEATH);
        }
    }

    public boolean isAlive() {
        return health > 0;
    }

    public List<PowerUp> getDroppedPowerUps() {
        return droppedPowerUps;
    }

    private void dropPowerUp(PowerUpManager powerUpManager, EntityManager entities) {

        powerUpManager.trySpawnPowerUp(x + width / 2, y + height, entities);

    }

    public boolean isDead() {
        return health <= 0;
    }

    public CollisionSide checkCollision(Ball ball) {
        double bx = ball.getX();
        double by = ball.getY();
        double br = ball.getRadius();
        double bvx = ball.getVx();
        double bvy = ball.getVy();

        // Kiểm tra không chạm
        if (bx + br < x || bx - br > x + width || by + br < y || by - br > y + height)
            return CollisionSide.NONE;

        // Tính khoảng chồng lấn (overlap)
        double overlapLeft = (bx + br) - x;
        double overlapRight = (x + width) - (bx - br);
        double overlapTop = (by + br) - y;
        double overlapBottom = (y + height) - (by - br);

        // Xác định hướng có chồng lấn nhỏ nhất
        double minOverlap = Math.min(Math.min(overlapLeft, overlapRight),
                Math.min(overlapTop, overlapBottom));

        // Trường hợp va chạm góc
        // Nếu chồng lấn X và Y gần bằng nhau, xử lý tùy theo hướng vận tốc
        double diffX = Math.abs(overlapLeft - overlapRight);
        double diffY = Math.abs(overlapTop - overlapBottom);
        if (Math.abs(minOverlap - overlapTop) < 2 && Math.abs(minOverlap - overlapLeft) < 2) {
            if (Math.abs(bvx) > Math.abs(bvy)) return bvx > 0 ? CollisionSide.LEFT : CollisionSide.RIGHT;
            else return bvy > 0 ? CollisionSide.TOP : CollisionSide.BOTTOM;
        }

        // Xác định hướng phản xạ chính
        if (minOverlap == overlapTop && bvy > 0) return CollisionSide.TOP;
        if (minOverlap == overlapBottom && bvy < 0) return CollisionSide.BOTTOM;
        if (minOverlap == overlapLeft && bvx > 0) return CollisionSide.LEFT;
        if (minOverlap == overlapRight && bvx < 0) return CollisionSide.RIGHT;

        // Nếu không xác định rõ, chọn hướng theo vận tốc
        if (Math.abs(bvx) > Math.abs(bvy))
            return bvx > 0 ? CollisionSide.LEFT : CollisionSide.RIGHT;
        else
            return bvy > 0 ? CollisionSide.TOP : CollisionSide.BOTTOM;
    }

    public enum CollisionSide {NONE, TOP, BOTTOM, LEFT, RIGHT}
}
