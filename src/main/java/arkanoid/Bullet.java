package arkanoid;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Bullet {
    public double x, y;
    public double w = 6, h = 12;
    private final double vy = -480; // tốc độ đạn
    private boolean alive = true;

    public Bullet(double x, double y) {
        this.x = x - w / 2.0;
        this.y = y - h / 2.0;
    }

    public void update(double dt) {
        y += vy * dt;
        if (y + h < -50) alive = false;
    }

    public void render(GraphicsContext gc) {
        gc.setFill(Color.CYAN);
        gc.fillRect(x, y, w, h);
        gc.setStroke(Color.WHITE);
        gc.strokeRect(x, y, w, h);
    }

    public boolean isAlive() {
        return alive;
    }

    public void kill() {
        alive = false;
    }

    // kiểm tra va chạm AABB với brick
    public boolean collidesWithBrick(Brick b) {
        return x < b.getX() + b.getWidth() && x + w > b.getX() &&
                y < b.getY() + b.getHeight() && y + h > b.getY();
    }
}
