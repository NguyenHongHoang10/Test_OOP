
package arkanoid;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class BossBullet {
    private double x, y, width, height;
    private double speed; // pixel/s
    private double angle; // góc bắn (radian)
    private double dx, dy; // vector vận tốc

    public BossBullet(double x, double y, double width, double height, double speed, double angle) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.speed = speed;
        this.angle = angle;

        // Tính hướng bay dựa vào góc
        this.dx = Math.cos(angle) * speed;
        this.dy = Math.sin(angle) * speed;
    }

    public void update(double dt) {
        x += dx * dt;
        y += dy * dt;
    }

    public void render(GraphicsContext gc) {
        gc.setFill(Color.ORANGERED);
        gc.fillOval(x, y, width, height);
    }

    // Kiểm tra va chạm với paddle
    public boolean collidesWith(Paddle paddle) {
        if (paddle == null) return false;
        return paddle.getX() + paddle.getWidth() > x &&
                paddle.getX() < x + width &&
                paddle.getY() + paddle.getHeight() > y &&
                paddle.getY() < y + height;
    }

    public double getY() {
        return y;
    }

    public double getX() {
        return x;
    }

    public double getHeight() {
        return height;
    }

    public double getWidth() {
        return width;
    }

    public double getSpeed() {
        return speed;
    }

    public double getAngle() {
        return angle;
    }
}
