package arkanoid;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class PowerUp {
    public enum PowerType { SHRINK_PADDLE, EXPAND_PADDLE, TINY_BALL, MULTI_BALL, SLOW_BALL, FAST_BALL, NEXT_LEVEL,
        EXTRA_LIFE, SUDDEN_DEATH, LASER_PADDLE, FIREBALL, BARRIER, WEAKEN, SCORE_MULTIPLIER }

    double x, y, w = 24, h = 24;
    double vy = 80; // rơi xuống
    PowerType type;

    public PowerUp(double x, double y, PowerType type) {
        this.x = x; this.y = y; this.type = type;
    }

    public void update(double dt) {
        y += vy * dt;
    }

    public void render(GraphicsContext gc) {
        gc.setFill(colorForType(type));
        gc.fillOval(x - w/2, y - h/2, w, h);
        gc.setStroke(Color.WHITE);
        gc.strokeOval(x - w/2, y - h/2, w, h);
    }

    public boolean collidesWithPaddle(Paddle paddle) {
        double px = paddle.getX();
        double py = paddle.getY();
        double pw = paddle.getWidth();
        double ph = paddle.getHeight();
        return x + w/2 > px && x - w/2 < px + pw && y + h/2 > py && y - h/2 < py + ph;
    }

    public Color colorForType(PowerType t) {
        switch (t) {
            case SHRINK_PADDLE: return Color.BROWN;
            case EXPAND_PADDLE: return Color.LIGHTGREEN;
            case TINY_BALL: return Color.SKYBLUE;
            case MULTI_BALL: return Color.GOLD;
            case SLOW_BALL: return Color.CORNFLOWERBLUE;
            case FAST_BALL: return Color.ORANGERED;
            case NEXT_LEVEL: return Color.DARKVIOLET;
            case EXTRA_LIFE: return Color.PINK;
            case SUDDEN_DEATH: return Color.BLACK;
            case LASER_PADDLE: return Color.SALMON;
            case FIREBALL: return Color.RED;
            case BARRIER: return Color.DEEPSKYBLUE;
            case WEAKEN: return Color.DARKORANGE;
            case SCORE_MULTIPLIER: return Color.BEIGE;
            default: return Color.WHITE;
        }
    }
}