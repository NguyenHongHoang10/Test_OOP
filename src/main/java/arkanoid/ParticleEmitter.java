package arkanoid;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlendMode;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class ParticleEmitter {
    private final List<Particle> active = new ArrayList<>();
    private final List<Particle> pool = new ArrayList<>();

    private Particle obtain(double x, double y, double vx, double vy,
                            double life, double size, Color color) {
        if (!pool.isEmpty()) {
            Particle p = pool.remove(pool.size() - 1);
            p.init(x, y, vx, vy, life, size, color);
            return p;
        }
        return new Particle(x, y, vx, vy, life, size, color);
    }

    public void emitExplosion(double cx, double cy, int count) {
        for (int i = 0; i < count; i++) {
            double angle = Math.random() * Math.PI * 2;
            double speed = 80 + Math.random() * 380;
            double vx = Math.cos(angle) * speed;
            double vy = Math.sin(angle) * speed * 0.6 - Math.random() * 120;
            double life = 0.35 + Math.random() * 0.45;
            double size = 2 + Math.random() * 6;
            int r = 200 + (int) (Math.random() * 55);
            int g = 80 + (int) (Math.random() * 80);
            int b = 20 + (int) (Math.random() * 40);
            Color col = Color.rgb(r, g, b);
            active.add(obtain(cx, cy, vx, vy, life, size, col));
        }
    }

    public void emitSmoke(double cx, double cy, int count) {
        for (int i = 0; i < count; i++) {
            double angle = -Math.PI / 2 + (Math.random() - 0.5) * Math.PI * 0.4;
            double speed = 10 + Math.random() * 40;
            double vx = Math.cos(angle) * speed;
            double vy = Math.sin(angle) * speed - Math.random() * 40;
            double life = 0.6 + Math.random() * 0.8;
            double size = 8 + Math.random() * 10;
            Color col = Color.rgb(120, 120, 120, 0.6);
            active.add(obtain(cx, cy, vx, vy, life, size, col));
        }
    }

    public void update(double dt) {
        Iterator<Particle> it = active.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.update(dt);
            if (!p.isAlive()) {
                it.remove();
                pool.add(p);
            }
        }
    }

    public void render(GraphicsContext gc) {
        if (active.isEmpty()) return;

        // Vẽ tia lửa
        gc.setGlobalBlendMode(BlendMode.ADD);
        for (Particle p : active) {
            double a = Math.min(1.0, p.alpha() * 1.2);
            gc.setGlobalAlpha(a);
            gc.setFill(p.color);
            double s = p.size;
            gc.fillOval(p.x - s / 2.0, p.y - s / 2.0, s, s);
        }
        gc.setGlobalAlpha(1.0);
        gc.setGlobalBlendMode(BlendMode.SRC_OVER);

        for (Particle p : active) {
            if (p.size > 6) {
                double a = p.alpha() * 0.6;
                gc.setGlobalAlpha(a);
                gc.setFill(Color.rgb(120, 120, 120, 0.35));
                double s = p.size * 1.4;
                gc.fillOval(p.x - s / 2.0, p.y - s / 2.0, s, s);
            }
        }
        gc.setGlobalAlpha(1.0);
    }

    public int activeCount() {
        return active.size();
    }
}
