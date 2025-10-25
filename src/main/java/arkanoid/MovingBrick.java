package arkanoid;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class MovingBrick extends Brick {
    private double speed = 0.3; // tốc độ di chuyển
    private double direction = 1; // 1 = phải, -1 = trái
    private final double leftBound;
    private final double rightBound;

    public MovingBrick(double x, double y, double width, double height, int hits,
                       double leftBound, double rightBound) {
        super(x, y, width, height, hits);

        double range = width * 3;
        // Giới hạn phạm vi di chuyển trong màn hình
        this.leftBound = Math.max(leftBound, x - range / 2);
        this.rightBound = Math.min(rightBound - width, x + range / 2);
        // Xen kẽ hướng di chuyển giữa các dòng

    }
    public void setX(double x) { this.x = x; }
    public double getX() { return x; }
    public double getWidth() { return width; }
    public javafx.geometry.Rectangle2D getBounds() {
        return new javafx.geometry.Rectangle2D(x, y, width, height);
    }


    public void update(double dt) {
        x += direction * speed;
        if (x <= leftBound) {
            x = leftBound;
            direction = 1;
        } else if (x + width >= rightBound) {
            x = rightBound - width;
            direction = -1;
        }
    }

    @Override
    public void render(GraphicsContext gc) {
        gc.setFill(Color.ORANGE);
        gc.fillRect(x, y, width, height);
        gc.setStroke(Color.BLACK);
        gc.strokeRect(x, y, width, height);
    }

    public void setDirection(double dir) {
        this.direction = dir;
    }

}
