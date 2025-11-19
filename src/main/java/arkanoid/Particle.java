package arkanoid;

import javafx.scene.paint.Color;

/**
 * Particle: một hạt đơn lẻ dùng cho hiệu ứng (sparks, smoke, v.v.)
 * Có thể dùng kết hợp với ParticleEmitter để tiết kiệm tạo mới (object pool).
 */
public class Particle {
    public double x, y;
    public double vx, vy;
    public double life, maxLife;
    public double size;
    public Color color;

    public Particle(double x, double y, double vx, double vy, double life, double size, Color color) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.life = this.maxLife = life;
        this.size = size;
        this.color = color;
    }

    public Particle() {
        // empty constructor for object pool reuse
    }

    public void init(double x, double y, double vx, double vy, double life, double size, Color color) {
        this.x = x; this.y = y; this.vx = vx; this.vy = vy;
        this.life = this.maxLife = life; this.size = size; this.color = color;
    }

    public void update(double dt) {
        life -= dt;
        x += vx * dt;
        y += vy * dt;
        // small gravity for sparks/smoke - tweak as needed
        vy += 200 * dt;
    }

    public boolean isAlive() { return life > 0; }

    public double alpha() { return Math.max(0, life / maxLife); }
}
