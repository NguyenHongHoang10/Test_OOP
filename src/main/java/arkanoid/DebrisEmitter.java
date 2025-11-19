package arkanoid;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DebrisEmitter {
    private final List<Debris> active = new ArrayList<>();
    private final List<Debris> pool = new ArrayList<>();

    private Debris obtain() {
        if (!pool.isEmpty()) return pool.remove(pool.size() - 1);
        return new Debris();
    }

    public void emitDebris(double cx, double cy, double brickW, double brickH, int count, Color color) {
        // sinh ra số mảnh vụn trong khu vực ảnh hưởng của viên gạch
        for (int i = 0; i < count; i++) {
            Debris d = obtain();
            double sizeW = (brickW / 6.0) * (0.6 + Math.random() * 0.9);
            double sizeH = (brickH / 6.0) * (0.6 + Math.random() * 0.9);
            // vị trí ngẫu nhiên bên trong giới hạn gạch
            double px = cx - brickW / 2.0 + Math.random() * brickW;
            double py = cy - brickH / 2.0 + Math.random() * brickH;
            // độ lệch vận tốc tỏa ra khỏi center
            double dirX = px - cx;
            double dirY = py - cy;
            double len = Math.sqrt(dirX * dirX + dirY * dirY) + 0.0001;
            dirX /= len;
            dirY /= len;
            double spread = 120 + Math.random() * 220;
            double vx = dirX * (spread * (0.6 + Math.random() * 0.9)) + (Math.random() - 0.5) * 80;
            double vy = dirY * (spread * (0.2 + Math.random() * 0.6)) - Math.random() * 160; // bias upward
            double angle = Math.random() * Math.PI * 2;
            double angularV = (Math.random() - 0.5) * 10.0;
            double life = 0.8 + Math.random() * 0.9;
            d.init(px - sizeW / 2.0, py - sizeH / 2.0, sizeW, sizeH, vx, vy, angle, angularV, life, color);
            active.add(d);
        }
    }

    public void update(double dt) {
        Iterator<Debris> it = active.iterator();
        while (it.hasNext()) {
            Debris d = it.next();
            d.update(dt);
            if (!d.isAlive()) {
                it.remove();
                pool.add(d);
            }
        }
    }

    public void render(GraphicsContext gc) {
        if (active.isEmpty()) return;
        for (Debris d : active) d.render(gc);
    }

    public int activeCount() {
        return active.size();
    }
}
