package arkanoid;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Boss {
    private double x, y, width, height;
    private double speed = 1;
    private double direction = 1;
    private double leftBound, rightBound;
    private double shootCooldown = 2; // thời gian giữa 2 lần bắn (giây)
    private double timeSinceLastShot = 0;

    private double health;
    private double maxHealth;

    public List<BossBullet> bullets = new ArrayList<>();


    public Boss(double x, double y, double width, double height, double leftBound, double rightBound) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.leftBound = leftBound;
        this.rightBound = rightBound;

        this.maxHealth = 500;   // máu tối đa
        this.health = maxHealth; // máu hiện tại
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

        // Bắn đạn
        timeSinceLastShot += dt;
        if (timeSinceLastShot >= shootCooldown) {
            shoot();
            timeSinceLastShot = 0;
        }

        // Cập nhật đạn
        Iterator<BossBullet> it = bullets.iterator();
        while (it.hasNext()) {
            BossBullet b = it.next();
            b.update(dt);
            if (b.getY() > 600) { // bay ra ngoài màn
                it.remove();
            }
        }
    }

    private void shoot() {
        double bulletY = y + height;
        bullets.add(new BossBullet(x + width / 2 - 20, bulletY, 8, 16, 300));
        bullets.add(new BossBullet(x + width / 2 + 12, bulletY, 8, 16, 300));
    }

    public void render(GraphicsContext gc) {
        // Vẽ thân boss
        gc.setFill(Color.DARKRED);
        gc.fillRect(x, y, width, height);

        // Vẽ viền
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        gc.strokeRect(x, y, width, height);

        // Vẽ thanh máu
        double barWidth = width * (health / maxHealth);
        gc.setFill(Color.LIMEGREEN);
        gc.fillRect(x, y - 10, barWidth, 6);
        gc.setStroke(Color.BLACK);
        gc.strokeRect(x, y - 10, width, 6);

        // Vẽ đạn
        for (BossBullet bullet : bullets) {
            bullet.render(gc);
        }
    }

    // Getter cho Game truy cập
    public List<BossBullet> getBullets() { return bullets; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }

    // Trừ máu Boss khi bị đánh
    public void takeDamage(double damage) {
        health -= damage;
        if (health < 0) health = 0;
    }

    public boolean isDead() {
        return health <= 0;
    }

    public CollisionSide checkCollision(Ball ball) {
        double bx = ball.getX();
        double by = ball.getY();
        double br = ball.getRadius();

        // Nếu không chạm
        if (bx + br < x || bx - br > x + width || by + br < y || by - br > y + height)
            return CollisionSide.NONE;

        // Tính mức chồng lấn theo 4 hướng
        double overlapLeft = (bx + br) - x;
        double overlapRight = (x + width) - (bx - br);
        double overlapTop = (by + br) - y;
        double overlapBottom = (y + height) - (by - br);

        // Tìm hướng chồng lấn nhỏ nhất
        double minOverlap = Math.min(Math.min(overlapLeft, overlapRight), Math.min(overlapTop, overlapBottom));

        if (minOverlap == overlapTop) return CollisionSide.TOP;
        if (minOverlap == overlapBottom) return CollisionSide.BOTTOM;
        if (minOverlap == overlapLeft) return CollisionSide.LEFT;
        return CollisionSide.RIGHT;
    }

    // Enum giúp xác định hướng va chạm
    public enum CollisionSide { NONE, TOP, BOTTOM, LEFT, RIGHT }
}
