package arkanoid;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class MovingBrick extends Brick {
    private double speed = 0.3; // tốc độ di chuyển
    private double direction = 1; // 1 = phải, -1 = trái
    private final double leftBound;
    private final double rightBound;
    private final double x0;
    private final BrickType type;

    public enum BrickType {
        WEAK(1, Color.LIGHTGREEN),
        MEDIUM(2, Color.GOLD),
        STRONG(3, Color.ORANGERED),
        ULTRA(5, Color.GRAY);

        public final int hits;
        public final Color color;

        BrickType(int hits, Color color) {
            this.hits = hits;
            this.color = color;
        }
    }


    public MovingBrick(double x, double y, double width, double height, int hits,
                       BrickType type) {
        super(x, y, width, height, hits);
        this.x0 = x;
        this.type = type;
        // giới hạn phạm vi di chuyển
        this.leftBound = 30;
        this.rightBound = 860 - width;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getX() {
        return x;
    }

    public double getWidth() {
        return width;
    }

    public double getX0() {
        return x0;
    }

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
        Color fill = colorByHits(getHits());
        gc.setFill(fill);
        gc.fillRoundRect(x, y, width, height, 6, 6);
        gc.setStroke(Color.DARKGRAY);
        gc.strokeRoundRect(x, y, width, height, 6, 6);
    }
    public void setDirection(double dir) {
        this.direction = dir;
    }

}
