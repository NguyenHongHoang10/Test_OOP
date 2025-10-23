package arkanoid;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;

import java.util.HashSet;
import java.util.Set;

public class Paddle extends GameObject {
    // Tập các phím đang nhấn (LEFT/RIGHT hoặc A/D)
    private final Set<KeyCode> keys = new HashSet<>();
    // Tốc độ di chuyển theo px/giây
    private final double speed = 450;
    // Chiều rộng của khu vực chơi dùng để giới hạn paddle không đi quá biên
    private final double arenaWidth;
    private boolean hasLaser = false;

    public Paddle(double x, double y, double width, double height, double arenaWidth) {
        super(x, y, width, height);
        this.arenaWidth = arenaWidth;
    }

    // Gọi khi phím được nhấn
    public void press(KeyCode code) { keys.add(code); }
    // Gọi khi phím được nhả
    public void release(KeyCode code) { keys.remove(code); }

    @Override
    public void update(double deltaTime) {
        // Di chuyển trái/phải theo tập keys
        if (keys.contains(KeyCode.LEFT) || keys.contains(KeyCode.A)) {
            x -= speed * deltaTime;
        }
        if (keys.contains(KeyCode.RIGHT) || keys.contains(KeyCode.D)) {
            x += speed * deltaTime;
        }
        // giới hạn vùng của paddle
        if (x < 0) x = 0;
        if (x + width > arenaWidth) x = arenaWidth - width;
    }

    public void setHasLaser(boolean on) { this.hasLaser = on; }
    public boolean hasLaser() { return this.hasLaser; }

    // thêm helper trả về 2 vị trí nòng súng (center x,y)
    public double[] getLaserGunPositions() {
        // trả về array length 4: [x1,y1,x2,y2]
        double gx1 = getX() + 12; // offset trái (tùy chỉnh theo width)
        double gx2 = getX() + getWidth() - 12;
        double gy = getY() - 6; // ngay trên paddle
        return new double[]{gx1, gy, gx2, gy};
    }

    @Override
    public void render(GraphicsContext gc) {
        // Vẽ paddle dưới dạng hình chữ nhật bo góc
        gc.setFill(Color.DODGERBLUE);
        gc.fillRoundRect(x, y, width, height, 10, 10);
    }


}
