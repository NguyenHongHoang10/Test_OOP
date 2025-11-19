package arkanoid;

public class Explosion {
    double x, y; // trung tâm vụ nổ
    double maxRadius;
    double radius;
    double life;     // remaining life (s)
    double maxLife;  // initial life (s)

    Explosion(double x, double y, double maxRadius, double maxLife) {
        this.x = x;
        this.y = y;
        this.maxRadius = maxRadius;
        this.maxLife = maxLife;
        this.radius = 0;
        this.life = maxLife;
    }

    void update(double dt) {
        life -= dt;
        if (life < 0) life = 0;
        // giảm bán kính từ 0 đến maxRadius
        double t = 1.0 - (life / maxLife);
        radius = maxRadius * t;
    }

    boolean isAlive() {
        return life > 0;
    }

    void render(javafx.scene.canvas.GraphicsContext gc) {
        double alpha = Math.max(0, life / maxLife);
        gc.setGlobalAlpha(alpha * 0.9);
        gc.setFill(javafx.scene.paint.Color.rgb(255, 180, 60));
        gc.fillOval(x - radius, y - radius, radius * 2, radius * 2);
        gc.setGlobalAlpha(alpha);
        gc.setStroke(javafx.scene.paint.Color.rgb(255, 240, 200));
        gc.strokeOval(x - radius, y - radius, radius * 2, radius * 2);
        gc.setGlobalAlpha(1.0);
    }
}
