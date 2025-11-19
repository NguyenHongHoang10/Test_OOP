package arkanoid;

import javafx.scene.paint.Color;

public class Particle {
    public double x, y;
    public double vx, vy;
    public double life, maxLife;
    public double size;
    public Color color;

    public Particle(double x, double y, double vx, double vy, double life,
                    double size, Color color) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.life = this.maxLife = life;
        this.size = size;
        this.color = color;
    }

    public Particle() {
    }

    public void init(double x, double y, double vx, double vy, double life,
                     double size, Color color) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.life = this.maxLife = life;
        this.size = size;
        this.color = color;
    }

    public void update(double dt) {
        life -= dt;
        x += vx * dt;
        y += vy * dt;
        vy += 200 * dt;
    }

    public boolean isAlive() {
        return life > 0;
    }

    public double alpha() {
        return Math.max(0, life / maxLife);
    }
}
