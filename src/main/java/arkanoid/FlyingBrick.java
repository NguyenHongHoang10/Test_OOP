package arkanoid;

public class FlyingBrick {
    public final Brick brick;
    public double x, y;
    public double vx, vy;
    public double angle;
    public double angularV;
    public double scale = 1.0;
    public boolean arrived = false;

    public FlyingBrick(Brick b, double cx, double cy, double vx, double vy, double angV) {
        this.brick = b;
        this.x = cx;
        this.y = cy;
        this.vx = vx;
        this.vy = vy;
        this.angle = 0.0;
        this.angularV = angV;
        this.scale = 1.0;
    }
}
