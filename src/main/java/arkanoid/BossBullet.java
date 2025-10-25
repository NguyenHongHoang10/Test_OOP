package arkanoid;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class BossBullet {
    private double x, y, width, height;
    private double speed; // pixel/s

    public BossBullet(double x, double y, double width, double height, double speed) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.speed = speed;
    }

    public void update(double dt) {
        y += speed * dt;
    }

    // Hàm vẽ duy nhất
    public void render(GraphicsContext gc) {
        gc.setFill(Color.ORANGERED);
        gc.fillOval(x, y, width, height);
    }

    // Kiểm tra va chạm với paddle
    public boolean collidesWith(Paddle paddle) {
        if (paddle == null) return false; // tránh NullPointerException
        return paddle.getX() + paddle.getWidth() > x &&
                paddle.getX() < x + width &&
                paddle.getY() + paddle.getHeight() > y &&
                paddle.getY() < y + height;
    }

    public double getY() { return y; }

    public double getX() {
        return x;
    }

    public double getHeight() {
        return height;
    }

    public double getWidth() {
        return width;
    }
}
