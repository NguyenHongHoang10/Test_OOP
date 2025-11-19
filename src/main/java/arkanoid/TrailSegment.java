package arkanoid;

/**
 * vệt đuôi cho ball
 */
public class TrailSegment {
    double x, y;
    double radius;
    double life;    // remaining
    double maxLife;

    TrailSegment(double x, double y, double radius, double maxLife) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.maxLife = maxLife;
        this.life = maxLife;
    }

    void update(double dt) {
        life -= dt;
        if (life < 0) life = 0;
    }

    public boolean isAlive() {
        return life > 0;
    }

    public double alpha() {
        return Math.max(0, life / maxLife);
    }
}
