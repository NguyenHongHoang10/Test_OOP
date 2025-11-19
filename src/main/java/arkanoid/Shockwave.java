package arkanoid;


import javafx.scene.canvas.GraphicsContext;


public class Shockwave {
    public double x, y;
    public double radius = 0.0;
    public final double maxRadius;
    public final double speed;
    public final double thickness;
    public boolean finished = false;


    public Shockwave(double ox, double oy, double speedPxPerS, double maxR, double thickness) {
        this.x = ox;
        this.y = oy;
        this.radius = 0.0;
        this.speed = speedPxPerS;
        this.maxRadius = maxR;
        this.thickness = thickness;
    }


    public void update(double dt) {
        radius += speed * dt;
        if (radius >= maxRadius) {
            finished = true;
        }
    }


    // kiểm tra xem tâm gạch có nằm trong bán kính hiện tại không
    public boolean touchesBrick(Brick b) {
        double bx = b.getX() + b.getWidth() * 0.5;
        double by = b.getY() + b.getHeight() * 0.5;
        double dx = bx - x, dy = by - y;
        double dist = Math.hypot(dx, dy);
        return dist <= radius && dist >= (radius - thickness - 1.0);
    }


    public void render(GraphicsContext gc) {
        gc.save();
        gc.setGlobalBlendMode(javafx.scene.effect.BlendMode.ADD);
        double alphaFill = Math.max(0, 0.28 * (1.0 - radius / maxRadius));
        gc.setGlobalAlpha(alphaFill);
        javafx.scene.paint.RadialGradient rg = new javafx.scene.paint.RadialGradient(
                0, 0, x, y, radius + thickness * 0.5, false, javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0.0, javafx.scene.paint.Color.rgb(140, 120, 180, alphaFill)),
                new javafx.scene.paint.Stop(1.0, javafx.scene.paint.Color.TRANSPARENT));
        gc.setFill(rg);
        gc.fillOval(x - (radius + thickness), y - (radius + thickness), (radius + thickness) * 2, (radius + thickness) * 2);


        gc.setGlobalAlpha(Math.max(0.0, 0.85 * (1.0 - radius / maxRadius)));
        gc.setStroke(javafx.scene.paint.Color.rgb(200, 180, 220, 0.9));
        gc.setLineWidth(Math.max(2.0, thickness));
        gc.strokeOval(x - radius, y - radius, radius * 2, radius * 2);


        gc.setGlobalBlendMode(javafx.scene.effect.BlendMode.SRC_OVER);
        gc.setGlobalAlpha(1.0);
        gc.restore();
    }
}





