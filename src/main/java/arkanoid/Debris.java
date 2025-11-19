package arkanoid;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Debris {
    public double x, y;      // góc trên-trái (so với canvas)
    public double w, h;
    public double vx, vy;
    public double angle;     // radians
    public double angularV;  // radians/giây
    public double life, maxLife;
    public Color color;

    public Debris() {
    }

    public void init(double x, double y, double w, double h, double vx, double vy, double angle, double angularV, double life, Color color) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.vx = vx;
        this.vy = vy;
        this.angle = angle;
        this.angularV = angularV;
        this.life = this.maxLife = life;
        this.color = color;
    }

    public void update(double dt) {
        life -= dt;
        if (life < 0) life = 0;
        // vận tốc chịu trọng lực nhỏ
        vy += 500 * dt; // trọng lực px/s^2
        // vận tốc cản không lớn
        vx *= 0.995;
        vy *= 0.998;

        x += vx * dt;
        y += vy * dt;
        angle += angularV * dt;
    }

    public boolean isAlive() {
        return life > 0;
    }

    public void render(GraphicsContext gc) {
        double cx = x + w / 2.0;
        double cy = y + h / 2.0;
        gc.save();
        gc.translate(cx, cy);
        gc.rotate(Math.toDegrees(angle));
        double alpha = Math.max(0, life / maxLife);
        gc.setGlobalAlpha(alpha);
        gc.setFill(color);
        gc.fillRect(-w / 2.0, -h / 2.0, w, h);
        gc.setGlobalAlpha(1.0);
        gc.restore();
    }
}
